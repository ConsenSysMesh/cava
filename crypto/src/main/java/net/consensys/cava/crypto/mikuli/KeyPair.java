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

import net.consensys.cava.crypto.mikuli.group.G1Point;
import net.consensys.cava.crypto.mikuli.group.Scalar;

import org.apache.milagro.amcl.BLS381.BIG;
import org.apache.milagro.amcl.BLS381.ECP;
import org.apache.milagro.amcl.BLS381.ROM;
import org.apache.milagro.amcl.RAND;

/**
 * KeyPair represents a public and private key.
 */
public final class KeyPair {

  private final PrivateKey privateKey;
  private final PublicKey publicKey;
  static public final G1Point g1Generator = new G1Point(ECP.generator());
  static public final BIG curveOrder = new BIG(ROM.CURVE_Order);

  private KeyPair(PrivateKey privateKey, PublicKey publicKey) {
    this.privateKey = privateKey;
    this.publicKey = publicKey;
  }

  static public KeyPair random() {
    RAND rng = new RAND();
    Scalar secret = new Scalar(BIG.randomnum(curveOrder, rng));

    PrivateKey privateKey = new PrivateKey(secret);
    G1Point g1Point = g1Generator.mul(secret);
    PublicKey publicKey = new PublicKey(g1Point);
    return new KeyPair(privateKey, publicKey);
  }

  public PublicKey publicKey() {
    return publicKey;
  }

  public PrivateKey privateKey() {
    return privateKey;
  }
}
