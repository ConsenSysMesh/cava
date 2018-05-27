package net.consensys.cava.trie.experimental

import net.consensys.cava.bytes.Bytes
import net.consensys.cava.bytes.Bytes32
import net.consensys.cava.crypto.Hash.keccak256
import net.consensys.cava.rlp.RLP

internal class NullNode<V> private constructor() : Node<V> {

  companion object {
    private val RLP_NULL = RLP.encodeByteArray(ByteArray(0))
    private val HASH = keccak256(RLP_NULL)
    private val instance = NullNode<Any>()

    @Suppress("UNCHECKED_CAST")
    fun <V> instance(): NullNode<V> = instance as NullNode<V>
  }

  override suspend fun accept(visitor: NodeVisitor<V>, path: Bytes): Node<V> = visitor.visit(this, path)

  override suspend fun path(): Bytes = Bytes.EMPTY

  override suspend fun value(): V? = null

  override fun rlp(): Bytes = RLP_NULL

  override fun rlpRef(): Bytes = RLP_NULL

  override fun hash(): Bytes32 = HASH

  override suspend fun replacePath(path: Bytes): Node<V> = this
}
