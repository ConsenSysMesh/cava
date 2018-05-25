package net.consensys.cava.trie;

import static net.consensys.cava.crypto.Hash.keccak256;

import net.consensys.cava.bytes.Bytes;
import net.consensys.cava.bytes.Bytes32;
import net.consensys.cava.bytes.MutableBytes;
import net.consensys.cava.concurrent.AsyncResult;
import net.consensys.cava.rlp.RLP;

import java.lang.ref.SoftReference;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Optional;
import java.util.function.Function;

final class BranchNode<V> implements Node<V> {
  public static final byte RADIX = CompactEncoding.LEAF_TERMINATOR;
  @SuppressWarnings("rawtypes")
  private static final Node NULL_NODE = NullNode.instance();

  private final ArrayList<Node<V>> children;
  private final Optional<V> value;
  private final NodeFactory<V> nodeFactory;
  private final Function<V, Bytes> valueSerializer;
  private WeakReference<Bytes> rlp;
  private SoftReference<Bytes32> hash;

  BranchNode(
      ArrayList<Node<V>> children,
      Optional<V> value,
      NodeFactory<V> nodeFactory,
      Function<V, Bytes> valueSerializer) {
    assert (children.size() == RADIX);
    this.children = children;
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
    return AsyncResult.completed(Bytes.EMPTY);
  }

  @Override
  public AsyncResult<Optional<V>> value() {
    return AsyncResult.completed(value);
  }

  public AsyncResult<Node<V>> child(byte index) {
    return AsyncResult.completed(children.get(index));
  }

  @Override
  public Bytes rlp() {
    if (rlp != null) {
      Bytes encoded = rlp.get();
      if (encoded != null) {
        return encoded;
      }
    }
    Bytes encoded = RLP.encodeList(out -> {
      for (int i = 0; i < RADIX; ++i) {
        out.writeRLP(children.get(i).rlpRef());
      }
      if (value.isPresent()) {
        out.writeValue(valueSerializer.apply(value.get()));
      } else {
        out.writeValue(Bytes.EMPTY);
      }
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
  public AsyncResult<Node<V>> replacePath(Bytes newPath) {
    return nodeFactory.createExtension(newPath, this);
  }

  public AsyncResult<Node<V>> replaceChild(byte index, Node<V> updatedChild) {
    ArrayList<Node<V>> newChildren = new ArrayList<>(children);
    newChildren.set(index, updatedChild);

    if (updatedChild == NULL_NODE) {
      if (value.isPresent() && !hasChildren()) {
        return nodeFactory.createLeaf(Bytes.of(index), value.get());
      } else if (!value.isPresent()) {
        Optional<AsyncResult<Node<V>>> flattened = maybeFlatten(newChildren);
        if (flattened.isPresent()) {
          return flattened.get();
        }
      }
    }

    return nodeFactory.createBranch(newChildren, value);
  }

  public AsyncResult<Node<V>> replaceValue(V value) {
    return nodeFactory.createBranch(children, Optional.of(value));
  }

  public AsyncResult<Node<V>> removeValue() {
    return maybeFlatten(children).orElse(nodeFactory.createBranch(children, Optional.empty()));
  }

  private boolean hasChildren() {
    for (Node<V> child : children) {
      if (child != NULL_NODE) {
        return true;
      }
    }
    return false;
  }

  private static <V> Optional<AsyncResult<Node<V>>> maybeFlatten(ArrayList<Node<V>> children) {
    int onlyChildIndex = findOnlyChild(children);
    if (onlyChildIndex >= 0) {
      // replace the path of the only child and return it
      Node<V> onlyChild = children.get(onlyChildIndex);
      return Optional.of(onlyChild.path().then(onlyChildPath -> {
        MutableBytes completePath = MutableBytes.create(1 + onlyChildPath.size());
        completePath.set(0, (byte) onlyChildIndex);
        onlyChildPath.copyTo(completePath, 1);
        return onlyChild.replacePath(completePath);
      }));
    }
    return Optional.empty();
  }

  private static <V> int findOnlyChild(ArrayList<Node<V>> children) {
    int onlyChildIndex = -1;
    assert (children.size() == RADIX);
    for (int i = 0; i < RADIX; ++i) {
      if (children.get(i) != NULL_NODE) {
        if (onlyChildIndex >= 0) {
          return -1;
        }
        onlyChildIndex = i;
      }
    }
    return onlyChildIndex;
  }
}
