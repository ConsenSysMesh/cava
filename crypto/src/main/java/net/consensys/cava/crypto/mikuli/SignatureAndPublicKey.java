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

public final class SignatureAndPublicKey {
  private final Signature signature;
  private final PublicKey publicKey;

  SignatureAndPublicKey(Signature signature, PublicKey pubKey) {
    if (signature == null || pubKey == null) {
      throw new NullPointerException("SignatureAndPublicKey was not properly initialized");
    }
    this.signature = signature;
    this.publicKey = pubKey;
  }

  public PublicKey publicKey() {
    return publicKey;
  }

  public Signature signature() {
    return signature;
  }

  public SignatureAndPublicKey combine(SignatureAndPublicKey sigAndPubKey) {
    Signature newSignature = signature.combine(sigAndPubKey.signature);
    PublicKey newPubKey = publicKey.combine(sigAndPubKey.publicKey);
    return new SignatureAndPublicKey(newSignature, newPubKey);
  }
}
