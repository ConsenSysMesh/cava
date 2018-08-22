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

import java.net.InetAddress;

import io.lettuce.core.codec.RedisCodec;

/**
 * A key-value store backed by Redis.
 */
public interface RedisKeyValueStore extends KeyValueStore {

  /**
   * Open a Redis-backed key-value store.
   *
   * @param uri The uri to the Redis store.
   * @return A key-value store.
   */
  static RedisKeyValueStore open(String uri) {
    return new net.consensys.cava.kv.experimental.RedisKeyValueStore(uri);
  }

  /**
   * Open a Redis-backed key-value store.
   *
   * @param port The port for the Redis store.
   * @return A key-value store.
   */
  static RedisKeyValueStore open(int port) {
    return new net.consensys.cava.kv.experimental.RedisKeyValueStore(port);
  }

  /**
   * Open a Redis-backed key-value store.
   *
   * @param address The address for the Redis store.
   * @return A key-value store.
   */
  static RedisKeyValueStore open(InetAddress address) {
    return new net.consensys.cava.kv.experimental.RedisKeyValueStore(6379, address);
  }

  /**
   * Open a Redis-backed key-value store.
   *
   * @param port The port for the Redis store.
   * @param address The address for the Redis store.
   * @return A key-value store.
   */
  static RedisKeyValueStore open(int port, InetAddress address) {
    return new net.consensys.cava.kv.experimental.RedisKeyValueStore(port, address);
  }

  /**
   * A {@link RedisCodec} for working with cava Bytes classes.
   *
   * @return A {@link RedisCodec} for working with cava Bytes classes.
   */
  static RedisCodec<Bytes, Bytes> codec() {
    return new RedisBytesCodec();
  }
}
