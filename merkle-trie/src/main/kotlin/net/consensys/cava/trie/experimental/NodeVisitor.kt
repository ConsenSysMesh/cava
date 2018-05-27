package net.consensys.cava.trie.experimental

import net.consensys.cava.bytes.Bytes

internal interface NodeVisitor<V> {

  suspend fun visit(extensionNode: ExtensionNode<V>, path: Bytes): Node<V>

  suspend fun visit(branchNode: BranchNode<V>, path: Bytes): Node<V>

  suspend fun visit(leafNode: LeafNode<V>, path: Bytes): Node<V>

  suspend fun visit(nullNode: NullNode<V>, path: Bytes): Node<V>
}
