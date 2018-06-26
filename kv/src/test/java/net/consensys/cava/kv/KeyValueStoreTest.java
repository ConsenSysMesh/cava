/*
 * Copyright 2018, ConsenSys Inc.
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
import net.consensys.cava.junit.TempDirectory;
import net.consensys.cava.junit.TempDirectoryExtension;

import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

@ExtendWith(TempDirectoryExtension.class)
class KeyValueStoreTest {

  @Test
  void testPutAndGet() throws Exception {
    Map<Bytes, Bytes> map = new HashMap<>();
    KeyValueStore store = new MapKeyValueStore(map);
    AsyncCompletion completion = store.putAsync(Bytes.of(123), Bytes.of(10, 12, 13));
    completion.join();
    Optional<Bytes> value = store.getAsync(Bytes.of(123)).get();
    assertTrue(value.isPresent());
    assertEquals(Bytes.of(10, 12, 13), value.get());
    assertEquals(Bytes.of(10, 12, 13), map.get(Bytes.of(123)));
  }

  @Test
  void testNoValue() throws Exception {
    Map<Bytes, Bytes> map = new HashMap<>();
    KeyValueStore store = new MapKeyValueStore(map);
    assertFalse(store.getAsync(Bytes.of(123)).get().isPresent());
  }

  @Test
  void testLevelDBWithoutOptions(@TempDirectory Path tempDirectory) throws Exception {
    try (LevelDBKeyValueStore leveldb = new LevelDBKeyValueStore(tempDirectory.resolve("foo").resolve("bar"))) {
      AsyncCompletion completion = leveldb.putAsync(Bytes.of(123), Bytes.of(10, 12, 13));
      completion.join();
      Optional<Bytes> value = leveldb.getAsync(Bytes.of(123)).get();
      assertTrue(value.isPresent());
      assertEquals(Bytes.of(10, 12, 13), value.get());
    }
  }
}
