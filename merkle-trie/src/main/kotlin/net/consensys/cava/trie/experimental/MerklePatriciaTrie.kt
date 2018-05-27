package net.consensys.cava.trie.experimental

import net.consensys.cava.bytes.Bytes
import net.consensys.cava.bytes.Bytes32
import net.consensys.cava.trie.CompactEncoding.bytesToPath
import java.util.function.Function

/**
 * An in-memory [MerkleTrie].
 *
 * @param <V> The type of values stored by this trie.
 * @param valueSerializer A function for serializing values to bytes.
 * @constructor Creates an empty trie.
 */
class MerklePatriciaTrie<in K : Bytes, V>(valueSerializer: (V) -> Bytes) : MerkleTrie<K, V> {
  private val getVisitor = GetVisitor<V>()
  private val removeVisitor = RemoveVisitor<V>()
  private val nodeFactory: DefaultNodeFactory<V> = DefaultNodeFactory(valueSerializer)
  private var root: Node<V> = NullNode.instance()

  /**
   * Creates an empty trie.
   *
   * @param valueSerializer A function for serializing values to bytes.
   */
  constructor(valueSerializer: Function<V, Bytes>) : this(valueSerializer::apply)

  override suspend fun get(key: K): V? = root.accept(getVisitor, bytesToPath(key)).value()

  override suspend fun put(key: K, value: V?) {
    if (value == null) {
      return remove(key)
    }
    this.root = root.accept(PutVisitor(nodeFactory, value), bytesToPath(key))
  }

  override suspend fun remove(key: K) {
    this.root = root.accept(removeVisitor, bytesToPath(key))
  }

  override fun rootHash(): Bytes32 = root.hash()

  override fun toString(): String {
    return javaClass.simpleName + "[" + rootHash() + "]"
  }
}
