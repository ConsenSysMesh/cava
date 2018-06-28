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

import jnr.ffi.Pointer;

/**
 * Key exchange.
 *
 * <p>
 * Allows two parties can securely compute a set of shared keys using their peer's public key and their own secret key.
 */
public final class KeyExchange {

  /**
   * A KeyExchange public key.
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
    public static PublicKey fromBytes(Bytes bytes) {
      return fromBytes(bytes.toArrayUnsafe());
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
    public static PublicKey fromBytes(byte[] bytes) {
      if (bytes.length != Sodium.crypto_kx_publickeybytes()) {
        throw new IllegalArgumentException(
            "key must be " + Sodium.crypto_kx_publickeybytes() + " bytes, got " + bytes.length);
      }
      return Sodium.dup(bytes, PublicKey::new);
    }

    /**
     * Obtain the length of the key in bytes (32).
     *
     * @return The length of the key in bytes (32).
     */
    public static int length() {
      long keybytes = Sodium.crypto_kx_publickeybytes();
      if (keybytes > Integer.MAX_VALUE) {
        throw new SodiumException("crypto_kx_publickeybytes: " + keybytes + " is too large");
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
   * A KeyExchange secret key.
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
    public static SecretKey fromBytes(Bytes bytes) {
      return fromBytes(bytes.toArrayUnsafe());
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
    public static SecretKey fromBytes(byte[] bytes) {
      if (bytes.length != Sodium.crypto_kx_secretkeybytes()) {
        throw new IllegalArgumentException(
            "key must be " + Sodium.crypto_kx_secretkeybytes() + " bytes, got " + bytes.length);
      }
      return Sodium.dup(bytes, SecretKey::new);
    }

    /**
     * Obtain the length of the key in bytes (32).
     *
     * @return The length of the key in bytes (32).
     */
    public static int length() {
      long keybytes = Sodium.crypto_kx_secretkeybytes();
      if (keybytes > Integer.MAX_VALUE) {
        throw new SodiumException("crypto_kx_secretkeybytes: " + keybytes + " is too large");
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
   * A KeyExchange key pair seed.
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
    public static Seed fromBytes(Bytes bytes) {
      return fromBytes(bytes.toArrayUnsafe());
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
    public static Seed fromBytes(byte[] bytes) {
      if (bytes.length != Sodium.crypto_kx_seedbytes()) {
        throw new IllegalArgumentException(
            "key must be " + Sodium.crypto_kx_seedbytes() + " bytes, got " + bytes.length);
      }
      return Sodium.dup(bytes, Seed::new);
    }

    /**
     * Obtain the length of the seed in bytes (32).
     *
     * @return The length of the seed in bytes (32).
     */
    public static int length() {
      long seedbytes = Sodium.crypto_kx_seedbytes();
      if (seedbytes > Integer.MAX_VALUE) {
        throw new SodiumException("crypto_kx_seedbytes: " + seedbytes + " is too large");
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
   * A KeyExchange key pair.
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
     * Create a {@link KeyPair} from a secret key.
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
        int rc = Sodium.crypto_kx_keypair(publicKey, secretKey);
        if (rc != 0) {
          throw new SodiumException("crypto_kx_keypair: failed with result " + rc);
        }
        PublicKey pk = new PublicKey(publicKey);
        publicKey = null;
        SecretKey sk = new SecretKey(secretKey);
        secretKey = null;
        return new KeyPair(pk, sk);
      } catch (Throwable e) {
        if (publicKey != null) {
          Sodium.sodium_free(publicKey);
        }
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
        int rc = Sodium.crypto_kx_seed_keypair(publicKey, secretKey, seed.ptr);
        if (rc != 0) {
          throw new SodiumException("crypto_kx_seed_keypair: failed with result " + rc);
        }
        PublicKey pk = new PublicKey(publicKey);
        publicKey = null;
        SecretKey sk = new SecretKey(secretKey);
        secretKey = null;
        return new KeyPair(pk, sk);
      } catch (Throwable e) {
        if (publicKey != null) {
          Sodium.sodium_free(publicKey);
        }
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
   * A KeyExchange session key.
   */
  public static final class SessionKey {
    private final Pointer ptr;

    private SessionKey(Pointer ptr) {
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
    public static PublicKey fromBytes(Bytes bytes) {
      return fromBytes(bytes.toArrayUnsafe());
    }

    /**
     * Create a {@link SessionKey} from an array of bytes.
     *
     * <p>
     * The byte array must be of length {@link #length()}.
     *
     * @param bytes The bytes for the public key.
     * @return A public key.
     */
    public static PublicKey fromBytes(byte[] bytes) {
      if (bytes.length != Sodium.crypto_kx_sessionkeybytes()) {
        throw new IllegalArgumentException(
            "key must be " + Sodium.crypto_kx_sessionkeybytes() + " bytes, got " + bytes.length);
      }
      return Sodium.dup(bytes, PublicKey::new);
    }

    /**
     * Obtain the length of the key in bytes (32).
     *
     * @return The length of the key in bytes (32).
     */
    public static int length() {
      long keybytes = Sodium.crypto_kx_sessionkeybytes();
      if (keybytes > Integer.MAX_VALUE) {
        throw new SodiumException("crypto_kx_sessionkeybytes: " + keybytes + " is too large");
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
   * A KeyExchange session key pair.
   */
  public static final class SessionKeyPair {

    private final SessionKey rxKey;
    private final SessionKey txKey;

    /**
     * Create a {@link KeyPair} from pair of keys.
     *
     * @param rxKey The bytes for the secret key.
     * @param txKey The bytes for the public key.
     */
    public SessionKeyPair(SessionKey rxKey, SessionKey txKey) {
      this.rxKey = rxKey;
      this.txKey = txKey;
    }

    /** @return The session key that will be used to receive data. */
    public SessionKey rx() {
      return rxKey;
    }

    /** @return The session key that will be used to send data. */
    public SessionKey tx() {
      return txKey;
    }
  }

  /**
   * Computer a pair of session keys for use by a client.
   *
   * @param clientKeys The client key pair.
   * @param serverKey The server public key.
   * @return A pair of session keys.
   */
  public static SessionKeyPair client(KeyPair clientKeys, PublicKey serverKey) {
    long sessionkeybytes = Sodium.crypto_kx_sessionkeybytes();
    Pointer rxPtr = null;
    Pointer txPtr = null;
    try {
      rxPtr = Sodium.malloc(sessionkeybytes);
      txPtr = Sodium.malloc(sessionkeybytes);
      int rc = Sodium.crypto_kx_client_session_keys(
          rxPtr,
          txPtr,
          clientKeys.publicKey.ptr,
          clientKeys.secretKey.ptr,
          serverKey.ptr);
      if (rc != 0) {
        throw new SodiumException("crypto_kx_client_session_keys: failed with result " + rc);
      }
      SessionKey rxKey = new SessionKey(rxPtr);
      rxPtr = null;
      SessionKey txKey = new SessionKey(txPtr);
      txPtr = null;
      return new SessionKeyPair(rxKey, txKey);
    } catch (Throwable e) {
      if (rxPtr != null) {
        Sodium.sodium_free(rxPtr);
      }
      if (txPtr != null) {
        Sodium.sodium_free(txPtr);
      }
      throw e;
    }
  }

  /**
   * Computer a pair of session keys for use by a client.
   *
   * @param serverKeys The server key pair.
   * @param clientKey The client public key.
   * @return A pair of session keys.
   */
  public static SessionKeyPair server(KeyPair serverKeys, PublicKey clientKey) {
    long sessionkeybytes = Sodium.crypto_kx_sessionkeybytes();
    Pointer rxPtr = null;
    Pointer txPtr = null;
    try {
      rxPtr = Sodium.malloc(sessionkeybytes);
      txPtr = Sodium.malloc(sessionkeybytes);
      int rc = Sodium.crypto_kx_server_session_keys(
          rxPtr,
          txPtr,
          serverKeys.publicKey.ptr,
          serverKeys.secretKey.ptr,
          clientKey.ptr);
      if (rc != 0) {
        throw new SodiumException("crypto_kx_client_session_keys: failed with result " + rc);
      }
      SessionKey rxKey = new SessionKey(rxPtr);
      rxPtr = null;
      SessionKey txKey = new SessionKey(txPtr);
      txPtr = null;
      return new SessionKeyPair(rxKey, txKey);
    } catch (Throwable e) {
      if (rxPtr != null) {
        Sodium.sodium_free(rxPtr);
      }
      if (txPtr != null) {
        Sodium.sodium_free(txPtr);
      }
      throw e;
    }
  }
}
