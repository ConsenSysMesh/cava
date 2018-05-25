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
 * A {@link AsyncMerkleTrie} that persists trie nodes to a {@link MerkleStorage} key/value store.
 *
 * @param <V> The type of values stored by this trie.
 */
public final class StoredAsyncMerklePatriciaTrie<K extends Bytes, V> implements AsyncMerkleTrie<K, V> {
  private final GetVisitor<V> getVisitor = new GetVisitor<>();
  private final RemoveVisitor<V> removeVisitor = new RemoveVisitor<>();
  private final MerkleStorage storage;
  private final StoredNodeFactory<V> nodeFactory;

  private Node<V> root;

  /**
   * Create a trie.
   *
   * @param storage The storage to use for persistence.
   * @param valueSerializer A function for serializing values to bytes.
   * @param valueDeserializer A function for deserializing values from bytes.
   */
  public StoredAsyncMerklePatriciaTrie(
      MerkleStorage storage,
      Function<V, Bytes> valueSerializer,
      Function<Bytes, V> valueDeserializer) {
    this(storage, AsyncMerkleTrie.EMPTY_TRIE_ROOT_HASH, valueSerializer, valueDeserializer);
  }

  /**
   * Create a trie.
   *
   * @param storage The storage to use for persistence.
   * @param rootHash The initial root has for the trie, which should be already present in {@code storage}.
   * @param valueSerializer A function for serializing values to bytes.
   * @param valueDeserializer A function for deserializing values from bytes.
   */
  public StoredAsyncMerklePatriciaTrie(
      MerkleStorage storage,
      Bytes32 rootHash,
      Function<V, Bytes> valueSerializer,
      Function<Bytes, V> valueDeserializer) {
    this.storage = storage;
    this.nodeFactory = new StoredNodeFactory<>(storage, valueSerializer, valueDeserializer);
    this.root = rootHash.equals(AsyncMerkleTrie.EMPTY_TRIE_ROOT_HASH) ? NullNode.instance()
        : new StoredNode<>(nodeFactory, rootHash);
  }

  @Override
  public AsyncResult<Optional<V>> get(K key) {
    checkNotNull(key);
    return root.accept(getVisitor, bytesToPath(key)).then(Node::value);
  }

  @Override
  public AsyncCompletion put(K key, V value) {
    checkNotNull(key);
    checkNotNull(value);
    return root.accept(new PutVisitor<>(nodeFactory, value), bytesToPath(key)).thenCompose(this::updateRoot);
  }

  @Override
  public AsyncCompletion remove(K key) {
    checkNotNull(key);
    return root.accept(removeVisitor, bytesToPath(key)).thenCompose(this::updateRoot);
  }

  @Override
  public Bytes32 rootHash() {
    return root.hash();
  }

  /**
   * Forces any cached trie nodes to be released, so they can be garbage collected.
   *
   * <p>
   * Note: nodes are already stored using {@link java.lang.ref.SoftReference}'s, so they will be released automatically
   * based on memory demands.
   */
  public void clearCache() {
    if (root instanceof StoredNode) {
      ((StoredNode<V>) root).unload();
    }
  }

  private AsyncCompletion updateRoot(Node<V> newRoot) {
    if (!(newRoot instanceof StoredNode)) {
      return storage.put(newRoot.hash(), newRoot.rlp()).thenRun(() -> this.root = newRoot);
    } else {
      this.root = newRoot;
      return AsyncCompletion.completed();
    }
  }

  @Override
  public String toString() {
    return getClass().getSimpleName() + "[" + rootHash() + "]";
  }
}
