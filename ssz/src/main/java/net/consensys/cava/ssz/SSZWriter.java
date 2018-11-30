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
import static net.consensys.cava.ssz.SSZ.encodeNumber;

import net.consensys.cava.bytes.Bytes;
import net.consensys.cava.units.bigints.UInt256;

import java.math.BigInteger;
import javax.annotation.Nullable;


/**
 * A writer for encoding values to SSZ.
 */
public interface SSZWriter {

  /**
   * Append an already SSZ encoded value.
   *
   * <p>
   * Note that this method <b>may not</b> validate that {@code value} is a valid SSZ sequence. Appending an invalid SSZ
   * sequence will cause the entire SSZ encoding produced by this writer to also be invalid.
   *
   * @param value The SSZ encoded bytes to append.
   */
  void writeSSZ(Bytes value);

  /**
   * Encode a {@link Bytes} value to SSZ.
   *
   * @param value The byte array to encode.
   */
  void writeValue(Bytes value);

  /**
   * Encode a byte array to SSZ.
   *
   * @param value The byte array to encode.
   */
  default void writeByteArray(byte[] value) {
    writeValue(Bytes.wrap(value));
  }

  /**
   * Encode a byte to SSZ.
   *
   * @param value The byte value to encode.
   */
  default void writeByte(byte value) {
    writeValue(Bytes.of(value));
  }

  /**
   * Write an integer to the output.
   *
   * @param value The integer to write.
   */
  default void writeInt(int value) {
    writeLong(value);
  }

  /**
   * Write an integer to the output.
   *
   * @param value The integer to write.
   * @param size The number of bytes to write.
   */
  default void writeInt(int value, int size) {
    writeLong(value, size);
  }

  /**
   * Write a long to the output.
   *
   * @param value The long value to write.
   */
  void writeLong(long value);

  /**
   * Write a long to the output.
   *
   * @param value The long value to write.
   * @param size The number of bytes to write.
   */
  void writeLong(long value, int size);

  /**
   * Write a {@link UInt256} to the output.
   *
   * @param value The {@link UInt256} value to write.
   */
  default void writeUInt256(UInt256 value) {
    writeSSZ(value.toBytes());
  }

  /**
   * Write a big integer to the output.
   *
   * @param value The integer to write.
   */
  default void writeBigInteger(BigInteger value) {
    writeSSZ(Bytes.wrap(encodeNumber(value)));
  }

  /**
   * Write a big integer to the output.
   *
   * @param value The integer to write.
   * @param size The number of bytes to write.
   */
  default void writeBigInteger(BigInteger value, int size) {
    writeSSZ(Bytes.wrap(encodeNumber(value, size)));
  }

  /**
   * Write a string to the output.
   *
   * @param str The string to write.
   */
  default void writeString(String str) {
    writeByteArray(str.getBytes(UTF_8));
  }

  /**
   * Write a list of strings, which must be of the same length
   *
   * @param elements The strings to write as a list.
   * @throws IllegalArgumentException if the elements are not of the same length
   */
  default void writeList(String... elements) {
    writeInt(elements.length, 4);
    for (String value : elements) {
      writeString(value);
    }
  }

  /**
   * Write a list of integers.
   *
   * @param size the number of bytes to allocate per element
   * @param elements The integers to write as a list.
   * @throws IllegalArgumentException if an integer cannot be stored in the number of bytes provided
   */
  default void writeList(int size, int... elements) {
    writeInt(elements.length, 4);
    for (int value : elements) {
      writeInt(value, size);
    }
  }

  /**
   * Write a list of integers.
   *
   * @param size the number of bytes to allocate per element
   * @param elements The integers to write as a list.
   * @throws IllegalArgumentException if an integer cannot be stored in the number of bytes provided
   */
  default void writeList(int size, long... elements) {
    writeInt(elements.length, 4);
    for (long value : elements) {
      writeLong(value, size);
    }
  }

  /**
   * Write a list of integers.
   *
   * @param size the number of bytes to allocate per element
   * @param elements The integers to write as a list.
   * @throws IllegalArgumentException if an integer cannot be stored in the number of bytes provided
   */
  default void writeList(int size, BigInteger... elements) {
    writeInt(elements.length, 4);
    for (BigInteger value : elements) {
      writeBigInteger(value);
    }
  }

  /**
   * Write a list of booleans.
   *
   * @param elements The booleans to write as a list.
   */
  default void writeList(boolean... elements) {
    writeInt(elements.length, 4);
    for (boolean value : elements) {
      writeBoolean(value);
    }
  }

  /**
   * Write a list of unsigned 256-bit integers.
   *
   * @param elements The integers to write as a list.
   */
  default void writeList(UInt256... elements) {
    writeInt(elements.length, 4);
    for (UInt256 value : elements) {
      writeUInt256(value);
    }
  }

  /**
   * Write a list of addresses.
   *
   * @param addresses The addresses to write as a list.
   * @throws IllegalArgumentException if the addresses are not 20 bytes long.
   */
  default void writeListOfAddresses(Bytes... addresses) {
    writeList(20, addresses);
  }

  /**
   * Write a list of hashes.
   *
   * @param hashes The hashes to write as a list.
   * @throws IllegalArgumentException if the hashes are not 32 bytes long.
   */
  default void writeListOfHashes(Bytes... hashes) {
    writeList(32, hashes);
  }

  /**
   * Write a list of bytes.
   *
   * @param bytes The bytes to write as a list.
   * @throws IllegalArgumentException if the byte values have different lengths.
   */
  default void writeList(Bytes... bytes) {
    writeList(null, bytes);
  }

  /**
   * Write a list of bytes.
   *
   * @param length if not null, the length of an element of the list
   * @param bytes The bytes to write as a list.
   * @throws IllegalArgumentException if the byte values length don't match expected length.
   */
  default void writeList(@Nullable Integer length, Bytes... bytes) {
    writeInt(bytes.length, 4);
    for (Bytes value : bytes) {
      if (length != null && value.size() != length) {
        throw new IllegalArgumentException("value " + value.size() + " does not match expected length: " + length);
      }
      writeValue(value);
    }
  }

  /**
   * Write an address.
   *
   * @param address the address, exactly 20 bytes.
   * @throws IllegalArgumentException if {@code address.size != 20}
   */
  default void writeAddress(Bytes address) {
    if (address.size() != 20) {
      throw new IllegalArgumentException("Address of wrong length: should be 20 bytes long exactly");
    }
    writeSSZ(address);
  }

  /**
   * Write a hash.
   *
   * @param hash the hash, exactly 32 bytes.
   * @throws IllegalArgumentException if {@code hash.size != 32}
   */
  default void writeHash(Bytes hash) {
    if (hash.size() != 32) {
      throw new IllegalArgumentException("Hash of wrong length: should be 32 bytes long exactly");
    }
    writeSSZ(hash);
  }

  /**
   * Write a boolean.
   *
   * @param bool the boolean value
   */
  default void writeBoolean(Boolean bool) {
    if (bool) {
      writeSSZ(Bytes.of((byte) 0x01));
    } else {
      writeSSZ(Bytes.of((byte) 0x00));
    }
  }
}
