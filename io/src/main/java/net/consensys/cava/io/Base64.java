package net.consensys.cava.io;

import static java.nio.charset.StandardCharsets.UTF_8;

import net.consensys.cava.bytes.Bytes;

/**
 * Utility methods for encoding and decoding base64 strings.
 */
public final class Base64 {
  private Base64() {}

  /**
   * Encode a byte array to a base64 encoded string.
   *
   * @param bytes The bytes to encode.
   * @return A base64 encoded string.
   */
  public static String encodeBytes(byte[] bytes) {
    return new String(java.util.Base64.getEncoder().encode(bytes), UTF_8);
  }

  /**
   * Encode bytes to a base64 encoded string.
   *
   * @param bytes The bytes to encode.
   * @return A base64 encoded string.
   */
  public static String encode(Bytes bytes) {
    return encodeBytes(bytes.toArrayUnsafe());
  }

  /**
   * Decode a base64 encoded string to a byte array.
   *
   * @param b64 The base64 encoded string.
   * @return A byte array.
   */
  public static byte[] decodeBytes(String b64) {
    return java.util.Base64.getDecoder().decode(b64.getBytes(UTF_8));
  }

  /**
   * Decode a base64 encoded string to bytes.
   *
   * @param b64 The base64 encoded string.
   * @return The decoded bytes.
   */
  public static Bytes decode(String b64) {
    return Bytes.wrap(decodeBytes(b64));
  }
}
