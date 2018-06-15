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
// https://github.com/jedisct1/libsodium-doc/blob/424b7480562c2e063bc8c52c452ef891621c8480/public-key_cryptography/authenticated_encryption.md

/**
 * Public-key authenticated encryption.
 *
 * <p>
 * Using public-key authenticated encryption, Bob can encrypt a confidential message specifically for Alice, using
 * Alice's public key.
 *
 * <p>
 * Using Bob's public key, Alice can compute a shared secret key. Using Alice's public key and his secret key, Bob can
 * compute the exact same shared secret key. That shared secret key can be used to verify that the encrypted message was
 * not tampered with, before eventually decrypting it.
 *
 * <p>
 * Alice only needs Bob's public key, the nonce and the ciphertext. Bob should never ever share his secret key, even
 * with Alice.
 *
 * <p>
 * And in order to send messages to Alice, Bob only needs Alice's public key. Alice should never ever share her secret
 * key either, even with Bob.
 *
 * <p>
 * Alice can reply to Bob using the same system, without having to generate a distinct key pair.
 *
 * <p>
 * The nonce doesn't have to be confidential, but it should be used with just one encryption for a particular pair of
 * public and secret keys.
 *
 * <p>
 * One easy way to generate a nonce is to use {@link Nonce#random()}, considering the size of the nonces the risk of any
 * random collisions is negligible. For some applications, if you wish to use nonces to detect missing messages or to
 * ignore replayed messages, it is also acceptable to use an incrementing counter as a nonce.
 *
 * <p>
 * When doing so you must ensure that the same value can never be re-used (for example you may have multiple threads or
 * even hosts generating messages using the same key pairs).
 *
 * <p>
 * As stated above, senders can decrypt their own messages, and compute a valid authentication tag for any messages
 * encrypted with a given shared secret key. This is generally not an issue for online protocols.
 *
 * <p>
 * This class depends upon the JNR-FFI library being available on the classpath, along with its dependencies. See
 * https://github.com/jnr/jnr-ffi. JNR-FFI can be included using the gradle dependency 'com.github.jnr:jnr-ffi'.
 */
public final class Box implements AutoCloseable {

  /**
   * A Box public key.
   */
  public static final class PublicKey {
    private final Pointer ptr;

    private PublicKey(Pointer ptr) {
      this.ptr = ptr;
    }

    @Override
    protected void finalize() {
      Sodium.sodium_free(ptr);
    }

    /**
     * Create a {@link PublicKey} from an array of bytes.
     *
     * <p>
     * The byte array must be of length {@link #length()}.
     *
     * @param bytes The bytes for the public key.
     * @return A public key.
     */
    public static PublicKey forBytes(Bytes bytes) {
      return forBytes(bytes.toArrayUnsafe());
    }

    /**
     * Create a {@link PublicKey} from an array of bytes.
     *
     * <p>
     * The byte array must be of length {@link #length()}.
     *
     * @param bytes The bytes for the public key.
     * @return A public key.
     */
    public static PublicKey forBytes(byte[] bytes) {
      if (bytes.length != Sodium.crypto_box_publickeybytes()) {
        throw new IllegalArgumentException(
            "key must be " + Sodium.crypto_box_publickeybytes() + " bytes, got " + bytes.length);
      }
      return Sodium.dup(bytes, PublicKey::new);
    }

    /**
     * Obtain the length of the key in bytes (32).
     *
     * @return The length of the key in bytes (32).
     */
    public static int length() {
      long keybytes = Sodium.crypto_box_publickeybytes();
      if (keybytes > Integer.MAX_VALUE) {
        throw new SodiumException("crypto_box_publickeybytes: " + keybytes + " is too large");
      }
      return (int) keybytes;
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
   * A Box secret key.
   */
  public static final class SecretKey {
    private final Pointer ptr;

    private SecretKey(Pointer ptr) {
      this.ptr = ptr;
    }

    @Override
    protected void finalize() {
      Sodium.sodium_free(ptr);
    }

    /**
     * Create a {@link SecretKey} from an array of bytes.
     *
     * <p>
     * The byte array must be of length {@link #length()}.
     *
     * @param bytes The bytes for the secret key.
     * @return A secret key.
     */
    public static SecretKey forBytes(Bytes bytes) {
      return forBytes(bytes.toArrayUnsafe());
    }

    /**
     * Create a {@link SecretKey} from an array of bytes.
     *
     * <p>
     * The byte array must be of length {@link #length()}.
     *
     * @param bytes The bytes for the secret key.
     * @return A secret key.
     */
    public static SecretKey forBytes(byte[] bytes) {
      if (bytes.length != Sodium.crypto_box_secretkeybytes()) {
        throw new IllegalArgumentException(
            "key must be " + Sodium.crypto_box_secretkeybytes() + " bytes, got " + bytes.length);
      }
      return Sodium.dup(bytes, SecretKey::new);
    }

    /**
     * Obtain the length of the key in bytes (32).
     *
     * @return The length of the key in bytes (32).
     */
    public static int length() {
      long keybytes = Sodium.crypto_box_secretkeybytes();
      if (keybytes > Integer.MAX_VALUE) {
        throw new SodiumException("crypto_box_secretkeybytes: " + keybytes + " is too large");
      }
      return (int) keybytes;
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
   * A Box key pair seed.
   */
  public static final class Seed {
    private final Pointer ptr;

    private Seed(Pointer ptr) {
      this.ptr = ptr;
    }

    @Override
    protected void finalize() {
      Sodium.sodium_free(ptr);
    }

    /**
     * Create a {@link Seed} from an array of bytes.
     *
     * <p>
     * The byte array must be of length {@link #length()}.
     *
     * @param bytes The bytes for the seed.
     * @return A seed.
     */
    public static Seed forBytes(Bytes bytes) {
      return forBytes(bytes.toArrayUnsafe());
    }

    /**
     * Create a {@link Seed} from an array of bytes.
     *
     * <p>
     * The byte array must be of length {@link #length()}.
     *
     * @param bytes The bytes for the seed.
     * @return A seed.
     */
    public static Seed forBytes(byte[] bytes) {
      if (bytes.length != Sodium.crypto_box_seedbytes()) {
        throw new IllegalArgumentException(
            "key must be " + Sodium.crypto_box_seedbytes() + " bytes, got " + bytes.length);
      }
      return Sodium.dup(bytes, Seed::new);
    }

    /**
     * Obtain the length of the seed in bytes (32).
     *
     * @return The length of the seed in bytes (32).
     */
    public static int length() {
      long seedbytes = Sodium.crypto_box_seedbytes();
      if (seedbytes > Integer.MAX_VALUE) {
        throw new SodiumException("crypto_box_seedbytes: " + seedbytes + " is too large");
      }
      return (int) seedbytes;
    }

    /**
     * Generate a new {@link Seed} using a random generator.
     *
     * @return A randomly generated seed.
     */
    public static Seed random() {
      return Sodium.randomBytes(length(), Seed::new);
    }

    /**
     * @return The bytes of this seed.
     */
    public Bytes bytes() {
      return Bytes.wrap(bytesArray());
    }

    /**
     * @return The bytes of this seed.
     */
    public byte[] bytesArray() {
      return Sodium.reify(ptr, length());
    }
  }

  /**
   * A Box key pair.
   */
  public static final class KeyPair {

    private final PublicKey publicKey;
    private final SecretKey secretKey;

    /**
     * Create a {@link KeyPair} from pair of keys.
     *
     * @param publicKey The bytes for the public key.
     * @param secretKey The bytes for the secret key.
     */
    public KeyPair(PublicKey publicKey, SecretKey secretKey) {
      this.publicKey = publicKey;
      this.secretKey = secretKey;
    }

    /**
     * Create a {@link KeyPair} from an array of secret key bytes.
     *
     * @param secretKey The secret key.
     * @return A {@link KeyPair}.
     */
    public static KeyPair forSecretKey(SecretKey secretKey) {
      return Sodium.scalarMultBase(secretKey.ptr, SecretKey.length(), (ptr, len) -> {
        if (len != PublicKey.length()) {
          throw new IllegalStateException(
              "Public key length " + PublicKey.length() + " is not same as generated key length " + len);
        }
        return new KeyPair(new PublicKey(ptr), secretKey);
      });
    }

    /**
     * Generate a new key using a random generator.
     *
     * @return A randomly generated key pair.
     */
    public static KeyPair random() {
      Pointer publicKey = Sodium.malloc(PublicKey.length());
      Pointer secretKey = null;
      try {
        secretKey = Sodium.malloc(SecretKey.length());
        int rc = Sodium.crypto_box_keypair(publicKey, secretKey);
        if (rc != 0) {
          throw new SodiumException("crypto_box_keypair: failed with result " + rc);
        }
        PublicKey pk = new PublicKey(publicKey);
        publicKey = null;
        SecretKey sk = new SecretKey(secretKey);
        secretKey = null;
        return new KeyPair(pk, sk);
      } catch (Throwable e) {
        Sodium.sodium_free(publicKey);
        if (secretKey != null) {
          Sodium.sodium_free(secretKey);
        }
        throw e;
      }
    }

    /**
     * Generate a new key using a seed.
     *
     * @param seed A seed.
     * @return The generated key pair.
     */
    public static KeyPair fromSeed(Seed seed) {
      Pointer publicKey = Sodium.malloc(PublicKey.length());
      Pointer secretKey = null;
      try {
        secretKey = Sodium.malloc(SecretKey.length());
        int rc = Sodium.crypto_box_seed_keypair(publicKey, secretKey, seed.ptr);
        if (rc != 0) {
          throw new SodiumException("crypto_box_keypair: failed with result " + rc);
        }
        PublicKey pk = new PublicKey(publicKey);
        publicKey = null;
        SecretKey sk = new SecretKey(secretKey);
        secretKey = null;
        return new KeyPair(pk, sk);
      } catch (Throwable e) {
        Sodium.sodium_free(publicKey);
        if (secretKey != null) {
          Sodium.sodium_free(secretKey);
        }
        throw e;
      }
    }

    /**
     * @return The public key of the key pair.
     */
    public PublicKey publicKey() {
      return publicKey;
    }

    /**
     * @return The secret key of the key pair.
     */
    public SecretKey secretKey() {
      return secretKey;
    }
  }

  /**
   * A Box nonce.
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
      if (bytes.length != Sodium.crypto_box_noncebytes()) {
        throw new IllegalArgumentException(
            "nonce must be " + Sodium.crypto_box_noncebytes() + " bytes, got " + bytes.length);
      }
      return Sodium.dup(bytes, Nonce::new);
    }

    /**
     * Obtain the length of the nonce in bytes (24).
     *
     * @return The length of the nonce in bytes (24).
     */
    public static int length() {
      long npubbytes = Sodium.crypto_box_noncebytes();
      if (npubbytes > Integer.MAX_VALUE) {
        throw new SodiumException("crypto_box_noncebytes: " + npubbytes + " is too large");
      }
      return (int) npubbytes;
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

  private Pointer ctx;

  private Box(PublicKey publicKey, SecretKey secretKey) {
    ctx = Sodium.malloc(Sodium.crypto_box_beforenmbytes());
    try {
      int rc = Sodium.crypto_box_beforenm(ctx, publicKey.ptr, secretKey.ptr);
      if (rc != 0) {
        throw new SodiumException("crypto_box_beforenm: failed with result " + rc);
      }
    } catch (Throwable e) {
      Sodium.sodium_free(ctx);
      ctx = null;
      throw e;
    }
  }

  /**
   * Precompute the shared key for a given sender and receiver.
   *
   * <p>
   * Note that the returned instance of {@link Box} should be closed using {@link #close()} (or try-with-resources) to
   * ensure timely release of the shared key, which is held in native memory.
   *
   * @param receiver The public key of the receiver.
   * @param sender The secret key of the sender.
   * @return A {@link Box} instance.
   */
  public static Box forKeys(PublicKey receiver, SecretKey sender) {
    return new Box(receiver, sender);
  }

  /**
   * Encrypt a message for a given key.
   *
   * @param message The message to encrypt.
   * @param receiver The public key of the receiver.
   * @param sender The secret key of the sender.
   * @param nonce A unique nonce.
   * @return The encrypted data.
   */
  public static Bytes encrypt(Bytes message, PublicKey receiver, SecretKey sender, Nonce nonce) {
    return Bytes.wrap(encrypt(message.toArrayUnsafe(), receiver, sender, nonce));
  }

  /**
   * Encrypt a message for a given key.
   *
   * @param message The message to encrypt.
   * @param receiver The public key of the receiver.
   * @param sender The secret key of the sender.
   * @param nonce A unique nonce.
   * @return The encrypted data.
   */
  public static byte[] encrypt(byte[] message, PublicKey receiver, SecretKey sender, Nonce nonce) {
    byte[] cipherText = new byte[combinedCypherTextLength(message)];

    int rc = Sodium.crypto_box_easy(cipherText, message, message.length, nonce.ptr, receiver.ptr, sender.ptr);
    if (rc != 0) {
      throw new SodiumException("crypto_box_easy: failed with result " + rc);
    }

    return cipherText;
  }

  /**
   * Encrypt a message for a given key.
   *
   * @param message The message to encrypt.
   * @param nonce A unique nonce.
   * @return The encrypted data.
   */
  public Bytes encrypt(Bytes message, Nonce nonce) {
    return Bytes.wrap(encrypt(message.toArrayUnsafe(), nonce));
  }

  /**
   * Encrypt a message for a given key.
   *
   * @param message The message to encrypt.
   * @param nonce A unique nonce.
   * @return The encrypted data.
   */
  public byte[] encrypt(byte[] message, Nonce nonce) {
    assertOpen();

    byte[] cipherText = new byte[combinedCypherTextLength(message)];

    int rc = Sodium.crypto_box_easy_afternm(cipherText, message, message.length, nonce.ptr, ctx);
    if (rc != 0) {
      throw new SodiumException("crypto_box_easy_afternm: failed with result " + rc);
    }

    return cipherText;
  }

  /**
   * Encrypt a sealed message for a given key.
   *
   * <p>
   * Sealed boxes are designed to anonymously send messages to a recipient given its public key.
   *
   * <p>
   * Only the recipient can decrypt these messages, using its private key. While the recipient can verify the integrity
   * of the message, it cannot verify the identity of the sender.
   *
   * <p>
   * A message is encrypted using an ephemeral key pair, whose secret part is destroyed right after the encryption
   * process.
   *
   * <p>
   * Without knowing the secret key used for a given message, the sender cannot decrypt its own message later. And
   * without additional data, a message cannot be correlated with the identity of its sender.
   *
   * @param message The message to encrypt.
   * @param receiver The public key of the receiver.
   * @return The encrypted data.
   */
  public static Bytes encryptSealed(Bytes message, PublicKey receiver) {
    return Bytes.wrap(encryptSealed(message.toArrayUnsafe(), receiver));
  }

  /**
   * Encrypt a sealed message for a given key.
   *
   * <p>
   * Sealed boxes are designed to anonymously send messages to a recipient given its public key.
   *
   * <p>
   * Only the recipient can decrypt these messages, using its private key. While the recipient can verify the integrity
   * of the message, it cannot verify the identity of the sender.
   *
   * <p>
   * A message is encrypted using an ephemeral key pair, whose secret part is destroyed right after the encryption
   * process.
   *
   * <p>
   * Without knowing the secret key used for a given message, the sender cannot decrypt its own message later. And
   * without additional data, a message cannot be correlated with the identity of its sender.
   *
   * @param message The message to encrypt.
   * @param receiver The public key of the receiver.
   * @return The encrypted data.
   */
  public static byte[] encryptSealed(byte[] message, PublicKey receiver) {
    long sealbytes = Sodium.crypto_box_sealbytes();
    if (sealbytes > Integer.MAX_VALUE) {
      throw new IllegalStateException("crypto_box_sealbytes: " + sealbytes + " is too large");
    }
    byte[] cipherText = new byte[(int) sealbytes + message.length];

    int rc = Sodium.crypto_box_seal(cipherText, message, message.length, receiver.ptr);
    if (rc != 0) {
      throw new SodiumException("crypto_box_seal: failed with result " + rc);
    }

    return cipherText;
  }

  private static int combinedCypherTextLength(byte[] message) {
    long macbytes = Sodium.crypto_box_macbytes();
    if (macbytes > Integer.MAX_VALUE) {
      throw new IllegalStateException("crypto_box_macbytes: " + macbytes + " is too large");
    }
    return (int) macbytes + message.length;
  }

  /**
   * Encrypt a message for a given key, generating a detached message authentication code.
   *
   * @param message The message to encrypt.
   * @param receiver The public key of the receiver.
   * @param sender The secret key of the sender.
   * @param nonce A unique nonce.
   * @return The encrypted data and message authentication code.
   */
  public static DetachedEncryptionResult encryptDetached(
      Bytes message,
      PublicKey receiver,
      SecretKey sender,
      Nonce nonce) {
    return encryptDetached(message.toArrayUnsafe(), receiver, sender, nonce);
  }

  /**
   * Encrypt a message for a given key, generating a detached message authentication code.
   *
   * @param message The message to encrypt.
   * @param receiver The public key of the receiver.
   * @param sender The secret key of the sender.
   * @param nonce A unique nonce.
   * @return The encrypted data and message authentication code.
   */
  public static DetachedEncryptionResult encryptDetached(
      byte[] message,
      PublicKey receiver,
      SecretKey sender,
      Nonce nonce) {
    byte[] cipherText = new byte[message.length];
    long macbytes = Sodium.crypto_box_macbytes();
    if (macbytes > Integer.MAX_VALUE) {
      throw new IllegalStateException("crypto_box_macbytes: " + macbytes + " is too large");
    }
    byte[] mac = new byte[(int) macbytes];

    int rc = Sodium.crypto_box_detached(cipherText, mac, message, message.length, nonce.ptr, receiver.ptr, sender.ptr);
    if (rc != 0) {
      throw new SodiumException("crypto_box_detached: failed with result " + rc);
    }

    return new DefaultDetachedEncryptionResult(cipherText, mac);
  }

  /**
   * Encrypt a message, generating a detached message authentication code.
   *
   * @param message The message to encrypt.
   * @param nonce A unique nonce.
   * @return The encrypted data and message authentication code.
   */
  public DetachedEncryptionResult encryptDetached(Bytes message, Nonce nonce) {
    return encryptDetached(message.toArrayUnsafe(), nonce);
  }

  /**
   * Encrypt a message, generating a detached message authentication code.
   *
   * @param message The message to encrypt.
   * @param nonce A unique nonce.
   * @return The encrypted data and message authentication code.
   */
  public DetachedEncryptionResult encryptDetached(byte[] message, Nonce nonce) {
    assertOpen();

    byte[] cipherText = new byte[message.length];
    long macbytes = Sodium.crypto_box_macbytes();
    if (macbytes > Integer.MAX_VALUE) {
      throw new IllegalStateException("crypto_box_macbytes: " + macbytes + " is too large");
    }
    byte[] mac = new byte[(int) macbytes];

    int rc = Sodium.crypto_box_detached_afternm(cipherText, mac, message, message.length, nonce.ptr, ctx);
    if (rc != 0) {
      throw new SodiumException("crypto_box_detached_afternm: failed with result " + rc);
    }

    return new DefaultDetachedEncryptionResult(cipherText, mac);
  }

  /**
   * Decrypt a message using a given key.
   *
   * @param cipherText The cipher text to decrypt.
   * @param sender The public key of the sender.
   * @param receiver The secret key of the receiver.
   * @param nonce The nonce that was used for encryption.
   * @return The decrypted data, or {@code Optional.empty()} if verification failed.
   */
  public static Optional<Bytes> decrypt(Bytes cipherText, PublicKey sender, SecretKey receiver, Nonce nonce) {
    return decrypt(cipherText.toArrayUnsafe(), sender, receiver, nonce).map(Bytes::wrap);
  }

  /**
   * Decrypt a message using a given key.
   *
   * @param cipherText The cipher text to decrypt.
   * @param sender The public key of the sender.
   * @param receiver The secret key of the receiver.
   * @param nonce The nonce that was used for encryption.
   * @return The decrypted data, or {@code Optional.empty()} if verification failed.
   */
  public static Optional<byte[]> decrypt(byte[] cipherText, PublicKey sender, SecretKey receiver, Nonce nonce) {
    byte[] clearText = new byte[clearTextLength(cipherText)];

    int rc = Sodium.crypto_box_open_easy(clearText, cipherText, cipherText.length, nonce.ptr, sender.ptr, receiver.ptr);
    if (rc == -1) {
      return Optional.empty();
    }
    if (rc != 0) {
      throw new SodiumException("crypto_box_open_easy: failed with result " + rc);
    }

    return Optional.of(clearText);
  }

  /**
   * Decrypt a message using a given key.
   *
   * @param cipherText The cipher text to decrypt.
   * @param nonce The nonce that was used for encryption.
   * @return The decrypted data, or {@code Optional.empty()} if verification failed.
   */
  public Optional<Bytes> decrypt(Bytes cipherText, Nonce nonce) {
    return decrypt(cipherText.toArrayUnsafe(), nonce).map(Bytes::wrap);
  }

  /**
   * Decrypt a message using a given key.
   *
   * @param cipherText The cipher text to decrypt.
   * @param nonce The nonce that was used for encryption.
   * @return The decrypted data, or {@code Optional.empty()} if verification failed.
   */
  public Optional<byte[]> decrypt(byte[] cipherText, Nonce nonce) {
    assertOpen();

    byte[] clearText = new byte[clearTextLength(cipherText)];

    int rc = Sodium.crypto_box_open_easy_afternm(clearText, cipherText, cipherText.length, nonce.ptr, ctx);
    if (rc == -1) {
      return Optional.empty();
    }
    if (rc != 0) {
      throw new SodiumException("crypto_box_open_easy_afternm: failed with result " + rc);
    }

    return Optional.of(clearText);
  }

  private static int clearTextLength(byte[] cipherText) {
    long macbytes = Sodium.crypto_box_macbytes();
    if (macbytes > Integer.MAX_VALUE) {
      throw new IllegalStateException("crypto_box_macbytes: " + macbytes + " is too large");
    }
    if (macbytes > cipherText.length) {
      throw new IllegalArgumentException("cipherText is too short");
    }
    return cipherText.length - ((int) macbytes);
  }

  /**
   * Decrypt a sealed message using a given key.
   *
   * @param cipherText The cipher text to decrypt.
   * @param sender The public key of the sender.
   * @param receiver The secret key of the receiver.
   * @return The decrypted data, or {@code Optional.empty()} if verification failed.
   */
  public static Optional<Bytes> decryptSealed(Bytes cipherText, PublicKey sender, SecretKey receiver) {
    return decryptSealed(cipherText.toArrayUnsafe(), sender, receiver).map(Bytes::wrap);
  }

  /**
   * Decrypt a sealed message using a given key.
   *
   * @param cipherText The cipher text to decrypt.
   * @param sender The public key of the sender.
   * @param receiver The secret key of the receiver.
   * @return The decrypted data, or {@code Optional.empty()} if verification failed.
   */
  public static Optional<byte[]> decryptSealed(byte[] cipherText, PublicKey sender, SecretKey receiver) {
    long sealbytes = Sodium.crypto_box_sealbytes();
    if (sealbytes > Integer.MAX_VALUE) {
      throw new IllegalStateException("crypto_box_sealbytes: " + sealbytes + " is too large");
    }
    if (sealbytes > cipherText.length) {
      throw new IllegalArgumentException("cipherText is too short");
    }
    byte[] clearText = new byte[cipherText.length - ((int) sealbytes)];

    int rc = Sodium.crypto_box_seal_open(clearText, cipherText, cipherText.length, sender.ptr, receiver.ptr);
    if (rc == -1) {
      return Optional.empty();
    }
    if (rc != 0) {
      throw new SodiumException("crypto_box_seal_open: failed with result " + rc);
    }

    return Optional.of(clearText);
  }

  /**
   * Decrypt a message using a given key and a detached message authentication code.
   *
   * @param cipherText The cipher text to decrypt.
   * @param mac The message authentication code.
   * @param sender The public key of the sender.
   * @param receiver The secret key of the receiver.
   * @param nonce The nonce that was used for encryption.
   * @return The decrypted data, or {@code Optional.empty()} if verification failed.
   */
  public static Optional<Bytes> decryptDetached(
      Bytes cipherText,
      Bytes mac,
      PublicKey sender,
      SecretKey receiver,
      Nonce nonce) {
    return decryptDetached(cipherText.toArrayUnsafe(), mac.toArrayUnsafe(), sender, receiver, nonce).map(Bytes::wrap);
  }

  /**
   * Decrypt a message using a given key and a detached message authentication code.
   *
   * @param cipherText The cipher text to decrypt.
   * @param mac The message authentication code.
   * @param sender The public key of the sender.
   * @param receiver The secret key of the receiver.
   * @param nonce The nonce that was used for encryption.
   * @return The decrypted data, or {@code Optional.empty()} if verification failed.
   */
  public static Optional<byte[]> decryptDetached(
      byte[] cipherText,
      byte[] mac,
      PublicKey sender,
      SecretKey receiver,
      Nonce nonce) {
    long macbytes = Sodium.crypto_box_macbytes();
    if (macbytes > Integer.MAX_VALUE) {
      throw new IllegalStateException("crypto_box_macbytes: " + macbytes + " is too large");
    }
    if (mac.length != macbytes) {
      throw new IllegalArgumentException("mac must be " + macbytes + " bytes, got " + mac.length);
    }

    byte[] clearText = new byte[cipherText.length];
    int rc = Sodium
        .crypto_box_open_detached(clearText, cipherText, mac, cipherText.length, nonce.ptr, sender.ptr, receiver.ptr);
    if (rc == -1) {
      return Optional.empty();
    }
    if (rc != 0) {
      throw new SodiumException("crypto_box_open_detached: failed with result " + rc);
    }

    return Optional.of(clearText);
  }

  /**
   * Decrypt a message using a detached message authentication code.
   *
   * @param cipherText The cipher text to decrypt.
   * @param mac The message authentication code.
   * @param nonce The nonce that was used for encryption.
   * @return The decrypted data, or {@code Optional.empty()} if verification failed.
   */
  public Optional<Bytes> decryptDetached(Bytes cipherText, Bytes mac, Nonce nonce) {
    return decryptDetached(cipherText.toArrayUnsafe(), mac.toArrayUnsafe(), nonce).map(Bytes::wrap);
  }

  /**
   * Decrypt a message using a detached message authentication code.
   *
   * @param cipherText The cipher text to decrypt.
   * @param mac The message authentication code.
   * @param nonce The nonce that was used for encryption.
   * @return The decrypted data, or {@code Optional.empty()} if verification failed.
   */
  public Optional<byte[]> decryptDetached(byte[] cipherText, byte[] mac, Nonce nonce) {
    long macbytes = Sodium.crypto_box_macbytes();
    if (macbytes > Integer.MAX_VALUE) {
      throw new IllegalStateException("crypto_box_macbytes: " + macbytes + " is too large");
    }
    if (mac.length != macbytes) {
      throw new IllegalArgumentException("mac must be " + macbytes + " bytes, got " + mac.length);
    }

    byte[] clearText = new byte[cipherText.length];
    int rc = Sodium.crypto_box_open_detached_afternm(clearText, cipherText, mac, cipherText.length, nonce.ptr, ctx);
    if (rc == -1) {
      return Optional.empty();
    }
    if (rc != 0) {
      throw new SodiumException("crypto_box_open_detached_afternm: failed with result " + rc);
    }

    return Optional.of(clearText);
  }

  private void assertOpen() {
    if (ctx == null) {
      throw new IllegalStateException(getClass().getName() + ": already closed");
    }
  }

  @Override
  public void close() {
    if (ctx != null) {
      Sodium.sodium_free(ctx);
      ctx = null;
    }
  }

  @Override
  protected void finalize() {
    close();
  }
}
