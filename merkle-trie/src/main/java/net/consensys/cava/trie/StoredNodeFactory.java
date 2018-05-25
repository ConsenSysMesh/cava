package net.consensys.cava.trie;

import static java.lang.String.format;

import net.consensys.cava.bytes.Bytes;
import net.consensys.cava.bytes.Bytes32;
import net.consensys.cava.concurrent.AsyncResult;
import net.consensys.cava.rlp.RLP;
import net.consensys.cava.rlp.RLPException;
import net.consensys.cava.rlp.RLPReader;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Optional;
import java.util.function.Function;
import java.util.function.Supplier;

final class StoredNodeFactory<V> implements NodeFactory<V> {
  @SuppressWarnings("rawtypes")
  private static final NullNode NULL_NODE = NullNode.instance();

  private MerkleStorage storage;
  private Function<V, Bytes> valueSerializer;
  private Function<Bytes, V> valueDeserializer;

  StoredNodeFactory(MerkleStorage storage, Function<V, Bytes> valueSerializer, Function<Bytes, V> valueDeserializer) {
    this.storage = storage;
    this.valueSerializer = valueSerializer;
    this.valueDeserializer = valueDeserializer;
  }

  @Override
  public AsyncResult<Node<V>> createExtension(Bytes path, Node<V> child) {
    return maybeStore(new ExtensionNode<>(path, child, this));
  }

  @SuppressWarnings("unchecked")
  @Override
  public AsyncResult<Node<V>> createBranch(byte leftIndex, Node<V> left, byte rightIndex, Node<V> right) {
    assert (leftIndex <= BranchNode.RADIX);
    assert (rightIndex <= BranchNode.RADIX);
    assert (leftIndex != rightIndex);

    ArrayList<Node<V>> children = new ArrayList<>(Collections.nCopies(BranchNode.RADIX, (Node<V>) NULL_NODE));

    if (leftIndex == BranchNode.RADIX) {
      children.set(rightIndex, right);
      return left.value().then(value -> createBranch(children, value));
    } else if (rightIndex == BranchNode.RADIX) {
      children.set(leftIndex, left);
      return right.value().then(value -> createBranch(children, value));
    } else {
      children.set(leftIndex, left);
      children.set(rightIndex, right);
      return createBranch(children, Optional.empty());
    }
  }

  @Override
  public AsyncResult<Node<V>> createBranch(ArrayList<Node<V>> children, Optional<V> value) {
    return maybeStore(new BranchNode<>(children, value, this, valueSerializer));
  }

  @Override
  public AsyncResult<Node<V>> createLeaf(Bytes path, V value) {
    return maybeStore(new LeafNode<>(path, value, this, valueSerializer));
  }

  private AsyncResult<Node<V>> maybeStore(Node<V> node) {
    Bytes nodeRLP = node.rlp();
    if (nodeRLP.size() < 32) {
      return AsyncResult.completed(node);
    } else {
      return storage.put(node.hash(), node.rlp()).thenSupply(() -> new StoredNode<>(this, node));
    }
  }

  public AsyncResult<Node<V>> retrieve(Bytes32 hash) throws MerkleStorageException {
    return storage
        .get(hash)
        .thenApply(
            maybeBytes -> maybeBytes.orElseThrow(() -> new MerkleStorageException("Missing value for hash " + hash)))
        .thenApply(rlp -> {
          Node<V> node = decode(rlp, () -> format("Invalid RLP value for hash %s", hash));
          // recalculating the node.hash() is expensive, so we only do this as an assertion
          assert (hash.equals(node.hash())) : "Node hash " + node.hash() + " not equal to expected " + hash;
          return node;
        });
  }

  private Node<V> decode(Bytes rlp, Supplier<String> errMessage) throws MerkleStorageException {
    try {
      return RLP.decode(rlp, reader -> decode(reader, errMessage));
    } catch (RLPException ex) {
      throw new MerkleStorageException(errMessage.get(), ex);
    }
  }

  private Node<V> decode(RLPReader nodeRLPs, Supplier<String> errMessage) {
    return nodeRLPs.readList(listReader -> {
      int remaining = listReader.remaining();
      switch (remaining) {
        case 1:
          return decodeNull(listReader, errMessage);

        case 2:
          Bytes encodedPath = listReader.readValue();
          Bytes path;
          try {
            path = CompactEncoding.decode(encodedPath);
          } catch (IllegalArgumentException ex) {
            throw new MerkleStorageException(errMessage.get() + ": invalid path " + encodedPath, ex);
          }

          int size = path.size();
          if (size > 0 && path.get(size - 1) == CompactEncoding.LEAF_TERMINATOR) {
            return decodeLeaf(path, listReader, errMessage);
          } else {
            return decodeExtension(path, listReader, errMessage);
          }

        case (BranchNode.RADIX + 1):
          return decodeBranch(listReader, errMessage);

        default:
          throw new MerkleStorageException(errMessage.get() + format(": invalid list size %s", remaining));
      }
    });
  }

  private Node<V> decodeExtension(Bytes path, RLPReader valueRlp, Supplier<String> errMessage) {
    Node<V> child;
    if (valueRlp.nextIsList()) {
      child = decode(valueRlp, errMessage);
    } else {
      Bytes32 childHash;
      try {
        childHash = Bytes32.wrap(valueRlp.readValue());
      } catch (RLPException | IllegalArgumentException e) {
        throw new MerkleStorageException(errMessage.get() + ": invalid extension target");
      }
      child = new StoredNode<>(this, childHash);
    }
    return new ExtensionNode<>(path, child, this);
  }

  @SuppressWarnings("unchecked")
  private BranchNode<V> decodeBranch(RLPReader nodeRLPs, Supplier<String> errMessage) {
    ArrayList<Node<V>> children = new ArrayList<>(BranchNode.RADIX);
    for (int i = 0; i < BranchNode.RADIX; ++i) {
      if (nodeRLPs.nextIsEmpty()) {
        nodeRLPs.readValue();
        children.add(NULL_NODE);
      } else if (nodeRLPs.nextIsList()) {
        Node<V> child = decode(nodeRLPs, errMessage);
        children.add(new StoredNode<>(this, child));
      } else {
        Bytes32 childHash;
        try {
          childHash = Bytes32.wrap(nodeRLPs.readValue());
        } catch (RLPException | IllegalArgumentException e) {
          throw new MerkleStorageException(errMessage.get() + ": invalid branch child " + i);
        }
        children.add(new StoredNode<>(this, childHash));
      }
    }

    Optional<V> value;
    if (nodeRLPs.nextIsEmpty()) {
      nodeRLPs.readValue();
      value = Optional.empty();
    } else {
      value = Optional.of(decodeValue(nodeRLPs, errMessage));
    }

    return new BranchNode<>(children, value, this, valueSerializer);
  }

  private LeafNode<V> decodeLeaf(Bytes path, RLPReader valueRlp, Supplier<String> errMessage) {
    if (valueRlp.nextIsEmpty()) {
      throw new MerkleStorageException(errMessage.get() + ": leaf has null value");
    }
    V value = decodeValue(valueRlp, errMessage);
    return new LeafNode<>(path, value, this, valueSerializer);
  }

  @SuppressWarnings("unchecked")
  private NullNode<V> decodeNull(RLPReader nodeRLPs, Supplier<String> errMessage) {
    if (!nodeRLPs.nextIsEmpty()) {
      throw new MerkleStorageException(errMessage.get() + ": list size 1 but not null");
    }
    nodeRLPs.readValue();
    return NULL_NODE;
  }

  private V decodeValue(RLPReader valueRlp, Supplier<String> errMessage) {
    Bytes bytes;
    try {
      bytes = valueRlp.readValue();
    } catch (RLPException ex) {
      throw new MerkleStorageException(errMessage.get() + ": failed decoding value rlp " + valueRlp, ex);
    }
    return deserializeValue(errMessage, bytes);
  }

  private V deserializeValue(Supplier<String> errMessage, Bytes bytes) {
    V value;
    try {
      value = valueDeserializer.apply(bytes);
    } catch (IllegalArgumentException ex) {
      throw new MerkleStorageException(errMessage.get() + ": failed deserializing value " + bytes, ex);
    }
    return value;
  }
}
