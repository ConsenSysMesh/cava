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

final class FindNeighborsPayload extends Payload {

  static FindNeighborsPayload decode(Bytes payloadBytes) {
    return RLP.decodeList(payloadBytes, (listReader) -> {
      Bytes target = listReader.readValue();
      long expiration = listReader.readLong();
      return new FindNeighborsPayload(target, expiration);
    });
  }

  private final Bytes target;

  FindNeighborsPayload(Bytes target, long expiration) {
    super(expiration);
    this.target = target;
  }

  public Bytes target() {
    return target;
  }

  @Override
  public Bytes createPayloadBytes() {
    return RLP.encodeList(writer -> {
      writer.writeValue(target);
      writer.writeLong(expiration());
    });
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (!(o instanceof FindNeighborsPayload)) {
      return false;
    }
    FindNeighborsPayload that = (FindNeighborsPayload) o;
    return expiration() == that.expiration() && Objects.equal(target, that.target);
  }

  @Override
  public int hashCode() {
    return Objects.hashCode(target, expiration());
  }

  @Override
  public String toString() {
    return "FindNeighborsPayload{" + "target=" + target + '}';
  }
}
