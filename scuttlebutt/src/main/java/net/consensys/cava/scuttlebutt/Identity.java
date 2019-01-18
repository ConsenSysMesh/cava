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
package net.consensys.cava.scuttlebutt;

import net.consensys.cava.bytes.Bytes;
import net.consensys.cava.crypto.sodium.Signer;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.Objects;

/**
 * Scuttlebutt identity, based on a Ed25519 key pair.
 */
public final class Identity {

  public static Identity fromKeyPair(Signer.KeyPair keyPair) {
    return new Identity(keyPair);
  }

  public static Identity fromSecretKey(Signer.SecretKey secretKey) {
    return fromKeyPair(Signer.KeyPair.forSecretKey(secretKey));
  }

  public static Identity random() {
    return new Identity(Signer.KeyPair.random());
  }

  private final Signer.KeyPair keyPair;

  private Identity(Signer.KeyPair keyPair) {
    this.keyPair = keyPair;
  }

  public Bytes sign(Bytes message) {
    return Signer.signDetached(message, keyPair.secretKey());
  }

  public boolean verify(Bytes signature, Bytes message) {
    return Signer.verifyDetached(message, signature, keyPair.publicKey());
  }

  @Override
  public boolean equals(Object o) {
    if (this == o)
      return true;
    if (o == null || getClass() != o.getClass())
      return false;
    Identity identity = (Identity) o;
    return keyPair.equals(identity.keyPair);
  }

  @Override
  public int hashCode() {
    return Objects.hash(keyPair);
  }

  @Override
  public String toString() {
    try {
      StringBuilder builder = new StringBuilder();
      builder.append("@");
      keyPair.publicKey().bytes().appendHexTo(builder);
      builder.append(".ed25519");
      return builder.toString().toLowerCase();
    } catch (IOException e) {
      throw new UncheckedIOException(e);
    }
  }
}
