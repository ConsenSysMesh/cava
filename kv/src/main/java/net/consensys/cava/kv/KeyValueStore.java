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
package net.consensys.cava.kv;

import net.consensys.cava.bytes.Bytes;
import net.consensys.cava.concurrent.AsyncCompletion;
import net.consensys.cava.concurrent.AsyncResult;

import java.io.Closeable;

/**
 * A key-value store.
 */
public interface KeyValueStore extends Closeable {

  /**
   * Retrieves data from the store.
   *
   * @param key The key for the content.
   * @return An {@link AsyncResult} that will complete with the stored content, or <tt>null</tt> if no content was
   *         available.
   */
  AsyncResult<Bytes> getAsync(Bytes key);

  /**
   * Puts data into the store.
   *
   * Note: if the storage implementation already contains content for the given key, it does not need to replace the
   * existing content.
   *
   * @param key The key to associate with the data, for use when retrieving.
   * @param value The data to store.
   * @return An {@link AsyncCompletion} that will complete when the content is stored.
   */
  AsyncCompletion putAsync(Bytes key, Bytes value);
}
