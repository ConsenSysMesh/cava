package net.consensys.cava.trie;

import net.consensys.cava.bytes.Bytes;
import net.consensys.cava.bytes.Bytes32;
import net.consensys.cava.concurrent.AsyncResult;

import java.util.Optional;

interface Node<V> {

  AsyncResult<Node<V>> accept(NodeVisitor<V> visitor, Bytes path);

  AsyncResult<Bytes> path();

  AsyncResult<Optional<V>> value();

  Bytes rlp();

  Bytes rlpRef();

  Bytes32 hash();

  AsyncResult<Node<V>> replacePath(Bytes path);
}
