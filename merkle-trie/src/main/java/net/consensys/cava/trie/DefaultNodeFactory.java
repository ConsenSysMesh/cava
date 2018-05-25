package net.consensys.cava.trie;

import net.consensys.cava.bytes.Bytes;
import net.consensys.cava.concurrent.AsyncResult;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Optional;
import java.util.function.Function;

final class DefaultNodeFactory<V> implements NodeFactory<V> {
  @SuppressWarnings("rawtypes")
  private static final Node NULL_NODE = NullNode.instance();

  private Function<V, Bytes> valueSerializer;

  DefaultNodeFactory(Function<V, Bytes> valueSerializer) {
    this.valueSerializer = valueSerializer;
  }

  @Override
  public AsyncResult<Node<V>> createExtension(Bytes path, Node<V> child) {
    return AsyncResult.completed(new ExtensionNode<>(path, child, this));
  }

  @SuppressWarnings("unchecked")
  @Override
  public AsyncResult<Node<V>> createBranch(byte leftIndex, Node<V> left, byte rightIndex, Node<V> right) {
    assert (leftIndex <= BranchNode.RADIX);
    assert (rightIndex <= BranchNode.RADIX);
    assert (leftIndex != rightIndex);

    ArrayList<Node<V>> children = new ArrayList<>(Collections.nCopies(BranchNode.RADIX, (Node<V>) NULL_NODE));
    if (leftIndex == BranchNode.RADIX) {
      children.set(rightIndex, right);
      return left.value().then(value -> createBranch(children, value));
    } else if (rightIndex == BranchNode.RADIX) {
      children.set(leftIndex, left);
      return right.value().then(value -> createBranch(children, value));
    } else {
      children.set(leftIndex, left);
      children.set(rightIndex, right);
      return createBranch(children, Optional.empty());
    }
  }

  @Override
  public AsyncResult<Node<V>> createBranch(ArrayList<Node<V>> children, Optional<V> value) {
    return AsyncResult.completed(new BranchNode<>(children, value, this, valueSerializer));
  }

  @Override
  public AsyncResult<Node<V>> createLeaf(Bytes path, V value) {
    return AsyncResult.completed(new LeafNode<>(path, value, this, valueSerializer));
  }
}
