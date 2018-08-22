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

import java.io.IOException;
import java.nio.file.Path;

import org.iq80.leveldb.Options;

/**
 * A key-value store backed by LevelDB.
 */
public interface LevelDBKeyValueStore extends KeyValueStore {

  /**
   * Open a LevelDB-backed key-value store.
   *
   * @param dbPath The path to the levelDB database.
   * @return A key-value store.
   * @throws IOException If an I/O error occurs.
   */
  static LevelDBKeyValueStore open(Path dbPath) throws IOException {
    return new net.consensys.cava.kv.experimental.LevelDBKeyValueStore(dbPath);
  }

  /**
   * Open a LevelDB-backed key-value store.
   *
   * @param dbPath The path to the levelDB database.
   * @param options Options for the levelDB database.
   * @return A key-value store.
   * @throws IOException If an I/O error occurs.
   */
  static LevelDBKeyValueStore open(Path dbPath, Options options) throws IOException {
    return new net.consensys.cava.kv.experimental.LevelDBKeyValueStore(dbPath, options);
  }
}
