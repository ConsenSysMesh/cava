package net.consensys.cava.trie;

import static com.google.common.base.Preconditions.checkNotNull;
import static net.consensys.cava.trie.CompactEncoding.bytesToPath;

import net.consensys.cava.bytes.Bytes;
import net.consensys.cava.bytes.Bytes32;
import net.consensys.cava.concurrent.AsyncCompletion;
import net.consensys.cava.concurrent.AsyncResult;

import java.util.Optional;
import java.util.function.Function;

/**
 * An in-memory {@link AsyncMerkleTrie}.
 *
 * @param <V> The type of values stored by this trie.
 */
public final class AsyncMerklePatriciaTrie<K extends Bytes, V> implements AsyncMerkleTrie<K, V> {
  private final NodeVisitor<V> getVisitor = new GetVisitor<>();
  private final NodeVisitor<V> removeVisitor = new RemoveVisitor<>();
  private final DefaultNodeFactory<V> nodeFactory;

  private Node<V> root;

  /**
   * Create a trie.
   *
   * @param valueSerializer A function for serializing values to bytes.
   */
  public AsyncMerklePatriciaTrie(Function<V, Bytes> valueSerializer) {
    this.nodeFactory = new DefaultNodeFactory<>(valueSerializer);
    this.root = NullNode.instance();
  }

  @Override
  public AsyncResult<Optional<V>> get(K key) {
    checkNotNull(key);
    return root.accept(getVisitor, bytesToPath(key)).then(Node::value);
  }

  @Override
  public AsyncCompletion put(K key, V value) {
    checkNotNull(key);
    if (value == null) {
      return remove(key);
    }
    return root.accept(new PutVisitor<>(nodeFactory, value), bytesToPath(key)).thenAccept(root -> {
      this.root = root;
    });
  }

  @Override
  public AsyncCompletion remove(K key) {
    checkNotNull(key);
    return root.accept(removeVisitor, bytesToPath(key)).thenAccept(root -> {
      this.root = root;
    });
  }

  @Override
  public Bytes32 rootHash() {
    return root.hash();
  }

  @Override
  public String toString() {
    return getClass().getSimpleName() + "[" + rootHash() + "]";
  }
}
