package net.consensys.cava.trie.experimental

import net.consensys.cava.bytes.Bytes
import net.consensys.cava.bytes.Bytes32
import net.consensys.cava.trie.CompactEncoding.bytesToPath
import java.util.function.Function

/**
 * A [MerkleTrie] that persists trie nodes to a [MerkleStorage] key/value store.
 *
 * @param <V> The type of values stored by this trie.
 */
class StoredMerklePatriciaTrie<V> : MerkleTrie<Bytes, V> {

  companion object {
    /**
     * Create a trie with value of type [Bytes].
     *
     * @param storage The storage to use for persistence.
     */
    @JvmStatic
    fun storingBytes(storage: MerkleStorage): StoredMerklePatriciaTrie<Bytes> =
      StoredMerklePatriciaTrie(storage, ::bytesIdentity, ::bytesIdentity)

    /**
     * Create a trie with keys and values of type [Bytes].
     *
     * @param storage The storage to use for persistence.
     * @param rootHash The initial root has for the trie, which should be already present in `storage`.
     */
    @JvmStatic
    fun storingBytes(storage: MerkleStorage, rootHash: Bytes32): StoredMerklePatriciaTrie<Bytes> =
      StoredMerklePatriciaTrie(storage, rootHash, ::bytesIdentity, ::bytesIdentity)

    /**
     * Create a trie with value of type [String].
     *
     * Strings are stored in UTF-8 encoding.
     *
     * @param storage The storage to use for persistence.
     */
    @JvmStatic
    fun storingStrings(storage: MerkleStorage): StoredMerklePatriciaTrie<String> =
      StoredMerklePatriciaTrie(storage, ::stringSerializer, ::stringDeserializer)

    /**
     * Create a trie with keys and values of type [String].
     *
     * Strings are stored in UTF-8 encoding.
     *
     * @param storage The storage to use for persistence.
     * @param rootHash The initial root has for the trie, which should be already present in `storage`.
     */
    @JvmStatic
    fun storingStrings(storage: MerkleStorage, rootHash: Bytes32): StoredMerklePatriciaTrie<String> =
      StoredMerklePatriciaTrie(storage, rootHash, ::stringSerializer, ::stringDeserializer)
  }

  private val getVisitor = GetVisitor<V>()
  private val removeVisitor = RemoveVisitor<V>()
  private val storage: MerkleStorage
  private val nodeFactory: StoredNodeFactory<V>
  private var root: Node<V>

  /**
   * Create a trie.
   *
   * @param storage The storage to use for persistence.
   * @param valueSerializer A function for serializing values to bytes.
   * @param valueDeserializer A function for deserializing values from bytes.
   */
  constructor(
    storage: MerkleStorage,
    valueSerializer: Function<V, Bytes>,
    valueDeserializer: Function<Bytes, V>
  ) : this(storage, MerkleTrie.EMPTY_TRIE_ROOT_HASH, valueSerializer::apply, valueDeserializer::apply)

  /**
   * Create a trie.
   *
   * @param storage The storage to use for persistence.
   * @param valueSerializer A function for serializing values to bytes.
   * @param valueDeserializer A function for deserializing values from bytes.
   */
  constructor(
    storage: MerkleStorage,
    valueSerializer: (V) -> Bytes,
    valueDeserializer: (Bytes) -> V
  ) : this(storage, MerkleTrie.EMPTY_TRIE_ROOT_HASH, valueSerializer, valueDeserializer)

  /**
   * Create a trie.
   *
   * @param storage The storage to use for persistence.
   * @param rootHash The initial root has for the trie, which should be already present in `storage`.
   * @param valueSerializer A function for serializing values to bytes.
   * @param valueDeserializer A function for deserializing values from bytes.
   */
  constructor(
    storage: MerkleStorage,
    rootHash: Bytes32,
    valueSerializer: Function<V, Bytes>,
    valueDeserializer: Function<Bytes, V>
  ) : this(storage, rootHash, valueSerializer::apply, valueDeserializer::apply)

  /**
   * Create a trie.
   *
   * @param storage The storage to use for persistence.
   * @param rootHash The initial root has for the trie, which should be already present in `storage`.
   * @param valueSerializer A function for serializing values to bytes.
   * @param valueDeserializer A function for deserializing values from bytes.
   */
  constructor(
    storage: MerkleStorage,
    rootHash: Bytes32,
    valueSerializer: (V) -> Bytes,
    valueDeserializer: (Bytes) -> V
  ) {
    this.storage = storage
    this.nodeFactory = StoredNodeFactory(storage, valueSerializer, valueDeserializer)

    this.root = if (rootHash == MerkleTrie.EMPTY_TRIE_ROOT_HASH) {
      NullNode.instance()
    } else {
      StoredNode(nodeFactory, rootHash)
    }
  }

  override suspend fun get(key: Bytes): V? = root.accept(getVisitor, bytesToPath(key)).value()

  override suspend fun put(key: Bytes, value: V?) {
    if (value == null) {
      return remove(key)
    }
    updateRoot(root.accept(PutVisitor(nodeFactory, value), bytesToPath(key)))
  }

  override suspend fun remove(key: Bytes) = updateRoot(root.accept(removeVisitor, bytesToPath(key)))

  override fun rootHash(): Bytes32 = root.hash()

  /**
   * Forces any cached trie nodes to be released, so they can be garbage collected.
   *
   * Note: nodes are already stored using [java.lang.ref.SoftReference]'s, so they will be released automatically
   * based on memory demands.
   */
  fun clearCache() {
    val currentRoot = root
    if (currentRoot is StoredNode<*>) {
      currentRoot.unload()
    }
  }

  private suspend fun updateRoot(newRoot: Node<V>) {
    this.root = if (newRoot is StoredNode<*>) {
      newRoot
    } else {
      storage.put(newRoot.hash(), newRoot.rlp())
      StoredNode(nodeFactory, newRoot)
    }
  }

  /**
   * @return A string representation of the object.
   */
  override fun toString(): String {
    return javaClass.simpleName + "[" + rootHash() + "]"
  }
}
