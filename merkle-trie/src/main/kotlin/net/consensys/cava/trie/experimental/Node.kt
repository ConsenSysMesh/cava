package net.consensys.cava.trie.experimental

import net.consensys.cava.bytes.Bytes
import net.consensys.cava.bytes.Bytes32

internal interface Node<V> {

  suspend fun accept(visitor: NodeVisitor<V>, path: Bytes): Node<V>

  suspend fun path(): Bytes

  suspend fun value(): V?

  fun rlp(): Bytes

  fun rlpRef(): Bytes

  fun hash(): Bytes32

  suspend fun replacePath(path: Bytes): Node<V>
}
