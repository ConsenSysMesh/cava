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
 * HMAC (Hash-based message authentication code) support.
 * <p>
 * The keyed message authentication codes HMAC-SHA-256, HMAC-SHA-512 and HMAC-SHA512-256 (truncated HMAC-SHA-512) are
 * provided.'
 * <p>
 * This API supports generating and verifying MACs.
 */
public final class MessageAuthenticationCodes {

  /**
   * Message authentication code support for HMAC-SHA-256.
   */
  public static final class HMACSHA256 {

    private HMACSHA256() {}

    /**
     * Authenticates a message using a secret into a HMAC-SHA-256 authenticator.
     *
     * @param message the message to authenticate
     * @param secret the secret to use for authentication
     * @return the authenticator of the message
     */
    public static Bytes authenticate(Bytes message, Bytes secret) {
      return Bytes.wrap(authenticate(message.toArrayUnsafe(), secret.toArrayUnsafe()));
    }

    /**
     * Authenticates a message using a secret into a HMAC-SHA-256 authenticator.
     *
     * @param message the message to authenticate
     * @param secret the secret to use for authentication
     * @return the authenticator of the message
     */
    public static byte[] authenticate(byte[] message, byte[] secret) {
      if (secret.length != Sodium.crypto_auth_hmacsha256_keybytes()) {
        throw new IllegalArgumentException(
            "Expected secret of "
                + Sodium.crypto_auth_hmacsha256_keybytes()
                + " bytes, got "
                + secret.length
                + " instead");
      }
      byte[] out = new byte[(int) Sodium.crypto_auth_hmacsha256_bytes()];
      Sodium.crypto_auth_hmacsha256(out, message, message.length, secret);
      return out;
    }

    /**
     * Verifies the authenticator of a message matches according to a secret.
     *
     * @param authenticator the authenticator to verify
     * @param in the message to match against the authenticator
     * @param secret the secret to use for verification
     * @return true if the authenticator verifies the message according to the secret, false otherwise
     */
    public static boolean verify(Bytes authenticator, Bytes in, Bytes secret) {
      return verify(authenticator.toArrayUnsafe(), in.toArrayUnsafe(), secret.toArrayUnsafe());
    }

    /**
     * Verifies the authenticator of a message matches according to a secret.
     *
     * @param authenticator the authenticator to verify
     * @param in the message to match against the authenticator
     * @param secret the secret to use for verification
     * @return true if the authenticator verifies the message according to the secret, false otherwise
     */
    public static boolean verify(byte[] authenticator, byte[] in, byte[] secret) {
      if (secret.length != Sodium.crypto_auth_hmacsha256_keybytes()) {
        throw new IllegalArgumentException(
            "Expected secret of "
                + Sodium.crypto_auth_hmacsha256_keybytes()
                + " bytes, got "
                + secret.length
                + " instead");
      }
      if (authenticator.length != Sodium.crypto_auth_hmacsha256_bytes()) {
        throw new IllegalArgumentException(
            "Expected authenticator of "
                + Sodium.crypto_auth_hmacsha256_bytes()
                + " bytes, got "
                + authenticator.length
                + " instead");
      }
      int result = Sodium.crypto_auth_hmacsha256_verify(authenticator, in, in.length, secret);
      return result == 0;
    }
  }

  /**
   * Message authentication code support for HMAC-SHA-512.
   */
  public static final class HMACSHA512 {

    private HMACSHA512() {}

    /**
     * Authenticates a message using a secret into a HMAC-SHA-512 authenticator.
     *
     * @param message the message to authenticate
     * @param secret the secret to use for authentication
     * @return the authenticator of the message
     */
    public static Bytes authenticate(Bytes message, Bytes secret) {
      return Bytes.wrap(authenticate(message.toArrayUnsafe(), secret.toArrayUnsafe()));
    }

    /**
     * Authenticates a message using a secret into a HMAC-SHA-512 authenticator.
     *
     * @param message the message to authenticate
     * @param secret the secret to use for authentication
     * @return the authenticator of the message
     */
    public static byte[] authenticate(byte[] message, byte[] secret) {
      if (secret.length != Sodium.crypto_auth_hmacsha512_keybytes()) {
        throw new IllegalArgumentException(
            "Expected secret of "
                + Sodium.crypto_auth_hmacsha512_keybytes()
                + " bytes, got "
                + secret.length
                + " instead");
      }
      byte[] out = new byte[(int) Sodium.crypto_auth_hmacsha512_bytes()];
      Sodium.crypto_auth_hmacsha512(out, message, message.length, secret);
      return out;
    }

    /**
     * Verifies the authenticator of a message matches according to a secret.
     *
     * @param authenticator the authenticator to verify
     * @param in the message to match against the authenticator
     * @param secret the secret to use for verification
     * @return true if the authenticator verifies the message according to the secret, false otherwise
     */
    public static boolean verify(Bytes authenticator, Bytes in, Bytes secret) {
      return verify(authenticator.toArrayUnsafe(), in.toArrayUnsafe(), secret.toArrayUnsafe());
    }

    /**
     * Verifies the authenticator of a message matches according to a secret.
     *
     * @param authenticator the authenticator to verify
     * @param in the message to match against the authenticator
     * @param secret the secret to use for verification
     * @return true if the authenticator verifies the message according to the secret, false otherwise
     */
    public static boolean verify(byte[] authenticator, byte[] in, byte[] secret) {
      if (secret.length != Sodium.crypto_auth_hmacsha512_keybytes()) {
        throw new IllegalArgumentException(
            "Expected secret of "
                + Sodium.crypto_auth_hmacsha512_keybytes()
                + " bytes, got "
                + secret.length
                + " instead");
      }
      if (authenticator.length != Sodium.crypto_auth_hmacsha512_bytes()) {
        throw new IllegalArgumentException(
            "Expected authenticator of "
                + Sodium.crypto_auth_hmacsha512_bytes()
                + " bytes, got "
                + authenticator.length
                + " instead");
      }
      int result = Sodium.crypto_auth_hmacsha512_verify(authenticator, in, in.length, secret);
      return result == 0;
    }
  }

  /**
   * Message authentication code support for HMAC-SHA-512-256.
   * <p>
   * HMAC-SHA-512-256 is implemented as HMAC-SHA-512 with the output truncated to 256 bits. This is slightly faster than
   * HMAC-SHA-256. Note that this construction is not the same as HMAC-SHA-512/256, which is HMAC using the SHA-512/256
   * function.
   */
  public static final class HMACSHA512256 {

    private HMACSHA512256() {}

    /**
     * Authenticates a message using a secret into a HMAC-SHA-512-256 authenticator.
     *
     * @param message the message to authenticate
     * @param secret the secret to use for authentication
     * @return the authenticator of the message
     */
    public static Bytes authenticate(Bytes message, Bytes secret) {
      return Bytes.wrap(authenticate(message.toArrayUnsafe(), secret.toArrayUnsafe()));
    }

    /**
     * Authenticates a message using a secret into a HMAC-SHA-512-256 authenticator.
     *
     * @param message the message to authenticate
     * @param secret the secret to use for authentication
     * @return the authenticator of the message
     */
    public static byte[] authenticate(byte[] message, byte[] secret) {
      if (secret.length != Sodium.crypto_auth_hmacsha512256_keybytes()) {
        throw new IllegalArgumentException(
            "Expected secret of "
                + Sodium.crypto_auth_hmacsha512256_keybytes()
                + " bytes, got "
                + secret.length
                + " instead");
      }
      byte[] out = new byte[(int) Sodium.crypto_auth_hmacsha512256_bytes()];
      Sodium.crypto_auth_hmacsha512256(out, message, message.length, secret);
      return out;
    }

    /**
     * Verifies the authenticator of a message matches according to a secret.
     *
     * @param authenticator the authenticator to verify
     * @param in the message to match against the authenticator
     * @param secret the secret to use for verification
     * @return true if the authenticator verifies the message according to the secret, false otherwise
     */
    public static boolean verify(Bytes authenticator, Bytes in, Bytes secret) {
      return verify(authenticator.toArrayUnsafe(), in.toArrayUnsafe(), secret.toArrayUnsafe());
    }

    /**
     * Verifies the authenticator of a message matches according to a secret.
     *
     * @param authenticator the authenticator to verify
     * @param in the message to match against the authenticator
     * @param secret the secret to use for verification
     * @return true if the authenticator verifies the message according to the secret, false otherwise
     */
    public static boolean verify(byte[] authenticator, byte[] in, byte[] secret) {
      if (secret.length != Sodium.crypto_auth_hmacsha512256_keybytes()) {
        throw new IllegalArgumentException(
            "Expected secret of "
                + Sodium.crypto_auth_hmacsha512256_keybytes()
                + " bytes, got "
                + secret.length
                + " instead");
      }
      if (authenticator.length != Sodium.crypto_auth_hmacsha512256_bytes()) {
        throw new IllegalArgumentException(
            "Expected authenticator of "
                + Sodium.crypto_auth_hmacsha512256_bytes()
                + " bytes, got "
                + authenticator.length
                + " instead");
      }
      int result = Sodium.crypto_auth_hmacsha512256_verify(authenticator, in, in.length, secret);
      return result == 0;
    }
  }
}
