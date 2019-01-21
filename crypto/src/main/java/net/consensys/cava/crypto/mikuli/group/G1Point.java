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
package net.consensys.cava.crypto.mikuli.group;

import org.apache.milagro.amcl.BLS381.BIG;
import org.apache.milagro.amcl.BLS381.ECP;

public final class G1Point implements Group<G1Point> {
  final ECP point;
  private static final int fpPointSize = BIG.MODBYTES;

  public G1Point(ECP point) {
    if (point == null) {
      throw new NullPointerException("ECP point is null");
    }
    this.point = point;
  }

  @Override
  public G1Point add(G1Point other) {
    ECP sum = new ECP();
    sum.add(point);
    sum.add(other.point);
    sum.affine();
    return new G1Point(sum);
  }

  @Override
  public G1Point mul(Scalar scalar) {
    ECP newPoint = point.mul(scalar.value());
    return new G1Point(newPoint);
  }

  /**
   * @return byte[] the byte array representation of compressed point in G1
   */
  public byte[] toBytes() {
    // Size of the byte array representing compressed ECP point for BLS12-381 is
    // 49 bytes in milagro
    // size of the point = 48 bytes
    // meta information (parity bit, curve type etc) = 1 byte
    byte[] bytes = new byte[fpPointSize + 1];
    point.toBytes(bytes, true);
    return bytes;
  }

  public static G1Point fromBytes(byte[] bytes) {
    return new G1Point(ECP.fromBytes(bytes));
  }

  ECP ecpPoint() {
    return point;
  }

  @Override
  public String toString() {
    return point.toString();
  }

  @Override
  public int hashCode() {
    final int prime = 31;
    int result = 1;
    long x = point.getX().norm();
    long y = point.getY().norm();
    result = prime * result + (int) (x ^ (x >>> 32));
    result = prime * result + (int) (y ^ (y >>> 32));
    return result;
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj)
      return true;
    if (obj == null)
      return false;
    if (getClass() != obj.getClass())
      return false;
    G1Point other = (G1Point) obj;
    if (point == null) {
      if (other.point != null)
        return false;
    } else if (!point.equals(other.point))
      return false;
    return true;
  }
}
