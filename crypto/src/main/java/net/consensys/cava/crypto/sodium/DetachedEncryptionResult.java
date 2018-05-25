package net.consensys.cava.crypto.sodium;

import net.consensys.cava.bytes.Bytes;

/**
 * The result from a detached encryption.
 */
public interface DetachedEncryptionResult {

  /**
   * @return The cipher text.
   */
  Bytes cipherText();

  /**
   * @return The cipher text.
   */
  byte[] cipherTextArray();

  /**
   * @return The message authentication code.
   */
  Bytes mac();

  /**
   * @return The message authentication code.
   */
  byte[] macArray();
}
