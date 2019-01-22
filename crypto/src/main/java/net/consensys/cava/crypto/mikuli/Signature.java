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

import static java.util.Objects.requireNonNull;

import net.consensys.cava.crypto.mikuli.group.G2Point;

import com.google.common.base.Objects;

/**
 * This class represents a Signature on G2
 */
public final class Signature {
  private final G2Point point;

  Signature(G2Point point) {
    this.point = point;
  }

  @Override
  public String toString() {
    return "Signature [ecpPoint=" + point.toString() + "]";
  }

  public Signature combine(Signature sig) {
    return new Signature(point.add(sig.point));
  }

  /**
   * Signature serialization
   * 
   * @return byte array representation of the signature, not null
   */
  public byte[] encode() {
    return point.toBytes();
  }

  public static Signature decode(byte[] bytes) {
    G2Point point = G2Point.fromBytes(bytes);
    return new Signature(point);
  }

  @Override
  public int hashCode() {
    final int prime = 31;
    int result = 1;
    result = prime * result + ((point == null) ? 0 : point.hashCode());
    return result;
  }

  @Override
  public boolean equals(Object obj) {
    requireNonNull(obj);
    if (this == obj)
      return true;
    if (!(obj instanceof Signature))
      return false;
    Signature other = (Signature) obj;
    return Objects.equal(point, other.point);
  }

  G2Point g2Point() {
    return point;
  }
}
