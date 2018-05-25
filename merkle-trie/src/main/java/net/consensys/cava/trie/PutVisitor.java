package net.consensys.cava.trie;

import net.consensys.cava.bytes.Bytes;
import net.consensys.cava.concurrent.AsyncResult;

final class PutVisitor<V> implements NodeVisitor<V> {
  private final NodeFactory<V> nodeFactory;
  private final V value;

  PutVisitor(NodeFactory<V> nodeFactory, V value) {
    this.nodeFactory = nodeFactory;
    this.value = value;
  }

  @Override
  public AsyncResult<Node<V>> visit(ExtensionNode<V> extensionNode, Bytes path) {
    return extensionNode.path().then(extensionPath -> {
      int commonPathLength = extensionPath.commonPrefixLength(path);
      assert commonPathLength < path.size() : "Visiting path doesn't end with a non-matching terminator";

      if (commonPathLength == extensionPath.size()) {
        return extensionNode.child().then(child -> child.accept(this, path.slice(commonPathLength))).then(
            extensionNode::replaceChild);
      }

      // The path diverges before the end of the extension, so create a new branch

      byte leafIndex = path.get(commonPathLength);
      Bytes leafPath = path.slice(commonPathLength + 1);

      byte extensionIndex = extensionPath.get(commonPathLength);
      return extensionNode.replacePath(extensionPath.slice(commonPathLength + 1)).then(
          updatedExtension -> nodeFactory
              .createLeaf(leafPath, value)
              .then(leaf -> nodeFactory.createBranch(leafIndex, leaf, extensionIndex, updatedExtension))
              .then(branch -> {
                if (commonPathLength > 0) {
                  return nodeFactory.createExtension(extensionPath.slice(0, commonPathLength), branch);
                } else {
                  return AsyncResult.completed(branch);
                }
              }));
    });
  }

  @Override
  public AsyncResult<Node<V>> visit(BranchNode<V> branchNode, Bytes path) {
    assert path.size() > 0 : "Visiting path doesn't end with a non-matching terminator";

    byte childIndex = path.get(0);
    if (childIndex == CompactEncoding.LEAF_TERMINATOR) {
      return branchNode.replaceValue(value);
    }

    return branchNode.child(childIndex).then(child -> child.accept(this, path.slice(1))).then(
        updatedChild -> branchNode.replaceChild(childIndex, updatedChild));
  }

  @Override
  public AsyncResult<Node<V>> visit(LeafNode<V> leafNode, Bytes path) {
    return leafNode.path().then(leafPath -> {
      int commonPathLength = leafPath.commonPrefixLength(path);

      // Check if the current leaf node should be replaced
      if (commonPathLength == leafPath.size() && commonPathLength == path.size()) {
        return nodeFactory.createLeaf(leafPath, value);
      }

      assert commonPathLength < leafPath.size()
          && commonPathLength < path.size() : "Should not have consumed non-matching terminator";

      // The current leaf path must be split to accommodate the new value.

      byte newLeafIndex = path.get(commonPathLength);
      Bytes newLeafPath = path.slice(commonPathLength + 1);

      byte updatedLeafIndex = leafPath.get(commonPathLength);
      return leafNode.replacePath(leafPath.slice(commonPathLength + 1)).then(
          updatedLeaf -> nodeFactory
              .createLeaf(newLeafPath, value)
              .then(leaf -> nodeFactory.createBranch(updatedLeafIndex, updatedLeaf, newLeafIndex, leaf))
              .then(branch -> {
                if (commonPathLength > 0) {
                  return nodeFactory.createExtension(leafPath.slice(0, commonPathLength), branch);
                } else {
                  return AsyncResult.completed(branch);
                }
              }));
    });
  }

  @Override
  public AsyncResult<Node<V>> visit(NullNode<V> nullNode, Bytes path) {
    return nodeFactory.createLeaf(path, value);
  }
}
