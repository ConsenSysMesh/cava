/*
 * Copyright 2018, ConsenSys Inc.
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

import java.util.Optional;

import jnr.ffi.Pointer;

// Documentation copied under the ISC License, from
// https://github.com/jedisct1/libsodium-doc/blob/424b7480562c2e063bc8c52c452ef891621c8480/secret-key_cryptography/authenticated_encryption.md

/**
 * Secret-key authenticated encryption.
 *
 * <p>
 * Encrypts a message with a key and a nonce to keep it confidential, and computes an authentication tag. The tag is
 * used to make sure that the message hasn't been tampered with before decrypting it.
 *
 * <p>
 * A single key is used both to encrypt/sign and verify/decrypt messages. For this reason, it is critical to keep the
 * key confidential.
 *
 * <p>
 * The nonce doesn't have to be confidential, but it should never ever be reused with the same key. The easiest way to
 * generate a nonce is to use randombytes_buf().
 *
 * <p>
 * Messages encrypted are assumed to be independent. If multiple messages are sent using this API and random nonces,
 * there will be no way to detect if a message has been received twice, or if messages have been reordered.
 *
 * <p>
 * This class depends upon the JNR-FFI library being available on the classpath, along with its dependencies. See
 * https://github.com/jnr/jnr-ffi. JNR-FFI can be included using the gradle dependency 'com.github.jnr:jnr-ffi'.
 */
public final class SecretBox {
  private SecretBox() {}

  /**
   * A SecretBox key.
   */
  public static final class Key {
    private final Pointer ptr;

    private Key(Pointer ptr) {
      this.ptr = ptr;
    }

    @Override
    protected void finalize() {
      Sodium.sodium_free(ptr);
    }

    /**
     * Create a {@link Key} from an array of bytes.
     *
     * <p>
     * The byte array must be of length {@link #length()}.
     *
     * @param bytes The bytes for the key.
     * @return A key, based on the supplied bytes.
     */
    public static Key forBytes(Bytes bytes) {
      return forBytes(bytes.toArrayUnsafe());
    }

    /**
     * Create a {@link Key} from an array of bytes.
     *
     * <p>
     * The byte array must be of length {@link #length()}.
     *
     * @param bytes The bytes for the key.
     * @return A key, based on the supplied bytes.
     */
    public static Key forBytes(byte[] bytes) {
      if (bytes.length != Sodium.crypto_secretbox_keybytes()) {
        throw new IllegalArgumentException(
            "key must be " + Sodium.crypto_secretbox_keybytes() + " bytes, got " + bytes.length);
      }
      return Sodium.dup(bytes, Key::new);
    }

    /**
     * Obtain the length of the key in bytes (32).
     *
     * @return The length of the key in bytes (32).
     */
    public static int length() {
      long keybytes = Sodium.crypto_secretbox_keybytes();
      if (keybytes > Integer.MAX_VALUE) {
        throw new SodiumException("crypto_secretbox_keybytes: " + keybytes + " is too large");
      }
      return (int) keybytes;
    }

    /**
     * Generate a new key using a random generator.
     *
     * @return A randomly generated key.
     */
    public static Key random() {
      Pointer ptr = Sodium.malloc(length());
      try {
        Sodium.crypto_secretbox_keygen(ptr);
        return new Key(ptr);
      } catch (Throwable e) {
        Sodium.sodium_free(ptr);
        throw e;
      }
    }

    /**
     * @return The bytes of this key.
     */
    public Bytes bytes() {
      return Bytes.wrap(bytesArray());
    }

    /**
     * @return The bytes of this key.
     */
    public byte[] bytesArray() {
      return Sodium.reify(ptr, length());
    }
  }

  /**
   * A SecretBox nonce.
   */
  public static final class Nonce {
    private final Pointer ptr;

    private Nonce(Pointer ptr) {
      this.ptr = ptr;
    }

    @Override
    protected void finalize() {
      Sodium.sodium_free(ptr);
    }

    /**
     * Create a {@link Nonce} from an array of bytes.
     *
     * <p>
     * The byte array must be of length {@link #length()}.
     *
     * @param bytes The bytes for the nonce.
     * @return A nonce, based on these bytes.
     */
    public static Nonce forBytes(Bytes bytes) {
      return forBytes(bytes.toArrayUnsafe());
    }

    /**
     * Create a {@link Nonce} from an array of bytes.
     *
     * <p>
     * The byte array must be of length {@link #length()}.
     *
     * @param bytes The bytes for the nonce.
     * @return A nonce, based on these bytes.
     */
    public static Nonce forBytes(byte[] bytes) {
      if (bytes.length != Sodium.crypto_secretbox_noncebytes()) {
        throw new IllegalArgumentException(
            "nonce must be " + Sodium.crypto_secretbox_noncebytes() + " bytes, got " + bytes.length);
      }
      return Sodium.dup(bytes, Nonce::new);
    }

    /**
     * Obtain the length of the nonce in bytes (24).
     *
     * @return The length of the nonce in bytes (24).
     */
    public static int length() {
      long noncebytes = Sodium.crypto_secretbox_noncebytes();
      if (noncebytes > Integer.MAX_VALUE) {
        throw new SodiumException("crypto_secretbox_noncebytes: " + noncebytes + " is too large");
      }
      return (int) noncebytes;
    }

    /**
     * Generate a new {@link Nonce} using a random generator.
     *
     * @return A randomly generated nonce.
     */
    public static Nonce random() {
      return Sodium.randomBytes(length(), Nonce::new);
    }

    /**
     * Increment this nonce.
     *
     * <p>
     * Note that this is not synchronized. If multiple threads are creating encrypted messages and incrementing this
     * nonce, then external synchronization is required to ensure no two encrypt operations use the same nonce.
     *
     * @return A new {@link Nonce}.
     */
    public Nonce increment() {
      return Sodium.dupAndIncrement(ptr, length(), Nonce::new);
    }

    /**
     * @return The bytes of this nonce.
     */
    public Bytes bytes() {
      return Bytes.wrap(bytesArray());
    }

    /**
     * @return The bytes of this nonce.
     */
    public byte[] bytesArray() {
      return Sodium.reify(ptr, length());
    }
  }

  /**
   * Encrypt a message for a given key.
   *
   * @param message The message to encrypt.
   * @param key The key to encrypt for.
   * @param nonce A unique nonce.
   * @return The encrypted data.
   */
  public static Bytes encrypt(Bytes message, Key key, Nonce nonce) {
    return Bytes.wrap(encrypt(message.toArrayUnsafe(), key, nonce));
  }

  /**
   * Encrypt a message for a given key.
   *
   * @param message The message to encrypt.
   * @param key The key to encrypt for.
   * @param nonce A unique nonce.
   * @return The encrypted data.
   */
  public static byte[] encrypt(byte[] message, Key key, Nonce nonce) {
    byte[] cipherText = new byte[combinedCypherTextLength(message)];

    int rc = Sodium.crypto_secretbox_easy(cipherText, message, message.length, nonce.ptr, key.ptr);
    if (rc != 0) {
      throw new SodiumException("crypto_secretbox_easy: failed with result " + rc);
    }

    return cipherText;
  }

  private static int combinedCypherTextLength(byte[] message) {
    long macbytes = Sodium.crypto_secretbox_macbytes();
    if (macbytes > Integer.MAX_VALUE) {
      throw new IllegalStateException("crypto_secretbox_macbytes: " + macbytes + " is too large");
    }
    return (int) macbytes + message.length;
  }

  /**
   * Encrypt a message for a given key, generating a detached message authentication code.
   *
   * @param message The message to encrypt.
   * @param key The key to encrypt for.
   * @param nonce A unique nonce.
   * @return The encrypted data and message authentication code.
   */
  public static DetachedEncryptionResult encryptDetached(Bytes message, Key key, Nonce nonce) {
    return encryptDetached(message.toArrayUnsafe(), key, nonce);
  }

  /**
   * Encrypt a message for a given key, generating a detached message authentication code.
   *
   * @param message The message to encrypt.
   * @param key The key to encrypt for.
   * @param nonce A unique nonce.
   * @return The encrypted data and message authentication code.
   */
  public static DetachedEncryptionResult encryptDetached(byte[] message, Key key, Nonce nonce) {
    byte[] cipherText = new byte[message.length];
    long macbytes = Sodium.crypto_secretbox_macbytes();
    if (macbytes > Integer.MAX_VALUE) {
      throw new IllegalStateException("crypto_secretbox_macbytes: " + macbytes + " is too large");
    }
    byte[] mac = new byte[(int) macbytes];

    int rc = Sodium.crypto_secretbox_detached(cipherText, mac, message, message.length, nonce.ptr, key.ptr);
    if (rc != 0) {
      throw new SodiumException("crypto_secretbox_detached: failed with result " + rc);
    }

    return new DefaultDetachedEncryptionResult(cipherText, mac);
  }

  /**
   * Decrypt a message using a given key.
   *
   * @param cipherText The cipher text to decrypt.
   * @param key The key to use for decryption.
   * @param nonce The nonce that was used for encryption.
   * @return The decrypted data, or {@code Optional.empty()} if verification failed.
   */
  public static Optional<Bytes> decrypt(Bytes cipherText, Key key, Nonce nonce) {
    return decrypt(cipherText.toArrayUnsafe(), key, nonce).map(Bytes::wrap);
  }

  /**
   * Decrypt a message using a given key.
   *
   * @param cipherText The cipher text to decrypt.
   * @param key The key to use for decryption.
   * @param nonce The nonce that was used for encryption.
   * @return The decrypted data, or {@code Optional.empty()} if verification failed.
   */
  public static Optional<byte[]> decrypt(byte[] cipherText, Key key, Nonce nonce) {
    byte[] clearText = new byte[clearTextLength(cipherText)];

    int rc = Sodium.crypto_secretbox_open_easy(clearText, cipherText, cipherText.length, nonce.ptr, key.ptr);
    if (rc == -1) {
      return Optional.empty();
    }
    if (rc != 0) {
      throw new SodiumException("crypto_secretbox_open_easy: failed with result " + rc);
    }

    return Optional.of(clearText);
  }

  private static int clearTextLength(byte[] cipherText) {
    long macbytes = Sodium.crypto_secretbox_macbytes();
    if (macbytes > Integer.MAX_VALUE) {
      throw new IllegalStateException("crypto_secretbox_macbytes: " + macbytes + " is too large");
    }
    if (macbytes > cipherText.length) {
      throw new IllegalArgumentException("cipherText is too short");
    }
    return cipherText.length - ((int) macbytes);
  }

  /**
   * Decrypt a message using a given key and a detached message authentication code.
   *
   * @param cipherText The cipher text to decrypt.
   * @param mac The message authentication code.
   * @param key The key to use for decryption.
   * @param nonce The nonce that was used for encryption.
   * @return The decrypted data, or {@code Optional.empty()} if verification failed.
   */
  public static Optional<Bytes> decryptDetached(Bytes cipherText, Bytes mac, Key key, Nonce nonce) {
    return decryptDetached(cipherText.toArrayUnsafe(), mac.toArrayUnsafe(), key, nonce).map(Bytes::wrap);
  }

  /**
   * Decrypt a message using a given key and a detached message authentication code.
   *
   * @param cipherText The cipher text to decrypt.
   * @param mac The message authentication code.
   * @param key The key to use for decryption.
   * @param nonce The nonce that was used for encryption.
   * @return The decrypted data, or {@code Optional.empty()} if verification failed.
   */
  public static Optional<byte[]> decryptDetached(byte[] cipherText, byte[] mac, Key key, Nonce nonce) {
    long macbytes = Sodium.crypto_secretbox_macbytes();
    if (macbytes > Integer.MAX_VALUE) {
      throw new IllegalStateException("crypto_secretbox_macbytes: " + macbytes + " is too large");
    }
    if (mac.length != macbytes) {
      throw new IllegalArgumentException("mac must be " + macbytes + " bytes, got " + mac.length);
    }

    byte[] clearText = new byte[cipherText.length];
    int rc = Sodium.crypto_secretbox_open_detached(clearText, cipherText, mac, cipherText.length, nonce.ptr, key.ptr);
    if (rc == -1) {
      return Optional.empty();
    }
    if (rc != 0) {
      throw new SodiumException("crypto_secretbox_open_detached: failed with result " + rc);
    }

    return Optional.of(clearText);
  }
}
