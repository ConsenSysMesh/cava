package net.consensys.cava.trie;

import static net.consensys.cava.crypto.Hash.keccak256;

import net.consensys.cava.bytes.Bytes;
import net.consensys.cava.bytes.Bytes32;
import net.consensys.cava.concurrent.AsyncCompletion;
import net.consensys.cava.concurrent.AsyncResult;
import net.consensys.cava.rlp.RLP;

import java.util.Optional;

/**
 * An asynchronous Merkle Trie.
 */
public interface AsyncMerkleTrie<K, V> {

  Bytes32 EMPTY_TRIE_ROOT_HASH = keccak256(RLP.encodeValue(Bytes.EMPTY));

  /**
   * Returns the value that corresponds to the specified key, or an empty byte array if no such value exists.
   *
   * @param key The key of the value to be returned.
   * @return An Optional containing the value that corresponds to the specified key, or an empty Optional if no such
   *         value exists.
   */
  AsyncResult<Optional<V>> get(K key);

  /**
   * Updates the value that corresponds to the specified key, creating the value if one does not already exist.
   * <p>
   * If the value is null, deletes the value that corresponds to the specified key, if such a value exists.
   *
   * @param key The key that corresponds to the value to be updated.
   * @param value The value to associate the key with.
   * @return A completion that will complete when the value has been put into the trie.
   */
  AsyncCompletion put(K key, V value);

  /**
   * Deletes the value that corresponds to the specified key, if such a value exists.
   *
   * @param key The key of the value to be deleted.
   * @return A completion that will complete when the value has been removed.
   */
  AsyncCompletion remove(K key);

  /**
   * Returns the KECCAK256 hash of the root node of the trie.
   *
   * @return The KECCAK256 hash of the root node of the trie.
   */
  Bytes32 rootHash();
}
