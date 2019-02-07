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
import net.consensys.cava.eth.Address
import net.consensys.cava.eth.BlockHeader
import net.consensys.cava.eth.Hash
import net.consensys.cava.units.bigints.UInt256
import net.consensys.cava.units.ethereum.Gas
import org.apache.lucene.document.Field
import org.apache.lucene.document.LongPoint
import org.apache.lucene.document.StringField
import org.apache.lucene.index.IndexWriter
import org.apache.lucene.index.IndexableField
import org.apache.lucene.index.Term
import org.apache.lucene.search.BooleanClause
import org.apache.lucene.search.BooleanQuery
import org.apache.lucene.search.IndexSearcher
import org.apache.lucene.search.MatchAllDocsQuery
import org.apache.lucene.search.Query
import org.apache.lucene.search.SearcherFactory
import org.apache.lucene.search.SearcherManager
import org.apache.lucene.search.Sort
import org.apache.lucene.search.SortField
import org.apache.lucene.search.TermQuery
import org.apache.lucene.search.TermRangeQuery
import org.apache.lucene.util.BytesRef
import java.io.IOException
import java.io.UncheckedIOException

/**
 * Reader of a blockchain index.
 *
 * Allows to query for fields for exact or range matches.
 */
interface BlockchainIndexReader {

  /**
   * Find a value in a range.
   *
   * @param field the name of the field
   * @param minValue the minimum value, inclusive
   * @param maxValue the maximum value, inclusive
   * @return the matching block header hashes.
   */
  fun findInRange(field: BlockHeaderFields, minValue: UInt256, maxValue: UInt256): List<Hash>

  /**
   * Find exact matches for a field.
   *
   * @param field the name of the field
   * @param value the value of the field.
   * @return the matching block header hashes.
   */
  fun findBy(field: BlockHeaderFields, value: Bytes): List<Hash>

  /**
   * Find exact matches for a field.
   *
   * @param field the name of the field
   * @param value the value of the field.
   * @return the matching block header hashes.
   */
  fun findBy(field: BlockHeaderFields, value: Long): List<Hash>

  /**
   * Find exact matches for a field.
   *
   * @param field the name of the field
   * @param value the value of the field.
   * @return the matching block header hashes.
   */
  fun findBy(field: BlockHeaderFields, value: Gas): List<Hash>

  /**
   * Find exact matches for a field.
   *
   * @param field the name of the field
   * @param value the value of the field.
   *
   * @return the matching block header hashes.
   */
  fun findBy(field: BlockHeaderFields, value: UInt256): List<Hash>

  /**
   * Find exact matches for a field.
   *
   * @param field the name of the field
   * @param value the value of the field.
   * @return the matching block header hashes.
   */
  fun findBy(field: BlockHeaderFields, value: Address): List<Hash>

  /**
   * Find exact matches for a field.
   *
   * @param field the name of the field
   * @param value the value of the field.
   * @return the matching block header hashes.
   */
  fun findBy(field: BlockHeaderFields, value: Hash): List<Hash>

  /**
   * Finds hashes of blocks by hash or number.
   *
   * @param hashOrNumber the hash of a block header, or its number as a 32-byte word
   * @return the matching block header hashes.
   */
  fun findByHashOrNumber(hashOrNumber: Bytes32): List<Hash>

  /**
   * Find the hash of the block header with the largest value of a specific block header field
   *
   * @param field the field to query on
   * @return the matching hash with the largest field value.
   */
  fun findByLargest(field: BlockHeaderFields): Hash?
}

/**
 * Indexer for blockchain elements.
 */
interface BlockchainIndexWriter {

  /**
   * Indexes a block header.
   *
   * @param blockHeader the block header to index
   */
  fun indexBlockHeader(blockHeader: BlockHeader)
}

/**
 * Exception thrown when an issue arises when reading the index.
 */
internal class IndexReadException(e: Exception) : RuntimeException(e)

/**
 * Exception thrown when an issue arises while writing to the index.
 */
internal class IndexWriteException(e: Exception) : RuntimeException(e)

/**
 * A Lucene-backed indexer capable of indexing blocks and block headers.
 */
class BlockchainIndex(private val indexWriter: IndexWriter) : BlockchainIndexWriter, BlockchainIndexReader {
  private val searcherManager: SearcherManager

  init {
    if (!indexWriter.isOpen) {
      throw IllegalArgumentException("Index writer should be opened")
    }
    try {
      searcherManager = SearcherManager(indexWriter, SearcherFactory())
    } catch (e: IOException) {
      throw UncheckedIOException(e)
    }
  }

  /**
   * Provides a function to index elements and committing them. If an exception is thrown in the function, the write is
   * rolled back.
   *
   * @param indexer function indexing data to be committed
   */
  fun index(indexer: (BlockchainIndexWriter) -> Unit) {
    try {
      indexer(this)
      try {
        indexWriter.commit()
        searcherManager.maybeRefresh()
      } catch (e: IOException) {
        throw IndexWriteException(e)
      }
    } catch (t: Throwable) {
      try {
        indexWriter.rollback()
      } catch (e: IOException) {
        throw IndexWriteException(e)
      }

      throw t
    }
  }

  override fun indexBlockHeader(blockHeader: BlockHeader) {
    val document = ArrayList<IndexableField>()
    val id = toBytesRef(blockHeader.hash())
    document.add(StringField("_id", id, Field.Store.YES))
    blockHeader.parentHash().let {
      document.add(
        StringField(
          BlockHeaderFields.PARENT_HASH.fieldName,
          toBytesRef(blockHeader.parentHash()!!),
          Field.Store.NO
        )
      )
    }
    document.add(
      StringField(BlockHeaderFields.OMMERS_HASH.fieldName, toBytesRef(blockHeader.ommersHash()), Field.Store.NO)
    )
    document
      .add(StringField(BlockHeaderFields.COINBASE.fieldName, toBytesRef(blockHeader.coinbase()), Field.Store.NO))
    document.add(
      StringField(BlockHeaderFields.STATE_ROOT.fieldName, toBytesRef(blockHeader.stateRoot()), Field.Store.NO)
    )
    document.add(
      StringField(BlockHeaderFields.DIFFICULTY.fieldName, toBytesRef(blockHeader.difficulty()), Field.Store.NO)
    )
    document.add(
      StringField(BlockHeaderFields.DIFFICULTY.fieldName, toBytesRef(blockHeader.difficulty()), Field.Store.NO)
    )
    document.add(StringField(BlockHeaderFields.NUMBER.fieldName, toBytesRef(blockHeader.number()), Field.Store.NO))
    document.add(
      StringField(BlockHeaderFields.GAS_LIMIT.fieldName, toBytesRef(blockHeader.gasLimit()), Field.Store.NO)
    )
    document
      .add(StringField(BlockHeaderFields.GAS_USED.fieldName, toBytesRef(blockHeader.gasUsed()), Field.Store.NO))
    document.add(
      StringField(BlockHeaderFields.EXTRA_DATA.fieldName, toBytesRef(blockHeader.extraData()), Field.Store.NO)
    )
    document.add(LongPoint(BlockHeaderFields.TIMESTAMP.fieldName, blockHeader.timestamp().toEpochMilli()))

    try {
      indexWriter.updateDocument(Term("_id", id), document)
    } catch (e: IOException) {
      throw IndexWriteException(e)
    }
  }

  private fun query(query: Query): List<Hash> {
    var searcher: IndexSearcher? = null
    try {
      searcher = searcherManager.acquire()
      val topDocs = searcher!!.search(query, HITS)

      val hashes = ArrayList<Hash>()
      for (hit in topDocs.scoreDocs) {
        val doc = searcher.doc(hit.doc, setOf("_id"))
        val bytes = doc.getBinaryValue("_id")
        hashes.add(Hash.fromBytes(Bytes32.wrap(bytes.bytes)))
      }
      return hashes
    } catch (e: IOException) {
      throw IndexReadException(e)
    } finally {
      try {
        searcherManager.release(searcher)
      } catch (e: IOException) {
      }
    }
  }

  override fun findInRange(field: BlockHeaderFields, minValue: UInt256, maxValue: UInt256): List<Hash> {
    return query(TermRangeQuery(field.fieldName, toBytesRef(minValue), toBytesRef(maxValue), true, true))
  }

  override fun findBy(field: BlockHeaderFields, value: Bytes): List<Hash> {
    return findByOneTerm(field, toBytesRef(value))
  }

  override fun findBy(field: BlockHeaderFields, value: Long): List<Hash> {
    return query(LongPoint.newExactQuery(field.fieldName, value))
  }

  override fun findByLargest(field: BlockHeaderFields): Hash? {
    var searcher: IndexSearcher? = null
    try {
      searcher = searcherManager.acquire()
      val topDocs = searcher!!.search(
        MatchAllDocsQuery(),
        HITS,
        Sort(SortField.FIELD_SCORE, SortField(field.fieldName, SortField.Type.DOC))
      )

      for (hit in topDocs.scoreDocs) {
        val doc = searcher.doc(hit.doc, setOf("_id"))
        val bytes = doc.getBinaryValue("_id")

        return Hash.fromBytes(Bytes32.wrap(bytes.bytes))
      }
      return null
    } catch (e: IOException) {
      throw IndexReadException(e)
    } finally {
      try {
        searcherManager.release(searcher)
      } catch (e: IOException) {
      }
    }
  }

  override fun findBy(field: BlockHeaderFields, value: Gas): List<Hash> {
    return findByOneTerm(field, toBytesRef(value))
  }

  override fun findBy(field: BlockHeaderFields, value: UInt256): List<Hash> {
    return findByOneTerm(field, toBytesRef(value))
  }

  override fun findBy(field: BlockHeaderFields, value: Address): List<Hash> {
    return findByOneTerm(field, toBytesRef(value))
  }

  override fun findBy(field: BlockHeaderFields, value: Hash): List<Hash> {
    return findByOneTerm(field, toBytesRef(value))
  }

  override fun findByHashOrNumber(hashOrNumber: Bytes32): List<Hash> {
    val query = BooleanQuery.Builder()
      .setMinimumNumberShouldMatch(1)
      .add(BooleanClause(TermQuery(Term("_id", toBytesRef(hashOrNumber))), BooleanClause.Occur.SHOULD))
      .add(
        BooleanClause(
          TermQuery(Term(BlockHeaderFields.NUMBER.fieldName, toBytesRef(hashOrNumber))),
          BooleanClause.Occur.SHOULD
        )
      )
      .build()
    return query(query)
  }

  private fun findByOneTerm(field: BlockHeaderFields, value: BytesRef): List<Hash> {
    return query(TermQuery(Term(field.fieldName, value)))
  }

  private fun toBytesRef(gas: Gas): BytesRef {
    return BytesRef(gas.toBytes().toArrayUnsafe())
  }

  private fun toBytesRef(bytes: Bytes): BytesRef {
    return BytesRef(bytes.toArrayUnsafe())
  }

  private fun toBytesRef(uint: UInt256): BytesRef {
    return toBytesRef(uint.toBytes())
  }

  private fun toBytesRef(address: Address): BytesRef {
    return toBytesRef(address.toBytes())
  }

  private fun toBytesRef(hash: Hash): BytesRef {
    return toBytesRef(hash.toBytes())
  }

  companion object {

    private val HITS = 10
  }
}
