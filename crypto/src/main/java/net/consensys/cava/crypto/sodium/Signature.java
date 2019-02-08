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

// Documentation copied under the ISC License, from
// https://github.com/jedisct1/libsodium-doc/blob/424b7480562c2e063bc8c52c452ef891621c8480/public-key_cryptography/public-key_signatures.md

import net.consensys.cava.bytes.Bytes;

import java.util.Arrays;
import java.util.Objects;

import jnr.ffi.byref.LongLongByReference;

/**
 * Public-key signatures.
 *
 * <p>
 * In this system, a signer generates a key pair:
 * <ul>
 *
 * <li>a secret key, that will be used to append a signature to any number of messages</li>
 *
 * <li>a public key, that anybody can use to verify that the signature appended to a message was actually issued by the
 * creator of the public key.</li>
 *
 * </ul>
 *
 * <p>
 * Verifiers need to already know and ultimately trust a public key before messages signed using it can be verified.
 *
 * <p>
 * Warning: this is different from authenticated encryption. Appending a signature does not change the representation of
 * the message itself.
 *
 * <p>
 * This class depends upon the JNR-FFI library being available on the classpath, along with its dependencies. See
 * https://github.com/jnr/jnr-ffi. JNR-FFI can be included using the gradle dependency 'com.github.jnr:jnr-ffi'.
 */
public final class Signature {

  /**
   * A signing public key.
   */
  public static final class PublicKey {
    private final Bytes bytes;

    private PublicKey(Bytes bytes) {
      if (bytes.size() != Signature.PublicKey.length()) {
        throw new IllegalArgumentException(
            "key must be " + Signature.PublicKey.length() + " bytes, got " + bytes.size());
      }
      this.bytes = bytes;
    }

    /**
     * Create a {@link Signature.PublicKey} from an array of bytes.
     *
     * <p>
     * The byte array must be of length {@link #length()}.
     *
     * @param bytes The bytes for the public key.
     * @return A public key.
     */
    public static Signature.PublicKey fromBytes(Bytes bytes) {
      return new Signature.PublicKey(bytes);
    }

    /**
     * Create a {@link Signature.PublicKey} from an array of bytes.
     *
     * <p>
     * The byte array must be of length {@link #length()}.
     *
     * @param bytes The bytes for the public key.
     * @return A public key.
     */
    public static Signature.PublicKey fromBytes(byte[] bytes) {
      return new Signature.PublicKey(Bytes.wrap(bytes));
    }

    /**
     * Obtain the length of the key in bytes (32).
     *
     * @return The length of the key in bytes (32).
     */
    public static int length() {
      long keybytes = Sodium.crypto_sign_publickeybytes();
      if (keybytes > Integer.MAX_VALUE) {
        throw new SodiumException("crypto_sign_publickeybytes: " + keybytes + " is too large");
      }
      return (int) keybytes;
    }

    /**
     * Verifies the signature of a message.
     *
     * @param message the message itself
     * @param signature the signature of the message
     * @return true if the signature matches the message according to this public key
     */
    public boolean verify(Bytes message, Bytes signature) {
      return Signature.verifyDetached(message, signature, this);
    }

    @Override
    public boolean equals(Object obj) {
      if (obj == this) {
        return true;
      }
      if (!(obj instanceof Signature.PublicKey)) {
        return false;
      }
      Signature.PublicKey other = (Signature.PublicKey) obj;
      return other.bytes.equals(this.bytes);
    }

    @Override
    public int hashCode() {
      return Arrays.hashCode(this.bytesArray());
    }

    /**
     * @return The bytes of this key.
     */
    public Bytes bytes() {
      return bytes;
    }

    /**
     * @return The bytes of this key.
     */
    public byte[] bytesArray() {
      return bytes.toArrayUnsafe();
    }
  }

  /**
   * A Signature secret key.
   */
  public static final class SecretKey {

    private final Bytes bytes;

    private SecretKey(Bytes bytes) {
      this.bytes = bytes;
    }

    /**
     * Create a {@link Signature.SecretKey} from an array of bytes.
     *
     * <p>
     * The byte array must be of length {@link #length()}.
     *
     * @param bytes The bytes for the secret key.
     * @return A secret key.
     */
    public static Signature.SecretKey fromBytes(Bytes bytes) {
      return fromBytes(bytes.toArrayUnsafe());
    }

    /**
     * Create a {@link Signature.SecretKey} from an array of bytes.
     *
     * <p>
     * The byte array must be of length {@link #length()}.
     *
     * @param bytes The bytes for the secret key.
     * @return A secret key.
     */
    public static Signature.SecretKey fromBytes(byte[] bytes) {
      if (bytes.length != Signature.SecretKey.length()) {
        throw new IllegalArgumentException(
            "key must be " + Signature.SecretKey.length() + " bytes, got " + bytes.length);
      }
      return new Signature.SecretKey(Bytes.wrap(bytes));
    }

    /**
     * Obtain the length of the key in bytes (32).
     *
     * @return The length of the key in bytes (32).
     */
    public static int length() {
      long keybytes = Sodium.crypto_sign_secretkeybytes();
      if (keybytes > Integer.MAX_VALUE) {
        throw new SodiumException("crypto_sign_secretkeybytes: " + keybytes + " is too large");
      }
      return (int) keybytes;
    }

    /**
     * Hashes content using the secret key. Hashes can be verified using the public key.
     * 
     * @see Signature.PublicKey#verify
     *
     * @param content the content to sign
     * @return the hash of the content signed with the secret key
     */
    public Bytes sign(Bytes content) {
      return Signature.signDetached(content, this);
    }

    @Override
    public boolean equals(Object obj) {
      if (obj == this) {
        return true;
      }
      if (!(obj instanceof Signature.SecretKey)) {
        return false;
      }
      Signature.SecretKey other = (Signature.SecretKey) obj;
      return other.bytes.equals(this.bytes);
    }

    @Override
    public int hashCode() {
      return Arrays.hashCode(bytesArray());
    }

    /**
     * @return The bytes of this key.
     */
    public Bytes bytes() {
      return bytes;
    }

    /**
     * @return The bytes of this key.
     */
    public byte[] bytesArray() {
      return bytes.toArrayUnsafe();
    }
  }

  /**
   * A Signature key pair seed.
   */
  public static final class Seed {
    private final Bytes bytes;

    private Seed(Bytes bytes) {
      if (bytes.size() != Signature.Seed.length()) {
        throw new IllegalArgumentException("key must be " + Signature.Seed.length() + " bytes, got " + bytes.size());
      }
      this.bytes = bytes;
    }

    /**
     * Create a {@link Signature.Seed} from an array of bytes.
     *
     * <p>
     * The byte array must be of length {@link #length()}.
     *
     * @param bytes The bytes for the seed.
     * @return A seed.
     */
    public static Signature.Seed fromBytes(Bytes bytes) {
      return new Signature.Seed(bytes);
    }

    /**
     * Create a {@link Signature.Seed} from an array of bytes.
     *
     * <p>
     * The byte array must be of length {@link #length()}.
     *
     * @param bytes The bytes for the seed.
     * @return A seed.
     */
    public static Signature.Seed fromBytes(byte[] bytes) {
      return fromBytes(Bytes.wrap(bytes));
    }

    /**
     * Obtain the length of the seed in bytes (32).
     *
     * @return The length of the seed in bytes (32).
     */
    public static int length() {
      long seedbytes = Sodium.crypto_sign_seedbytes();
      if (seedbytes > Integer.MAX_VALUE) {
        throw new SodiumException("crypto_sign_seedbytes: " + seedbytes + " is too large");
      }
      return (int) seedbytes;
    }

    /**
     * Generate a new {@link Signature.Seed} using a random generator.
     *
     * @return A randomly generated seed.
     */
    public static Signature.Seed random() {
      Bytes bytes = Bytes.wrap(new byte[length()]);
      Sodium.randombytes(bytes.toArrayUnsafe(), bytes.size());
      return Signature.Seed.fromBytes(bytes);
    }

    @Override
    public boolean equals(Object obj) {
      if (obj == this) {
        return true;
      }
      if (!(obj instanceof Signature.Seed)) {
        return false;
      }
      Signature.Seed other = (Signature.Seed) obj;
      return other.bytes.equals(this.bytes);
    }

    @Override
    public int hashCode() {
      return Arrays.hashCode(bytesArray());
    }

    /**
     * @return The bytes of this seed.
     */
    public Bytes bytes() {
      return bytes;
    }

    /**
     * @return The bytes of this seed.
     */
    public byte[] bytesArray() {
      return bytes.toArrayUnsafe();
    }
  }

  /**
   * A Signature key pair.
   */
  public static final class KeyPair {

    private final Signature.PublicKey publicKey;
    private final Signature.SecretKey secretKey;

    /**
     * Create a {@link Signature.KeyPair} from pair of keys.
     *
     * @param publicKey The bytes for the public key.
     * @param secretKey The bytes for the secret key.
     */
    public KeyPair(Signature.PublicKey publicKey, Signature.SecretKey secretKey) {
      this.publicKey = publicKey;
      this.secretKey = secretKey;
    }

    /**
     * Create a {@link Signature.KeyPair} from an array of secret key bytes.
     *
     * @param secretKey The secret key.
     * @return A {@link Signature.KeyPair}.
     */
    public static Signature.KeyPair forSecretKey(Signature.SecretKey secretKey) {
      byte[] pubKey = new byte[Signature.PublicKey.length()];
      Sodium.crypto_sign_ed25519_sk_to_pk(pubKey, secretKey.bytesArray());

      return new Signature.KeyPair(PublicKey.fromBytes(pubKey), secretKey);
    }

    /**
     * Generate a new key using a random generator.
     *
     * @return A randomly generated key pair.
     */
    public static Signature.KeyPair random() {
      Bytes secretKeyBytes = Bytes.wrap(new byte[Signature.SecretKey.length()]);
      Bytes publicKeyBytes = Bytes.wrap(new byte[Signature.PublicKey.length()]);

      int rc = Sodium.crypto_sign_keypair(publicKeyBytes.toArrayUnsafe(), secretKeyBytes.toArrayUnsafe());
      if (rc != 0) {
        throw new SodiumException("crypto_sign_keypair: failed with result " + rc);
      }
      Signature.PublicKey pk = new Signature.PublicKey(publicKeyBytes);
      Signature.SecretKey sk = new Signature.SecretKey(secretKeyBytes);
      return new Signature.KeyPair(pk, sk);
    }

    /**
     * Generate a new key using a seed.
     *
     * @param seed A seed.
     * @return The generated key pair.
     */
    public static Signature.KeyPair fromSeed(Signature.Seed seed) {
      Bytes publicKey = Bytes.wrap(new byte[Signature.PublicKey.length()]);
      Bytes secretKey = Bytes.wrap(new byte[Signature.SecretKey.length()]);

      int rc = Sodium.crypto_sign_seed_keypair(publicKey.toArrayUnsafe(), secretKey.toArrayUnsafe(), seed.bytesArray());
      if (rc != 0) {
        throw new SodiumException("crypto_sign_keypair: failed with result " + rc);
      }
      Signature.PublicKey pk = new Signature.PublicKey(publicKey);
      Signature.SecretKey sk = new Signature.SecretKey(secretKey);
      return new Signature.KeyPair(pk, sk);
    }

    /**
     * @return The public key of the key pair.
     */
    public Signature.PublicKey publicKey() {
      return publicKey;
    }

    /**
     * @return The secret key of the key pair.
     */
    public Signature.SecretKey secretKey() {
      return secretKey;
    }

    @Override
    public boolean equals(Object obj) {
      if (obj == this) {
        return true;
      }
      if (!(obj instanceof Signature.KeyPair)) {
        return false;
      }
      Signature.KeyPair other = (Signature.KeyPair) obj;
      return this.publicKey.equals(other.publicKey) && this.secretKey.equals(other.secretKey);
    }

    @Override
    public int hashCode() {
      return Objects.hash(publicKey, secretKey);
    }
  }

  private Signature() {}

  /**
   * Signs a message for a given key.
   *
   * @param message The message to sign.
   * @param secretKey The secret key to sign the message with.
   * @return The signature of the message.
   */
  public static Bytes signDetached(Bytes message, Signature.SecretKey secretKey) {
    return Bytes.wrap(signDetached(message.toArrayUnsafe(), secretKey));
  }

  /**
   * Signs a message for a given key.
   *
   * @param message The message to sign.
   * @param secretKey The secret key to sign the message with.
   * @return The signature of the message.
   */
  public static byte[] signDetached(byte[] message, Signature.SecretKey secretKey) {
    byte[] signature = new byte[(int) Sodium.crypto_sign_bytes()];
    LongLongByReference signatureLengthReference = new LongLongByReference();
    int rc = Sodium
        .crypto_sign_detached(signature, signatureLengthReference, message, message.length, secretKey.bytesArray());
    if (rc != 0) {
      throw new SodiumException("crypto_sign_detached: failed with result " + rc);
    }

    return signature;
  }

  /**
   * Decrypt a message using a given key.
   *
   * @param message The cipher text to decrypt.
   * @param signature The public key of the sender.
   * @param publicKey The secret key of the receiver.
   * @return whether the signature matches the message according to the public key.
   */
  public static boolean verifyDetached(Bytes message, Bytes signature, Signature.PublicKey publicKey) {
    return verifyDetached(message.toArrayUnsafe(), signature.toArrayUnsafe(), publicKey);
  }

  /**
   * Decrypt a message using a given key.
   *
   * @param message The cipher text to decrypt.
   * @param signature The public key of the sender.
   * @param publicKey The secret key of the receiver.
   * @return whether the signature matches the message according to the public key.
   */
  public static boolean verifyDetached(byte[] message, byte[] signature, Signature.PublicKey publicKey) {
    int rc = Sodium.crypto_sign_verify_detached(signature, message, message.length, publicKey.bytesArray());
    if (rc == -1) {
      return false;
    }
    if (rc != 0) {
      throw new SodiumException("crypto_sign_verify_detached: failed with result " + rc);
    }

    return true;
  }

  /**
   * Signs a message for a given key.
   *
   * @param message The message to sign.
   * @param secretKey The secret key to sign the message with.
   * @return The signature prepended to the message
   */
  public static Bytes sign(Bytes message, Signature.SecretKey secretKey) {
    return Bytes.wrap(sign(message.toArrayUnsafe(), secretKey));
  }

  /**
   * Signs a message for a given key.
   *
   * @param message The message to sign.
   * @param secretKey The secret key to sign the message with.
   * @return The signature prepended to the message
   */
  public static byte[] sign(byte[] message, Signature.SecretKey secretKey) {
    byte[] signature = new byte[(int) Sodium.crypto_sign_bytes() + message.length];
    LongLongByReference signatureLengthReference = new LongLongByReference();
    int rc = Sodium.crypto_sign(signature, signatureLengthReference, message, message.length, secretKey.bytesArray());
    if (rc != 0) {
      throw new SodiumException("crypto_sign: failed with result " + rc);
    }

    return signature;
  }

  /**
   * Verifies the signature of the signed message using the public key and returns the message.
   * 
   * @param signed signed message (signature + message)
   * @param publicKey pk used to verify the signature
   * @return the message
   */
  public static Bytes verify(Bytes signed, Signature.PublicKey publicKey) {
    return Bytes.wrap(verify(signed.toArrayUnsafe(), publicKey));
  }

  /**
   * Verifies the signature of the signed message using the public key and returns the message.
   * 
   * @param signed signed message (signature + message)
   * @param publicKey pk used to verify the signature
   * @return the message
   */
  public static byte[] verify(byte[] signed, Signature.PublicKey publicKey) {

    byte[] message = new byte[signed.length];
    LongLongByReference messageLongReference = new LongLongByReference();
    int rc = Sodium.crypto_sign_open(message, messageLongReference, signed, signed.length, publicKey.bytesArray());
    if (rc != 0) {
      throw new SodiumException("crypto_sign_open: failed with result " + rc);
    }

    return Arrays.copyOfRange(message, 0, messageLongReference.intValue());
  }
}
