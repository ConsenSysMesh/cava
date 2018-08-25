/*
 * Copyright 2018 ConsenSys AG.
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
package net.consensys.cava.trie.experimental

import net.consensys.cava.concurrent.AsyncCompletion
import net.consensys.cava.concurrent.AsyncResult
import net.consensys.cava.concurrent.coroutines.experimental.asyncCompletion
import net.consensys.cava.concurrent.coroutines.experimental.asyncResult

/**
 * A Merkle Trie.
 */
interface MerkleTrie<in K, V> : net.consensys.cava.trie.MerkleTrie<K, V> {

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
  override fun getAsync(key: K): AsyncResult<V?> = asyncResult { get(key) }

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
  override fun putAsync(key: K, value: V?): AsyncCompletion = asyncCompletion { put(key, value) }

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
  override fun removeAsync(key: K): AsyncCompletion = asyncCompletion { remove(key) }
}
