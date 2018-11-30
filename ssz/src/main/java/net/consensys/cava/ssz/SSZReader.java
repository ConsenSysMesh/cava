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
package net.consensys.cava.ssz;

import static java.nio.charset.StandardCharsets.UTF_8;

import net.consensys.cava.bytes.Bytes;
import net.consensys.cava.units.bigints.UInt256;

import java.math.BigInteger;

/**
 * A reader for consuming values from an SSZ encoded source.
 */
public interface SSZReader {

  /**
   * Read the next value from the SSZ source.
   *
   * @param length the number of bytes to read
   * @return The bytes for the next value.
   * @throws EndOfSSZException If there are no more SSZ values to read.
   */
  Bytes readValue(int length);

  /**
   * Read a byte array from the SSZ source.
   *
   * @param length the number of bytes to read
   * @return The byte array for the next value.
   * @throws EndOfSSZException If there are no more SSZ values to read.
   */
  default byte[] readByteArray(int length) {
    return readValue(length).toArrayUnsafe();
  }

  /**
   * Read the next value from the SSZ source.
   *
   * @return The bytes for the next value.
   * @throws EndOfSSZException If there are no more SSZ values to read.
   */
  default Bytes readValue() {
    int size = readInt(4);
    return readValue(size);
  }

  /**
   * Read a byte from the SSZ source.
   *
   * @return The byte for the next value.
   * @throws EndOfSSZException If there are no more SSZ values to read.
   */
  default byte readByte() {
    Bytes bytes = readValue(1);
    return bytes.get(0);
  }

  /**
   * Read an integer value from the SSZ source.
   *
   * @param length the number of bytes to read
   * @return An integer.
   * @throws EndOfSSZException If there are no more SSZ values to read.
   */
  default int readInt(int length) {
    Bytes bytes = readValue(length);
    try {
      return bytes.toInt();
    } catch (IllegalArgumentException e) {
      throw new InvalidSSZTypeException("Value is too large to be represented as an int");
    }
  }

  /**
   * Read a long value from the SSZ source.
   *
   * @param length the number of bytes to read
   * @return A long.
   * @throws InvalidSSZTypeException If the next SSZ value cannot be represented as a long.
   * @throws EndOfSSZException If there are no more SSZ values to read.
   */
  default long readLong(int length) {
    Bytes bytes = readValue(length);
    try {
      return bytes.toLong();
    } catch (IllegalArgumentException e) {
      throw new InvalidSSZTypeException("Value is too large to be represented as a long");
    }
  }

  default boolean readBoolean() {
    return readByte() == (byte) 0x01;
  }

  /**
   * Read a {@link UInt256} value from the SSZ source.
   *
   * @return A {@link UInt256} value.
   * @throws InvalidSSZTypeException If the next SSZ value cannot be represented as a long.
   * @throws EndOfSSZException If there are no more SSZ values to read.
   */
  default UInt256 readUInt256() {
    Bytes bytes = readValue(32);
    try {
      return UInt256.fromBytes(bytes);
    } catch (IllegalArgumentException e) {
      throw new InvalidSSZTypeException("Value is too large to be represented as a UInt256");
    }
  }

  /**
   * Read a big integer value from the SSZ source.
   *
   * @return A big integer.
   * @throws InvalidSSZTypeException If the next SSZ value cannot be represented as a big integer.
   * @throws EndOfSSZException If there are no more SSZ values to read.
   */
  default BigInteger readBigInteger(int length) {
    Bytes bytes = readValue(length);
    return bytes.toUnsignedBigInteger();
  }

  /**
   * Read a string value from the SSZ source.
   *
   * @return A string.
   * @throws InvalidSSZTypeException If the next SSZ value cannot be represented as a string.
   * @throws EndOfSSZException If there are no more SSZ values to read.
   */
  default String readString() {
    return new String(readValue().toArrayUnsafe(), UTF_8);
  }

  /**
   * Check if all values have been read.
   *
   * @return {@code true} if all values have been read.
   */
  boolean isComplete();
}
