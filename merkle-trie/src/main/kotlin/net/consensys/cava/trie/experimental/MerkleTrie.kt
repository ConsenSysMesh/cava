package net.consensys.cava.trie.experimental

import net.consensys.cava.bytes.Bytes
import net.consensys.cava.bytes.Bytes32
import net.consensys.cava.concurrent.AsyncCompletion
import net.consensys.cava.concurrent.AsyncResult
import net.consensys.cava.concurrent.coroutines.experimental.asyncCompletion
import net.consensys.cava.concurrent.coroutines.experimental.asyncResult
import net.consensys.cava.crypto.Hash.keccak256
import net.consensys.cava.rlp.RLP
import java.util.Optional

/**
 * A Merkle Trie.
 */
interface MerkleTrie<in K, V> {

  companion object {
    val EMPTY_TRIE_ROOT_HASH: Bytes32 = keccak256(RLP.encodeValue(Bytes.EMPTY))
  }

  /**
   * Returns the value that corresponds to the specified key, or an empty byte array if no such value exists.
   *
   * @param key The key of the value to be returned.
   * @return The value that corresponds to the specified key, or null if no such value exists.
   * @throws MerkleStorageException If there is an error while accessing or decoding data from storage.
   */
  suspend fun get(key: K): V?

  /**
   * Returns the value that corresponds to the specified key, or an empty byte array if no such value exists.
   *
   * @param key The key of the value to be returned.
   * @return An Optional containing the value that corresponds to the specified key, or an empty Optional if no such
   * value exists.
   */
  fun getAsync(key: K): AsyncResult<Optional<V>> = asyncResult { Optional.ofNullable(get(key)) }

  /**
   * Updates the value that corresponds to the specified key, creating the value if one does not already exist.
   *
   * If the value is null, deletes the value that corresponds to the specified key, if such a value exists.
   *
   * @param key The key that corresponds to the value to be updated.
   * @param value The value to associate the key with.
   * @throws MerkleStorageException If there is an error while writing to storage.
   */
  suspend fun put(key: K, value: V?)

  /**
   * Updates the value that corresponds to the specified key, creating the value if one does not already exist.
   *
   * If the value is null, deletes the value that corresponds to the specified key, if such a value exists.
   *
   * @param key The key that corresponds to the value to be updated.
   * @param value The value to associate the key with.
   * @return A completion that will complete when the value has been put into the trie.
   */
  fun putAsync(key: K, value: V?): AsyncCompletion = asyncCompletion { put(key, value) }

  /**
   * Deletes the value that corresponds to the specified key, if such a value exists.
   *
   * @param key The key of the value to be deleted.
   * @throws MerkleStorageException If there is an error while writing to storage.
   */
  suspend fun remove(key: K)

  /**
   * Deletes the value that corresponds to the specified key, if such a value exists.
   *
   * @param key The key of the value to be deleted.
   * @return A completion that will complete when the value has been removed.
   */
  fun removeAsync(key: K): AsyncCompletion = asyncCompletion { remove(key) }

  /**
   * Returns the KECCAK256 hash of the root node of the trie.
   *
   * @return The KECCAK256 hash of the root node of the trie.
   */
  fun rootHash(): Bytes32
}
