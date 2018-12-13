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

import static net.consensys.cava.bytes.Bytes.fromHexString;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import net.consensys.cava.bytes.Bytes;

import java.time.Instant;
import java.util.Collections;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Supplier;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class PeerRepositoryTest {

  private Supplier<Instant> currentTimeSupplier;
  private PeerRepository peerRepository;

  private Instant now = Instant.now();

  @SuppressWarnings("unchecked")
  @BeforeEach
  void setup() {
    currentTimeSupplier = () -> now;
    peerRepository = new PeerRepository(currentTimeSupplier);
  }

  @Test
  void shouldReturnInactivePeerWithNoEndpointForUnknownId() {
    Peer peer = peerRepository.get(Bytes.of(1, 2, 3));
    assertFalse(peer.isActive());
    assertFalse(peer.endpoint().isPresent());
  }

  @Test
  void shouldReturnInactivePeerWithEndpointForUnknownIdAndEndpoint() {
    Endpoint endpoint = new Endpoint("127.0.0.1", 30303, 30303);
    Peer peer = peerRepository.get(Bytes.of(1, 2, 3), endpoint);
    assertFalse(peer.isActive());
    assertTrue(peer.endpoint().isPresent());
    assertEquals(endpoint, peer.endpoint().get());
  }

  @Test
  void shouldReturnInactivePeerBasedOnURI() {
    Peer peer = peerRepository.get(
        "enode://c7849b663d12a2b5bf05b1ebf5810364f4870d5f1053fbd7500d38bc54c705b453d7511ca8a4a86003d34d4c8ee0bbfcd387aa724f5b240b3ab4bbb994a1e09b@172.20.0.4:7654");
    assertFalse(peer.isActive());
    assertTrue(peer.endpoint().isPresent());

    assertEquals(
        fromHexString(
            "c7849b663d12a2b5bf05b1ebf5810364f4870d5f1053fbd7500d38bc54c705b453d7511ca8a4a86003d34d4c8ee0bbfcd387aa724f5b240b3ab4bbb994a1e09b"),
        peer.nodeId());
    assertEquals("172.20.0.4", peer.endpoint().get().host());
    assertEquals(7654, peer.endpoint().get().udpPort());
    assertEquals(7654, peer.endpoint().get().tcpPort());
  }

  @Test
  void shouldReturnPeerWithDefaultPortsWhenMissingFromURI() {
    Peer peer = peerRepository.get(
        "enode://c7849b663d12a2b5bf05b1ebf5810364f4870d5f1053fbd7500d38bc54c705b453d7511ca8a4a86003d34d4c8ee0bbfcd387aa724f5b240b3ab4bbb994a1e09b@172.20.0.4");
    assertFalse(peer.isActive());
    assertTrue(peer.endpoint().isPresent());

    assertEquals(
        fromHexString(
            "c7849b663d12a2b5bf05b1ebf5810364f4870d5f1053fbd7500d38bc54c705b453d7511ca8a4a86003d34d4c8ee0bbfcd387aa724f5b240b3ab4bbb994a1e09b"),
        peer.nodeId());
    assertEquals("172.20.0.4", peer.endpoint().get().host());
    assertEquals(30303, peer.endpoint().get().udpPort());
    assertEquals(30303, peer.endpoint().get().tcpPort());
  }

  @Test
  void shouldReturnPeerWithDifferentPortsWhenQueryParamInURI() {
    Peer peer = peerRepository.get(
        "enode://c7849b663d12a2b5bf05b1ebf5810364f4870d5f1053fbd7500d38bc54c705b453d7511ca8a4a86003d34d4c8ee0bbfcd387aa724f5b240b3ab4bbb994a1e09b@172.20.0.4:54789?discport=23456");
    assertFalse(peer.isActive());
    assertTrue(peer.endpoint().isPresent());

    assertEquals(
        fromHexString(
            "c7849b663d12a2b5bf05b1ebf5810364f4870d5f1053fbd7500d38bc54c705b453d7511ca8a4a86003d34d4c8ee0bbfcd387aa724f5b240b3ab4bbb994a1e09b"),
        peer.nodeId());
    assertEquals("172.20.0.4", peer.endpoint().get().host());
    assertEquals(23456, peer.endpoint().get().udpPort());
    assertEquals(54789, peer.endpoint().get().tcpPort());
  }

  @Test
  void shouldThrowWhenNotEnodeURI() {
    assertThrows(IllegalArgumentException.class, () -> {
      peerRepository.get(
          "http://c7849b663d12a2b5bf05b1ebf5810364f4870d5f1053fbd7500d38bc54c705b453d7511ca8a4a86003d34d4c8ee0bbfcd387aa724f5b240b3ab4bbb994a1e09b@172.20.0.4:30303");
    });
  }

  @Test
  void shouldThrowWhenNoNodeIdInURI() {
    assertThrows(IllegalArgumentException.class, () -> {
      peerRepository.get("enode://172.20.0.4:30303");
    });
  }

  @Test
  void shouldThrowWhenInvalidPortInURI() {
    assertThrows(
        IllegalArgumentException.class,
        () -> peerRepository.get(
            "enode://c7849b663d12a2b5bf05b1ebf5810364f4870d5f1053fbd7500d38bc54c705b453d7511ca8a4a86003d34d4c8ee0bbfcd387aa724f5b240b3ab4bbb994a1e09b@172.20.0.4:98766"));
  }

  @Test
  void shouldThrowWhenOutOfRangeDiscPortInURI() {
    assertThrows(
        IllegalArgumentException.class,
        () -> peerRepository.get(
            "enode://c7849b663d12a2b5bf05b1ebf5810364f4870d5f1053fbd7500d38bc54c705b453d7511ca8a4a86003d34d4c8ee0bbfcd387aa724f5b240b3ab4bbb994a1e09b@172.20.0.4:54789?discport=98765"));
  }

  @Test
  void shouldThrowWhenInvalidDiscPortInURI() {
    assertThrows(
        IllegalArgumentException.class,
        () -> peerRepository.get(
            "enode://c7849b663d12a2b5bf05b1ebf5810364f4870d5f1053fbd7500d38bc54c705b453d7511ca8a4a86003d34d4c8ee0bbfcd387aa724f5b240b3ab4bbb994a1e09b@172.20.0.4:54789?discport=abcd"));
  }

  @Test
  void shouldIgnoreAdditionalQueryParametersInURI() {
    Peer peer = peerRepository.get(
        "enode://c7849b663d12a2b5bf05b1ebf5810364f4870d5f1053fbd7500d38bc54c705b453d7511ca8a4a86003d34d4c8ee0bbfcd387aa724f5b240b3ab4bbb994a1e09b@172.20.0.4:54789?foo=bar&discport=23456&bar=foo");
    assertFalse(peer.isActive());
    assertTrue(peer.endpoint().isPresent());

    assertEquals(
        fromHexString(
            "c7849b663d12a2b5bf05b1ebf5810364f4870d5f1053fbd7500d38bc54c705b453d7511ca8a4a86003d34d4c8ee0bbfcd387aa724f5b240b3ab4bbb994a1e09b"),
        peer.nodeId());
    assertEquals("172.20.0.4", peer.endpoint().get().host());
    assertEquals(23456, peer.endpoint().get().udpPort());
    assertEquals(54789, peer.endpoint().get().tcpPort());
  }

  @Test
  void shouldUpdatePeerWhenGettingInactiveWithNewEndpoint() {
    Bytes nodeId = Bytes.of(1, 2, 3);
    Peer peer1 = peerRepository.get(nodeId);
    Endpoint endpoint = new Endpoint("127.0.0.1", 30303, 30303);
    Peer peer2 = peerRepository.get(nodeId, endpoint);
    assertSame(peer2, peer1);
    assertTrue(peer1.endpoint().isPresent());
    assertEquals(endpoint, peer1.endpoint().get());
  }

  @Test
  void shouldNotUpdateEndpointWhenGettingActivePeer() {
    Endpoint endpoint1 = new Endpoint("127.0.0.1", 30303, 30303);
    Peer peer1 = peerRepository.get(Bytes.of(1, 2, 3));
    peer1.setActive(endpoint1);
    Endpoint endpoint2 = new Endpoint("127.0.0.2", 30304, 30304);
    Peer peer2 = peerRepository.get(Bytes.of(1, 2, 3), endpoint2);
    assertSame(peer2, peer1);
    assertTrue(peer1.endpoint().isPresent());
    assertEquals(endpoint1, peer1.endpoint().get());
  }

  @Test
  void shouldNotUpdateEndpointWhenSettingActivePeerToActive() {
    Endpoint endpoint1 = new Endpoint("127.0.0.1", 30303, 30303);
    Peer peer = peerRepository.get(Bytes.of(1, 2, 3));
    peer.setActive(endpoint1);
    Endpoint endpoint2 = new Endpoint("127.0.0.2", 30304, 30304);
    peer.setActive(endpoint2);
    assertTrue(peer.endpoint().isPresent());
    assertEquals(endpoint1, peer.endpoint().get());
  }

  @Test
  void shouldNotUpdateCapabilitiesForInactivePeer() {
    Peer peer = peerRepository.get(Bytes.of(1, 2, 3));
    peer.setCapabilities("eth");
    assertEquals(Collections.emptySet(), peer.capabilities());
    peer.setActive(new Endpoint("127.0.0.1", 30303, 30303));
    peer.setInactive();
    peer.setCapabilities(Collections.singletonList("eth"));
    assertEquals(Collections.emptySet(), peer.capabilities());
  }

  @Test
  void shouldNotifyObserversWhenPeerIsAdded() {
    AtomicReference<Peer> notifiedPeer = new AtomicReference<>();
    Consumer<Peer> observer = notifiedPeer::set;
    peerRepository.observePeerAddition(observer);
    Peer peer = peerRepository.get(Bytes.of(1, 2, 3));
    assertEquals(peer, notifiedPeer.get());
    assertTrue(peerRepository.unObservePeerAddition(observer));
  }

  @Test
  void shouldNotifyObserversWhenPeerBecomesActive() {
    AtomicReference<Peer> notifiedPeer = new AtomicReference<>();
    Consumer<Peer> observer = notifiedPeer::set;
    peerRepository.observePeerActive(observer);

    Endpoint endpoint = new Endpoint("127.0.0.1", 30303, 30303);
    Peer peer = peerRepository.get(Bytes.of(1, 2, 3));
    peer.setActive(endpoint);

    assertTrue(peer.isActive());
    assertEquals(peer, notifiedPeer.get());

    assertTrue(peerRepository.unObservePeerActive(observer));
  }

  @Test
  void shouldNotifyObserversWhenPeerBecomesInActive() {
    AtomicReference<Peer> notifiedPeer = new AtomicReference<>();
    Consumer<Peer> observer = notifiedPeer::set;
    peerRepository.observePeerInactive(observer);

    Endpoint endpoint = new Endpoint("127.0.0.1", 30303, 30303);
    Peer peer = peerRepository.get(Bytes.of(1, 2, 3));
    peer.setActive(endpoint);

    assertTrue(peer.isActive());
    assertEquals(null, notifiedPeer.get());

    peer.setInactive();
    assertFalse(peer.isActive());
    assertEquals(peer, notifiedPeer.get());

    assertTrue(peerRepository.unObservePeerInactive(observer));
  }

  @Test
  void shouldNotifyObserversWhenPeerCapabilitiesAreUpdated() {
    AtomicReference<Set<String>> notifiedCapabilities = new AtomicReference<>();
    AtomicReference<Peer> notifiedPeer = new AtomicReference<>();
    BiConsumer<Set<String>, Peer> observer = (caps, peer) -> {
      notifiedCapabilities.set(caps);
      notifiedPeer.set(peer);
    };
    peerRepository.observePeerCapabilities(observer);

    Endpoint endpoint = new Endpoint("127.0.0.1", 30303, 30303);
    Peer peer = peerRepository.get(Bytes.of(1, 2, 3));
    peer.setActive(endpoint);

    assertTrue(peer.isActive());
    assertEquals(null, notifiedPeer.get());

    peer.setCapabilities(Collections.singletonList("eth"));
    assertTrue(peer.hasCapability("eth"));
    assertEquals(Collections.singleton("eth"), peer.capabilities());

    assertEquals(Collections.emptySet(), notifiedCapabilities.get());
    assertEquals(peer, notifiedPeer.get());

    assertTrue(peerRepository.unObservePeerCapabilities(observer));
  }

  @Test
  void shouldNotifyCapabilityObserversWhenPeerBecomesInactive() {
    Endpoint endpoint = new Endpoint("127.0.0.1", 30303, 30303);
    Peer peer = peerRepository.get(Bytes.of(1, 2, 3));
    peer.setActive(endpoint);
    peer.setCapabilities("eth");

    AtomicReference<Set<String>> notifiedCapabilities = new AtomicReference<>();
    AtomicReference<Peer> notifiedPeer = new AtomicReference<>();
    BiConsumer<Set<String>, Peer> observer = (caps, observedPeer) -> {
      notifiedCapabilities.set(caps);
      notifiedPeer.set(observedPeer);
    };
    peerRepository.observePeerCapabilities(observer);

    peer.setInactive();

    assertEquals(Collections.emptySet(), peer.capabilities());

    assertEquals(Collections.singleton("eth"), notifiedCapabilities.get());
    assertEquals(peer, notifiedPeer.get());

    assertTrue(peerRepository.unObservePeerCapabilities(observer));
  }

  @Test
  void setLastSeenToCurrentTime() {
    Endpoint endpoint = new Endpoint("127.0.0.1", 30303, 30303);
    Peer peer = peerRepository.get(Bytes.of(1, 2, 3));
    peer.setActive(endpoint);
    peer.updateLastSeen();

    assertEquals(Optional.of(now), peer.lastSeen());
  }
}
