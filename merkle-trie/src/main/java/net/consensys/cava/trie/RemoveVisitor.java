package net.consensys.cava.trie;

import net.consensys.cava.bytes.Bytes;
import net.consensys.cava.concurrent.AsyncResult;

final class RemoveVisitor<V> implements NodeVisitor<V> {
  private final AsyncResult<Node<V>> nullNodeResult = AsyncResult.completed(NullNode.instance());

  @Override
  public AsyncResult<Node<V>> visit(ExtensionNode<V> extensionNode, Bytes path) {
    return extensionNode.path().then(extensionPath -> {
      int commonPathLength = extensionPath.commonPrefixLength(path);
      assert commonPathLength < path.size() : "Visiting path doesn't end with a non-matching terminator";

      if (commonPathLength == extensionPath.size()) {
        return extensionNode.child().then(child -> child.accept(this, path.slice(commonPathLength))).then(
            extensionNode::replaceChild);
      }

      // The path diverges before the end of the extension, so it cannot match

      return AsyncResult.completed(extensionNode);
    });
  }

  @Override
  public AsyncResult<Node<V>> visit(BranchNode<V> branchNode, Bytes path) {
    assert path.size() > 0 : "Visiting path doesn't end with a non-matching terminator";

    byte childIndex = path.get(0);
    if (childIndex == CompactEncoding.LEAF_TERMINATOR) {
      return branchNode.removeValue();
    }

    return branchNode.child(childIndex).then(
        child -> child.accept(this, path.slice(1)).then(
            updatedChild -> branchNode.replaceChild(childIndex, updatedChild)));
  }

  @Override
  public AsyncResult<Node<V>> visit(LeafNode<V> leafNode, Bytes path) {
    return leafNode.path().then(leafPath -> {
      int commonPathLength = leafPath.commonPrefixLength(path);
      return (commonPathLength == leafPath.size()) ? nullNodeResult : AsyncResult.completed(leafNode);
    });
  }

  @Override
  public AsyncResult<Node<V>> visit(NullNode<V> nullNode, Bytes path) {
    return nullNodeResult;
  }
}
