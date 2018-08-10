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
package net.consensys.cava.kv

import kotlinx.coroutines.experimental.newFixedThreadPoolContext
import kotlinx.coroutines.experimental.withContext
import net.consensys.cava.bytes.Bytes
import org.fusesource.leveldbjni.JniDBFactory
import org.iq80.leveldb.DB
import org.iq80.leveldb.Options
import java.io.IOException
import java.nio.file.Files
import java.nio.file.Path
import kotlin.coroutines.experimental.CoroutineContext

/**
 * A key-value store backed by LevelDB.
 */
class LevelDBKeyValueStore
@Throws(IOException::class)
@JvmOverloads
constructor(
  databasePath: Path,
  options: Options = Options().createIfMissing(true).cacheSize((100 * 1048576).toLong()),
  // TODO: replace with IO context when https://github.com/Kotlin/kotlinx.coroutines/issues/79 is resolved
  private val context: CoroutineContext = newFixedThreadPoolContext(4, "LevelDBKeyValueStore")
) : KeyValueStore {

  private val db: DB

  init {
    Files.createDirectories(databasePath)
    db = JniDBFactory.factory.open(databasePath.toFile(), options)
  }

  override suspend fun get(key: Bytes): Bytes? = withContext(context) {
    val rawValue = db[key.toArrayUnsafe()]
    if (rawValue == null) {
      null
    } else {
      Bytes.wrap(rawValue)
    }
  }

  override suspend fun put(key: Bytes, value: Bytes) = withContext(context) {
    db.put(key.toArrayUnsafe(), value.toArrayUnsafe())
  }

  /**
   * Closes the underlying LevelDB instance.
   */
  override fun close() = db.close()
}
