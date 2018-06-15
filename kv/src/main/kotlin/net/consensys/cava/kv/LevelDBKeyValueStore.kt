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

import kotlinx.coroutines.experimental.Unconfined
import kotlinx.coroutines.experimental.withContext
import net.consensys.cava.bytes.Bytes
import org.iq80.leveldb.DB

/**
 * A key-value store backed by LevelDB.
 */
class LevelDBKeyValueStore(private val levelDB: DB) : KeyValueStore {

  override suspend fun get(key: Bytes): Bytes? = withContext(Unconfined) {
    val rawValue = levelDB[key.toArrayUnsafe()]
    if (rawValue == null) {
      null
    } else {
      Bytes.wrap(rawValue)
    }
  }

  override suspend fun put(key: Bytes, value: Bytes) = withContext(Unconfined) {
    levelDB.put(key.toArrayUnsafe(), value.toArrayUnsafe())
  }
}
