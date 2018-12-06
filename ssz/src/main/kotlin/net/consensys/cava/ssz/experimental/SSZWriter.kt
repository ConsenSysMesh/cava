/*
 * Copyright 2018 ConsenSys AG.
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
package net.consensys.cava.ssz.experimental

import net.consensys.cava.bytes.Bytes
import net.consensys.cava.ssz.SSZ
import net.consensys.cava.units.bigints.UInt256
import java.math.BigInteger

@ExperimentalUnsignedTypes
interface SSZWriter {

  /**
   * Append an already SSZ encoded value.
   *
   * Note that this method **may not** validate that `value` is a valid SSZ sequence. Appending an invalid SSZ
   * sequence will cause the entire SSZ encoding produced by this writer to also be invalid.
   *
   * @param value The SSZ encoded bytes to append.
   */
  fun writeSSZ(value: Bytes)

  /**
   * Append an already SSZ encoded value.
   *
   * Note that this method **may not** validate that `value` is a valid SSZ sequence. Appending an invalid SSZ
   * sequence will cause the entire SSZ encoding produced by this writer to also be invalid.
   *
   * @param value The SSZ encoded bytes to append.
   */
  fun writeSSZ(value: ByteArray) = writeSSZ(Bytes.wrap(value))

  /**
   * Encode a [Bytes] value to SSZ.
   *
   * @param value The byte array to encode.
   */
  fun writeBytes(value: Bytes)

  /**
   * Encode a byte array to SSZ.
   *
   * @param value The byte array to encode.
   */
  fun writeBytes(value: ByteArray)

  /**
   * Write a string to the output.
   *
   * @param str The string to write.
   */
  fun writeString(str: String)

  /**
   * Write a two's-compliment integer to the output.
   *
   * @param value The integer to write.
   * @param bitLength The bit length of the integer value.
   * @throws IllegalArgumentException If the value is too large for the specified bit length.
   */
  fun writeInt(value: Int, bitLength: Int)

  /**
   * Write a two's-compliment long to the output.
   *
   * @param value The long value to write.
   * @param bitLength The bit length of the integer value.
   * @throws IllegalArgumentException If the value is too large for the specified bit length.
   */
  fun writeLong(value: Long, bitLength: Int)

  /**
   * Write a big integer to the output.
   *
   * @param value The integer to write.
   * @param bitLength The bit length of the integer value.
   * @throws IllegalArgumentException If the value is too large for the specified bit length.
   */
  fun writeBigInteger(value: BigInteger, bitLength: Int) {
    writeSSZ(SSZ.encodeBigIntegerToByteArray(value, bitLength))
  }

  /**
   * Write an 8-bit two's-compliment integer to the output.
   *
   * @param value The integer to write.
   * @throws IllegalArgumentException If the value is too large for the specified bit length.
   */
  fun writeInt8(value: Int) {
    writeInt(value, 8)
  }

  /**
   * Write a 16-bit two's-compliment integer to the output.
   *
   * @param value The integer to write.
   * @throws IllegalArgumentException If the value is too large for the specified bit length.
   */
  fun writeInt16(value: Int) {
    writeInt(value, 16)
  }

  /**
   * Write a 32-bit two's-compliment integer to the output.
   *
   * @param value The integer to write.
   */
  fun writeInt32(value: Int) {
    writeInt(value, 32)
  }

  /**
   * Write a 64-bit two's-compliment integer to the output.
   *
   * @param value The long to write.
   */
  fun writeInt64(value: Long) {
    writeLong(value, 64)
  }

  /**
   * Write an unsigned integer to the output.
   *
   * @param value The integer to write.
   * @param bitLength The bit length of the integer value.
   * @throws IllegalArgumentException If the value is too large for the specified bit length.
   */
  fun writeUInt(value: UInt, bitLength: Int)

  /**
   * Write an unsigned long to the output.
   *
   * @param value The long value to write.
   * @param bitLength The bit length of the integer value.
   * @throws IllegalArgumentException If the value is too large for the specified bit length.
   */
  fun writeULong(value: ULong, bitLength: Int)

  /**
   * Write an 8-bit unsigned integer to the output.
   *
   * @param value The integer to write.
   * @throws IllegalArgumentException If the value is too large for the specified bit length.
   */
  fun writeUInt8(value: UInt) {
    writeUInt(value, 8)
  }

  /**
   * Write a 16-bit unsigned integer to the output.
   *
   * @param value The integer to write.
   * @throws IllegalArgumentException If the value is too large for the specified bit length.
   */
  fun writeUInt16(value: UInt) {
    writeUInt(value, 16)
  }

  /**
   * Write a 32-bit unsigned integer to the output.
   *
   * @param value The integer to write.
   */
  fun writeUInt32(value: UInt) {
    writeUInt(value, 32)
  }

  /**
   * Write a 64-bit unsigned integer to the output.
   *
   * @param value The long to write.
   */
  fun writeUInt64(value: ULong) {
    writeULong(value, 64)
  }

  /**
   * Write a [UInt256] to the output.
   *
   * @param value The [UInt256] to write.
   */
  fun writeUInt256(value: UInt256) {
    writeSSZ(SSZ.encodeUInt256(value))
  }

  /**
   * Write a boolean to the output.
   *
   * @param value The boolean value.
   */
  fun writeBoolean(value: Boolean) {
    writeSSZ(SSZ.encodeBoolean(value))
  }

  /**
   * Write an address.
   *
   * @param address The address (must be exactly 20 bytes).
   * @throws IllegalArgumentException If `address.size != 20`.
   */
  fun writeAddress(address: Bytes) {
    writeSSZ(SSZ.encodeAddress(address))
  }

  /**
   * Write a hash.
   *
   * @param hash The hash.
   */
  fun writeHash(hash: Bytes) {
    writeSSZ(SSZ.encodeHash(hash))
  }

  /**
   * Write a list of bytes.
   *
   * @param elements The bytes to write as a list.
   */
  fun writeBytesList(vararg elements: Bytes)

  /**
   * Write a list of strings, which must be of the same length
   *
   * @param elements The strings to write as a list.
   */
  fun writeStringList(vararg elements: String)

  /**
   * Write a list of two's compliment integers.
   *
   * @param bitLength The bit length of the encoded integers (must be a multiple of 8).
   * @param elements The integers to write as a list.
   * @throws IllegalArgumentException If any values are too large for the specified bit length.
   */
  fun writeIntList(bitLength: Int, vararg elements: Int)

  /**
   * Write a list of two's compliment long integers.
   *
   * @param bitLength The bit length of the encoded integers (must be a multiple of 8).
   * @param elements The long integers to write as a list.
   * @throws IllegalArgumentException If any values are too large for the specified bit length.
   */
  fun writeLongIntList(bitLength: Int, vararg elements: Long)

  /**
   * Write a list of big integers.
   *
   * @param bitLength The bit length of each integer.
   * @param elements The integers to write as a list.
   * @throws IllegalArgumentException if an integer cannot be stored in the number of bytes provided
   */
  fun writeBigIntegerList(bitLength: Int, vararg elements: BigInteger)

  /**
   * Write a list of 8-bit two's compliment integers.
   *
   * @param elements The integers to write as a list.
   * @throws IllegalArgumentException If any values are too large for the specified bit length.
   */
  fun writeInt8List(vararg elements: Int) {
    writeIntList(8, *elements)
  }

  /**
   * Write a list of 16-bit two's compliment integers.
   *
   * @param elements The integers to write as a list.
   * @throws IllegalArgumentException If any values are too large for the specified bit length.
   */
  fun writeInt16List(vararg elements: Int) {
    writeIntList(16, *elements)
  }

  /**
   * Write a list of 32-bit two's compliment integers.
   *
   * @param elements The integers to write as a list.
   * @throws IllegalArgumentException If any values are too large for the specified bit length.
   */
  fun writeInt32List(vararg elements: Int) {
    writeIntList(32, *elements)
  }

  /**
   * Write a list of 64-bit two's compliment integers.
   *
   * @param elements The integers to write as a list.
   * @throws IllegalArgumentException If any values are too large for the specified bit length.
   */
  fun writeInt64List(vararg elements: Int) {
    writeIntList(64, *elements)
  }

  /**
   * Write a list of unsigned integers.
   *
   * @param bitLength The bit length of the encoded integers (must be a multiple of 8).
   * @param elements The integers to write as a list.
   * @throws IllegalArgumentException If any values are too large for the specified bit length.
   */
  fun writeUIntList(bitLength: Int, vararg elements: UInt)

  /**
   * Write a list of unsigned long integers.
   *
   * @param bitLength The bit length of the encoded integers (must be a multiple of 8).
   * @param elements The long integers to write as a list.
   * @throws IllegalArgumentException If any values are too large for the specified bit length.
   */
  fun writeULongIntList(bitLength: Int, vararg elements: ULong)

  /**
   * Write a list of 8-bit unsigned integers.
   *
   * @param elements The integers to write as a list.
   * @throws IllegalArgumentException If any values are too large for the specified bit length.
   */
  fun writeUInt8List(vararg elements: UInt) {
    writeUIntList(8, *elements)
  }

  /**
   * Write a list of 16-bit unsigned integers.
   *
   * @param elements The integers to write as a list.
   * @throws IllegalArgumentException If any values are too large for the specified bit length.
   */
  fun writeUInt16List(vararg elements: UInt) {
    writeUIntList(16, *elements)
  }

  /**
   * Write a list of 32-bit unsigned integers.
   *
   * @param elements The integers to write as a list.
   * @throws IllegalArgumentException If any values are too large for the specified bit length.
   */
  fun writeUInt32List(vararg elements: ULong) {
    writeULongIntList(32, *elements)
  }

  /**
   * Write a list of 64-bit unsigned integers.
   *
   * @param elements The integers to write as a list.
   * @throws IllegalArgumentException If any values are too large for the specified bit length.
   */
  fun writeUInt64List(vararg elements: ULong) {
    writeULongIntList(64, *elements)
  }

  /**
   * Write a list of unsigned 256-bit integers.
   *
   * @param elements The integers to write as a list.
   */
  fun writeUInt256List(vararg elements: UInt256)

  /**
   * Write a list of hashes.
   *
   * @param elements The hashes to write as a list.
   */
  fun writeHashList(vararg elements: Bytes)

  /**
   * Write a list of addresses.
   *
   * @param elements The addresses to write as a list.
   * @throws IllegalArgumentException If any `address.size != 20`.
   */
  fun writeAddressList(vararg elements: Bytes)

  /**
   * Write a list of booleans.
   *
   * @param elements The booleans to write as a list.
   */
  fun writeBooleanList(vararg elements: Boolean)
}
