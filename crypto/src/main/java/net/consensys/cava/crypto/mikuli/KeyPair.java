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

public final class KeyPair {

  private final PrivateKey privateKey;
  private final PublicKey publicKey;

  KeyPair(PrivateKey privateKey, PublicKey publicKey) {
    if (privateKey == null || publicKey == null) {
      throw new NullPointerException("KeyPair was not properly initialized");
    }
    this.privateKey = privateKey;
    this.publicKey = publicKey;
  }

  public PublicKey publicKey() {
    return publicKey;
  }

  public PrivateKey privateKey() {
    return privateKey;
  }
}
