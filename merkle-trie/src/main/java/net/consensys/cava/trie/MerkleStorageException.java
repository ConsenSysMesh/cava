package net.consensys.cava.trie;

/**
 * This exception is thrown when there is an issue retrieving or decoding values from {@link MerkleStorage}.
 */
public class MerkleStorageException extends RuntimeException {

  public MerkleStorageException(String message) {
    super(message);
  }

  public MerkleStorageException(String message, Exception cause) {
    super(message, cause);
  }
}
