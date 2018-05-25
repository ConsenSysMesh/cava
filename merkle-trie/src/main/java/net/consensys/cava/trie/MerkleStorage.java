package net.consensys.cava.trie;

import net.consensys.cava.bytes.Bytes;
import net.consensys.cava.bytes.Bytes32;
import net.consensys.cava.concurrent.AsyncCompletion;
import net.consensys.cava.concurrent.AsyncResult;

import java.util.Optional;

/**
 * Storage for use in a {@link StoredAsyncMerklePatriciaTrie}.
 */
public interface MerkleStorage {

  /**
   * Get the stored content under the given hash.
   *
   * @param hash The hash for the content.
   * @return An {@link AsyncResult} that will complete with the stored content, or an exception.
   */
  AsyncResult<Optional<Bytes>> get(Bytes32 hash);

  /**
   * Store content with a given hash.
   * <p>
   * Note: if the storage implementation already contains content for the given hash, it does not need to replace the
   * existing content.
   *
   * @param hash The hash for the content.
   * @param content The content to store.
   * @return A {@link AsyncCompletion} that will complete when the content is stored, or with an exception.
   */
  AsyncCompletion put(Bytes32 hash, Bytes content);
}
