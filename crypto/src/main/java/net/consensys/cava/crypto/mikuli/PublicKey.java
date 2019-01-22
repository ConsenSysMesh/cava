/*
 * Copyright 2019 ConsenSys AG.
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
package net.consensys.cava.crypto.mikuli;

import net.consensys.cava.bytes.Bytes;

import java.util.List;
import java.util.Objects;

/**
 * This class represents a BLS12-381 public key.
 */
public final class PublicKey {

  private final G1Point point;

  PublicKey(G1Point point) {
    this.point = point;
  }

  PublicKey combine(PublicKey pk) {
    return new PublicKey(point.add(pk.point));
  }

  /**
   * Public key serialization
   * 
   * @return byte array representation of the public key
   */
  public byte[] toByteArray() {
    return point.toByteArray();
  }

  /**
   * Public key serialization
   * 
   * @return byte array representation of the public key
   */
  public Bytes toBytes() {
    return Bytes.wrap(point.toByteArray());
  }

  public static PublicKey fromBytes(byte[] bytes) {
    G1Point point = G1Point.fromBytes(bytes);
    return new PublicKey(point);
  }

  public static PublicKey fromBytes(Bytes bytes) {
    return fromBytes(bytes.toArray());
  }

  @Override
  public int hashCode() {
    final int prime = 31;
    int result = 1;
    result = prime * result + Objects.hashCode(point);
    return result;
  }

  @Override
  public boolean equals(Object obj) {
    if (Objects.isNull(obj)) {
      return false;
    }
    if (this == obj) {
      return true;
    }
    if (!(obj instanceof PublicKey)) {
      return false;
    }
    PublicKey other = (PublicKey) obj;
    return point.equals(other.point);
  }

  public G1Point g1Point() {
    return point;
  }

  /**
   * Aggregates list of PublicKey pairs
   * 
   * @param keys The list of public keys to aggregate, not null
   * @return PublicKey The public key, not null
   * @throws IllegalArgumentException if parameter list is empty
   */
  public static PublicKey aggregate(List<PublicKey> keys) {
    if (keys.isEmpty()) {
      throw new IllegalArgumentException("Parameter list is empty");
    }
    return keys.stream().reduce((a, b) -> a.combine(b)).get();
  }
}
