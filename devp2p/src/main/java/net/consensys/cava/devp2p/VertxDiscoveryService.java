/*
 * Copyright 2018 ConsenSys AG.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on
 * an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations under the License.
 */
package net.consensys.cava.devp2p;

import static com.google.common.base.Preconditions.checkArgument;
import static java.lang.String.format;

import net.consensys.cava.bytes.Bytes;
import net.consensys.cava.concurrent.AsyncCompletion;
import net.consensys.cava.concurrent.CompletableAsyncCompletion;
import net.consensys.cava.crypto.SECP256K1.KeyPair;

import java.net.BindException;
import java.net.InetSocketAddress;
import java.net.SocketException;
import java.util.List;
import java.util.OptionalInt;
import java.util.function.LongSupplier;

import com.google.common.annotations.VisibleForTesting;
import io.vertx.core.Vertx;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.datagram.DatagramPacket;
import io.vertx.core.datagram.DatagramSocket;
import io.vertx.core.datagram.DatagramSocketOptions;
import io.vertx.core.net.SocketAddress;

/**
 * Vert.x 3.x implementation of the DiscoveryService.
 * <p>
 * Note that the <code>cava-devp2p</code> library depends optionally on Vert.x. Please add
 * <code>'io.vertx:vertx-core:3.5.0'</code> to your dependencies.
 */
public final class VertxDiscoveryService {
  private static final int MAX_PACKET_SIZE_BYTES = 1280;

  private final Vertx vertx;
  private final PeerRoutingTable peerRoutingTable;
  private final PeerRepository peerRepository;
  private final List<String> bootstrapPeers;
  private final InetSocketAddress listenAddress;
  private final KeyPair keyPair;
  private final LongSupplier timeSupplier;
  private final int wireProtocolPort;
  private PeerLifecycleManager peerLifecycleManager;
  private DatagramSocket socket;

  /**
   * Creates a new VertxDiscoveryService.
   *
   * @param vertx a Vertx instance
   * @param peerRepository a peer repository
   * @param bootstrapPeers a set of peer URIs to connect on startup
   * @param listenAddress the address the service should listen to
   * @param peerRoutingTable the peer routing table
   * @param keyPair the encryption key pair to associate with the server communications
   * @param wireProtocolPort the port to advertise as
   * @param timeSupplier
   */
  public VertxDiscoveryService(
      Vertx vertx,
      PeerRepository peerRepository,
      List<String> bootstrapPeers,
      InetSocketAddress listenAddress,
      PeerRoutingTable peerRoutingTable,
      KeyPair keyPair,
      int wireProtocolPort,
      LongSupplier timeSupplier) {
    checkArgument(vertx != null);
    checkArgument(peerRepository != null);
    checkArgument(bootstrapPeers != null);
    checkArgument(!bootstrapPeers.isEmpty());
    checkArgument(listenAddress != null);
    checkArgument(peerRoutingTable != null);
    checkArgument(keyPair != null);
    checkArgument(timeSupplier != null);
    checkArgument(wireProtocolPort > 0 && wireProtocolPort < 65536);
    this.vertx = vertx;
    this.peerRepository = peerRepository;
    this.bootstrapPeers = bootstrapPeers;
    this.listenAddress = listenAddress;
    this.peerRoutingTable = peerRoutingTable;
    this.keyPair = keyPair;
    this.timeSupplier = timeSupplier;
    this.wireProtocolPort = wireProtocolPort;
  }

  @VisibleForTesting
  void sendPacket(Endpoint to, Packet<?> packet) {
    socket.send(Buffer.buffer(packet.toBytes().toArrayUnsafe()), to.udpPort(), to.host(), ar -> {
      if (ar.failed()) {
        //logger.error("Failed to send packet on socket", ar.cause());
      }
    });
  }

  @VisibleForTesting
  void receivePacket(DatagramPacket datagram) {
    int length = datagram.data().length();
    if (length > MAX_PACKET_SIZE_BYTES) {
      throw new PeerDiscoveryPacketDecodingException(format("Packet too large. Actual size (bytes): %s", length));
    }
    peerLifecycleManager.receivePacket(Bytes.wrapBuffer(datagram.data()));
  }

  private void handlePacketProcessingException(Throwable throwable) {
    if (throwable instanceof PeerDiscoveryPacketDecodingException) {
      // ignore
      return;
    }
    // logger.warn("unexpected exception in packet handling", throwable);
  }


  /**
   * Returns an optional wrapping the port used by the service, or empty if the service is not in use.
   *
   * @return the port of the service or empty if the service is not started.
   */
  public OptionalInt port() {
    if (socket == null) {
      return OptionalInt.empty();
    } else {
      return OptionalInt.of(socket.localAddress().port());
    }
  }

  /**
   * Starts the service asynchronously.
   *
   * @return a completion that will report the result of the operation.
   */
  public AsyncCompletion start() {
    CompletableAsyncCompletion completion = AsyncCompletion.incomplete();
    int listenPort = listenAddress.getPort();
    String listenHost = listenAddress.getAddress().getHostAddress();
    vertx.createDatagramSocket(new DatagramSocketOptions()).listen(listenPort, listenHost, res -> {
      if (res.failed()) {
        Throwable cause = res.cause();
        if (cause instanceof BindException || cause instanceof SocketException) {
          cause = new PeerDiscoveryServiceException(
              format(
                  "Failed to bind Ethereum P2P UDP listener to %s:%d: %s",
                  listenHost,
                  listenPort,
                  cause.getMessage()));
        }
        completion.completeExceptionally(cause);
        return;
      }

      this.socket = res.result();
      socket.exceptionHandler(this::handlePacketProcessingException);
      SocketAddress socketAddress = socket.localAddress();
      Endpoint thisEndpoint = new Endpoint(socketAddress.host(), socketAddress.port(), wireProtocolPort);
      this.peerLifecycleManager = new PeerLifecycleManager(
          peerRepository,
          bootstrapPeers,
          keyPair,
          this::sendPacket,
          thisEndpoint,
          peerRoutingTable,
          timeSupplier);
      socket.handler(this::receivePacket);

      completion.complete();
    });
    return completion;
  }

  /**
   * Stops the service asynchronously.
   *
   * @return a completion that will report the result of the operation.
   */
  public AsyncCompletion stop() {
    CompletableAsyncCompletion completion = AsyncCompletion.incomplete();
    socket.close(res -> {
      socket = null;
      if (res.failed()) {
        completion.completeExceptionally(res.cause());
      } else {
        completion.complete();
      }
    });
    return completion;
  }
}
