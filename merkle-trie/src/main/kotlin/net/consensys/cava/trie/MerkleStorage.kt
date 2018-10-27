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
package net.consensys.cava.trie

import net.consensys.cava.bytes.Bytes
import net.consensys.cava.bytes.Bytes32
import net.consensys.cava.concurrent.AsyncCompletion
import net.consensys.cava.concurrent.AsyncResult
import net.consensys.cava.concurrent.coroutines.experimental.await

/**
 * Storage for use in a [StoredMerklePatriciaTrie].
 */
interface MerkleStorage {

  /**
   * Get the stored content under the given hash.
   *
   * @param hash The hash for the content.
   * @return An [AsyncResult] that will complete with the stored content or {@code null} if not found.
   */
  fun getAsync(hash: Bytes32): AsyncResult<Bytes?>

  /**
   * Store content with a given hash.
   *
   * Note: if the storage implementation already contains content for the given hash, it does not need to replace the
   * existing content.
   *
   * @param hash The hash for the content.
   * @param content The content to store.
   * @return An [AsyncCompletion] that will complete when the content is stored.
   */
  fun putAsync(hash: Bytes32, content: Bytes): AsyncCompletion
}

internal class CoroutineMerkleStorageAdapter(
  private val storage: MerkleStorage
) : net.consensys.cava.trie.experimental.MerkleStorage {

  override suspend fun get(hash: Bytes32): Bytes? = storage.getAsync(hash).await()

  override suspend fun put(hash: Bytes32, content: Bytes) = storage.putAsync(hash, content).await()
}
