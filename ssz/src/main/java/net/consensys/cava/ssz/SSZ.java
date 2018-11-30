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

import static com.google.common.base.Preconditions.checkArgument;
import static java.nio.charset.StandardCharsets.UTF_8;
import static java.util.Objects.requireNonNull;

import net.consensys.cava.bytes.Bytes;
import net.consensys.cava.units.bigints.UInt256;

import java.math.BigInteger;
import java.nio.BufferOverflowException;
import java.nio.ByteBuffer;
import java.nio.ReadOnlyBufferException;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Function;

/**
 * Simple Serialize (SSZ) encoding and decoding.
 */
public final class SSZ {

  private SSZ() {}

  /**
   * Encode values to a {@link Bytes} value.
   * <p>
   *
   * @param fn A consumer that will be provided with a {@link SSZWriter} that can consume values.
   * @return The SSZ encoding in a {@link Bytes} value.
   */
  public static Bytes encode(Consumer<SSZWriter> fn) {
    requireNonNull(fn);
    BytesSSZWriter writer = new BytesSSZWriter();
    fn.accept(writer);
    return writer.toBytes();
  }

  /**
   * Encode values to a {@link ByteBuffer}.
   * <p>
   *
   * @param buffer The buffer to write into, starting from its current position.
   * @param fn A consumer that will be provided with a {@link SSZWriter} that can consume values.
   * @param <T> The type of the buffer.
   * @return The buffer.
   * @throws BufferOverflowException If the writer attempts to write more than the provided buffer can hold.
   * @throws ReadOnlyBufferException If the provided buffer is read-only.
   */
  public static <T extends ByteBuffer> T encodeTo(T buffer, Consumer<SSZWriter> fn) {
    requireNonNull(fn);
    ByteBufferSSZWriter writer = new ByteBufferSSZWriter(buffer);
    fn.accept(writer);
    return buffer;
  }

  /**
   * Encode a value to a {@link Bytes} value.
   *
   * @param value The value to encode.
   * @return The SSZ encoding in a {@link Bytes} value.
   */
  public static Bytes encodeValue(Bytes value) {
    requireNonNull(value);
    return encodeByteArray(value.toArrayUnsafe());
  }

  /**
   * Encode a value to a {@link Bytes} value.
   *
   * @param value The value to encode.
   * @return The SSZ encoding in a {@link Bytes} value.
   */
  public static Bytes encodeByteArray(byte[] value) {
    requireNonNull(value);
    ByteBuffer buffer = ByteBuffer.allocate(Integer.BYTES);
    buffer.putInt(value.length);
    byte[] size = buffer.array();
    return Bytes.concatenate(Bytes.wrap(size), Bytes.wrap(value));
  }

  static void encodeByteArray(byte[] value, Consumer<byte[]> appender) {
    requireNonNull(value);
    ByteBuffer buffer = ByteBuffer.allocate(Integer.BYTES);
    buffer.putInt(value.length);
    byte[] size = buffer.array();
    appender.accept(size);
    appender.accept(value);
  }

  /**
   * Encode a integer to a {@link Bytes} value.
   *
   * @param value The integer to encode.
   * @return The SSZ encoding in a {@link Bytes} value.
   */
  public static Bytes encodeInt(int value) {
    return encodeLong(value);
  }

  /**
   * Encode a long to a {@link Bytes} value.
   *
   * @param value The long to encode.
   * @return The SSZ encoding in a {@link Bytes} value.
   */
  public static Bytes encodeLong(long value) {
    return Bytes.wrap(encodeNumber(value));
  }

  static byte[] encodeNumber(long value) {
    int length = (int) Math.ceil(Long.toHexString(value).length() / 2.0);
    return encodeNumber(value, length);
  }

  static byte[] encodeNumber(BigInteger value) {
    byte[] byteArray = value.toByteArray();
    int length = byteArray.length;
    if (byteArray[0] == 0) {
      length--;
    }
    return encodeNumber(value, length);
  }

  static byte[] encodeNumber(BigInteger value, int size) {
    byte[] byteArray = value.toByteArray();
    Bytes bytesValue = Bytes.wrap(value.toByteArray());
    if (byteArray[0] == 0) {
      bytesValue = bytesValue.slice(1);
    }

    if (bytesValue.size() > size) {
      throw new IllegalArgumentException(
          "Cannot write " + bytesValue.size() + " bytes in allocated " + size + " bytes");
    }

    return Bytes.concatenate(Bytes.wrap(new byte[size - bytesValue.size()]), bytesValue).toArrayUnsafe();
  }

  static byte[] encodeNumber(long value, int size) {
    if (Long.bitCount(value) / 8 > size) {
      throw new IllegalArgumentException("Cannot write a number " + value + " with " + size + " bytes");
    }
    ByteBuffer buffer = ByteBuffer.allocate(size);
    for (int i = size; i > 0; i--) {
      buffer.put((byte) (value >> ((i - 1) * 8) & 0xFF));
    }

    return buffer.array();
  }

  /**
   * Encode a big integer to a {@link Bytes} value.
   *
   * @param value The big integer to encode.
   * @return The SSZ encoding in a {@link Bytes} value.
   */
  public static Bytes encodeBigInteger(BigInteger value) {
    requireNonNull(value);
    return encode(writer -> writer.writeBigInteger(value));
  }

  /**
   * Encode a string to a {@link Bytes} value.
   *
   * @param str The string to encode.
   * @return The SSZ encoding in a {@link Bytes} value.
   */
  public static Bytes encodeString(String str) {
    requireNonNull(str);
    return encodeByteArray(str.getBytes(UTF_8));
  }

  /**
   * Encode a list of integers.
   *
   * @param size the number of bytes an integer will be written to.
   * @param elements the integers to write
   * @return SSZ encoding in a {@link Bytes} value.
   */
  public static Bytes encodeList(int size, int... elements) {
    requireNonNull(elements);
    return encode(writer -> writer.writeList(size, elements));
  }

  /**
   * Encode a list of strings.
   *
   * @param elements the strings to write
   * @return SSZ encoding in a {@link Bytes} value.
   */
  public static Bytes encodeList(String... elements) {
    requireNonNull(elements);
    return encode(writer -> writer.writeList(elements));
  }

  /**
   * Encode a list of bytes.
   *
   * @param elements the bytes to write
   * @return SSZ encoding in a {@link Bytes} value.
   */
  public static Bytes encodeList(Bytes... elements) {
    requireNonNull(elements);
    return encode(writer -> writer.writeList(elements));
  }

  /**
   * Encode a list of integers.
   *
   * @param size the number of bytes an integer will be written to.
   * @param elements the integers to write
   * @return SSZ encoding in a {@link Bytes} value.
   */
  public static Bytes encodeList(int size, long... elements) {
    requireNonNull(elements);
    return encode(writer -> writer.writeList(size, elements));
  }

  /**
   * Encode a list of hashes.
   *
   * @param hashes the hashes to write
   * @return SSZ encoding in a {@link Bytes} value.
   */
  public static Bytes encodeListOfHashes(Bytes... hashes) {
    requireNonNull(hashes);
    return encode(writer -> writer.writeListOfHashes(hashes));
  }

  /**
   * Encode a list of hashes.
   *
   * @param addresses the addresses to write
   * @return SSZ encoding in a {@link Bytes} value.
   */
  public static Bytes encodeListOfAddresses(Bytes... addresses) {
    requireNonNull(addresses);
    return encode(writer -> writer.writeListOfAddresses(addresses));
  }

  /**
   * Encode a list of booleans.
   *
   * @param booleans the booleans to write
   * @return SSZ encoding in a {@link Bytes} value.
   */
  public static Bytes encodeList(boolean... booleans) {
    requireNonNull(booleans);
    return encode(writer -> writer.writeList(booleans));
  }

  /**
   * Encode a list of strings to a buffer.
   *
   * @param buffer the buffer to encode into
   * @param elements the strings to write
   * @return the buffer after appending the bytes
   */
  public static <T extends ByteBuffer> T encodeListTo(T buffer, String... elements) {
    requireNonNull(elements);
    return encodeTo(buffer, writer -> writer.writeList(elements));
  }

  /**
   * Encode a list of hashes to a buffer.
   *
   * @param buffer the buffer to encode into
   * @param elements the hashes to write
   * @return the buffer after appending the bytes
   */
  public static <T extends ByteBuffer> T encodeListOfHashesTo(T buffer, Bytes... elements) {
    requireNonNull(elements);
    return encodeTo(buffer, writer -> writer.writeListOfHashes(elements));
  }

  /**
   * Encode a list of addresses to a buffer.
   *
   * @param buffer the buffer to encode into
   * @param elements the hashes to write
   * @return the buffer after appending the bytes
   */
  public static <T extends ByteBuffer> T encodeListOfAddressesTo(T buffer, Bytes... elements) {
    requireNonNull(elements);
    return encodeTo(buffer, writer -> writer.writeListOfAddresses(elements));
  }

  /**
   * Encode a list of bytes to a buffer.
   *
   * @param buffer the buffer to encode into
   * @param elements the bytes to write
   * @return the buffer after appending the bytes
   */
  public static <T extends ByteBuffer> T encodeListTo(T buffer, Bytes... elements) {
    requireNonNull(elements);
    return encodeTo(buffer, writer -> writer.writeList(elements));
  }

  /**
   * Encode a list of booleans to a buffer.
   *
   * @param buffer the buffer to encode into
   * @param elements the booleans to write
   * @return the buffer after appending the bytes
   */
  public static <T extends ByteBuffer> T encodeListTo(T buffer, boolean... elements) {
    requireNonNull(elements);
    return encodeTo(buffer, writer -> writer.writeList(elements));
  }

  /**
   * Encode a list of integers to a buffer.
   *
   * @param buffer the buffer to encode into
   * @param elements the integers to write
   * @return the buffer after appending the bytes
   */
  public static <T extends ByteBuffer> T encodeListTo(T buffer, int size, int... elements) {
    requireNonNull(elements);
    return encodeTo(buffer, writer -> writer.writeList(size, elements));
  }

  /**
   * Encode a list of integers to a buffer.
   *
   * @param buffer the buffer to encode into
   * @param elements the integers to write
   * @return the buffer after appending the bytes
   */
  public static <T extends ByteBuffer> T encodeListTo(T buffer, int size, long... elements) {
    requireNonNull(elements);
    return encodeTo(buffer, writer -> writer.writeList(size, elements));
  }

  /**
   * Encode a list of integers to a buffer.
   *
   *
   * @param buffer the buffer to encode into
   * @param elements the integers to write
   * @return the buffer after appending the bytes
   */
  public static <T extends ByteBuffer> T encodeListTo(T buffer, int size, BigInteger... elements) {
    requireNonNull(elements);
    return encodeTo(buffer, writer -> writer.writeList(size, elements));
  }

  /**
   * Encode a list of 256-bit unsigned integers to a buffer.
   *
   * @param buffer the buffer to encode into
   * @param elements the integers to write
   * @return the buffer after appending the bytes
   */
  public static <T extends ByteBuffer> T encodeListTo(T buffer, UInt256... elements) {
    requireNonNull(elements);
    return encodeTo(buffer, writer -> writer.writeList(elements));
  }

  /**
   * Read and decode SSZ from a {@link Bytes} value.
   * <p>
   *
   * @param source The SSZ encoded bytes.
   * @param fn A function that will be provided a {@link SSZReader}.
   * @param <T> The result type of the reading function.
   * @return The result from the reading function.
   */
  public static <T> T decode(Bytes source, Function<SSZReader, T> fn) {
    requireNonNull(source);
    requireNonNull(fn);
    return fn.apply(new BytesSSZReader(source));
  }

  /**
   * Read an SSZ encoded value from a {@link Bytes} value.
   *
   * @param source The SSZ encoded bytes.
   * @return The bytes for the value.
   * @throws EndOfSSZException If there are no SSZ values to read.
   */
  public static Bytes decodeValue(Bytes source) {
    requireNonNull(source);
    return decode(source, (reader) -> reader.readValue(source.size()));
  }

  /**
   * Read an SSZ encoded integer from a {@link Bytes} value.
   *
   * @param source The SSZ encoded bytes.
   * @return An integer.
   */
  public static int decodeInt(Bytes source) {
    requireNonNull(source);
    checkArgument(source.size() > 0, "source is empty");
    return decode(source, (reader) -> reader.readInt(source.size()));
  }

  /**
   * Read an SSZ encoded long from a {@link Bytes} value.
   *
   * @param source The SSZ encoded bytes.
   * @return A long.
   */
  public static long decodeLong(Bytes source) {
    requireNonNull(source);
    checkArgument(source.size() > 0, "source is empty");
    return decode(source, (reader) -> reader.readLong(source.size()));
  }

  public static List<String> decodeListOfStrings(Bytes source, int stringSize) {
    return decode(source, reader -> {
      int length = reader.readInt(4);
      List<String> result = new ArrayList<>();
      for (int i = 0; i < length; i++) {
        result.add(reader.readString());
      }
      return result;
    });
  }

  public static List<Bytes> decodeListOfBytes(Bytes source, int stringSize) {
    return decode(source, reader -> {
      int length = reader.readInt(4);
      List<Bytes> result = new ArrayList<>();
      for (int i = 0; i < length; i++) {
        result.add(reader.readValue());
      }
      return result;
    });
  }

  public static List<Bytes> decodeListOfHashes(Bytes source) {
    return decodeListOfBytes(source, 32);
  }

  public static List<Bytes> decodeListOfAddresses(Bytes source) {
    return decodeListOfBytes(source, 20);
  }

  public static List<Integer> decodeListOfIntegers(Bytes source, int intSize) {
    return decode(source, reader -> {
      int length = reader.readInt(4);
      List<Integer> result = new ArrayList<>();
      for (int i = 0; i < length; i++) {
        result.add(reader.readInt(intSize));
      }
      return result;
    });
  }

  public static List<Boolean> decodeListOfBooleans(Bytes source) {
    return decode(source, reader -> {
      int length = reader.readInt(4);
      List<Boolean> result = new ArrayList<>();
      for (int i = 0; i < length; i++) {
        result.add(reader.readBoolean());
      }
      return result;
    });
  }

  /**
   * Read an SSZ encoded big integer from a {@link Bytes} value.
   *
   * @param source The SSZ encoded bytes.
   * @return A {@link BigInteger}.
   */
  public static BigInteger decodeBigInteger(Bytes source) {
    requireNonNull(source);
    checkArgument(source.size() > 0, "source is empty");
    return decode(source, (reader) -> reader.readBigInteger(source.size()));
  }

  /**
   * Read an SSZ encoded string from a {@link Bytes} value.
   *
   * @param source The SSZ encoded bytes.
   * @return A string.
   */
  public static String decodeString(Bytes source) {
    requireNonNull(source);
    checkArgument(source.size() > 0, "source is empty");
    return decode(source, (reader) -> reader.readString());
  }

}
