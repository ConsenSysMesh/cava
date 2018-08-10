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

import io.lettuce.core.RedisClient
import io.lettuce.core.RedisURI
import io.lettuce.core.api.StatefulRedisConnection
import io.lettuce.core.api.async.RedisAsyncCommands
import io.lettuce.core.codec.RedisCodec
import kotlinx.coroutines.experimental.future.await
import net.consensys.cava.bytes.Bytes
import java.net.InetAddress
import java.nio.ByteBuffer
import java.util.concurrent.CompletionStage

class RedisKeyValueStore(uri: String)
  : KeyValueStore {

  private val conn: StatefulRedisConnection<Bytes, Bytes>
  private val asyncCommands: RedisAsyncCommands<Bytes, Bytes>

  @JvmOverloads
  constructor(
    port: Int = 6379,
    networkInterface: InetAddress = InetAddress.getLoopbackAddress()
  ) : this(RedisURI.create(networkInterface.hostAddress, port).toURI().toString())

  init {
    val redisClient = RedisClient.create(uri)
    conn = redisClient.connect(BytesRedisCodec())
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

  class BytesRedisCodec : RedisCodec<Bytes, Bytes> {
    override fun decodeKey(bytes: ByteBuffer?): Bytes? {
      return if (bytes == null) {
        null
      } else {
        Bytes.wrapByteBuffer(bytes)
      }
    }

    override fun encodeValue(value: Bytes?): ByteBuffer {
      return ByteBuffer.wrap(value?.toArrayUnsafe() ?: ByteArray(0))
    }

    override fun encodeKey(key: Bytes?): ByteBuffer {
      return ByteBuffer.wrap(key?.toArrayUnsafe() ?: ByteArray(0))
    }

    override fun decodeValue(bytes: ByteBuffer?): Bytes? {
      return if (bytes == null) {
        null
      } else {
        Bytes.wrapByteBuffer(bytes)
      }
    }
  }
}
