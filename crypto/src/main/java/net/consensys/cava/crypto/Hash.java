package net.consensys.cava.crypto;

import net.consensys.cava.bytes.Bytes;
import net.consensys.cava.bytes.Bytes32;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

/**
 * Various utilities for providing hashes (digests) of arbitrary data.
 *
 * Requires the BouncyCastleProvider to be loaded and available. See
 * https://www.bouncycastle.org/wiki/display/JA1/Provider+Installation for detail.
 */
public final class Hash {
  private Hash() {}

  private static String KECCAK_256 = "KECCAK-256";
  private static String SHA_256 = "SHA-256";
  private static String SHA3_256 = "SHA3-256";

  /**
   * Helper method to generate a digest using the provided algorithm.
   *
   * @param input The input bytes to produce the digest for.
   * @param alg The name of the digest algorithm to use.
   * @return A digest.
   * @throws NoSuchAlgorithmException If no Provider supports a MessageDigestSpi implementation for the specified
   *         algorithm.
   */
  public static byte[] digestUsingAlgorithm(byte[] input, String alg) throws NoSuchAlgorithmException {
    MessageDigest digest = MessageDigest.getInstance(alg);
    digest.update(input);
    return digest.digest();
  }

  /**
   * Helper method to generate a digest using the provided algorithm.
   *
   * @param input The input bytes to produce the digest for.
   * @param alg The name of the digest algorithm to use.
   * @return A digest.
   * @throws NoSuchAlgorithmException If no Provider supports a MessageDigestSpi implementation for the specified
   *         algorithm.
   */
  public static Bytes digestUsingAlgorithm(Bytes input, String alg) throws NoSuchAlgorithmException {
    MessageDigest digest = MessageDigest.getInstance(alg);
    input.update(digest);
    return Bytes.wrap(digest.digest());
  }

  /**
   * Digest using SHA2-256.
   *
   * @param input The input bytes to produce the digest for.
   * @return A digest.
   */
  public static byte[] sha256(byte[] input) {
    try {
      return digestUsingAlgorithm(input, SHA_256);
    } catch (NoSuchAlgorithmException e) {
      throw new IllegalStateException("Algorithm should be available but was not", e);
    }
  }

  /**
   * Digest using SHA2-256.
   *
   * @param input The input bytes to produce the digest for.
   * @return A digest.
   */
  public static Bytes32 sha256(Bytes input) {
    try {
      return Bytes32.wrap(digestUsingAlgorithm(input, SHA_256).toArrayUnsafe());
    } catch (NoSuchAlgorithmException e) {
      throw new IllegalStateException("Algorithm should be available but was not", e);
    }
  }

  /**
   * Digest using keccak-256.
   *
   * @param input The input bytes to produce the digest for.
   * @return A digest.
   */
  public static byte[] keccak256(byte[] input) {
    try {
      return digestUsingAlgorithm(input, KECCAK_256);
    } catch (NoSuchAlgorithmException e) {
      throw new IllegalStateException("Algorithm should be available but was not", e);
    }
  }

  /**
   * Digest using keccak-256.
   *
   * @param input The input bytes to produce the digest for.
   * @return A digest.
   */
  public static Bytes32 keccak256(Bytes input) {
    try {
      return Bytes32.wrap(digestUsingAlgorithm(input, KECCAK_256).toArrayUnsafe());
    } catch (NoSuchAlgorithmException e) {
      throw new IllegalStateException("Algorithm should be available but was not", e);
    }
  }

  /**
   * Digest using SHA-3-256.
   *
   * @param input The value to encode.
   * @return A digest.
   */
  public static byte[] sha3(byte[] input) {
    try {
      return digestUsingAlgorithm(input, SHA3_256);
    } catch (NoSuchAlgorithmException e) {
      throw new IllegalStateException("Algorithm should be available but was not", e);
    }
  }

  /**
   * Digest using SHA-3-256.
   *
   * @param input The value to encode.
   * @return A digest.
   */
  public static Bytes32 sha3(Bytes input) {
    try {
      return Bytes32.wrap(digestUsingAlgorithm(input, SHA3_256).toArrayUnsafe());
    } catch (NoSuchAlgorithmException e) {
      throw new IllegalStateException("Algorithm should be available but was not", e);
    }
  }
}
