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
import net.consensys.cava.eth.Address;
import net.consensys.cava.eth.BlockHeader;
import net.consensys.cava.eth.Hash;
import net.consensys.cava.junit.BouncyCastleExtension;
import net.consensys.cava.junit.LuceneIndex;
import net.consensys.cava.junit.LuceneIndexWriter;
import net.consensys.cava.junit.LuceneIndexWriterExtension;
import net.consensys.cava.units.bigints.UInt256;
import net.consensys.cava.units.ethereum.Gas;

import java.io.IOException;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;

import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.Term;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.ScoreDoc;
import org.apache.lucene.search.TermQuery;
import org.apache.lucene.search.TopScoreDocCollector;
import org.apache.lucene.store.Directory;
import org.apache.lucene.util.BytesRef;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

@ExtendWith({LuceneIndexWriterExtension.class, BouncyCastleExtension.class})
class BlockchainIndexTest {

  @Test
  void testIndexBlockHeader(@LuceneIndexWriter IndexWriter writer) throws IOException {

    BlockchainIndex blockchainIndex = new BlockchainIndex(writer);
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
    blockchainIndex.indexBlockHeader(header);

    IndexReader reader = DirectoryReader.open(writer);
    IndexSearcher searcher = new IndexSearcher(reader);
    TopScoreDocCollector collector = TopScoreDocCollector.create(10, new ScoreDoc(1, 1.0f));
    searcher.search(new TermQuery(new Term("_id", new BytesRef(header.hash().toBytes().toArrayUnsafe()))), collector);
    ScoreDoc[] hits = collector.topDocs().scoreDocs;
    assertEquals(1, hits.length);
  }

  @Test
  void testIndexBlockHeaderElements(@LuceneIndexWriter IndexWriter writer) throws IOException {

    BlockchainIndex blockchainIndex = new BlockchainIndex(writer);
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
    blockchainIndex.indexBlockHeader(header);

    IndexReader reader = DirectoryReader.open(writer);
    IndexSearcher searcher = new IndexSearcher(reader);
    TopScoreDocCollector collector = TopScoreDocCollector.create(10, new ScoreDoc(1, 1.0f));
    searcher.search(new TermQuery(new Term("_id", new BytesRef(header.hash().toBytes().toArrayUnsafe()))), collector);
    ScoreDoc[] hits = collector.topDocs().scoreDocs;
    assertEquals(1, hits.length);
  }

  @Test
  void testIndexCommit(@LuceneIndexWriter IndexWriter writer, @LuceneIndex Directory index) throws IOException {

    BlockchainIndex blockchainIndex = new BlockchainIndex(writer);
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
    blockchainIndex.index(w -> w.indexBlockHeader(header));

    IndexReader reader = DirectoryReader.open(index);
    IndexSearcher searcher = new IndexSearcher(reader);
    TopScoreDocCollector collector = TopScoreDocCollector.create(10, new ScoreDoc(1, 1.0f));
    searcher.search(new TermQuery(new Term("_id", new BytesRef(header.hash().toBytes().toArrayUnsafe()))), collector);
    ScoreDoc[] hits = collector.topDocs().scoreDocs;
    assertEquals(1, hits.length);
  }

  @Test
  void queryBlockHeaderByField(@LuceneIndexWriter IndexWriter writer) throws IOException {
    BlockchainIndex blockchainIndex = new BlockchainIndex(writer);
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
        Gas.valueOf(3000),
        Gas.valueOf(2000),
        Instant.now().plusSeconds(30).truncatedTo(ChronoUnit.SECONDS),
        Bytes.of(2, 3, 4, 5, 6, 7, 8, 9, 10),
        Hash.fromBytes(Bytes32.random()),
        Bytes32.random());
    blockchainIndex.index(w -> w.indexBlockHeader(header));

    BlockchainIndexReader reader = (BlockchainIndexReader) blockchainIndex;

    {
      List<Hash> entries = reader.findBy(BlockHeaderFields.PARENT_HASH, header.parentHash());
      assertEquals(1, entries.size());
      assertEquals(header.hash(), entries.get(0));
    }

    {
      List<Hash> entries = reader.findBy(BlockHeaderFields.OMMERS_HASH, header.ommersHash());
      assertEquals(1, entries.size());
      assertEquals(header.hash(), entries.get(0));
    }

    {
      List<Hash> entries = reader.findBy(BlockHeaderFields.COINBASE, header.coinbase());
      assertEquals(1, entries.size());
      assertEquals(header.hash(), entries.get(0));
    }

    {
      List<Hash> entries = reader.findBy(BlockHeaderFields.STATE_ROOT, header.stateRoot());
      assertEquals(1, entries.size());
      assertEquals(header.hash(), entries.get(0));
    }

    {
      List<Hash> entries = reader.findBy(BlockHeaderFields.STATE_ROOT, header.stateRoot());
      assertEquals(1, entries.size());
      assertEquals(header.hash(), entries.get(0));
    }

    {
      List<Hash> entries = reader.findBy(BlockHeaderFields.DIFFICULTY, header.difficulty());
      assertEquals(1, entries.size());
      assertEquals(header.hash(), entries.get(0));
    }

    {
      List<Hash> entries = reader.findBy(BlockHeaderFields.TIMESTAMP, header.timestamp().toEpochMilli());
      assertEquals(1, entries.size());
      assertEquals(header.hash(), entries.get(0));
    }

    {
      List<Hash> entries = reader.findBy(BlockHeaderFields.NUMBER, header.number());
      assertEquals(1, entries.size());
      assertEquals(header.hash(), entries.get(0));
    }

    {
      List<Hash> entries =
          reader.findInRange(BlockHeaderFields.NUMBER, header.number().subtract(5), header.number().add(5));
      assertEquals(1, entries.size());
      assertEquals(header.hash(), entries.get(0));
    }

    {
      List<Hash> entries = reader.findBy(BlockHeaderFields.EXTRA_DATA, header.extraData());
      assertEquals(1, entries.size());
      assertEquals(header.hash(), entries.get(0));
    }

    {
      List<Hash> entries = reader.findBy(BlockHeaderFields.GAS_LIMIT, header.gasLimit());
      assertEquals(1, entries.size(), entries.toString());
      assertEquals(header.hash(), entries.get(0));
    }

    {
      List<Hash> entries = reader.findBy(BlockHeaderFields.GAS_USED, header.gasUsed());
      assertEquals(1, entries.size());
      assertEquals(header.hash(), entries.get(0));
    }
  }
}
