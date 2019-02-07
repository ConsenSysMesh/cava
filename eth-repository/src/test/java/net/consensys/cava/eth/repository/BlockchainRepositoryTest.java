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

import static org.junit.jupiter.api.Assertions.assertEquals;

import net.consensys.cava.bytes.Bytes;
import net.consensys.cava.bytes.Bytes32;
import net.consensys.cava.crypto.SECP256K1;
import net.consensys.cava.eth.Address;
import net.consensys.cava.eth.Block;
import net.consensys.cava.eth.BlockBody;
import net.consensys.cava.eth.BlockHeader;
import net.consensys.cava.eth.Hash;
import net.consensys.cava.eth.Transaction;
import net.consensys.cava.junit.BouncyCastleExtension;
import net.consensys.cava.junit.LuceneIndexWriter;
import net.consensys.cava.junit.LuceneIndexWriterExtension;
import net.consensys.cava.kv.MapKeyValueStore;
import net.consensys.cava.units.bigints.UInt256;
import net.consensys.cava.units.ethereum.Gas;
import net.consensys.cava.units.ethereum.Wei;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Collections;

import org.apache.lucene.index.IndexWriter;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

@ExtendWith({BouncyCastleExtension.class, LuceneIndexWriterExtension.class})
class BlockchainRepositoryTest {

  @Test
  void storeAndRetrieveBlock(@LuceneIndexWriter IndexWriter writer) throws Exception {
    BlockHeader genesisHeader = new BlockHeader(
        Hash.fromBytes(Bytes32.random()),
        Hash.fromBytes(Bytes32.random()),
        Address.fromBytes(Bytes.random(20)),
        Hash.fromBytes(Bytes32.random()),
        Hash.fromBytes(Bytes32.random()),
        Hash.fromBytes(Bytes32.random()),
        Bytes32.random(),
        UInt256.fromBytes(Bytes32.random()),
        UInt256.fromBytes(Bytes32.random()),
        Gas.valueOf(3000),
        Gas.valueOf(2000),
        Instant.now().plusSeconds(30).truncatedTo(ChronoUnit.SECONDS),
        Bytes.of(2, 3, 4, 5, 6, 7, 8, 9, 10),
        Hash.fromBytes(Bytes32.random()),
        Bytes32.random());
    Block genesisBlock = new Block(genesisHeader, new BlockBody(Collections.emptyList(), Collections.emptyList()));
    BlockchainRepository repo = BlockchainRepository
        .init(
            new MapKeyValueStore(),
            new MapKeyValueStore(),
            new MapKeyValueStore(),
            new BlockchainIndex(writer),
            genesisBlock)
        .get();
    BlockHeader header = new BlockHeader(
        Hash.fromBytes(Bytes32.random()),
        Hash.fromBytes(Bytes32.random()),
        Address.fromBytes(Bytes.random(20)),
        Hash.fromBytes(Bytes32.random()),
        Hash.fromBytes(Bytes32.random()),
        Hash.fromBytes(Bytes32.random()),
        Bytes32.random(),
        UInt256.fromBytes(Bytes32.random()),
        UInt256.fromBytes(Bytes32.random()),
        Gas.valueOf(3),
        Gas.valueOf(2),
        Instant.now().truncatedTo(ChronoUnit.SECONDS),
        Bytes.of(2, 3, 4),
        Hash.fromBytes(Bytes32.random()),
        Bytes32.random());
    BlockBody body = new BlockBody(
        Collections.singletonList(
            new Transaction(
                UInt256.valueOf(1),
                Wei.valueOf(2),
                Gas.valueOf(2),
                Address.fromBytes(Bytes.random(20)),
                Wei.valueOf(2),
                Bytes.random(12),
                SECP256K1.KeyPair.random())),
        Collections.emptyList());
    Block block = new Block(header, body);
    repo.storeBlock(block).join();
    Block read = repo.retrieveBlock(block.header().hash().toBytes()).get();
    assertEquals(block, read);
    assertEquals(block.header(), repo.retrieveBlockHeader(block.header().hash()).get());
  }

  @Test
  void storeChainHead(@LuceneIndexWriter IndexWriter writer) throws Exception {
    BlockHeader genesisHeader = new BlockHeader(
        Hash.fromBytes(Bytes32.random()),
        Hash.fromBytes(Bytes32.random()),
        Address.fromBytes(Bytes.random(20)),
        Hash.fromBytes(Bytes32.random()),
        Hash.fromBytes(Bytes32.random()),
        Hash.fromBytes(Bytes32.random()),
        Bytes32.random(),
        UInt256.fromBytes(Bytes32.random()),
        UInt256.fromBytes(Bytes32.random()),
        Gas.valueOf(3000),
        Gas.valueOf(2000),
        Instant.now().plusSeconds(30).truncatedTo(ChronoUnit.SECONDS),
        Bytes.of(2, 3, 4, 5, 6, 7, 8, 9, 10),
        Hash.fromBytes(Bytes32.random()),
        Bytes32.random());
    Block genesisBlock = new Block(genesisHeader, new BlockBody(Collections.emptyList(), Collections.emptyList()));
    BlockchainRepository repo = BlockchainRepository
        .init(
            new MapKeyValueStore(),
            new MapKeyValueStore(),
            new MapKeyValueStore(),
            new BlockchainIndex(writer),
            genesisBlock)
        .get();

    BlockHeader header = new BlockHeader(
        Hash.fromBytes(Bytes32.random()),
        Hash.fromBytes(Bytes32.random()),
        Address.fromBytes(Bytes.random(20)),
        Hash.fromBytes(Bytes32.random()),
        Hash.fromBytes(Bytes32.random()),
        Hash.fromBytes(Bytes32.random()),
        Bytes32.random(),
        UInt256.fromBytes(Bytes32.random()),
        genesisHeader.number().add(UInt256.valueOf(1)),
        Gas.valueOf(3),
        Gas.valueOf(2),
        Instant.now().truncatedTo(ChronoUnit.SECONDS),
        Bytes.of(2, 3, 4),
        Hash.fromBytes(Bytes32.random()),
        Bytes32.random());
    BlockHeader biggerNumber = new BlockHeader(
        Hash.fromBytes(Bytes32.random()),
        Hash.fromBytes(Bytes32.random()),
        Address.fromBytes(Bytes.random(20)),
        Hash.fromBytes(Bytes32.random()),
        Hash.fromBytes(Bytes32.random()),
        Hash.fromBytes(Bytes32.random()),
        Bytes32.random(),
        UInt256.fromBytes(Bytes32.random()),
        header.number().add(UInt256.valueOf(1)),
        Gas.valueOf(3),
        Gas.valueOf(2),
        Instant.now().truncatedTo(ChronoUnit.SECONDS),
        Bytes.of(2, 3, 4),
        Hash.fromBytes(Bytes32.random()),
        Bytes32.random());
    BlockHeader biggerNumber2 = new BlockHeader(
        Hash.fromBytes(Bytes32.random()),
        Hash.fromBytes(Bytes32.random()),
        Address.fromBytes(Bytes.random(20)),
        Hash.fromBytes(Bytes32.random()),
        Hash.fromBytes(Bytes32.random()),
        Hash.fromBytes(Bytes32.random()),
        Bytes32.random(),
        UInt256.fromBytes(Bytes32.random()),
        header.number().add(UInt256.valueOf(2)),
        Gas.valueOf(3),
        Gas.valueOf(2),
        Instant.now().truncatedTo(ChronoUnit.SECONDS),
        Bytes.of(2, 3, 4),
        Hash.fromBytes(Bytes32.random()),
        Bytes32.random());
    BlockHeader biggerNumber3 = new BlockHeader(
        Hash.fromBytes(Bytes32.random()),
        Hash.fromBytes(Bytes32.random()),
        Address.fromBytes(Bytes.random(20)),
        Hash.fromBytes(Bytes32.random()),
        Hash.fromBytes(Bytes32.random()),
        Hash.fromBytes(Bytes32.random()),
        Bytes32.random(),
        UInt256.fromBytes(Bytes32.random()),
        header.number().add(UInt256.valueOf(3)),
        Gas.valueOf(3),
        Gas.valueOf(2),
        Instant.now().truncatedTo(ChronoUnit.SECONDS),
        Bytes.of(2, 3, 4),
        Hash.fromBytes(Bytes32.random()),
        Bytes32.random());

    repo.storeBlockHeader(header).join();
    repo.storeBlockHeader(biggerNumber).join();
    repo.storeBlockHeader(biggerNumber2).join();
    repo.storeBlockHeader(biggerNumber3).join();

    assertEquals(biggerNumber3.hash(), repo.retrieveChainHeadHeader().get().hash());
  }

  @Test
  void storeChainHeadBlocks(@LuceneIndexWriter IndexWriter writer) throws Exception {
    BlockHeader genesisHeader = new BlockHeader(
        Hash.fromBytes(Bytes32.random()),
        Hash.fromBytes(Bytes32.random()),
        Address.fromBytes(Bytes.random(20)),
        Hash.fromBytes(Bytes32.random()),
        Hash.fromBytes(Bytes32.random()),
        Hash.fromBytes(Bytes32.random()),
        Bytes32.random(),
        UInt256.fromBytes(Bytes32.random()),
        UInt256.fromBytes(Bytes32.random()),
        Gas.valueOf(3000),
        Gas.valueOf(2000),
        Instant.now().plusSeconds(30).truncatedTo(ChronoUnit.SECONDS),
        Bytes.of(2, 3, 4, 5, 6, 7, 8, 9, 10),
        Hash.fromBytes(Bytes32.random()),
        Bytes32.random());
    Block genesisBlock = new Block(genesisHeader, new BlockBody(Collections.emptyList(), Collections.emptyList()));
    BlockchainRepository repo = BlockchainRepository
        .init(
            new MapKeyValueStore(),
            new MapKeyValueStore(),
            new MapKeyValueStore(),
            new BlockchainIndex(writer),
            genesisBlock)
        .get();

    BlockHeader header = new BlockHeader(
        Hash.fromBytes(Bytes32.random()),
        Hash.fromBytes(Bytes32.random()),
        Address.fromBytes(Bytes.random(20)),
        Hash.fromBytes(Bytes32.random()),
        Hash.fromBytes(Bytes32.random()),
        Hash.fromBytes(Bytes32.random()),
        Bytes32.random(),
        UInt256.fromBytes(Bytes32.random()),
        genesisHeader.number().add(UInt256.valueOf(1)),
        Gas.valueOf(3),
        Gas.valueOf(2),
        Instant.now().truncatedTo(ChronoUnit.SECONDS),
        Bytes.of(2, 3, 4),
        Hash.fromBytes(Bytes32.random()),
        Bytes32.random());
    BlockHeader biggerNumber = new BlockHeader(
        Hash.fromBytes(Bytes32.random()),
        Hash.fromBytes(Bytes32.random()),
        Address.fromBytes(Bytes.random(20)),
        Hash.fromBytes(Bytes32.random()),
        Hash.fromBytes(Bytes32.random()),
        Hash.fromBytes(Bytes32.random()),
        Bytes32.random(),
        UInt256.fromBytes(Bytes32.random()),
        header.number().add(UInt256.valueOf(1)),
        Gas.valueOf(3),
        Gas.valueOf(2),
        Instant.now().truncatedTo(ChronoUnit.SECONDS),
        Bytes.of(2, 3, 4),
        Hash.fromBytes(Bytes32.random()),
        Bytes32.random());
    BlockHeader biggerNumber2 = new BlockHeader(
        Hash.fromBytes(Bytes32.random()),
        Hash.fromBytes(Bytes32.random()),
        Address.fromBytes(Bytes.random(20)),
        Hash.fromBytes(Bytes32.random()),
        Hash.fromBytes(Bytes32.random()),
        Hash.fromBytes(Bytes32.random()),
        Bytes32.random(),
        UInt256.fromBytes(Bytes32.random()),
        header.number().add(UInt256.valueOf(2)),
        Gas.valueOf(3),
        Gas.valueOf(2),
        Instant.now().truncatedTo(ChronoUnit.SECONDS),
        Bytes.of(2, 3, 4),
        Hash.fromBytes(Bytes32.random()),
        Bytes32.random());
    BlockHeader biggerNumber3 = new BlockHeader(
        Hash.fromBytes(Bytes32.random()),
        Hash.fromBytes(Bytes32.random()),
        Address.fromBytes(Bytes.random(20)),
        Hash.fromBytes(Bytes32.random()),
        Hash.fromBytes(Bytes32.random()),
        Hash.fromBytes(Bytes32.random()),
        Bytes32.random(),
        UInt256.fromBytes(Bytes32.random()),
        header.number().add(UInt256.valueOf(3)),
        Gas.valueOf(3),
        Gas.valueOf(2),
        Instant.now().truncatedTo(ChronoUnit.SECONDS),
        Bytes.of(2, 3, 4),
        Hash.fromBytes(Bytes32.random()),
        Bytes32.random());

    repo.storeBlock(new Block(header, new BlockBody(Collections.emptyList(), Collections.emptyList()))).join();
    repo.storeBlock(new Block(biggerNumber, new BlockBody(Collections.emptyList(), Collections.emptyList()))).join();
    repo.storeBlock(new Block(biggerNumber2, new BlockBody(Collections.emptyList(), Collections.emptyList()))).join();
    repo.storeBlock(new Block(biggerNumber3, new BlockBody(Collections.emptyList(), Collections.emptyList()))).join();

    assertEquals(biggerNumber3.hash(), repo.retrieveChainHeadHeader().get().hash());
  }
}
