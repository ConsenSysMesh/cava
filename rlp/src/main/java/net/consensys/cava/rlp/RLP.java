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
package net.consensys.cava.rlp;

import static com.google.common.base.Preconditions.checkArgument;
import static java.nio.charset.StandardCharsets.UTF_8;
import static java.util.Objects.requireNonNull;

import net.consensys.cava.bytes.Bytes;

import java.math.BigInteger;
import java.util.List;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Function;


/**
 * Recursive Length Prefix (RLP) encoding and decoding.
 */
public final class RLP {
  private RLP() {}

  /**
   * Encode values to a {@link Bytes} value.
   * <p>
   * Important: this method does not write any list prefix to the result. If you are writing a RLP encoded list of
   * values, you usually want to use {@link #encodeList(Consumer)}.
   *
   * @param fn A consumer that will be provided with a {@link RLPWriter} that can consume values.
   * @return The RLP encoding in a {@link Bytes} value.
   */
  public static Bytes encode(Consumer<RLPWriter> fn) {
    requireNonNull(fn);
    BytesValueRLPWriter writer = new BytesValueRLPWriter();
    fn.accept(writer);
    return writer.toBytes();
  }

  /**
   * Encode a list of values to a {@link Bytes} value.
   *
   * @param fn A consumer that will be provided with a {@link RLPWriter} that can consume values.
   * @return The RLP encoding in a {@link Bytes} value.
   */
  public static Bytes encodeList(Consumer<RLPWriter> fn) {
    requireNonNull(fn);
    BytesValueRLPWriter writer = new BytesValueRLPWriter();
    writer.writeList(fn);
    return writer.toBytes();
  }

  /**
   * Encode a value to a {@link Bytes} value.
   *
   * @param value The value to encode.
   * @return The RLP encoding in a {@link Bytes} value.
   */
  public static Bytes encodeValue(Bytes value) {
    requireNonNull(value);
    return BytesValueRLPWriter.encodeValue(value.toArrayUnsafe());
  }

  /**
   * Encode a value to a {@link Bytes} value.
   *
   * @param value The value to encode.
   * @return The RLP encoding in a {@link Bytes} value.
   */
  public static Bytes encodeByteArray(byte[] value) {
    requireNonNull(value);
    return BytesValueRLPWriter.encodeValue(value);
  }

  /**
   * Encode a integer to a {@link Bytes} value.
   *
   * @param value The integer to encode.
   * @return The RLP encoding in a {@link Bytes} value.
   */
  public static Bytes encodeInt(int value) {
    return BytesValueRLPWriter.encodeLong(value);
  }

  /**
   * Encode a long to a {@link Bytes} value.
   *
   * @param value The long to encode.
   * @return The RLP encoding in a {@link Bytes} value.
   */
  public static Bytes encodeLong(long value) {
    return BytesValueRLPWriter.encodeLong(value);
  }

  /**
   * Encode a big integer to a {@link Bytes} value.
   *
   * @param value The big integer to encode.
   * @return The RLP encoding in a {@link Bytes} value.
   */
  public static Bytes encodeBigInteger(BigInteger value) {
    requireNonNull(value);
    return encode(writer -> writer.writeBigInteger(value));
  }

  /**
   * Encode a string to a {@link Bytes} value.
   *
   * @param str The string to encode.
   * @return The RLP encoding in a {@link Bytes} value.
   */
  public static Bytes encodeString(String str) {
    requireNonNull(str);
    return encodeByteArray(str.getBytes(UTF_8));
  }

  /**
   * Read and decode RLP from a {@link Bytes} value.
   * <p>
   * Important: this method does not consume any list prefix from the source data. If you are reading a RLP encoded list
   * of values, you usually want to use {@link #decodeList(Bytes, Function)}.
   *
   * @param source The RLP encoded bytes.
   * @param fn A function that will be provided a {@link RLPReader}.
   * @param <T> The result type of the reading function.
   * @return The result from the reading function.
   */
  public static <T> T decode(Bytes source, Function<RLPReader, T> fn) {
    requireNonNull(source);
    requireNonNull(fn);
    return fn.apply(new BytesValueRLPReader(source));
  }

  /**
   * Read an RLP encoded list of values from a {@link Bytes} value.
   *
   * @param source The RLP encoded bytes.
   * @param fn A function that will be provided a {@link RLPReader}.
   * @param <T> The result type of the reading function.
   * @return The result from the reading function.
   * @throws InvalidRLPEncodingException If there is an error decoding the RLP source.
   * @throws InvalidRLPTypeException If the first RLP value is not a list.
   */
  public static <T> T decodeList(Bytes source, Function<RLPReader, T> fn) {
    requireNonNull(source);
    requireNonNull(fn);
    checkArgument(source.size() > 0, "source is empty");
    return decode(source, reader -> reader.readList(fn));
  }

  /**
   * Read an RLP encoded list of values from a {@link Bytes} value, populating a mutable output list.
   *
   * @param source The RLP encoded bytes.
   * @param fn A function that will be provided a {@link RLPReader}.
   * @return The list supplied to {@code fn}.
   * @throws InvalidRLPEncodingException If there is an error decoding the RLP source.
   * @throws InvalidRLPTypeException If the first RLP value is not a list.
   */
  public static List<Object> decodeList(Bytes source, BiConsumer<RLPReader, List<Object>> fn) {
    requireNonNull(source);
    requireNonNull(fn);
    checkArgument(source.size() > 0, "source is empty");
    return decode(source, reader -> reader.readList(fn));
  }

  /**
   * Read an RLP encoded value from a {@link Bytes} value.
   *
   * @param source The RLP encoded bytes.
   * @return The bytes for the value.
   * @throws InvalidRLPEncodingException If there is an error decoding the RLP source.
   * @throws EndOfRLPException If there are no RLP values to read.
   */
  public static Bytes decodeValue(Bytes source) {
    requireNonNull(source);
    return decode(source, RLPReader::readValue);
  }

  /**
   * Read an RLP encoded integer from a {@link Bytes} value.
   *
   * @param source The RLP encoded bytes.
   * @return An integer.
   * @throws InvalidRLPEncodingException If there is an error decoding the RLP source.
   */
  public static int decodeInt(Bytes source) {
    requireNonNull(source);
    checkArgument(source.size() > 0, "source is empty");
    return decode(source, RLPReader::readInt);
  }

  /**
   * Read an RLP encoded long from a {@link Bytes} value.
   *
   * @param source The RLP encoded bytes.
   * @return A long.
   * @throws InvalidRLPEncodingException If there is an error decoding the RLP source.
   */
  public static long decodeLong(Bytes source) {
    requireNonNull(source);
    checkArgument(source.size() > 0, "source is empty");
    return decode(source, RLPReader::readLong);
  }

  /**
   * Read an RLP encoded big integer from a {@link Bytes} value.
   *
   * @param source The RLP encoded bytes.
   * @return A {@link BigInteger}.
   * @throws InvalidRLPEncodingException If there is an error decoding the RLP source.
   */
  public static BigInteger decodeBigInteger(Bytes source) {
    requireNonNull(source);
    checkArgument(source.size() > 0, "source is empty");
    return decode(source, RLPReader::readBigInteger);
  }

  /**
   * Read an RLP encoded string from a {@link Bytes} value.
   *
   * @param source The RLP encoded bytes.
   * @return A string.
   * @throws InvalidRLPEncodingException If there is an error decoding the RLP source.
   */
  public static String decodeString(Bytes source) {
    requireNonNull(source);
    checkArgument(source.size() > 0, "source is empty");
    return decode(source, RLPReader::readString);
  }

  /**
   * Check if the {@link Bytes} value contains an RLP encoded list.
   *
   * @param value The value to check.
   * @return <tt>true</tt> if the value contains a list.
   */
  public static boolean isList(Bytes value) {
    requireNonNull(value);
    checkArgument(value.size() > 0, "value is empty");
    return decode(value, RLPReader::nextIsList);
  }
}
