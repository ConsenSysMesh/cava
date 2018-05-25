package net.consensys.cava.trie;

import static net.consensys.cava.crypto.Hash.keccak256;

import net.consensys.cava.bytes.Bytes;
import net.consensys.cava.bytes.Bytes32;
import net.consensys.cava.concurrent.AsyncResult;
import net.consensys.cava.rlp.RLP;

import java.lang.ref.SoftReference;
import java.lang.ref.WeakReference;
import java.util.Optional;

final class ExtensionNode<V> implements Node<V> {
  private final Bytes path;
  private final Node<V> child;
  private final NodeFactory<V> nodeFactory;
  private WeakReference<Bytes> rlp;
  private SoftReference<Bytes32> hash;

  ExtensionNode(Bytes path, Node<V> child, NodeFactory<V> nodeFactory) {
    assert (path.size() > 0);
    assert (path.get(path.size() - 1) != CompactEncoding.LEAF_TERMINATOR) : "Extension path ends in a leaf terminator";
    this.path = path;
    this.child = child;
    this.nodeFactory = nodeFactory;
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
    throw new UnsupportedOperationException();
  }

  public AsyncResult<Node<V>> child() {
    return AsyncResult.completed(child);
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
      writer.writeRLP(child.rlpRef());
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
    Bytes rlp = rlp();
    Bytes32 hashed = keccak256(rlp);
    hash = new SoftReference<>(hashed);
    return hashed;
  }

  public AsyncResult<Node<V>> replaceChild(Node<V> updatedChild) {
    // collapse this extension - if the child is a branch, it will create a new extension
    return updatedChild.path().then(childPath -> updatedChild.replacePath(Bytes.concatenate(path, childPath)));
  }

  @Override
  public AsyncResult<Node<V>> replacePath(Bytes path) {
    if (path.size() == 0) {
      return AsyncResult.completed(child);
    }
    return nodeFactory.createExtension(path, child);
  }
}
