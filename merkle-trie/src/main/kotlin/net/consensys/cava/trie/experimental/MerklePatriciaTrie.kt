package net.consensys.cava.trie.experimental

import net.consensys.cava.bytes.Bytes
import net.consensys.cava.bytes.Bytes32
import net.consensys.cava.trie.CompactEncoding.bytesToPath
import java.util.function.Function

internal fun bytesIdentity(b: Bytes): Bytes = b
internal fun stringSerializer(s: String): Bytes = Bytes.wrap(s.toByteArray(Charsets.UTF_8))
internal fun stringDeserializer(b: Bytes): String = String(b.toArrayUnsafe(), Charsets.UTF_8)

/**
 * An in-memory [MerkleTrie].
 *
 * @param <V> The type of values stored by this trie.
 * @param valueSerializer A function for serializing values to bytes.
 * @constructor Creates an empty trie.
 */
class MerklePatriciaTrie<V>(valueSerializer: (V) -> Bytes) : MerkleTrie<Bytes, V> {

  companion object {
    /**
     * Create a trie with keys and values of type [Bytes].
     */
    @JvmStatic
    fun storingBytes(): MerklePatriciaTrie<Bytes> = MerklePatriciaTrie(::bytesIdentity)

    /**
     * Create a trie with value of type [String].
     *
     * Strings are stored in UTF-8 encoding.
     */
    @JvmStatic
    fun storingStrings(): MerklePatriciaTrie<String> = MerklePatriciaTrie(::stringSerializer)
  }

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

  override suspend fun get(key: Bytes): V? = root.accept(getVisitor, bytesToPath(key)).value()

  override suspend fun put(key: Bytes, value: V?) {
    if (value == null) {
      return remove(key)
    }
    this.root = root.accept(PutVisitor(nodeFactory, value), bytesToPath(key))
  }

  override suspend fun remove(key: Bytes) {
    this.root = root.accept(removeVisitor, bytesToPath(key))
  }

  override fun rootHash(): Bytes32 = root.hash()

  override fun toString(): String {
    return javaClass.simpleName + "[" + rootHash() + "]"
  }
}
