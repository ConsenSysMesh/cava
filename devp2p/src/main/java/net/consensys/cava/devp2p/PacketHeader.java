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

import static com.google.common.base.Preconditions.checkArgument;

import net.consensys.cava.bytes.Bytes;
import net.consensys.cava.crypto.Hash;
import net.consensys.cava.crypto.SECP256K1;
import net.consensys.cava.crypto.SECP256K1.KeyPair;
import net.consensys.cava.crypto.SECP256K1.PublicKey;
import net.consensys.cava.crypto.SECP256K1.Signature;

import com.google.common.base.Objects;

final class PacketHeader {

  static PacketHeader decode(Bytes data) {
    Bytes hash = data.slice(0, 32);
    Bytes encodedSignature = data.slice(32, 65);
    Signature signature;
    try {
      signature = Signature.fromBytes(encodedSignature);
    } catch (IllegalArgumentException e) {
      throw new PeerDiscoveryPacketDecodingException(
          "Could not retrieve the public key from the signature and signed data");
    }
    Bytes signedPayload = data.slice(97, data.size() - 97);
    byte typeByte = data.get(97);
    Bytes payloadBytes = data.slice(98);

    PublicKey publicKey = PublicKey.recoverFromSignature(signedPayload, signature);
    if (publicKey == null) {
      throw new PeerDiscoveryPacketDecodingException(
          "Could not retrieve the public key from the signature and signed data");
    }
    Bytes computedHash = Hash.sha3_256(Bytes.wrap(Bytes.wrap(signature.bytes(), Bytes.of(typeByte)), payloadBytes));
    if (!computedHash.equals(hash)) {
      throw new PeerDiscoveryPacketDecodingException("Hash does not match content");
    }

    return new PacketHeader(hash, publicKey, signature, typeByte);
  }

  private final Bytes hash;
  private final PublicKey publicKey;
  private final Signature signature;
  private final byte packetType;

  PacketHeader(KeyPair keyPair, byte packetType, Bytes payload) {
    checkArgument(keyPair != null, "keyPair cannot be null");
    checkArgument(payload != null, "payload cannot be null");
    Bytes typeBytes = Bytes.of(packetType);
    this.packetType = packetType;
    this.signature = SECP256K1.sign(Bytes.wrap(typeBytes, payload), keyPair);
    this.hash = Hash.sha3_256(Bytes.wrap(signature.bytes(), typeBytes, payload));
    this.publicKey = keyPair.publicKey();
  }

  PacketHeader(Bytes hash, PublicKey publicKey, Signature signature, byte packetType) {
    checkArgument(hash != null, "hash cannot be null");
    checkArgument(hash.size() == 32, String.format("hash should be 32 bytes long, got %s instead", hash.size()));
    checkArgument(publicKey != null, "publicKey cannot be null");
    checkArgument(signature != null, "signature cannot be null");
    this.hash = hash;
    this.publicKey = publicKey;
    this.signature = signature;
    this.packetType = packetType;
  }

  Bytes hash() {
    return hash;
  }

  Bytes nodeId() {
    return publicKey.bytes();
  }

  byte packetType() {
    return packetType;
  }

  Signature signature() {
    return signature;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (!(o instanceof PacketHeader)) {
      return false;
    }
    PacketHeader that = (PacketHeader) o;
    return packetType == that.packetType
        && Objects.equal(hash, that.hash)
        && Objects.equal(publicKey, that.publicKey)
        && Objects.equal(signature, that.signature);
  }

  @Override
  public int hashCode() {
    return Objects.hashCode(hash, publicKey, signature, packetType);
  }
}
