/*
 * Copyright 2019 ConsenSys AG.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on
 * an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations under the License.
 */
package net.consensys.cava.eth.repository;

import net.consensys.cava.bytes.Bytes;
import net.consensys.cava.bytes.Bytes32;
import net.consensys.cava.concurrent.AsyncCompletion;
import net.consensys.cava.concurrent.AsyncResult;
import net.consensys.cava.eth.Block;
import net.consensys.cava.eth.BlockHeader;
import net.consensys.cava.eth.Hash;
import net.consensys.cava.kv.KeyValueStore;

import java.nio.charset.StandardCharsets;
import java.util.List;

/**
 * Repository housing blockchain information.
 *
 * This repository allows storing blocks, block headers and metadata about the blockchain, such as forks and head
 * information.
 */
public final class BlockchainRepository {

  private final KeyValueStore chainMetadata;
  private final KeyValueStore blockStore;
  private final KeyValueStore blockHeaderStore;
  private final BlockchainIndex blockchainIndex;

  /**
   * Initializes a blockchain repository with metadata, placing it in key-value stores.
   *
   * @return a new blockchain repository made from the metadata passed in parameter.
   */
  public static AsyncResult<BlockchainRepository> init(
      KeyValueStore blockStore,
      KeyValueStore blockHeaderStore,
      KeyValueStore chainMetadata,
      BlockchainIndex blockchainIndex,
      Block genesisBlock) {
    BlockchainRepository repo = new BlockchainRepository(chainMetadata, blockStore, blockHeaderStore, blockchainIndex);
    return repo.setGenesisBlock(genesisBlock).thenCombine(repo.storeBlock(genesisBlock)).thenSupply(() -> repo);
  }

  /**
   * Default constructor.
   * 
   * @param chainMetadata the key-value store to store chain metadata
   * @param blockStore the key-value store to store blocks
   * @param blockHeaderStore the key-value store to store block headers
   * @param blockchainIndex the blockchain index to index values
   */
  public BlockchainRepository(
      KeyValueStore chainMetadata,
      KeyValueStore blockStore,
      KeyValueStore blockHeaderStore,
      BlockchainIndex blockchainIndex) {
    this.chainMetadata = chainMetadata;
    this.blockStore = blockStore;
    this.blockHeaderStore = blockHeaderStore;
    this.blockchainIndex = blockchainIndex;
  }

  /**
   * Stores a block into the repository.
   * 
   * @param block the block to store
   * @return a handle to the storage operation completion
   */
  public AsyncCompletion storeBlock(Block block) {
    AsyncCompletion store = AsyncCompletion.allOf(
        blockStore.putAsync(block.header().hash().toBytes(), block.toBytes()),
        blockHeaderStore.putAsync(block.header().hash().toBytes(), block.header().toBytes()));
    blockchainIndex.indexBlockHeader(block.header());

    return store.thenCombine(isChainHead(block.header()).thenCompose(isChainHead -> {
      if (isChainHead) {
        return setChainHead(block.header());
      } else {
        return AsyncCompletion.completed();
      }
    }));
  }

  /**
   * Stores a block header in the repository.
   *
   * @param header the block header to store
   * @return handle to the storage operation completion
   */
  public AsyncCompletion storeBlockHeader(BlockHeader header) {
    AsyncCompletion completion = blockHeaderStore.putAsync(header.hash().toBytes(), header.toBytes());
    return completion.thenCombine(isChainHead(header).thenCompose(isChainHead -> {
      if (isChainHead) {
        return setChainHead(header);
      } else {
        return AsyncCompletion.completed();
      }
    }));
  }

  private AsyncResult<Boolean> isChainHead(BlockHeader header) {
    return retrieveChainHeadHeader().thenApply(headHeader -> {
      return headHeader == null || headHeader.number().compareTo(header.number()) == -1;
    });
  }

  /**
   * Retrieves a block into the repository as its serialized RLP bytes representation.
   * 
   * @param blockHash the hash of the block stored
   * @return a future with the bytes if found
   */
  public AsyncResult<Bytes> retrieveBlockBytes(Hash blockHash) {
    return retrieveBlockBytes(blockHash.toBytes());
  }

  /**
   * Retrieves a block into the repository as its serialized RLP bytes representation.
   * 
   * @param blockHash the hash of the block stored
   * @return a future with the bytes if found
   */
  public AsyncResult<Bytes> retrieveBlockBytes(Bytes blockHash) {
    return blockStore.getAsync(blockHash);
  }

  /**
   * Retrieves a block into the repository.
   * 
   * @param blockHash the hash of the block stored
   * @return a future with the block if found
   */
  public AsyncResult<Block> retrieveBlock(Hash blockHash) {
    return retrieveBlock(blockHash.toBytes());
  }

  /**
   * Retrieves a block into the repository.
   * 
   * @param blockHash the hash of the block stored
   * @return a future with the block if found
   */
  public AsyncResult<Block> retrieveBlock(Bytes blockHash) {
    return retrieveBlockBytes(blockHash).thenApply(bytes -> {
      if (bytes == null) {
        return null;
      } else {
        return Block.fromBytes(bytes);
      }
    });
  }

  /**
   * Retrieves a block header into the repository as its serialized RLP bytes representation.
   * 
   * @param blockHash the hash of the block stored
   * @return a future with the block header bytes if found
   */
  public AsyncResult<Bytes> retrieveBlockHeaderBytes(Hash blockHash) {
    return retrieveBlockBytes(blockHash.toBytes());
  }

  /**
   * Retrieves a block header into the repository as its serialized RLP bytes representation.
   * 
   * @param blockHash the hash of the block stored
   * @return a future with the block header bytes if found
   */
  public AsyncResult<Bytes> retrieveBlockHeaderBytes(Bytes blockHash) {
    return blockHeaderStore.getAsync(blockHash);
  }

  /**
   * Retrieves a block header into the repository.
   * 
   * @param blockHash the hash of the block stored
   * @return a future with the block header if found
   */
  public AsyncResult<BlockHeader> retrieveBlockHeader(Hash blockHash) {
    return retrieveBlockHeaderBytes(blockHash.toBytes()).thenApply(bytes -> {
      if (bytes == null) {
        return null;
      }
      return BlockHeader.fromBytes(bytes);
    });
  }

  /**
   * Retrieves a block header into the repository.
   * 
   * @param blockHash the hash of the block stored
   * @return a future with the block header if found
   */
  public AsyncResult<BlockHeader> retrieveBlockHeader(Bytes blockHash) {
    return retrieveBlockHeaderBytes(blockHash).thenApply(bytes -> {
      if (bytes == null) {
        return null;
      }
      return BlockHeader.fromBytes(bytes);
    });
  }

  /**
   * Retrieves the block identified as the chain head
   * 
   * @return the current chain head, or the genesis block if no chain head is present.
   */
  public AsyncResult<Block> retrieveChainHead() {
    return chainMetadata.getAsync(Bytes.wrap("chainHead".getBytes(StandardCharsets.UTF_8))).then(bytes -> {
      if (bytes == null) {
        return retrieveGenesisBlock();
      } else {
        return retrieveBlock(bytes);
      }
    });
  }

  /**
   * Retrieves the block header identified as the chain head
   *
   * @return the current chain head header, or the genesis block if no chain head is present.
   */
  public AsyncResult<BlockHeader> retrieveChainHeadHeader() {
    return chainMetadata.getAsync(Bytes.wrap("chainHead".getBytes(StandardCharsets.UTF_8))).then(bytes -> {
      if (bytes == null) {
        return retrieveGenesisBlock().thenApply(Block::header);
      } else {
        return retrieveBlockHeader(bytes);
      }
    });
  }

  /**
   * Retrieves the block identified as the genesis block
   *
   * @return the genesis block
   */
  public AsyncResult<Block> retrieveGenesisBlock() {
    return chainMetadata.getAsync(Bytes.wrap("genesisBlock".getBytes(StandardCharsets.UTF_8))).then(
        this::retrieveBlock);
  }

  /**
   * Finds a block according to the bytes, which can be a block number or block hash.
   *
   * @param blockNumberOrBlockHash the number or hash of the block
   * @return the matching blocks
   */
  public List<Hash> findBlockByHashOrNumber(Bytes32 blockNumberOrBlockHash) {
    return blockchainIndex.findByHashOrNumber(blockNumberOrBlockHash);
  }

  private AsyncCompletion setChainHead(BlockHeader header) {
    return chainMetadata.putAsync(Bytes.wrap("chainHead".getBytes(StandardCharsets.UTF_8)), header.hash().toBytes());
  }

  private AsyncCompletion setGenesisBlock(Block block) {
    return chainMetadata
        .putAsync(Bytes.wrap("genesisBlock".getBytes(StandardCharsets.UTF_8)), block.header().hash().toBytes());
  }
}
