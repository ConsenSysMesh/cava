package net.consensys.cava.trie;

import static net.consensys.cava.crypto.Hash.keccak256;

import net.consensys.cava.bytes.Bytes;
import net.consensys.cava.bytes.Bytes32;
import net.consensys.cava.concurrent.AsyncResult;
import net.consensys.cava.rlp.RLP;

import java.lang.ref.SoftReference;
import java.lang.ref.WeakReference;
import java.util.Optional;
import java.util.function.Function;

final class LeafNode<V> implements Node<V> {
  private final Bytes path;
  private final V value;
  private final NodeFactory<V> nodeFactory;
  private final Function<V, Bytes> valueSerializer;
  private WeakReference<Bytes> rlp;
  private SoftReference<Bytes32> hash;

  LeafNode(Bytes path, V value, NodeFactory<V> nodeFactory, Function<V, Bytes> valueSerializer) {
    this.path = path;
    this.value = value;
    this.nodeFactory = nodeFactory;
    this.valueSerializer = valueSerializer;
  }

  @Override
  public AsyncResult<Node<V>> accept(NodeVisitor<V> visitor, Bytes path) {
    return visitor.visit(this, path);
  }

  @Override
  public AsyncResult<Bytes> path() {
    return AsyncResult.completed(path);
  }

  @Override
  public AsyncResult<Optional<V>> value() {
    return AsyncResult.completed(Optional.of(value));
  }

  @Override
  public Bytes rlp() {
    if (rlp != null) {
      Bytes encoded = rlp.get();
      if (encoded != null) {
        return encoded;
      }
    }

    Bytes encoded = RLP.encodeList(writer -> {
      writer.writeValue(CompactEncoding.encode(path));
      writer.writeValue(valueSerializer.apply(value));
    });
    rlp = new WeakReference<>(encoded);
    return encoded;
  }

  @Override
  public Bytes rlpRef() {
    Bytes rlp = rlp();
    if (rlp.size() < 32) {
      return rlp;
    } else {
      return RLP.encodeValue(hash());
    }
  }

  @Override
  public Bytes32 hash() {
    if (hash != null) {
      Bytes32 hashed = hash.get();
      if (hashed != null) {
        return hashed;
      }
    }
    Bytes32 hashed = keccak256(rlp());
    hash = new SoftReference<>(hashed);
    return hashed;
  }

  @Override
  public AsyncResult<Node<V>> replacePath(Bytes path) {
    return nodeFactory.createLeaf(path, value);
  }
}
