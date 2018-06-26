/*
 * Copyright 2018, ConsenSys Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package net.consensys.cava.kv

import net.consensys.cava.bytes.Bytes
import net.consensys.cava.concurrent.AsyncCompletion
import net.consensys.cava.concurrent.AsyncResult
import net.consensys.cava.concurrent.coroutines.experimental.asyncCompletion
import net.consensys.cava.concurrent.coroutines.experimental.asyncResult
import java.io.Closeable
import java.util.Optional

/**
 * A key-value store.
 */
interface KeyValueStore : Closeable {

  /**
   * Retrieves data from the store.
   *
   * @param key The key for the content.
   * @return The stored data, or null if no data was stored under the specified key.
   */
  suspend fun get(key: Bytes): Bytes?

  /**
   * Retrieves data from the store.
   *
   * @param key The key for the content.
   * @return An [AsyncResult] that will complete with the stored content,
   *         or an empty optional if no content was available.
   */
  fun getAsync(key: Bytes): AsyncResult<Optional<Bytes>> = asyncResult { Optional.ofNullable(get(key)) }

  /**
   * Puts data into the store.
   *
   * @param key The key to associate with the data, for use when retrieving.
   * @param value the data to store.
   */
  suspend fun put(key: Bytes, value: Bytes)

  /**
   * Puts data into the store.
   *
   * Note: if the storage implementation already contains content for the given key, it does not need to replace the
   * existing content.
   *
   * @param key The key to associate with the data, for use when retrieving.
   * @param value the data to store.
   * @return An [AsyncCompletion] that will complete when the content is stored.
   */
  fun putAsync(key: Bytes, value: Bytes): AsyncCompletion = asyncCompletion { put(key, value) }
}
