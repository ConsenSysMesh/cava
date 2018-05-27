package net.consensys.cava.trie.experimental

import net.consensys.cava.bytes.Bytes

internal interface NodeFactory<V> {

  suspend fun createExtension(path: Bytes, child: Node<V>): Node<V>

  suspend fun createBranch(leftIndex: Byte, left: Node<V>, rightIndex: Byte, right: Node<V>): Node<V>

  suspend fun createBranch(newChildren: List<Node<V>>, value: V?): Node<V>

  suspend fun createLeaf(path: Bytes, value: V): Node<V>
}
