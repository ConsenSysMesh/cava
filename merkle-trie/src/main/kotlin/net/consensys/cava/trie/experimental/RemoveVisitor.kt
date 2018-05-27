package net.consensys.cava.trie.experimental

import net.consensys.cava.bytes.Bytes
import net.consensys.cava.trie.CompactEncoding

internal class RemoveVisitor<V> : NodeVisitor<V> {

  override suspend fun visit(extensionNode: ExtensionNode<V>, path: Bytes): Node<V> {
    val extensionPath = extensionNode.path()
    val commonPathLength = extensionPath.commonPrefixLength(path)
    assert(commonPathLength < path.size()) { "Visiting path doesn't end with a non-matching terminator" }

    if (commonPathLength == extensionPath.size()) {
      val child = extensionNode.child()
      val updatedChild = child.accept(this, path.slice(commonPathLength))
      return extensionNode.replaceChild(updatedChild)
    }

    // The path diverges before the end of the extension, so it cannot match

    return extensionNode
  }

  override suspend fun visit(branchNode: BranchNode<V>, path: Bytes): Node<V> {
    assert(path.size() > 0) { "Visiting path doesn't end with a non-matching terminator" }

    val childIndex = path.get(0)
    if (childIndex == CompactEncoding.LEAF_TERMINATOR) {
      return branchNode.removeValue()
    }

    val updatedChild = branchNode.child(childIndex).accept(this, path.slice(1))
    return branchNode.replaceChild(childIndex, updatedChild)
  }

  override suspend fun visit(leafNode: LeafNode<V>, path: Bytes): Node<V> {
    val leafPath = leafNode.path()
    val commonPathLength = leafPath.commonPrefixLength(path)
    if (commonPathLength == leafPath.size()) {
      return NullNode.instance()
    }
    return leafNode
  }

  override suspend fun visit(nullNode: NullNode<V>, path: Bytes): Node<V> = NullNode.instance()
}
