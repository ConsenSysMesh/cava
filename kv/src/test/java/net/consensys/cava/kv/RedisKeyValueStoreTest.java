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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import net.consensys.cava.bytes.Bytes;
import net.consensys.cava.concurrent.AsyncCompletion;
import net.consensys.cava.junit.RedisPort;
import net.consensys.cava.junit.RedisServerExtension;
import net.consensys.cava.kv.RedisKeyValueStore.BytesRedisCodec;

import java.net.InetAddress;
import java.util.Optional;

import io.lettuce.core.RedisClient;
import io.lettuce.core.RedisURI;
import io.lettuce.core.api.StatefulRedisConnection;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

@ExtendWith(RedisServerExtension.class)
class RedisKeyValueStoreTest {

  @Test
  void testPutAndGet(@RedisPort Integer redisPort) throws Exception {
    KeyValueStore store = new RedisKeyValueStore(redisPort);
    AsyncCompletion completion = store.putAsync(Bytes.of(123), Bytes.of(10, 12, 13));
    completion.join();
    Optional<Bytes> value = store.getAsync(Bytes.of(123)).get();
    assertTrue(value.isPresent());
    assertEquals(Bytes.of(10, 12, 13), value.get());
    RedisClient client =
        RedisClient.create(RedisURI.create(InetAddress.getLoopbackAddress().getHostAddress(), redisPort));
    try (StatefulRedisConnection<Bytes, Bytes> conn = client.connect(new BytesRedisCodec())) {
      assertEquals(Bytes.of(10, 12, 13), conn.sync().get(Bytes.of(123)));
    }
  }

  @Test
  void testNoValue(@RedisPort Integer redisPort) throws Exception {
    KeyValueStore store = new RedisKeyValueStore(redisPort, InetAddress.getLoopbackAddress());
    assertFalse(store.getAsync(Bytes.of(124)).get().isPresent());
  }

  @Test
  void testRedisCloseable(@RedisPort Integer redisPort) throws Exception {
    try (RedisKeyValueStore redis = new RedisKeyValueStore("redis://127.0.0.1:" + redisPort)) {
      AsyncCompletion completion = redis.putAsync(Bytes.of(125), Bytes.of(10, 12, 13));
      completion.join();
      Optional<Bytes> value = redis.getAsync(Bytes.of(125)).get();
      assertTrue(value.isPresent());
      assertEquals(Bytes.of(10, 12, 13), value.get());
    }
  }
}
