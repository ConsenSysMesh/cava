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
import net.consensys.cava.eth.Hash;
import net.consensys.cava.units.bigints.UInt256;
import net.consensys.cava.units.ethereum.Gas;

import java.util.List;

/**
 * Reader of a blockchain index.
 *
 * Allows to query for fields for exact or range matches.
 */
public interface BlockchainIndexReader {

  /**
   * Find a value in a range.
   * 
   * @param field the name of the field
   * @param minValue the minimum value, inclusive
   * @param maxValue the maximum value, inclusive
   * @return the matching block header hashes.
   */
  List<Hash> findInRange(BlockHeaderFields field, UInt256 minValue, UInt256 maxValue);

  /**
   * Find exact matches for a field.
   * 
   * @param field the name of the field
   * @param value the value of the field.
   * @return the matching block header hashes.
   */
  List<Hash> findBy(BlockHeaderFields field, Bytes value);

  /**
   * Find exact matches for a field.
   * 
   * @param field the name of the field
   * @param value the value of the field.
   * @return the matching block header hashes.
   */
  List<Hash> findBy(BlockHeaderFields field, Long value);

  /**
   * Find exact matches for a field.
   * 
   * @param field the name of the field
   * @param value the value of the field.
   * @return the matching block header hashes.
   */
  List<Hash> findBy(BlockHeaderFields field, Gas value);

  /**
   * Find exact matches for a field.
   * 
   * @param field the name of the field
   * @param value the value of the field.
   *
   * @return the matching block header hashes.
   */
  List<Hash> findBy(BlockHeaderFields field, UInt256 value);

  /**
   * Find exact matches for a field.
   * 
   * @param field the name of the field
   * @param value the value of the field.
   * @return the matching block header hashes.
   */
  List<Hash> findBy(BlockHeaderFields field, Address value);

  /**
   * Find exact matches for a field.
   * 
   * @param field the name of the field
   * @param value the value of the field.
   * @return the matching block header hashes.
   */
  List<Hash> findBy(BlockHeaderFields field, Hash value);

  /**
   * Finds hashes of blocks by hash or number.
   *
   * @param hashOrNumber the hash of a block header, or its number as a 32-byte word
   * @return the matching block header hashes.
   */
  List<Hash> findByHashOrNumber(Bytes32 hashOrNumber);

  /**
   * Find the hash of the block header with the largest value of a specific block header field
   * 
   * @param field the field to query on
   * @return the matching hash with the largest field value.
   */
  Hash findByLargest(BlockHeaderFields field);
}
