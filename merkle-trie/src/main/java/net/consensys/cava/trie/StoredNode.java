package net.consensys.cava.trie;

import net.consensys.cava.bytes.Bytes;
import net.consensys.cava.bytes.Bytes32;
import net.consensys.cava.concurrent.AsyncResult;
import net.consensys.cava.concurrent.CompletableAsyncResult;
import net.consensys.cava.rlp.RLP;

import java.lang.ref.SoftReference;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;

final class StoredNode<V> implements Node<V> {
  private final StoredNodeFactory<V> nodeFactory;
  private final Bytes32 hash;
  private SoftReference<Node<V>> loaded;
  private AtomicReference<AsyncResult<Node<V>>> loader = new AtomicReference<>();

  StoredNode(StoredNodeFactory<V> nodeFactory, Bytes32 hash) {
    this.nodeFactory = nodeFactory;
    this.hash = hash;
  }

  StoredNode(StoredNodeFactory<V> nodeFactory, Node<V> node) {
    this.nodeFactory = nodeFactory;
    this.hash = node.hash();
    this.loaded = new SoftReference<>(node);
  }

  @Override
  public AsyncResult<Node<V>> accept(NodeVisitor<V> visitor, Bytes path) {
    return load().then(node -> node.accept(visitor, path).thenApply(resultNode -> {
      if (node == resultNode) {
        return this;
      }
      return resultNode;
    }));
  }

  @Override
  public AsyncResult<Bytes> path() {
    return load().then(Node::path);
  }

  @Override
  public AsyncResult<Optional<V>> value() {
    return load().then(Node::value);
  }

  @Override
  public Bytes rlp() {
    // Getting the rlp representation is only needed when persisting a concrete node
    throw new UnsupportedOperationException();
  }

  @Override
  public Bytes rlpRef() {
    if (loaded != null) {
      Node<V> node = loaded.get();
      if (node != null) {
        return node.rlpRef();
      }
    }
    // If this node was stored, then it must have a rlp larger than a hash
    return RLP.encodeValue(hash);
  }

  @Override
  public Bytes32 hash() {
    return hash;
  }

  @Override
  public AsyncResult<Node<V>> replacePath(Bytes path) {
    return load().then(node -> node.replacePath(path));
  }

  private AsyncResult<Node<V>> load() {
    if (loaded != null) {
      Node<V> node = loaded.get();
      if (node != null) {
        return AsyncResult.completed(node);
      }
    }

    CompletableAsyncResult<Node<V>> completableResult = AsyncResult.incomplete();
    while (!loader.compareAndSet(null, completableResult)) {
      // already loading
      AsyncResult<Node<V>> result = loader.get();
      if (result != null) {
        return result;
      }
    }

    // check for a loaded node again, in case a loader just completed
    if (loaded != null) {
      Node<V> node = loaded.get();
      if (node != null) {
        loader.set(null);
        return AsyncResult.completed(node);
      }
    }

    nodeFactory.retrieve(hash).accept((node, ex) -> {
      if (ex == null) {
        try {
          loaded = new SoftReference<>(node);
          loader.set(null);
          completableResult.complete(node);
        } catch (Throwable ex2) {
          completableResult.completeExceptionally(ex2);
        }
      } else {
        completableResult.completeExceptionally(ex);
      }
    });
    return completableResult;
  }

  public void unload() {
    loaded = null;
  }
}
