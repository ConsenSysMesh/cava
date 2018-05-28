package net.consensys.cava.trie.experimental

/**
 * This exception is thrown when there is an issue retrieving or decoding values from [MerkleStorage].
 */
class MerkleStorageException : RuntimeException {

  constructor(message: String) : super(message)

  constructor(message: String, cause: Exception) : super(message, cause)
}
