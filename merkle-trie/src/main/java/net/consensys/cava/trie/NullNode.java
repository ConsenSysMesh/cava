package net.consensys.cava.trie;

import static net.consensys.cava.crypto.Hash.keccak256;

import net.consensys.cava.bytes.Bytes;
import net.consensys.cava.bytes.Bytes32;
import net.consensys.cava.concurrent.AsyncResult;
import net.consensys.cava.rlp.RLP;

import java.util.Optional;

final class NullNode<V> implements Node<V> {
  private static final Bytes RLP_NULL = RLP.encodeByteArray(new byte[0]);
  private static final Bytes32 HASH = keccak256(RLP_NULL);
  @SuppressWarnings("rawtypes")
  private static NullNode instance = new NullNode();

  private NullNode() {}

  @SuppressWarnings("unchecked")
  static <V> NullNode<V> instance() {
    return instance;
  }

  @Override
  public AsyncResult<Node<V>> accept(NodeVisitor<V> visitor, Bytes path) {
    return visitor.visit(this, path);
  }

  @Override
  public AsyncResult<Bytes> path() {
    return AsyncResult.completed(Bytes.EMPTY);
  }

  @Override
  public AsyncResult<Optional<V>> value() {
    return AsyncResult.completed(Optional.empty());
  }

  @Override
  public Bytes rlp() {
    return RLP_NULL;
  }

  @Override
  public Bytes rlpRef() {
    return RLP_NULL;
  }

  @Override
  public Bytes32 hash() {
    return HASH;
  }

  @Override
  public AsyncResult<Node<V>> replacePath(Bytes path) {
    return AsyncResult.completed(this);
  }
}
