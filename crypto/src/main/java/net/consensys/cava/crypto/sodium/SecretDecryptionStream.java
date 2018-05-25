package net.consensys.cava.crypto.sodium;

import net.consensys.cava.bytes.Bytes;

/**
 * Used to decrypt a sequence of messages, or a single message split into arbitrary chunks.
 */
public interface SecretDecryptionStream {

  /**
   * Pull a message from this secret stream.
   *
   * @param cipherText The encrypted message.
   * @return The clear text.
   */
  default Bytes pull(Bytes cipherText) {
    return Bytes.wrap(pull(cipherText.toArrayUnsafe()));
  }

  /**
   * Pull a message from this secret stream.
   *
   * @param cipherText The encrypted message.
   * @return The clear text.
   */
  byte[] pull(byte[] cipherText);

  /** @return <tt>true</tt> if no more messages should be decrypted by this stream */
  boolean isComplete();
}
