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
import net.consensys.cava.rlp.RLP;

import com.google.common.base.Objects;

final class PingPayload extends Payload {

  private static final int VERSION = 4;

  static PingPayload decode(Bytes payloadBytes) {
    return RLP.decodeList(payloadBytes, (listReader) -> {
      int version = listReader.readInt();
      if (version != VERSION) {
        throw new PeerDiscoveryPacketDecodingException(
            String.format("Version mismatch in ping packet. Expected: %s, got: %s.", VERSION, version));
      }
      Endpoint from = listReader.readList(Endpoint::read);
      Endpoint to = listReader.readList(Endpoint::read);
      long expiration = listReader.readLong();
      return new PingPayload(from, to, expiration);
    });
  }

  private final Endpoint from;
  private final Endpoint to;

  PingPayload(Endpoint from, Endpoint to, long expiration) {
    super(expiration);
    this.from = from;
    this.to = to;
  }

  @Override
  public Bytes createPayloadBytes() {
    return RLP.encodeList(listWriter -> {
      listWriter.writeInt(VERSION);
      listWriter.writeList(from::write);
      listWriter.writeList(to::write);
      listWriter.writeLong(expiration());
    });
  }

  public Endpoint from() {
    return from;
  }

  public Endpoint to() {
    return to;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (!(o instanceof PingPayload)) {
      return false;
    }
    PingPayload that = (PingPayload) o;
    return Objects.equal(from, that.from) && Objects.equal(to, that.to) && expiration() == that.expiration();
  }

  @Override
  public int hashCode() {
    return Objects.hashCode(from, to, expiration());
  }

  @Override
  public String toString() {
    return "PingPayload{" + "from=" + from + ", to=" + to + '}';
  }
}
