package net.consensys.cava.trie;

import net.consensys.cava.bytes.Bytes;
import net.consensys.cava.concurrent.AsyncResult;

interface NodeVisitor<V> {

  AsyncResult<Node<V>> visit(ExtensionNode<V> extensionNode, Bytes path);

  AsyncResult<Node<V>> visit(BranchNode<V> branchNode, Bytes path);

  AsyncResult<Node<V>> visit(LeafNode<V> leafNode, Bytes path);

  AsyncResult<Node<V>> visit(NullNode<V> nullNode, Bytes path);
}
