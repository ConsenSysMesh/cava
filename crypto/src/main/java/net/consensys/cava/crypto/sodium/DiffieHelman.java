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
package net.consensys.cava.crypto.sodium;

import net.consensys.cava.bytes.Bytes;

/**
 * Sodium provides an API to perform scalar multiplication of elliptic curve points.
 * <p>
 * This can be used as a building block to construct key exchange mechanisms, or more generally to compute a public key
 * from a secret key.
 * <p>
 * On current libsodium versions, you generally want to use the crypto_kx API for key exchange instead.
 * 
 * @see KeyExchange
 */
public final class DiffieHelman {

  /**
   * This function can be used to compute a shared secret q given a user's secret key and another user's public key.
   * <p>
   * The secret key is crypto_scalarmult_SCALARBYTES bytes long, the public key and the output are
   * crypto_scalarmult_BYTES bytes long.
   * <p>
   * The secret represents the X coordinate of a point on the curve. As a result, the number of possible keys is limited
   * to the group size (≈2^252), which is smaller than the key space.
   *
   * @param secretKey the secret key to compute the scalar
   * @param publicKey the public key to compute the scalar
   * @return the shared secret between the two keys
   */
  public static Bytes scalarMultiply(Box.SecretKey secretKey, Box.PublicKey publicKey) {
    return Bytes.wrap(scalarMultiply(secretKey.bytesArray(), publicKey.bytesArray()));
  }

  /**
   * This function can be used to compute a shared secret q given a user's secret key and another user's public key.
   * <p>
   * The secret key is crypto_scalarmult_SCALARBYTES bytes long, the public key and the output are
   * crypto_scalarmult_BYTES bytes long.
   * <p>
   * The secret represents the X coordinate of a point on the curve. As a result, the number of possible keys is limited
   * to the group size (≈2^252), which is smaller than the key space.
   *
   * @param secretKey the secret key to compute the scalar
   * @param publicKey the public key to compute the scalar
   * @return the shared secret between the two keys
   */
  public static byte[] scalarMultiply(byte[] secretKey, byte[] publicKey) {
    byte[] sharedSecret = new byte[(int) Sodium.crypto_scalarmult_bytes()];
    Sodium.crypto_scalarmult(sharedSecret, secretKey, publicKey);
    return sharedSecret;
  }
}
