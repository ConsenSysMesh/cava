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
package net.consensys.cava.rlpx.vertx;

import net.consensys.cava.bytes.Bytes;
import net.consensys.cava.bytes.Bytes32;
import net.consensys.cava.concurrent.AsyncCompletion;
import net.consensys.cava.concurrent.CompletableAsyncCompletion;
import net.consensys.cava.crypto.SECP256K1.KeyPair;
import net.consensys.cava.crypto.SECP256K1.PublicKey;
import net.consensys.cava.rlpx.*;
import net.consensys.cava.rlpx.wire.SubProtocol;
import net.consensys.cava.rlpx.wire.SubProtocolHandler;
import net.consensys.cava.rlpx.wire.WireConnection;
import net.consensys.cava.rlpx.wire.WireSubProtocolMessage;

import java.net.InetSocketAddress;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;

import io.vertx.core.Handler;
import io.vertx.core.Vertx;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.net.*;
import org.logl.Logger;

/**
 * Implementation of RLPx service using Vert.x.
 */
public final class VertxRLPxService implements RLPxService {

  private final Logger logger;
  private final AtomicBoolean started = new AtomicBoolean(false);
  private final Vertx vertx;
  private final int listenPort;
  private final KeyPair keyPair;
  private final List<SubProtocol> subProtocols;

  private LinkedHashMap<SubProtocol, SubProtocolHandler> handlers;
  private NetClient client;
  private NetServer server;

  private final Map<String, WireConnection> wireConnections = new ConcurrentHashMap<>();

  public VertxRLPxService(
      Vertx vertx,
      Logger logger,
      int listenPort,
      KeyPair identityKeyPair,
      List<SubProtocol> subProtocols) {
    if (listenPort < 0 || listenPort > 65536) {
      throw new IllegalArgumentException("Invalid port: " + listenPort);
    }
    this.vertx = vertx;
    this.logger = logger;
    this.listenPort = listenPort;
    this.keyPair = identityKeyPair;
    this.subProtocols = subProtocols;
  }

  @Override
  public AsyncCompletion start() {
    if (started.compareAndSet(false, true)) {
      handlers = new LinkedHashMap<SubProtocol, SubProtocolHandler>();
      for (SubProtocol subProtocol : subProtocols) {
        handlers.put(subProtocol, subProtocol.createHandler(this));
      }
      client = vertx.createNetClient(new NetClientOptions());
      server = vertx.createNetServer(new NetServerOptions().setPort(listenPort)).connectHandler(this::receiveMessage);
      CompletableAsyncCompletion complete = AsyncCompletion.incomplete();
      server.listen(res -> {
        if (res.succeeded()) {
          complete.complete();
        } else {
          complete.completeExceptionally(res.cause());
        }
      });
      return complete;
    } else {
      return AsyncCompletion.completed();
    }
  }

  @Override
  public void send(WireSubProtocolMessage message) {
    WireConnection conn = wireConnection(message.connectionId());
    if (conn != null) {
      conn.sendMessage(message);
    }
  }

  @Override
  public void broadcast(WireSubProtocolMessage message) {
    if (!started.get()) {
      throw new IllegalStateException("The RLPx service is not active");
    }
    for (WireConnection conn : wireConnections.values()) {
      conn.sendMessage(message);
    }

  }

  private void receiveMessage(NetSocket netSocket) {
    netSocket.handler(new Handler<Buffer>() {

      private RLPxConnection conn;

      private WireConnection wireConnection;

      @Override
      public void handle(Buffer buffer) {
        if (conn == null) {
          conn = RLPxConnectionFactory.respondToHandshake(
              Bytes.wrapBuffer(buffer),
              keyPair.secretKey(),
              bytes -> netSocket.write(Buffer.buffer(bytes.toArrayUnsafe())));
          if (wireConnection == null) {
            String id = UUID.randomUUID().toString();
            wireConnection = new WireConnection(
                id,
                logger,
                message -> netSocket.write(Buffer.buffer(conn.write(message).toArrayUnsafe())),
                netSocket::end,
                handlers);
            initiateConnection(wireConnection);
          }
        } else {
          conn.stream(Bytes.wrapBuffer(buffer), wireConnection::messageReceived);
        }
      }
    });
  }

  @Override
  public AsyncCompletion stop() {
    if (started.compareAndSet(true, false)) {
      for (WireConnection conn : wireConnections.values()) {
        conn.disconnect(8); //TODO reason hardcoded for now.
      }
      wireConnections.clear();
      client.close();
      CompletableAsyncCompletion completableAsyncCompletion = AsyncCompletion.incomplete();
      server.close(res -> {
        if (res.succeeded()) {
          completableAsyncCompletion.complete();
        } else {
          completableAsyncCompletion.completeExceptionally(res.cause());
        }
      });
      return completableAsyncCompletion;
    } else {
      return AsyncCompletion.completed();
    }
  }

  /**
   *
   * @return the port used by the server
   * @throws IllegalStateException if the service is not started
   */
  public int actualPort() {
    if (!started.get()) {
      throw new IllegalStateException("The RLPx service is not active");
    }
    return server.actualPort();
  }

  @Override
  public void connectTo(PublicKey peerPublicKey, InetSocketAddress peerAddress) {
    client.connect(
        peerAddress.getPort(),
        peerAddress.getHostString(),
        netSocketFuture -> netSocketFuture.map(netSocket -> {
          Bytes32 nonce = RLPxConnectionFactory.generateRandomBytes32();
          KeyPair ephemeralKeyPair = KeyPair.random();
          Bytes initHandshakeMessage = RLPxConnectionFactory.init(keyPair, peerPublicKey, ephemeralKeyPair, nonce);
          netSocket.write(Buffer.buffer(initHandshakeMessage.toArrayUnsafe()));

          netSocket.handler(new Handler<Buffer>() {

            private RLPxConnection conn;

            private WireConnection wireConnection;

            @Override
            public void handle(Buffer buffer) {
              try {
                if (conn == null) {
                  Bytes responseBytes = Bytes.wrapBuffer(buffer);
                  HandshakeMessage responseMessage =
                      RLPxConnectionFactory.readResponse(responseBytes, keyPair.secretKey());
                  conn = RLPxConnectionFactory.createConnection(
                      true,
                      initHandshakeMessage,
                      responseBytes,
                      ephemeralKeyPair.secretKey(),
                      responseMessage.ephemeralPublicKey(),
                      nonce,
                      responseMessage.nonce());
                  String id = UUID.randomUUID().toString();
                  wireConnection = new WireConnection(
                      id,
                      logger,
                      message -> netSocket.write(Buffer.buffer(conn.write(message).toArrayUnsafe())),
                      netSocket::end,
                      handlers);
                  initiateConnection(wireConnection);
                  wireConnection.handleConnectionStart();
                } else {
                  conn.stream(Bytes.wrapBuffer(buffer), wireConnection::messageReceived);
                }
              } catch (InvalidMACException e) {
                logger.error(e.getMessage(), e);
              }
            }
          });
          return null;
        }));
  }

  Collection<WireConnection> wireConnections() {
    return wireConnections.values();
  }

  private void initiateConnection(WireConnection wireConnection) {
    wireConnections.put(wireConnection.id(), wireConnection);
  }

  private WireConnection wireConnection(String id) {
    if (!started.get()) {
      throw new IllegalStateException("The RLPx service is not active");
    }
    return wireConnections.get(id);
  }
}
