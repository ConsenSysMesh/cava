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
package net.consensys.cava.kv.experimental

import io.lettuce.core.RedisClient
import io.lettuce.core.RedisURI
import io.lettuce.core.api.StatefulRedisConnection
import io.lettuce.core.api.async.RedisAsyncCommands
import kotlinx.coroutines.future.await
import net.consensys.cava.bytes.Bytes
import java.net.InetAddress
import java.util.concurrent.CompletionStage

/**
 * A key-value store backed by Redis.
 *
 * @param uri The uri to the Redis store.
 * @constructor Open a Redis-backed key-value store.
 */
class RedisKeyValueStore(uri: String)
  : KeyValueStore, net.consensys.cava.kv.RedisKeyValueStore {

  private val conn: StatefulRedisConnection<Bytes, Bytes>
  private val asyncCommands: RedisAsyncCommands<Bytes, Bytes>

  /**
   * Open a Redis-backed key-value store.
   *
   * @param port The port for the Redis store.
   * @param address The address for the Redis store.
   */
  @JvmOverloads
  constructor(
    port: Int = 6379,
    address: InetAddress = InetAddress.getLoopbackAddress()
  ) : this(RedisURI.create(address.hostAddress, port).toURI().toString())

  init {
    val redisClient = RedisClient.create(uri)
    conn = redisClient.connect(net.consensys.cava.kv.RedisKeyValueStore.codec())
    asyncCommands = conn.async()
  }

  override suspend fun get(key: Bytes): Bytes? = asyncCommands.get(key).await()

  override suspend fun put(key: Bytes, value: Bytes) {
    val future: CompletionStage<String> = asyncCommands.set(key, value)
    future.await()
  }

  override fun close() {
    conn.close()
  }
}
