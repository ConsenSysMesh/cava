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
package net.consensys.cava.eth.repository

import net.consensys.cava.bytes.Bytes
import net.consensys.cava.bytes.Bytes32
import net.consensys.cava.eth.Block
import net.consensys.cava.eth.BlockHeader
import net.consensys.cava.eth.Hash
import net.consensys.cava.kv.KeyValueStore
import java.nio.charset.StandardCharsets

/**
 * Repository housing blockchain information.
 *
 * This repository allows storing blocks, block headers and metadata about the blockchain, such as forks and head
 * information.
 */
class BlockchainRepository
/**
 * Default constructor.
 *
 * @param chainMetadata the key-value store to store chain metadata
 * @param blockStore the key-value store to store blocks
 * @param blockHeaderStore the key-value store to store block headers
 * @param blockchainIndex the blockchain index to index values
 */
  (
    private val chainMetadata: KeyValueStore,
    private val blockStore: KeyValueStore,
    private val blockHeaderStore: KeyValueStore,
    private val blockchainIndex: BlockchainIndex
  ) {

  companion object {

    /**
     * Initializes a blockchain repository with metadata, placing it in key-value stores.
     *
     * @return a new blockchain repository made from the metadata passed in parameter.
     */
    suspend fun init(
      blockStore: KeyValueStore,
      blockHeaderStore: KeyValueStore,
      chainMetadata: KeyValueStore,
      blockchainIndex: BlockchainIndex,
      genesisBlock: Block
    ): BlockchainRepository {
      val repo = BlockchainRepository(chainMetadata, blockStore, blockHeaderStore, blockchainIndex)
      repo.setGenesisBlock(genesisBlock)
      repo.storeBlock(genesisBlock)
      return repo
    }
  }

  /**
   * Stores a block into the repository.
   *
   * @param block the block to store
   * @return a handle to the storage operation completion
   */
  suspend fun storeBlock(block: Block) {
    blockStore.put(block.header().hash().toBytes(), block.toBytes())
    blockHeaderStore.put(block.header().hash().toBytes(), block.header().toBytes())
    blockchainIndex.indexBlockHeader(block.header())
    if (isChainHead(block.header())) {
      setChainHead(block.header())
    }
  }

  /**
   * Stores a block header in the repository.
   *
   * @param header the block header to store
   * @return handle to the storage operation completion
   */
  suspend fun storeBlockHeader(header: BlockHeader) {
    blockHeaderStore.put(header.hash().toBytes(), header.toBytes())
    if (isChainHead(header)) {
        setChainHead(header)
    }
  }

  private suspend fun isChainHead(header: BlockHeader): Boolean {
    val headHeader = retrieveChainHeadHeader()
    return headHeader?.number()?.compareTo(header.number()) == -1
  }

  /**
   * Retrieves a block into the repository as its serialized RLP bytes representation.
   *
   * @param blockHash the hash of the block stored
   * @return a future with the bytes if found
   */
  suspend fun retrieveBlockBytes(blockHash: Hash): Bytes? {
    return retrieveBlockBytes(blockHash.toBytes())
  }

  /**
   * Retrieves a block into the repository as its serialized RLP bytes representation.
   *
   * @param blockHash the hash of the block stored
   * @return a future with the bytes if found
   */
  suspend fun retrieveBlockBytes(blockHash: Bytes): Bytes? {
    return blockStore.get(blockHash)
  }

  /**
   * Retrieves a block into the repository.
   *
   * @param blockHash the hash of the block stored
   * @return a future with the block if found
   */
  suspend fun retrieveBlock(blockHash: Hash): Block? {
    return retrieveBlock(blockHash.toBytes())
  }

  /**
   * Retrieves a block into the repository.
   *
   * @param blockHash the hash of the block stored
   * @return a future with the block if found
   */
  suspend fun retrieveBlock(blockHash: Bytes): Block? {
    return retrieveBlockBytes(blockHash)?.let { Block.fromBytes(it) } ?: return null
  }

  /**
   * Retrieves a block header into the repository as its serialized RLP bytes representation.
   *
   * @param blockHash the hash of the block stored
   * @return a future with the block header bytes if found
   */
  suspend fun retrieveBlockHeaderBytes(blockHash: Hash): Bytes? {
    return retrieveBlockBytes(blockHash.toBytes())
  }

  /**
   * Retrieves a block header into the repository as its serialized RLP bytes representation.
   *
   * @param blockHash the hash of the block stored
   * @return a future with the block header bytes if found
   */
  suspend fun retrieveBlockHeaderBytes(blockHash: Bytes): Bytes? {
    return blockHeaderStore.get(blockHash)
  }

  /**
   * Retrieves a block header into the repository.
   *
   * @param blockHash the hash of the block stored
   * @return a future with the block header if found
   */
  suspend fun retrieveBlockHeader(blockHash: Hash): BlockHeader? {
    return retrieveBlockHeaderBytes(blockHash.toBytes())?.let { BlockHeader.fromBytes(it) } ?: return null
  }

  /**
   * Retrieves a block header into the repository.
   *
   * @param blockHash the hash of the block stored
   * @return a future with the block header if found
   */
  suspend fun retrieveBlockHeader(blockHash: Bytes): BlockHeader? {
    val bytes = retrieveBlockHeaderBytes(blockHash) ?: return null
    return BlockHeader.fromBytes(bytes)
  }

  /**
   * Retrieves the block identified as the chain head
   *
   * @return the current chain head, or the genesis block if no chain head is present.
   */
  suspend fun retrieveChainHead(): Block? {
    return chainMetadata.get(Bytes.wrap("chainHead".toByteArray(StandardCharsets.UTF_8)))
      ?.let { retrieveBlock(it) } ?: retrieveGenesisBlock()
  }

  /**
   * Retrieves the block header identified as the chain head
   *
   * @return the current chain head header, or the genesis block if no chain head is present.
   */
  suspend fun retrieveChainHeadHeader(): BlockHeader? {
    return chainMetadata.get(Bytes.wrap("chainHead".toByteArray(StandardCharsets.UTF_8)))
      ?.let { retrieveBlockHeader(it) } ?: retrieveGenesisBlock()?.header()
  }

  /**
   * Retrieves the block identified as the genesis block
   *
   * @return the genesis block
   */
  suspend fun retrieveGenesisBlock(): Block? {
    return retrieveBlock(chainMetadata.get(Bytes.wrap("genesisBlock".toByteArray(StandardCharsets.UTF_8)))!!)
  }

  /**
   * Finds a block according to the bytes, which can be a block number or block hash.
   *
   * @param blockNumberOrBlockHash the number or hash of the block
   * @return the matching blocks
   */
  fun findBlockByHashOrNumber(blockNumberOrBlockHash: Bytes32): List<Hash> {
    return blockchainIndex.findByHashOrNumber(blockNumberOrBlockHash)
  }

  private suspend fun setChainHead(header: BlockHeader) {
    return chainMetadata.put(Bytes.wrap("chainHead".toByteArray(StandardCharsets.UTF_8)), header.hash().toBytes())
  }

  private suspend fun setGenesisBlock(block: Block) {
    return chainMetadata
      .put(Bytes.wrap("genesisBlock".toByteArray(StandardCharsets.UTF_8)), block.header().hash().toBytes())
  }
}
