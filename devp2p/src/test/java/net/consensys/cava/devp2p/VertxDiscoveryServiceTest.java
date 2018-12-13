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

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import net.consensys.cava.concurrent.AsyncCompletion;
import net.consensys.cava.crypto.SECP256K1.KeyPair;
import net.consensys.cava.junit.BouncyCastleExtension;
import net.consensys.cava.junit.VertxExtension;
import net.consensys.cava.junit.VertxInstance;

import java.net.InetSocketAddress;
import java.time.Instant;
import java.util.Collections;

import io.vertx.core.Vertx;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.datagram.DatagramPacket;
import io.vertx.core.net.SocketAddress;
import io.vertx.core.net.impl.SocketAddressImpl;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

@ExtendWith({VertxExtension.class, BouncyCastleExtension.class})
class VertxDiscoveryServiceTest {

  @Test
  void nullVertx() {
    assertThrows(IllegalArgumentException.class, () -> {
      new VertxDiscoveryService(
          null,
          new PeerRepository(),
          Collections.singletonList("enode://abcd@10.0.0.2"),
          new InetSocketAddress("localhost", 8888),
          new SimplePeerRoutingTable(),
          KeyPair.random(),
          30303,
          () -> Instant.now().toEpochMilli());
    });
  }

  @Test
  void noPeerRepository(@VertxInstance Vertx vertx) {
    assertThrows(IllegalArgumentException.class, () -> {
      new VertxDiscoveryService(
          vertx,
          null,
          Collections.singletonList("enode://abcd@10.0.0.2"),
          new InetSocketAddress("localhost", 8888),
          new SimplePeerRoutingTable(),
          KeyPair.random(),
          30303,
          () -> Instant.now().toEpochMilli());
    });
  }

  @Test
  void emptyBootstrapList(@VertxInstance Vertx vertx) {
    assertThrows(IllegalArgumentException.class, () -> {
      new VertxDiscoveryService(
          vertx,
          new PeerRepository(),
          Collections.emptyList(),
          new InetSocketAddress("localhost", 8888),
          new SimplePeerRoutingTable(),
          KeyPair.random(),
          30303,
          () -> Instant.now().toEpochMilli());
    });
  }

  @Test
  void nullBootstrapList(@VertxInstance Vertx vertx) {
    assertThrows(IllegalArgumentException.class, () -> {
      new VertxDiscoveryService(
          vertx,
          new PeerRepository(),
          null,
          new InetSocketAddress("localhost", 8888),
          new SimplePeerRoutingTable(),
          KeyPair.random(),
          30303,
          () -> Instant.now().toEpochMilli());
    });
  }

  @Test
  void nullSocketAddress(@VertxInstance Vertx vertx) {
    assertThrows(IllegalArgumentException.class, () -> {
      new VertxDiscoveryService(
          vertx,
          new PeerRepository(),
          Collections.singletonList("enode://abcd@10.0.0.2"),
          null,
          new SimplePeerRoutingTable(),
          KeyPair.random(),
          30303,
          () -> Instant.now().toEpochMilli());
    });
  }

  @Test
  void nullPeerRoutingTable(@VertxInstance Vertx vertx) {
    assertThrows(IllegalArgumentException.class, () -> {
      new VertxDiscoveryService(
          vertx,
          new PeerRepository(),
          Collections.singletonList("enode://abcd@10.0.0.2"),
          new InetSocketAddress("localhost", 8888),
          null,
          KeyPair.random(),
          30303,
          () -> Instant.now().toEpochMilli());
    });
  }

  @Test
  void nullKeyPair(@VertxInstance Vertx vertx) {
    assertThrows(IllegalArgumentException.class, () -> {
      new VertxDiscoveryService(
          vertx,
          new PeerRepository(),
          Collections.singletonList("enode://abcd@10.0.0.2"),
          new InetSocketAddress("localhost", 8888),
          new SimplePeerRoutingTable(),
          null,
          30303,
          () -> Instant.now().toEpochMilli());
    });
  }

  @Test
  void zeroWireProtocolPort(@VertxInstance Vertx vertx) {
    assertThrows(IllegalArgumentException.class, () -> {
      new VertxDiscoveryService(
          vertx,
          new PeerRepository(),
          Collections.singletonList("enode://abcd@10.0.0.2"),
          new InetSocketAddress("localhost", 8888),
          new SimplePeerRoutingTable(),
          null,
          0,
          () -> Instant.now().toEpochMilli());
    });
  }

  @Test
  void wireProtocolPortOverflow(@VertxInstance Vertx vertx) {
    assertThrows(IllegalArgumentException.class, () -> {
      new VertxDiscoveryService(
          vertx,
          new PeerRepository(),
          Collections.singletonList("enode://abcd@10.0.0.2"),
          new InetSocketAddress("localhost", 8888),
          new SimplePeerRoutingTable(),
          null,
          100000,
          () -> Instant.now().toEpochMilli());
    });
  }

  @Test
  void nullTimeSupplier(@VertxInstance Vertx vertx) {
    assertThrows(IllegalArgumentException.class, () -> {
      new VertxDiscoveryService(
          vertx,
          new PeerRepository(),
          Collections.singletonList("enode://abcd@10.0.0.2"),
          new InetSocketAddress("localhost", 8888),
          new SimplePeerRoutingTable(),
          null,
          100000,
          null);
    });
  }

  @Test
  void startAndStopOK(@VertxInstance Vertx vertx) throws Exception {
    VertxDiscoveryService discoveryService = new VertxDiscoveryService(
        vertx,
        new PeerRepository(),
        Collections.singletonList("enode://abcd@10.0.0.2"),
        new InetSocketAddress("localhost", 0),
        new SimplePeerRoutingTable(),
        KeyPair.random(),
        30303,
        () -> Instant.now().toEpochMilli());
    AsyncCompletion completion = discoveryService.start();
    completion.join();
    assertTrue(discoveryService.port().getAsInt() != 0);
    discoveryService.stop();
    completion.join();

  }

  @Test
  void messageTooLarge(@VertxInstance Vertx vertx) {
    VertxDiscoveryService discoveryService = new VertxDiscoveryService(
        vertx,
        new PeerRepository(),
        Collections.singletonList("enode://abcd@10.0.0.2"),
        new InetSocketAddress("localhost", 0),
        new SimplePeerRoutingTable(),
        KeyPair.random(),
        30303,
        () -> Instant.now().toEpochMilli());
    assertThrows(PeerDiscoveryPacketDecodingException.class, () -> {
      DatagramPacket packet = new DatagramPacket() {
        @Override
        public SocketAddress sender() {
          return new SocketAddressImpl(12345, "example.com");
        }

        @Override
        public Buffer data() {
          return Buffer.buffer(new byte[1821]);
        }
      };
      discoveryService.receivePacket(packet);
    });
  }
}
