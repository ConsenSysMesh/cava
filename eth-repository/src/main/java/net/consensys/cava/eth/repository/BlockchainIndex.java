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
import net.consensys.cava.eth.Address;
import net.consensys.cava.eth.BlockHeader;
import net.consensys.cava.eth.Hash;
import net.consensys.cava.units.bigints.UInt256;
import net.consensys.cava.units.ethereum.Gas;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.function.Consumer;

import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.document.LongPoint;
import org.apache.lucene.document.StringField;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexableField;
import org.apache.lucene.index.Term;
import org.apache.lucene.search.BooleanClause;
import org.apache.lucene.search.BooleanQuery;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.MatchAllDocsQuery;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.ScoreDoc;
import org.apache.lucene.search.SearcherFactory;
import org.apache.lucene.search.SearcherManager;
import org.apache.lucene.search.Sort;
import org.apache.lucene.search.SortField;
import org.apache.lucene.search.TermQuery;
import org.apache.lucene.search.TermRangeQuery;
import org.apache.lucene.search.TopDocs;
import org.apache.lucene.util.BytesRef;

/**
 * A Lucene-backed indexer capable of indexing blocks and block headers.
 */
public final class BlockchainIndex implements BlockchainIndexWriter, BlockchainIndexReader {

  private static final int HITS = 10;

  private final IndexWriter indexWriter;
  private final SearcherManager searcherManager;

  public BlockchainIndex(IndexWriter writer) {
    if (!writer.isOpen()) {
      throw new IllegalArgumentException("Index writer should be opened");
    }
    indexWriter = writer;
    try {
      searcherManager = new SearcherManager(writer, new SearcherFactory());
    } catch (IOException e) {
      throw new UncheckedIOException(e);
    }
  }

  /**
   * Provides a function to index elements and committing them. If an exception is thrown in the function, the write is
   * rolled back.
   *
   * @param indexer function indexing data to be committed
   */
  public void index(Consumer<BlockchainIndexWriter> indexer) {
    try {
      indexer.accept(this);
      try {
        indexWriter.commit();
        searcherManager.maybeRefresh();
      } catch (IOException e) {
        throw new IndexWriteException(e);
      }
    } catch (Throwable t) {
      try {
        indexWriter.rollback();
      } catch (IOException e) {
        throw new IndexWriteException(e);
      }
      throw t;
    }
  }

  @Override
  public void indexBlockHeader(BlockHeader blockHeader) {
    List<IndexableField> document = new ArrayList<>();
    BytesRef id = toBytesRef(blockHeader.hash());
    document.add(new StringField("_id", id, Field.Store.YES));
    if (blockHeader.parentHash() != null) {
      document.add(
          new StringField(
              BlockHeaderFields.PARENT_HASH.fieldName,
              toBytesRef(blockHeader.parentHash()),
              Field.Store.NO));
    }
    document.add(
        new StringField(BlockHeaderFields.OMMERS_HASH.fieldName, toBytesRef(blockHeader.ommersHash()), Field.Store.NO));
    document
        .add(new StringField(BlockHeaderFields.COINBASE.fieldName, toBytesRef(blockHeader.coinbase()), Field.Store.NO));
    document.add(
        new StringField(BlockHeaderFields.STATE_ROOT.fieldName, toBytesRef(blockHeader.stateRoot()), Field.Store.NO));
    document.add(
        new StringField(BlockHeaderFields.DIFFICULTY.fieldName, toBytesRef(blockHeader.difficulty()), Field.Store.NO));
    document.add(
        new StringField(BlockHeaderFields.DIFFICULTY.fieldName, toBytesRef(blockHeader.difficulty()), Field.Store.NO));
    document.add(new StringField(BlockHeaderFields.NUMBER.fieldName, toBytesRef(blockHeader.number()), Field.Store.NO));
    document.add(
        new StringField(BlockHeaderFields.GAS_LIMIT.fieldName, toBytesRef(blockHeader.gasLimit()), Field.Store.NO));
    document
        .add(new StringField(BlockHeaderFields.GAS_USED.fieldName, toBytesRef(blockHeader.gasUsed()), Field.Store.NO));
    document.add(
        new StringField(BlockHeaderFields.EXTRA_DATA.fieldName, toBytesRef(blockHeader.extraData()), Field.Store.NO));
    document.add(new LongPoint(BlockHeaderFields.TIMESTAMP.fieldName, blockHeader.timestamp().toEpochMilli()));

    try {
      indexWriter.updateDocument(new Term("_id", id), document);
    } catch (IOException e) {
      throw new IndexWriteException(e);
    }
  }

  private List<Hash> query(Query query) {
    IndexSearcher searcher = null;
    try {
      searcher = searcherManager.acquire();
      TopDocs topDocs = searcher.search(query, HITS);

      List<Hash> hashes = new ArrayList<>();
      for (ScoreDoc hit : topDocs.scoreDocs) {
        Document doc = searcher.doc(hit.doc, Collections.singleton("_id"));
        BytesRef bytes = doc.getBinaryValue("_id");
        hashes.add(Hash.fromBytes(Bytes32.wrap(bytes.bytes)));
      }
      return hashes;
    } catch (IOException e) {
      throw new IndexReadException(e);
    } finally {
      try {
        searcherManager.release(searcher);
      } catch (IOException e) {
      }
    }
  }

  @Override
  public List<Hash> findInRange(BlockHeaderFields field, UInt256 minValue, UInt256 maxValue) {
    return query(new TermRangeQuery(field.fieldName, toBytesRef(minValue), toBytesRef(maxValue), true, true));
  }

  @Override
  public List<Hash> findBy(BlockHeaderFields field, Bytes value) {
    return findByOneTerm(field, toBytesRef(value));
  }

  @Override
  public List<Hash> findBy(BlockHeaderFields field, Long value) {
    return query(LongPoint.newExactQuery(field.fieldName, value));
  }

  @Override
  public Hash findByLargest(BlockHeaderFields field) {
    IndexSearcher searcher = null;
    try {
      searcher = searcherManager.acquire();
      TopDocs topDocs = searcher.search(
          new MatchAllDocsQuery(),
          HITS,
          new Sort(SortField.FIELD_SCORE, new SortField(field.fieldName, SortField.Type.DOC)));

      for (ScoreDoc hit : topDocs.scoreDocs) {
        Document doc = searcher.doc(hit.doc, Collections.singleton("_id"));
        BytesRef bytes = doc.getBinaryValue("_id");

        return Hash.fromBytes(Bytes32.wrap(bytes.bytes));
      }
      return null;
    } catch (IOException e) {
      throw new IndexReadException(e);
    } finally {
      try {
        searcherManager.release(searcher);
      } catch (IOException e) {
      }

    }
  }

  @Override
  public List<Hash> findBy(BlockHeaderFields field, Gas value) {
    return findByOneTerm(field, toBytesRef(value));
  }

  @Override
  public List<Hash> findBy(BlockHeaderFields field, UInt256 value) {
    return findByOneTerm(field, toBytesRef(value));
  }

  @Override
  public List<Hash> findBy(BlockHeaderFields field, Address value) {
    return findByOneTerm(field, toBytesRef(value));
  }

  @Override
  public List<Hash> findBy(BlockHeaderFields field, Hash value) {
    return findByOneTerm(field, toBytesRef(value));
  }

  @Override
  public List<Hash> findByHashOrNumber(Bytes32 hashOrNumber) {
    BooleanQuery query = new BooleanQuery.Builder()
        .setMinimumNumberShouldMatch(1)
        .add(new BooleanClause(new TermQuery(new Term("_id", toBytesRef(hashOrNumber))), BooleanClause.Occur.SHOULD))
        .add(
            new BooleanClause(
                new TermQuery(new Term(BlockHeaderFields.NUMBER.fieldName, toBytesRef(hashOrNumber))),
                BooleanClause.Occur.SHOULD))
        .build();
    return query(query);
  }

  private List<Hash> findByOneTerm(BlockHeaderFields field, BytesRef value) {
    return query(new TermQuery(new Term(field.fieldName, value)));
  }

  private BytesRef toBytesRef(Gas gas) {
    return new BytesRef(gas.toBytes().toArrayUnsafe());
  }

  private BytesRef toBytesRef(Bytes bytes) {
    return new BytesRef(bytes.toArrayUnsafe());
  }

  private BytesRef toBytesRef(UInt256 uint) {
    return toBytesRef(uint.toBytes());
  }

  private BytesRef toBytesRef(Address address) {
    return toBytesRef(address.toBytes());
  }

  private BytesRef toBytesRef(Hash hash) {
    return toBytesRef(hash.toBytes());
  }
}
