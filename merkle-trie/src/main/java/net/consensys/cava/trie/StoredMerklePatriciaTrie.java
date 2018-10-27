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
package net.consensys.cava.trie;

import static java.nio.charset.StandardCharsets.UTF_8;

import net.consensys.cava.bytes.Bytes;
import net.consensys.cava.bytes.Bytes32;

import java.util.function.Function;

public interface StoredMerklePatriciaTrie<V> extends MerkleTrie<Bytes, V> {

  /**
   * Create a trie.
   *
   * @param storage The storage to use for persistence.
   * @param valueSerializer A function for serializing values to bytes.
   * @param valueDeserializer A function for deserializing values from bytes.
   * @param <V> The serialized type.
   * @return A new merkle trie.
   */
  static <V> StoredMerklePatriciaTrie<V> create(
      MerkleStorage storage,
      Function<V, Bytes> valueSerializer,
      Function<Bytes, V> valueDeserializer) {
    return new net.consensys.cava.trie.experimental.StoredMerklePatriciaTrie<>(
        new CoroutineMerkleStorageAdapter(storage),
        valueSerializer,
        valueDeserializer);
  }

  /**
   * Create a trie.
   *
   * @param storage The storage to use for persistence.
   * @param rootHash The initial root has for the trie, which should be already present in {@code storage}.
   * @param valueSerializer A function for serializing values to bytes.
   * @param valueDeserializer A function for deserializing values from bytes.
   * @param <V> The serialized type.
   * @return A new merkle trie.
   */
  static <V> StoredMerklePatriciaTrie<V> create(
      MerkleStorage storage,
      Bytes32 rootHash,
      Function<V, Bytes> valueSerializer,
      Function<Bytes, V> valueDeserializer) {
    return new net.consensys.cava.trie.experimental.StoredMerklePatriciaTrie<>(
        new CoroutineMerkleStorageAdapter(storage),
        rootHash,
        valueSerializer,
        valueDeserializer);
  }

  /**
   * Create a trie with value of type {@link Bytes}.
   *
   * @param storage The storage to use for persistence.
   * @return A new merkle trie.
   */
  static StoredMerklePatriciaTrie<Bytes> storingBytes(MerkleStorage storage) {
    return create(storage, bytes -> bytes, bytes -> bytes);
  }

  /**
   * Create a trie with value of type {@link Bytes}.
   *
   * @param storage The storage to use for persistence.
   * @param rootHash The initial root has for the trie, which should be already present in `storage`.
   * @return A new merkle trie.
   */
  static StoredMerklePatriciaTrie<Bytes> storingBytes(MerkleStorage storage, Bytes32 rootHash) {
    return create(storage, rootHash, bytes -> bytes, bytes -> bytes);
  }

  /**
   * Create a trie with value of type {@link String}.
   *
   * Strings are stored in UTF-8 encoding.
   *
   * @param storage The storage to use for persistence.
   * @return A new merkle trie.
   */
  static StoredMerklePatriciaTrie<String> storingStrings(MerkleStorage storage) {
    return create(storage, s -> Bytes.wrap(s.getBytes(UTF_8)), bytes -> new String(bytes.toArrayUnsafe(), UTF_8));
  }

  /**
   * Create a trie with value of type {@link String}.
   *
   * Strings are stored in UTF-8 encoding.
   *
   * @param storage The storage to use for persistence.
   * @param rootHash The initial root has for the trie, which should be already present in `storage`.
   * @return A new merkle trie.
   */
  static StoredMerklePatriciaTrie<String> storingStrings(MerkleStorage storage, Bytes32 rootHash) {
    return create(
        storage,
        rootHash,
        s -> Bytes.wrap(s.getBytes(UTF_8)),
        bytes -> new String(bytes.toArrayUnsafe(), UTF_8));
  }

  /**
   * Forces any cached trie nodes to be released, so they can be garbage collected.
   * <p>
   * Note: nodes are already stored using {@link java.lang.ref.SoftReference}'s, so they will be released automatically
   * based on memory demands.
   */
  void clearCache();
}
