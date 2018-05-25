package net.consensys.cava.trie;

import net.consensys.cava.bytes.Bytes;
import net.consensys.cava.concurrent.AsyncResult;

import java.util.ArrayList;
import java.util.Optional;

interface NodeFactory<V> {

  AsyncResult<Node<V>> createExtension(Bytes path, Node<V> child);

  AsyncResult<Node<V>> createBranch(byte leftIndex, Node<V> left, byte rightIndex, Node<V> right);

  AsyncResult<Node<V>> createBranch(ArrayList<Node<V>> newChildren, Optional<V> value);

  AsyncResult<Node<V>> createLeaf(Bytes path, V value);
}
