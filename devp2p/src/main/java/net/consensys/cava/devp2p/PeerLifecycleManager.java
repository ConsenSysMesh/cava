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

import net.consensys.cava.bytes.Bytes;
import net.consensys.cava.concurrent.ExpiringMap;
import net.consensys.cava.crypto.SECP256K1.KeyPair;
import net.consensys.cava.devp2p.NeighborsPayload.Neighbor;

import java.time.Instant;
import java.util.List;
import java.util.function.BiConsumer;
import java.util.function.LongSupplier;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import com.google.common.annotations.VisibleForTesting;

/**
 * Manages the actions reported by the network and the changes to the peer repository.
 */
final class PeerLifecycleManager {

  private static final long PONG_EXPIRATION = 60000;
  private static final long EXPIRATION_PERIOD_MS = 3000;
  private static final int MAX_NUMBER_OF_NEIGHBORS = 5;

  private final ExpiringMap<Bytes, Bytes> awaitingPongs = new ExpiringMap<>();
  private final PeerRepository peerRepository;
  private final LongSupplier currentTimeSupplier;
  private final BiConsumer<Endpoint, Packet<?>> packetSender;
  private final KeyPair keyPair;
  private final Endpoint endpoint;
  private final PeerRoutingTable peerRoutingTable;

  PeerLifecycleManager(
      PeerRepository peerRepository,
      List<String> bootstrapPeers,
      KeyPair keyPair,
      BiConsumer<Endpoint, Packet<?>> packetSender,
      Endpoint endpoint,
      PeerRoutingTable peerRoutingTable,
      LongSupplier currentTimeSupplier) {
    checkArgument(peerRepository != null);
    checkArgument(bootstrapPeers != null && !bootstrapPeers.isEmpty());
    checkArgument(keyPair != null);
    checkArgument(peerRoutingTable != null);
    checkArgument(currentTimeSupplier != null);
    checkArgument(packetSender != null);
    this.currentTimeSupplier = currentTimeSupplier;
    this.keyPair = keyPair;
    this.packetSender = packetSender;
    this.endpoint = endpoint;
    this.peerRepository = peerRepository;
    this.peerRoutingTable = peerRoutingTable;
    peerRepository.observePeerActive(this::peerActive);
    peerRepository.observePeerAddition(this::peerAddition);
    for (String peer : bootstrapPeers) {
      peerRepository.get(peer);
    }
  }

  private void peerActive(Peer peer) {
    long now = currentTimeSupplier.getAsLong();
    Endpoint to = peer.endpoint().get();
    Packet<FindNeighborsPayload> findNeighbors =
        Packet.createFindNeighbors(peer.nodeId(), now + EXPIRATION_PERIOD_MS, keyPair);
    packetSender.accept(to, findNeighbors);
  }

  private void peerAddition(Peer peer) {
    if (!peer.endpoint().isPresent() || peer.endpoint().get().equals(endpoint)) {
      return;
    }
    long now = currentTimeSupplier.getAsLong();
    Endpoint to = peer.endpoint().get();
    peerRoutingTable.add(peer);
    Packet<PingPayload> pingPacket = Packet.createPing(this.endpoint, to, now + EXPIRATION_PERIOD_MS, keyPair);
    awaitingPongs
        .put(pingPacket.header().hash(), peer.nodeId(), Instant.ofEpochMilli(now + PONG_EXPIRATION).toEpochMilli());
    packetSender.accept(to, pingPacket);
  }

  @VisibleForTesting
  void handleFindNeighbors(Packet<FindNeighborsPayload> findNeighborsPayloadPacket) {
    Peer peer = peerRepository.get(findNeighborsPayloadPacket.header().nodeId());
    if (peer.endpoint().isPresent() && peer.isActive()) {
      peer.updateLastSeen();
      Stream<Peer> peerStream = peerRoutingTable.nearest(findNeighborsPayloadPacket.payload().target());
      long now = currentTimeSupplier.getAsLong();
      Packet<NeighborsPayload> neighborsPacket = Packet.createNeighbors(
          peerStream.limit(MAX_NUMBER_OF_NEIGHBORS).map((p) -> new Neighbor(p.nodeId(), p.endpoint().get())).collect(
              Collectors.toList()),
          now + EXPIRATION_PERIOD_MS,
          keyPair);
      packetSender.accept(peer.endpoint().get(), neighborsPacket);
    }
  }

  @VisibleForTesting
  void handleNeighbors(Packet<NeighborsPayload> neighborsPayloadPacket) {
    Peer peer = peerRepository.get(neighborsPayloadPacket.header().nodeId());
    if (peer.endpoint().isPresent() && peer.isActive()) {
      peer.updateLastSeen();
      for (Neighbor neighbor : neighborsPayloadPacket.payload().neighbors()) {
        peerRepository.get(neighbor.nodeId(), neighbor.endpoint());
      }
    }
  }

  @VisibleForTesting
  void handlePing(Packet<PingPayload> ping) {
    Peer peer = peerRepository.get(ping.header().nodeId(), ping.payload().from());
    peer.updateLastSeen();
    peer.endpoint().ifPresent(to -> {
      long now = currentTimeSupplier.getAsLong();
      packetSender.accept(to, Packet.createPong(to, ping.header().hash(), now + EXPIRATION_PERIOD_MS, keyPair));
    });
  }

  @VisibleForTesting
  void handlePong(Packet<PongPayload> pong) {
    if (awaitingPongs.remove(pong.payload().pingHash(), pong.header().nodeId())) {
      Peer peer = peerRepository.get(pong.header().nodeId());
      peer.updateLastSeen();
      peer.endpoint().ifPresent(to -> {
        if (peerRoutingTable.contains(peer)) {
          peer.setActive(to);
        }
      });
    }
  }

  void receivePacket(Bytes data) {
    PacketHeader header = PacketHeader.decode(data);
    Bytes payloadBytes = data.slice(98);

    switch (header.packetType()) {
      case 0x01:
        handlePing(new Packet<>(PingPayload.decode(payloadBytes), header));
        break;
      case 0x02:
        handlePong(new Packet<>(PongPayload.decode(payloadBytes), header));
        break;
      case 0x03:
        handleFindNeighbors(new Packet<>(FindNeighborsPayload.decode(payloadBytes), header));
        break;
      case 0x04:
        handleNeighbors(new Packet<>(NeighborsPayload.decode(payloadBytes), header));
        break;
      default:
        throw new PeerDiscoveryPacketDecodingException(String.format("Invalid packet type %02X", header.packetType()));
    }
  }
}
