package net.consensys.cava.trie.experimental

import net.consensys.cava.bytes.Bytes
import net.consensys.cava.bytes.Bytes32
import net.consensys.cava.crypto.Hash.keccak256
import net.consensys.cava.rlp.RLP
import net.consensys.cava.trie.CompactEncoding
import java.lang.ref.SoftReference
import java.lang.ref.WeakReference

internal class LeafNode<V>(
  private val path: Bytes,
  private val value: V,
  private val nodeFactory: NodeFactory<V>,
  private val valueSerializer: (V) -> Bytes
) : Node<V> {
  private var rlp: WeakReference<Bytes>? = null
  private var hash: SoftReference<Bytes32>? = null

  override suspend fun accept(visitor: NodeVisitor<V>, path: Bytes): Node<V> = visitor.visit(this, path)

  override suspend fun path(): Bytes = path

  override suspend fun value(): V? = value

  override fun rlp(): Bytes {
    val prevEncoded = rlp?.get()
    if (prevEncoded != null) {
      return prevEncoded
    }

    val encoded = RLP.encodeList { writer ->
      writer.writeValue(CompactEncoding.encode(path))
      writer.writeValue(valueSerializer(value))
    }
    rlp = WeakReference(encoded)
    return encoded
  }

  override fun rlpRef(): Bytes {
    val rlp = rlp()
    return if (rlp.size() < 32) rlp else RLP.encodeValue(hash())
  }

  override fun hash(): Bytes32 {
    val prevHashed = hash?.get()
    if (prevHashed != null) {
      return prevHashed
    }
    val hashed = keccak256(rlp())
    hash = SoftReference(hashed)
    return hashed
  }

  override suspend fun replacePath(path: Bytes): Node<V> = nodeFactory.createLeaf(path, value)
}
