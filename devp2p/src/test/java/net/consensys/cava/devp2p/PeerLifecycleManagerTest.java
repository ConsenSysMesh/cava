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

import static org.junit.jupiter.api.Assertions.*;

import net.consensys.cava.bytes.Bytes;
import net.consensys.cava.crypto.SECP256K1.KeyPair;
import net.consensys.cava.devp2p.NeighborsPayload.Neighbor;
import net.consensys.cava.junit.BouncyCastleExtension;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

@ExtendWith(BouncyCastleExtension.class)
class PeerLifecycleManagerTest {

  @Test
  void peerRepositoryNull() {
    assertThrows(
        IllegalArgumentException.class,
        () -> new PeerLifecycleManager(
            null,
            Collections.singletonList("enode://example.com"),
            KeyPair.random(),
            ((endpoint, packet) -> {
            }),
            new Endpoint("127.0.0.1", 1234, 12345),
            new SimplePeerRoutingTable(),
            System::currentTimeMillis));
  }

  @Test
  void bootstrapPeersNull() {
    assertThrows(
        IllegalArgumentException.class,
        () -> new PeerLifecycleManager(new PeerRepository(), null, KeyPair.random(), ((endpoint, packet) -> {
        }), new Endpoint("127.0.0.1", 1234, 12345), new SimplePeerRoutingTable(), System::currentTimeMillis));
  }

  @Test
  void bootstrapPeersEmpty() {
    assertThrows(
        IllegalArgumentException.class,
        () -> new PeerLifecycleManager(
            new PeerRepository(),
            new ArrayList<>(),
            KeyPair.random(),
            ((endpoint, packet) -> {
            }),
            new Endpoint("127.0.0.1", 1234, 12345),
            new SimplePeerRoutingTable(),
            System::currentTimeMillis));
  }

  @Test
  void nullKeyPair() {
    assertThrows(IllegalArgumentException.class, () -> {
      new PeerLifecycleManager(
          new PeerRepository(),
          Collections.singletonList("enode://example.com"),
          null,
          ((endpoint, packet) -> {
          }),
          new Endpoint("127.0.0.1", 1234, 12345),
          new SimplePeerRoutingTable(),
          System::currentTimeMillis);
    });
  }

  @Test
  void nullPacketSender() {
    assertThrows(
        IllegalArgumentException.class,
        () -> new PeerLifecycleManager(
            new PeerRepository(),
            Collections.singletonList("enode://example.com"),
            KeyPair.random(),
            null,
            new Endpoint("127.0.0.1", 1234, 12345),
            new SimplePeerRoutingTable(),
            System::currentTimeMillis));
  }

  @Test
  void nullEndpoint() {
    assertThrows(
        IllegalArgumentException.class,
        () -> new PeerLifecycleManager(
            new PeerRepository(),
            Collections.singletonList("enode://example.com"),
            KeyPair.random(),
            ((endpoint, packet) -> {
            }),
            null,
            new SimplePeerRoutingTable(),
            System::currentTimeMillis));
  }

  @Test
  void nullPeerRoutingTable() {
    assertThrows(
        IllegalArgumentException.class,
        () -> new PeerLifecycleManager(
            new PeerRepository(),
            Collections.singletonList("enode://example.com"),
            KeyPair.random(),
            ((endpoint, packet) -> {
            }),
            new Endpoint("127.0.0.1", 1234, 12345),
            null,
            System::currentTimeMillis));
  }

  @Test
  void nullTimeSupplier() {
    assertThrows(
        IllegalArgumentException.class,
        () -> new PeerLifecycleManager(
            new PeerRepository(),
            Collections.singletonList("enode://example.com"),
            KeyPair.random(),
            ((endpoint, packet) -> {
            }),
            new Endpoint("127.0.0.1", 1234, 12345),
            new SimplePeerRoutingTable(),
            null));
  }

  private Endpoint capturedEndpoint;
  private Packet<?> capturedPacket;

  void capturePacket(Endpoint endpoint, Packet<?> capturedPacket) {
    this.capturedEndpoint = endpoint;
    this.capturedPacket = capturedPacket;
  }

  @Test
  void pingOnePeerOnInitialization() {
    Endpoint from = new Endpoint("127.0.0.1", 1234, 12345);
    Long now = System.currentTimeMillis();
    PeerLifecycleManager service = new PeerLifecycleManager(
        new PeerRepository(),
        Collections.singletonList("enode://abcd@192.168.1.10"),
        KeyPair.random(),
        this::capturePacket,
        from,
        new SimplePeerRoutingTable(),
        () -> now);
    Endpoint to = new Endpoint("192.168.1.10", 30303, 30303);
    assertEquals(this.capturedEndpoint, to);
    PingPayload pingPayload = (PingPayload) capturedPacket.payload();
    assertEquals(now + 3000, pingPayload.expiration());
    assertEquals(to, pingPayload.to());
    assertEquals(from, pingPayload.from());
  }

  @Test
  void shouldMarkPeerActiveWhenReceivesAPongBack() {
    Endpoint from = new Endpoint("127.0.0.1", 1234, 12345);
    Long now = Instant.now().toEpochMilli();
    PeerRepository peerRepository = new PeerRepository();
    KeyPair neighborKeyPair = KeyPair.random();
    PeerLifecycleManager service = new PeerLifecycleManager(
        peerRepository,
        Collections.singletonList(
            "enode://" + neighborKeyPair.publicKey().bytes().toHexString().substring(2) + "@192.168.1.10"),
        KeyPair.random(),
        this::capturePacket,
        from,
        new SimplePeerRoutingTable(),
        () -> now);

    assertEquals((byte) 0x01, capturedPacket.header().packetType());
    Packet<PongPayload> pongResponse =
        Packet.createPong(from, capturedPacket.header().hash(), Instant.now().toEpochMilli() + 10000, neighborKeyPair);
    AtomicReference<Peer> peerActivated = new AtomicReference<>();
    peerRepository.observePeerActive(peerActivated::set);
    service.receivePacket(pongResponse.toBytes());
    assertNotNull(peerActivated.get());
    assertEquals(pongResponse.header().nodeId(), peerActivated.get().nodeId());
    assertEquals((byte) 0x03, capturedPacket.header().packetType());
  }

  @Test
  void shouldAddNeighborsWhenReceivingAValidNeighborsResponse() {

  }

  @Test
  void shouldIgnoreUnsolicitedPongs() {
    AtomicReference<Peer> activatedPeer = new AtomicReference<>();
    Endpoint from = new Endpoint("127.0.0.1", 1234, 12345);
    Long now = System.currentTimeMillis();
    PeerRepository repository = new PeerRepository();
    PeerLifecycleManager service = new PeerLifecycleManager(
        repository,
        Collections.singletonList("enode://abcd@192.168.1.10"),
        KeyPair.random(),
        this::capturePacket,
        from,
        new SimplePeerRoutingTable(),
        () -> now);
    repository.observePeerActive(activatedPeer::set);
    Packet<PongPayload> unsolicitedPong = Packet.createPong(from, Bytes.EMPTY, now + 25, KeyPair.random());
    service.handlePong(unsolicitedPong);
    assertNull(activatedPeer.get());
  }

  @Test
  void shouldIgnoreUnsolicitedFindNeighbors() {
    Endpoint from = new Endpoint("127.0.0.1", 1234, 12345);
    Long now = System.currentTimeMillis();
    PeerRepository repository = new PeerRepository();
    PeerLifecycleManager service = new PeerLifecycleManager(
        repository,
        Collections.singletonList("enode://abcd@192.168.1.10"),
        KeyPair.random(),
        this::capturePacket,
        from,
        new SimplePeerRoutingTable(),
        () -> now);
    AtomicReference<Peer> addedPeer = new AtomicReference<>();
    repository.observePeerAddition(addedPeer::set);
    KeyPair unsolicitedPair = KeyPair.random();
    Packet<FindNeighborsPayload> unsolicitedFindNeighbors =
        Packet.createFindNeighbors(from.toBytes(), now + 25, unsolicitedPair);
    service.handleFindNeighbors(unsolicitedFindNeighbors);
    assertEquals(unsolicitedPair.publicKey().bytes(), addedPeer.get().nodeId());
  }

  @Test
  void shouldIgnoreUnsolicitedNeighbors() {
    Endpoint from = new Endpoint("127.0.0.1", 1234, 12345);
    Long now = System.currentTimeMillis();
    PeerRepository repository = new PeerRepository();
    PeerLifecycleManager service = new PeerLifecycleManager(
        repository,
        Collections.singletonList("enode://abcd@192.168.1.10"),
        KeyPair.random(),
        this::capturePacket,
        from,
        new SimplePeerRoutingTable(),
        () -> now);
    AtomicReference<Peer> addedPeer = new AtomicReference<>();
    repository.observePeerAddition(addedPeer::set);
    KeyPair unsolicitedPair = KeyPair.random();
    Neighbor neighbor = new Neighbor(KeyPair.random().publicKey().bytes(), new Endpoint("10.0.0.2", 1235, 1235));
    Packet<NeighborsPayload> unsolicitedNeighbors =
        Packet.createNeighbors(Collections.singletonList(neighbor), now + 100, unsolicitedPair);
    service.handleNeighbors(unsolicitedNeighbors);
    assertEquals(unsolicitedPair.publicKey().bytes(), addedPeer.get().nodeId());
  }

  private static class InProcessCommunications {

    private PeerLifecycleManager serviceA;
    private PeerLifecycleManager serviceB;

    private List<Packet<?>> toA = new ArrayList<Packet<?>>();
    private List<Packet<?>> toB = new ArrayList<Packet<?>>();

    void init(PeerLifecycleManager serviceA, PeerLifecycleManager serviceB) {
      this.serviceA = serviceA;
      this.serviceB = serviceB;
      for (Packet<?> p : new ArrayList<>(toA)) {
        serviceA.receivePacket(p.toBytes());
      }
      for (Packet<?> p : new ArrayList<>(toB)) {
        serviceB.receivePacket(p.toBytes());
      }
    }

    void sendToA(Endpoint endpoint, Packet<?> packet) {
      toA.add(packet);
      if (serviceA != null) {
        serviceA.receivePacket(packet.toBytes());
      }
    }

    void sendToB(Endpoint endpoint, Packet<?> packet) {
      toB.add(packet);
      if (serviceB != null) {
        serviceB.receivePacket(packet.toBytes());
      }
    }

    List<Packet<?>> toA() {
      return toA;
    }

    List<Packet<?>> toB() {
      return toB;
    }
  }

  @Test
  void shouldDiscoverEachOther() {
    InProcessCommunications ipc = new InProcessCommunications();
    Long now = System.currentTimeMillis();
    PeerRepository repositoryA = new PeerRepository();
    PeerRepository repositoryB = new PeerRepository();
    KeyPair keyPairA = KeyPair.random();
    KeyPair keyPairB = KeyPair.random();
    String uriA =
        "enode://" + keyPairA.publicKey().bytes().toHexString().substring(2) + "@127.0.0.1:12345?discport=1234";
    String uriB =
        "enode://" + keyPairB.publicKey().bytes().toHexString().substring(2) + "@127.0.0.1:12346?discport=1236";
    PeerLifecycleManager serviceA = new PeerLifecycleManager(
        repositoryA,
        Collections.singletonList(uriB),
        keyPairA,
        ipc::sendToB,
        new Endpoint("127.0.0.1", 1234, 12345),
        new SimplePeerRoutingTable(),
        () -> now);
    PeerLifecycleManager serviceB = new PeerLifecycleManager(
        repositoryB,
        Collections.singletonList(uriA),
        keyPairB,
        ipc::sendToA,
        new Endpoint("127.0.0.1", 1236, 12346),
        new SimplePeerRoutingTable(),
        () -> now);
    assertEquals(1, ipc.toA().size(), ipc.toA().toString());
    assertEquals(1, ipc.toB().size(), ipc.toB().toString());
    ipc.init(serviceA, serviceB);

    assertTrue(repositoryA.get(uriB).isActive());
    assertTrue(repositoryB.get(uriA).isActive());
  }
}
