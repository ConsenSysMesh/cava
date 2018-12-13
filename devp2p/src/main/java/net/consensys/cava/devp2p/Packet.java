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


import net.consensys.cava.bytes.Bytes;
import net.consensys.cava.crypto.SECP256K1.KeyPair;

import java.util.List;

final class Packet<T extends Payload> {

  static Packet<PingPayload> createPing(Endpoint from, Endpoint to, long expiration, KeyPair keyPair) {
    return new Packet<>(new PingPayload(from, to, expiration), keyPair, (byte) 0x01);
  }

  static Packet<PongPayload> createPong(Endpoint to, Bytes pingHash, long expiration, KeyPair keyPair) {
    return new Packet<>(new PongPayload(to, pingHash, expiration), keyPair, (byte) 0x02);
  }

  static Packet<FindNeighborsPayload> createFindNeighbors(Bytes target, long expiration, KeyPair keyPair) {
    return new Packet<>(new FindNeighborsPayload(target, expiration), keyPair, (byte) 0x03);
  }

  static Packet<NeighborsPayload> createNeighbors(
      List<NeighborsPayload.Neighbor> neighbors,
      long expiration,
      KeyPair keyPair) {
    return new Packet<>(new NeighborsPayload(neighbors, expiration), keyPair, (byte) 0x04);
  }

  private PacketHeader header;
  private final T payload;
  private Bytes bytes;
  private KeyPair keyPair;
  private byte packetType;

  private Packet(T payload, KeyPair keyPair, byte packetType) {
    this.payload = payload;
    this.keyPair = keyPair;
    this.packetType = packetType;
  }

  Packet(T payload, PacketHeader header) {
    this.payload = payload;
    this.header = header;
  }

  PacketHeader header() {
    PacketHeader header = this.header;
    if (header == null) {
      header = new PacketHeader(keyPair, packetType, payload.toBytes());
      this.header = header;
    }
    return header;
  }

  public T payload() {
    return payload;
  }

  Bytes toBytes() {
    Bytes packetBytes = bytes;
    if (packetBytes == null) {
      packetBytes = Bytes.concatenate(
          header().hash(),
          header().signature().bytes(),
          Bytes.of(header().packetType()),
          payload.toBytes());
      bytes = packetBytes;
    }
    return packetBytes;
  }

  @Override
  public String toString() {
    return "Packet{" + "packetType=" + packetType + ", payload=" + payload + '}';
  }
}
