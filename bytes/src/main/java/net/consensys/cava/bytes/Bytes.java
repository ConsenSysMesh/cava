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
package net.consensys.cava.bytes;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkElementIndex;
import static com.google.common.base.Preconditions.checkNotNull;
import static java.lang.String.format;

import java.io.IOException;
import java.math.BigInteger;
import java.nio.ByteBuffer;
import java.security.MessageDigest;
import java.util.Arrays;

import io.netty.buffer.ByteBuf;
import io.vertx.core.buffer.Buffer;

/**
 * A value made of bytes.
 *
 * <p>
 * This interface makes no thread-safety guarantee, and a {@link Bytes} value is generally not thread safe. However,
 * specific implementations may be thread-safe. For instance, the value returned by {@link #copy} is guaranteed to be
 * thread-safe as it is immutable.
 */
public interface Bytes {

  /**
   * The empty value (with 0 bytes).
   */
  Bytes EMPTY = wrap(new byte[0]);

  /**
   * Wrap the provided byte array as a {@link Bytes} value.
   *
   * <p>
   * Note that value is not copied and thus any future update to {@code value} will be reflected in the returned value.
   *
   * @param value The value to wrap.
   * @return A {@link Bytes} value wrapping {@code value}.
   */
  static Bytes wrap(byte[] value) {
    return wrap(value, 0, value.length);
  }

  /**
   * Wrap a slice of a byte array as a {@link Bytes} value.
   *
   * <p>
   * Note that value is not copied and thus any future update to {@code value} within the slice will be reflected in the
   * returned value.
   *
   * @param value The value to wrap.
   * @param offset The index (inclusive) in {@code value} of the first byte exposed by the returned value. In other
   *        words, you will have {@code wrap(value, o, l).get(0) == value[o]}.
   * @param length The length of the resulting value.
   * @return A {@link Bytes} value that expose the bytes of {@code value} from {@code offset} (inclusive) to
   *         {@code offset + length} (exclusive).
   * @throws IndexOutOfBoundsException if {@code offset &lt; 0 || (value.length > 0 && offset >=
   *     value.length)}.
   * @throws IllegalArgumentException if {@code length &lt; 0 || offset + length > value.length}.
   */
  static Bytes wrap(byte[] value, int offset, int length) {
    checkNotNull(value);
    if (length == 32) {
      return new ArrayWrappingBytes32(value, offset);
    }
    return new ArrayWrappingBytes(value, offset, length);
  }

  /**
   * Wrap a list of other values into a concatenated view.
   *
   * <p>
   * Note that the values are not copied and thus any future update to the values will be reflected in the returned
   * value. If copying the inputs is desired, use {@link #concatenate(Bytes...)}.
   *
   * @param values The values to wrap.
   * @return A value representing a view over the concatenation of all {@code values}.
   * @throws IllegalArgumentException if the result overflows an int.
   */
  static Bytes wrap(Bytes... values) {
    return ConcatenatedBytes.wrap(values);
  }

  /**
   * Create a value containing the concatenation of the values provided.
   *
   * @param values The values to copy and concatenate.
   * @return A value containing the result of concatenating the value from {@code values} in their provided order.
   * @throws IllegalArgumentException if the result overflows an int.
   */
  static Bytes concatenate(Bytes... values) {
    if (values.length == 0) {
      return EMPTY;
    }

    int size;
    try {
      size = Arrays.stream(values).mapToInt(Bytes::size).reduce(0, Math::addExact);
    } catch (ArithmeticException e) {
      throw new IllegalArgumentException("Combined length of values is too long (> Integer.MAX_VALUE)");
    }

    MutableBytes result = MutableBytes.create(size);
    int offset = 0;
    for (Bytes value : values) {
      value.copyTo(result, offset);
      offset += value.size();
    }
    return result;
  }

  /**
   * Wrap a full Vert.x {@link Buffer} as a {@link Bytes} value.
   *
   * <p>
   * Note that any change to the content of the buffer may be reflected in the returned value.
   *
   * @param buffer The buffer to wrap.
   * @return A {@link Bytes} value.
   */
  static Bytes wrapBuffer(Buffer buffer) {
    checkNotNull(buffer);
    if (buffer.length() == 0) {
      return EMPTY;
    }
    return new BufferWrappingBytes(buffer);
  }

  /**
   * Wrap a slice of a Vert.x {@link Buffer} as a {@link Bytes} value.
   *
   * <p>
   * Note that any change to the content of the buffer may be reflected in the returned value.
   *
   * @param buffer The buffer to wrap.
   * @param offset The offset in {@code buffer} from which to expose the bytes in the returned value. That is,
   *        {@code wrapBuffer(buffer, i, 1).get(0) == buffer.getByte(i)}.
   * @param size The size of the returned value.
   * @return A {@link Bytes} value.
   * @throws IndexOutOfBoundsException if {@code offset &lt; 0 || (buffer.length() > 0 && offset >=
   *     buffer.length())}.
   * @throws IllegalArgumentException if {@code length &lt; 0 || offset + length > buffer.length()}.
   */
  static Bytes wrapBuffer(Buffer buffer, int offset, int size) {
    checkNotNull(buffer);
    if (size == 0) {
      return EMPTY;
    }
    return new BufferWrappingBytes(buffer, offset, size);
  }

  /**
   * Wrap a full Netty {@link ByteBuf} as a {@link Bytes} value.
   *
   * <p>
   * Note that any change to the content of the byteBuf may be reflected in the returned value.
   *
   * @param byteBuf The {@link ByteBuf} to wrap.
   * @return A {@link Bytes} value.
   */
  static Bytes wrapByteBuf(ByteBuf byteBuf) {
    checkNotNull(byteBuf);
    if (byteBuf.capacity() == 0) {
      return EMPTY;
    }
    return new ByteBufWrappingBytes(byteBuf);
  }

  /**
   * Wrap a slice of a Netty {@link ByteBuf} as a {@link Bytes} value.
   *
   * <p>
   * Note that any change to the content of the buffer may be reflected in the returned value.
   *
   * @param byteBuf The {@link ByteBuf} to wrap.
   * @param offset The offset in {@code byteBuf} from which to expose the bytes in the returned value. That is,
   *        {@code wrapByteBuf(byteBuf, i, 1).get(0) == byteBuf.getByte(i)}.
   * @param size The size of the returned value.
   * @return A {@link Bytes} value.
   * @throws IndexOutOfBoundsException if {@code offset &lt; 0 || (byteBuf.capacity() > 0 && offset >=
   *     byteBuf.capacity())}.
   * @throws IllegalArgumentException if {@code length &lt; 0 || offset + length > byteBuf.capacity()}.
   */
  static Bytes wrapByteBuf(ByteBuf byteBuf, int offset, int size) {
    checkNotNull(byteBuf);
    if (size == 0) {
      return EMPTY;
    }
    return new ByteBufWrappingBytes(byteBuf, offset, size);
  }

  /**
   * Wrap a full Java NIO {@link ByteBuffer} as a {@link Bytes} value.
   *
   * <p>
   * Note that any change to the content of the byteBuf may be reflected in the returned value.
   *
   * @param byteBuffer The {@link ByteBuffer} to wrap.
   * @return A {@link Bytes} value.
   */
  static Bytes wrapByteBuffer(ByteBuffer byteBuffer) {
    checkNotNull(byteBuffer);
    if (byteBuffer.limit() == 0) {
      return EMPTY;
    }
    return new ByteBufferWrappingBytes(byteBuffer);
  }

  /**
   * Wrap a slice of a Java NIO {@link ByteBuf} as a {@link Bytes} value.
   *
   * <p>
   * Note that any change to the content of the buffer may be reflected in the returned value.
   *
   * @param byteBuffer The {@link ByteBuffer} to wrap.
   * @param offset The offset in {@code byteBuffer} from which to expose the bytes in the returned value. That is,
   *        {@code wrapByteBuffer(byteBuffer, i, 1).get(0) == byteBuffer.getByte(i)}.
   * @param size The size of the returned value.
   * @return A {@link Bytes} value.
   * @throws IndexOutOfBoundsException if {@code offset &lt; 0 || (byteBuffer.limit() > 0 && offset >=
   *     byteBuf.limit())}.
   * @throws IllegalArgumentException if {@code length &lt; 0 || offset + length > byteBuffer.limit()}.
   */
  static Bytes wrapByteBuffer(ByteBuffer byteBuffer, int offset, int size) {
    checkNotNull(byteBuffer);
    if (size == 0) {
      return EMPTY;
    }
    return new ByteBufferWrappingBytes(byteBuffer, offset, size);
  }

  /**
   * Create a value that contains the specified bytes in their specified order.
   *
   * @param bytes The bytes that must compose the returned value.
   * @return A value containing the specified bytes.
   */
  static Bytes of(byte... bytes) {
    return wrap(bytes);
  }

  /**
   * Create a value that contains the specified bytes in their specified order.
   *
   * @param bytes The bytes.
   * @return A value containing bytes are the one from {@code bytes}.
   * @throws IllegalArgumentException if any of the specified would be truncated when storing as a byte.
   */
  static Bytes of(int... bytes) {
    byte[] result = new byte[bytes.length];
    for (int i = 0; i < bytes.length; i++) {
      int b = bytes[i];
      checkArgument(b == (((byte) b) & 0xff), "%sth value %s does not fit a byte", i + 1, b);
      result[i] = (byte) b;
    }
    return Bytes.wrap(result);
  }

  /**
   * Return a 2 bytes value corresponding to the provided value interpreted as an unsigned short value.
   *
   * @param value The value, which must fit an unsigned short.
   * @return A 2 bytes value corresponding to {@code v}.
   * @throws IllegalArgumentException if {@code v < 0} or {@code v} is too big to fit an unsigned 2-bytes short (that
   *         is, if {@code v >= (1 << 16)}).
   */
  static Bytes ofUnsignedShort(int value) {
    checkArgument(
        value >= 0 && value <= BytesValues.MAX_UNSIGNED_SHORT,
        "Value %s cannot be represented as an unsigned short (it is negative or too big)",
        value);
    byte[] res = new byte[2];
    res[0] = (byte) ((value >> 8) & 0xFF);
    res[1] = (byte) (value & 0xFF);
    return Bytes.wrap(res);
  }

  /**
   * Return a 4 bytes value corresponding to the provided value interpreted as an unsigned int value.
   *
   * @param value The value, which must fit an unsigned int.
   * @return A 4 bytes value corresponding to {@code v}.
   * @throws IllegalArgumentException if {@code v < 0} or {@code v} is too big to fit an unsigned 4-bytes int (that is,
   *         if {@code v >= (1L << 32)}).
   */
  static Bytes ofUnsignedInt(long value) {
    checkArgument(
        value >= 0 && value <= BytesValues.MAX_UNSIGNED_INT,
        "Value %s cannot be represented as an unsigned int (it is negative or too big)",
        value);
    byte[] res = new byte[4];
    res[0] = (byte) ((value >> 24) & 0xFF);
    res[1] = (byte) ((value >> 16) & 0xFF);
    res[2] = (byte) ((value >> 8) & 0xFF);
    res[3] = (byte) ((value) & 0xFF);
    return Bytes.wrap(res);
  }

  /**
   * Return the smallest bytes value whose bytes correspond to the provided long. That is, the returned value may be of
   * size less than 8 if the provided long has leading zero bytes.
   *
   * @param value The long from which to create the bytes value.
   * @return The minimal bytes representation corresponding to {@code l}.
   */
  static Bytes minimalBytes(long value) {
    if (value == 0) {
      return Bytes.EMPTY;
    }

    int zeros = Long.numberOfLeadingZeros(value);
    int resultBytes = 8 - (zeros / 8);

    byte[] result = new byte[resultBytes];
    int shift = 0;
    for (int i = 0; i < resultBytes; i++) {
      result[resultBytes - i - 1] = (byte) ((value >> shift) & 0xFF);
      shift += 8;
    }
    return Bytes.wrap(result);
  }

  /**
   * Parse a hexadecimal string into a {@link Bytes} value.
   *
   * <p>
   * This method is lenient in that {@code str} may of an odd length, in which case it will behave exactly as if it had
   * an additional 0 in front.
   *
   * @param str The hexadecimal string to parse, which may or may not start with "0x".
   * @return The value corresponding to {@code str}.
   * @throws IllegalArgumentException if {@code str} does not correspond to valid hexadecimal representation.
   */
  static Bytes fromHexStringLenient(CharSequence str) {
    checkNotNull(str);
    return BytesValues.fromHexString(str, -1, true);
  }

  /**
   * Parse a hexadecimal string into a {@link Bytes} value of the provided size.
   *
   * <p>
   * This method allows for {@code str} to have an odd length, in which case it will behave exactly as if it had an
   * additional 0 in front.
   *
   * @param str The hexadecimal string to parse, which may or may not start with "0x".
   * @param destinationSize The size of the returned value, which must be big enough to hold the bytes represented by
   *        {@code str}. If it is strictly bigger those bytes from {@code str}, the returned value will be left padded
   *        with zeros.
   * @return A value of size {@code destinationSize} corresponding to {@code str} potentially left-padded.
   * @throws IllegalArgumentException if {@code str} does not correspond to valid hexadecimal representation, represents
   *         more bytes than {@code destinationSize} or {@code destinationSize &lt; 0}.
   */
  static Bytes fromHexStringLenient(CharSequence str, int destinationSize) {
    checkNotNull(str);
    checkArgument(destinationSize >= 0, "Invalid negative destination size %s", destinationSize);
    return BytesValues.fromHexString(str, destinationSize, true);
  }

  /**
   * Parse a hexadecimal string into a {@link Bytes} value.
   *
   * <p>
   * This method requires that {@code str} have an even length.
   *
   * @param str The hexadecimal string to parse, which may or may not start with "0x".
   * @return The value corresponding to {@code str}.
   * @throws IllegalArgumentException if {@code str} does not correspond to valid hexadecimal representation, or is of
   *         an odd length.
   */
  static Bytes fromHexString(CharSequence str) {
    checkNotNull(str);
    return BytesValues.fromHexString(str, -1, false);
  }

  /**
   * Parse a hexadecimal string into a {@link Bytes} value.
   *
   * <p>
   * This method requires that {@code str} have an even length.
   *
   * @param str The hexadecimal string to parse, which may or may not start with "0x".
   * @param destinationSize The size of the returned value, which must be big enough to hold the bytes represented by
   *        {@code str}. If it is strictly bigger those bytes from {@code str}, the returned value will be left padded
   *        with zeros.
   * @return A value of size {@code destinationSize} corresponding to {@code str} potentially left-padded.
   * @throws IllegalArgumentException if {@code str} does correspond to valid hexadecimal representation, or is of an
   *         odd length.
   * @throws IllegalArgumentException if {@code str} does not correspond to valid hexadecimal representation, or is of
   *         an odd length, or represents more bytes than {@code destinationSize} or {@code destinationSize &lt; 0}.
   */
  static Bytes fromHexString(CharSequence str, int destinationSize) {
    checkNotNull(str);
    checkArgument(destinationSize >= 0, "Invalid negative destination size %s", destinationSize);
    return BytesValues.fromHexString(str, destinationSize, false);
  }

  /** @return The number of bytes this value represents. */
  int size();

  /**
   * Retrieve a byte in this value.
   *
   * @param i The index of the byte to fetch within the value (0-indexed).
   * @return The byte at index {@code i} in this value.
   * @throws IndexOutOfBoundsException if {@code i &lt; 0} or {i &gt;= size()}.
   */
  byte get(int i);

  /**
   * Retrieve the 4 bytes starting at the provided index in this value as an integer.
   *
   * @param i The index from which to get the int, which must less than or equal to {@code size() -
   *     4}.
   * @return An integer whose value is the 4 bytes from this value starting at index {@code i}.
   * @throws IndexOutOfBoundsException if {@code i &lt; 0} or {@code i &gt; size() - 4}.
   */
  default int getInt(int i) {
    int size = size();
    checkElementIndex(i, size);
    if (i > (size - 4)) {
      throw new IndexOutOfBoundsException(
          format("Value of size %s has not enough bytes to read a 4 bytes int from index %s", size, i));
    }

    int value = 0;
    value |= ((int) get(i) & 0xFF) << 24;
    value |= ((int) get(i + 1) & 0xFF) << 16;
    value |= ((int) get(i + 2) & 0xFF) << 8;
    value |= ((int) get(i + 3) & 0xFF);
    return value;
  }

  /**
   * The value corresponding to interpreting these bytes as an integer.
   *
   * @return An value corresponding to this value interpreted as an integer.
   * @throws IllegalArgumentException if {@code size() &gt; 4}.
   */
  default int intValue() {
    int i = size();
    checkArgument(i <= 4, "Value of size %s has more than 4 bytes", size());
    if (i == 0) {
      return 0;
    }
    int value = ((int) get(--i) & 0xFF);
    if (i == 0) {
      return value;
    }
    value |= ((int) get(--i) & 0xFF) << 8;
    if (i == 0) {
      return value;
    }
    value |= ((int) get(--i) & 0xFF) << 16;
    if (i == 0) {
      return value;
    }
    return value | ((int) get(--i) & 0xFF) << 24;
  }

  /**
   * Whether this value contains no bytes.
   *
   * @return true if the value contains no bytes
   */
  default boolean isEmpty() {
    return size() == 0;
  }

  /**
   * Retrieves the 8 bytes starting at the provided index in this value as a long.
   *
   * @param i The index from which to get the long, which must less than or equal to {@code size() -
   *     8}.
   * @return A long whose value is the 8 bytes from this value starting at index {@code i}.
   * @throws IndexOutOfBoundsException if {@code i &lt; 0} or {@code i &gt; size() - 8}.
   */
  default long getLong(int i) {
    int size = size();
    checkElementIndex(i, size);
    if (i > (size - 8)) {
      throw new IndexOutOfBoundsException(
          format("Value of size %s has not enough bytes to read a 8 bytes long from index %s", size, i));
    }

    long value = 0;
    value |= ((long) get(i) & 0xFF) << 56;
    value |= ((long) get(i + 1) & 0xFF) << 48;
    value |= ((long) get(i + 2) & 0xFF) << 40;
    value |= ((long) get(i + 3) & 0xFF) << 32;
    value |= ((long) get(i + 4) & 0xFF) << 24;
    value |= ((long) get(i + 5) & 0xFF) << 16;
    value |= ((long) get(i + 6) & 0xFF) << 8;
    value |= ((long) get(i + 7) & 0xFF);
    return value;
  }

  /**
   * The value corresponding to interpreting these bytes as a long.
   *
   * @return An value corresponding to this value interpreted as a long.
   * @throws IllegalArgumentException if {@code size() &gt; 8}.
   */
  default long longValue() {
    int i = size();
    checkArgument(i <= 8, "Value of size %s has more than 8 bytes", size());
    if (i == 0) {
      return 0;
    }
    long value = ((long) get(--i) & 0xFF);
    if (i == 0) {
      return value;
    }
    value |= ((long) get(--i) & 0xFF) << 8;
    if (i == 0) {
      return value;
    }
    value |= ((long) get(--i) & 0xFF) << 16;
    if (i == 0) {
      return value;
    }
    value |= ((long) get(--i) & 0xFF) << 24;
    if (i == 0) {
      return value;
    }
    value |= ((long) get(--i) & 0xFF) << 32;
    if (i == 0) {
      return value;
    }
    value |= ((long) get(--i) & 0xFF) << 40;
    if (i == 0) {
      return value;
    }
    value |= ((long) get(--i) & 0xFF) << 48;
    if (i == 0) {
      return value;
    }
    return value | ((long) get(--i) & 0xFF) << 56;
  }

  /**
   * The BigInteger corresponding to interpreting these bytes as a two's-complement signed integer.
   *
   * @return A {@link BigInteger} corresponding to interpreting these bytes as a two's-complement signed integer.
   */
  default BigInteger bigIntegerValue() {
    if (size() == 0) {
      return BigInteger.ZERO;
    }
    return new BigInteger(toArrayUnsafe());
  }

  /**
   * The BigInteger corresponding to interpreting these bytes as an unsigned integer.
   *
   * @return A positive (or zero) {@link BigInteger} corresponding to interpreting these bytes as an unsigned integer.
   */
  default BigInteger unsignedBigIntegerValue() {
    return new BigInteger(1, toArrayUnsafe());
  }

  /**
   * Whether this value has only zero bytes.
   *
   * @return <tt>true</tt> if all the bits of this value are zeros.
   */
  default boolean isZero() {
    for (int i = size() - 1; i >= 0; --i) {
      if (get(i) != 0)
        return false;
    }
    return true;
  }

  /**
   * Whether the bytes start with a zero bit value.
   *
   * @return true if the first bit equals zero
   */
  default boolean hasLeadingZero() {
    return size() > 0 && (get(0) & 0x80) == 0;
  }

  /**
   * @return The number of zero bits preceding the highest-order ("leftmost") one-bit, or {@code size() * 8} if all bits
   *         are zero.
   */
  default int numberOfLeadingZeros() {
    int size = size();
    for (int i = 0; i < size; i++) {
      byte b = get(i);
      if (b == 0) {
        continue;
      }

      return (i * 8) + Integer.numberOfLeadingZeros(b & 0xFF) - 3 * 8;
    }
    return size * 8;
  }

  /**
   * Whether the bytes start with a zero byte value.
   *
   * @return true if the first byte equals zero
   */
  default boolean hasLeadingZeroByte() {
    return size() > 0 && get(0) == 0;
  }

  /**
   * @return The number of leading zero bytes of the value.
   */
  default int numberOfLeadingZeroBytes() {
    int size = size();
    for (int i = 0; i < size; i++) {
      if (get(i) != 0) {
        return i - 1;
      }
    }
    return size;
  }

  /**
   * @return The number of bits following and including the highest-order ("leftmost") one-bit, or zero if all bits are
   *         zero.
   */
  default int bitLength() {
    int size = size();
    for (int i = 0; i < size; i++) {
      byte b = get(i);
      if (b == 0)
        continue;

      return (size * 8) - (i * 8) - (Integer.numberOfLeadingZeros(b & 0xFF) - 3 * 8);
    }
    return 0;
  }

  /**
   * Return a bit-wise AND of these bytes and the supplied bytes.
   *
   * If this value and the supplied value are different lengths, then the shorter will be zero-padded to the left.
   *
   * @param other The bytes to perform the operation with.
   * @return The result of a bit-wise AND.
   */
  default Bytes and(Bytes other) {
    return and(other, MutableBytes.create(Math.max(size(), other.size())));
  }

  /**
   * Calculate a bit-wise AND of these bytes and the supplied bytes.
   *
   * <p>
   * If this value or the supplied value are shorter in length than the output vector, then they will be zero-padded to
   * the left. Likewise, if either this value or the supplied valid is longer in length than the output vector, then
   * they will be truncated to the left.
   *
   * @param other The bytes to perform the operation with.
   * @param result The mutable output vector for the result.
   * @param <T> The {@link MutableBytes} value type.
   * @return The {@code result} output vector.
   */
  default <T extends MutableBytes> T and(Bytes other, T result) {
    checkNotNull(other);
    checkNotNull(result);
    int rSize = result.size();
    int offsetSelf = rSize - size();
    int offsetOther = rSize - other.size();
    for (int i = 0; i < rSize; i++) {
      byte b1 = (i < offsetSelf) ? 0x00 : get(i - offsetSelf);
      byte b2 = (i < offsetOther) ? 0x00 : other.get(i - offsetOther);
      result.set(i, (byte) (b1 & b2));
    }
    return result;
  }

  /**
   * Return a bit-wise OR of these bytes and the supplied bytes.
   *
   * <p>
   * If this value and the supplied value are different lengths, then the shorter will be zero-padded to the left.
   *
   * @param other The bytes to perform the operation with.
   * @return The result of a bit-wise OR.
   */
  default Bytes or(Bytes other) {
    return or(other, MutableBytes.create(Math.max(size(), other.size())));
  }

  /**
   * Calculate a bit-wise OR of these bytes and the supplied bytes.
   *
   * <p>
   * If this value or the supplied value are shorter in length than the output vector, then they will be zero-padded to
   * the left. Likewise, if either this value or the supplied valid is longer in length than the output vector, then
   * they will be truncated to the left.
   *
   * @param other The bytes to perform the operation with.
   * @param result The mutable output vector for the result.
   * @param <T> The {@link MutableBytes} value type.
   * @return The {@code result} output vector.
   */
  default <T extends MutableBytes> T or(Bytes other, T result) {
    checkNotNull(other);
    checkNotNull(result);
    int rSize = result.size();
    int offsetSelf = rSize - size();
    int offsetOther = rSize - other.size();
    for (int i = 0; i < rSize; i++) {
      byte b1 = (i < offsetSelf) ? 0x00 : get(i - offsetSelf);
      byte b2 = (i < offsetOther) ? 0x00 : other.get(i - offsetOther);
      result.set(i, (byte) (b1 | b2));
    }
    return result;
  }

  /**
   * Return a bit-wise XOR of these bytes and the supplied bytes.
   *
   * <p>
   * If this value and the supplied value are different lengths, then the shorter will be zero-padded to the left.
   *
   * @param other The bytes to perform the operation with.
   * @return The result of a bit-wise XOR.
   */
  default Bytes xor(Bytes other) {
    return xor(other, MutableBytes.create(Math.max(size(), other.size())));
  }

  /**
   * Calculate a bit-wise XOR of these bytes and the supplied bytes.
   *
   * <p>
   * If this value or the supplied value are shorter in length than the output vector, then they will be zero-padded to
   * the left. Likewise, if either this value or the supplied valid is longer in length than the output vector, then
   * they will be truncated to the left.
   *
   * @param other The bytes to perform the operation with.
   * @param result The mutable output vector for the result.
   * @param <T> The {@link MutableBytes} value type.
   * @return The {@code result} output vector.
   */
  default <T extends MutableBytes> T xor(Bytes other, T result) {
    checkNotNull(other);
    checkNotNull(result);
    int rSize = result.size();
    int offsetSelf = rSize - size();
    int offsetOther = rSize - other.size();
    for (int i = 0; i < rSize; i++) {
      byte b1 = (i < offsetSelf) ? 0x00 : get(i - offsetSelf);
      byte b2 = (i < offsetOther) ? 0x00 : other.get(i - offsetOther);
      result.set(i, (byte) (b1 ^ b2));
    }
    return result;
  }

  /**
   * Return a bit-wise NOT of these bytes.
   *
   * @return The result of a bit-wise NOT.
   */
  default Bytes not() {
    return not(MutableBytes.create(size()));
  }

  /**
   * Calculate a bit-wise NOT of these bytes.
   *
   * <p>
   * If this value is shorter in length than the output vector, then it will be zero-padded to the left. Likewise, if
   * this value is longer in length than the output vector, then it will be truncated to the left.
   *
   * @param result The mutable output vector for the result.
   * @param <T> The {@link MutableBytes} value type.
   * @return The {@code result} output vector.
   */
  default <T extends MutableBytes> T not(T result) {
    checkNotNull(result);
    int rSize = result.size();
    int offsetSelf = rSize - size();
    for (int i = 0; i < rSize; i++) {
      byte b1 = (i < offsetSelf) ? 0x00 : get(i - offsetSelf);
      result.set(i, (byte) ~b1);
    }
    return result;
  }

  /**
   * Shift all bits in this value to the right.
   *
   * @param distance The number of bits to shift by.
   * @return A value containing the shifted bits.
   */
  default Bytes shiftRight(int distance) {
    return shiftRight(distance, MutableBytes.create(size()));
  }

  /**
   * Shift all bits in this value to the right.
   *
   * <p>
   * If this value is shorter in length than the output vector, then it will be zero-padded to the left. Likewise, if
   * this value is longer in length than the output vector, then it will be truncated to the left (after shifting).
   *
   * @param distance The number of bits to shift by.
   * @param result The mutable output vector for the result.
   * @param <T> The {@link MutableBytes} value type.
   * @return The {@code result} output vector.
   */
  default <T extends MutableBytes> T shiftRight(int distance, T result) {
    checkNotNull(result);
    int rSize = result.size();
    int offsetSelf = rSize - size();

    int d = distance / 8;
    int s = distance % 8;
    int resIdx = rSize - 1;
    for (int i = rSize - 1 - d; i >= 0; i--) {
      byte res;
      if (i < offsetSelf) {
        res = 0;
      } else {
        int selfIdx = i - offsetSelf;
        int leftSide = (get(selfIdx) & 0xFF) >>> s;
        int rightSide = (selfIdx == 0) ? 0 : get(selfIdx - 1) << (8 - s);
        res = (byte) (leftSide | rightSide);
      }
      result.set(resIdx--, res);
    }
    for (; resIdx >= 0; resIdx--) {
      result.set(resIdx, (byte) 0);
    }
    return result;
  }

  /**
   * Shift all bits in this value to the left.
   *
   * @param distance The number of bits to shift by.
   * @return A value containing the shifted bits.
   */
  default Bytes shiftLeft(int distance) {
    return shiftLeft(distance, MutableBytes.create(size()));
  }

  /**
   * Shift all bits in this value to the left.
   *
   * <p>
   * If this value is shorter in length than the output vector, then it will be zero-padded to the left. Likewise, if
   * this value is longer in length than the output vector, then it will be truncated to the left.
   *
   * @param distance The number of bits to shift by.
   * @param result The mutable output vector for the result.
   * @param <T> The {@link MutableBytes} value type.
   * @return The {@code result} output vector.
   */
  default <T extends MutableBytes> T shiftLeft(int distance, T result) {
    checkNotNull(result);
    int size = size();
    int rSize = result.size();
    int offsetSelf = rSize - size;

    int d = distance / 8;
    int s = distance % 8;
    int resIdx = 0;
    for (int i = d; i < rSize; i++) {
      byte res;
      if (i < offsetSelf) {
        res = 0;
      } else {
        int selfIdx = i - offsetSelf;
        int leftSide = get(selfIdx) << s;
        int rightSide = (selfIdx == size - 1) ? 0 : (get(selfIdx + 1) & 0xFF) >>> (8 - s);
        res = (byte) (leftSide | rightSide);
      }
      result.set(resIdx++, res);
    }
    for (; resIdx < rSize; resIdx++) {
      result.set(resIdx, (byte) 0);
    }
    return result;
  }

  /**
   * Create a new value representing (a view of) a slice of the bytes of this value.
   *
   * <p>
   * Please note that the resulting slice is only a view and as such maintains a link to the underlying full value. So
   * holding a reference to the returned slice may hold more memory than the slide represents. Use {@link #copy} on the
   * returned slice if that is not what you want.
   *
   * @param i The start index for the slice.
   * @return A new value providing a view over the bytes from index {@code i} (included) to the end.
   * @throws IndexOutOfBoundsException if {@code i &lt; 0}.
   */
  default Bytes slice(int i) {
    int size = size();
    if (i >= size) {
      return EMPTY;
    }
    return slice(i, size - i);
  }

  /**
   * Create a new value representing (a view of) a slice of the bytes of this value.
   *
   * <p>
   * Please note that the resulting slice is only a view and as such maintains a link to the underlying full value. So
   * holding a reference to the returned slice may hold more memory than the slide represents. Use {@link #copy} on the
   * returned slice if that is not what you want.
   *
   * @param i The start index for the slice.
   * @param length The length of the resulting value.
   * @return A new value providing a view over the bytes from index {@code i} (included) to {@code i + length}
   *         (excluded).
   * @throws IllegalArgumentException if {@code length &lt; 0}.
   * @throws IndexOutOfBoundsException if {@code i &lt; 0} or {i &gt;= size()} or {i + length &gt; size()} .
   */
  Bytes slice(int i, int length);

  /**
   * Return a value equivalent to this one but guaranteed to 1) be deeply immutable (i.e. the underlying value will be
   * immutable) and 2) to not retain more bytes than exposed by the value.
   *
   * @return A value, equals to this one, but deeply immutable and that doesn't retain any "unreachable" bytes. For
   *         performance reasons, this is allowed to return this value however if it already fit those constraints.
   */
  Bytes copy();

  /**
   * Return a new mutable value initialized with the content of this value.
   *
   * @return A mutable copy of this value. This will copy bytes, modifying the returned value will <b>not</b> modify
   *         this value.
   */
  MutableBytes mutableCopy();

  /**
   * Copy the bytes of this value to the provided mutable one, which must have the same size.
   *
   * @param destination The mutable value to which to copy the bytes to, which must have the same size as this value. If
   *        you want to copy value where size differs, you should use {@link #slice} and/or
   *        {@link MutableBytes#mutableSlice} and apply the copy to the result.
   * @throws IllegalArgumentException if {@code this.size() != destination.size()}.
   */
  default void copyTo(MutableBytes destination) {
    checkNotNull(destination);
    checkArgument(
        destination.size() == size(),
        "Cannot copy %s bytes to destination of non-equal size %s",
        size(),
        destination.size());
    copyTo(destination, 0);
  }

  /**
   * Copy the bytes of this value to the provided mutable one from a particular offset.
   *
   * <p>
   * This is a (potentially slightly more efficient) shortcut for {@code
   * copyTo(destination.mutableSlice(destinationOffset, this.size()))}.
   *
   * @param destination The mutable value to which to copy the bytes to, which must have enough bytes from
   *        {@code destinationOffset} for the copied value.
   * @param destinationOffset The offset in {@code destination} at which the copy starts.
   * @throws IllegalArgumentException if the destination doesn't have enough room, that is if {@code
   *     this.size() &gt; (destination.size() - destinationOffset)}.
   */
  default void copyTo(MutableBytes destination, int destinationOffset) {
    checkNotNull(destination);

    // Special casing an empty source or the following checks might throw (even though we have
    // nothing to copy anyway) and this gets inconvenient for generic methods using copyTo() as
    // they may have to special case empty values because of this. As an example,
    // concatenate(EMPTY, EMPTY) would need to be special cased without this.
    int size = size();
    if (size == 0) {
      return;
    }

    checkElementIndex(destinationOffset, destination.size());
    checkArgument(
        destination.size() - destinationOffset >= size,
        "Cannot copy %s bytes, destination has only %s bytes from index %s",
        size,
        destination.size() - destinationOffset,
        destinationOffset);

    for (int i = 0; i < size; i++) {
      destination.set(destinationOffset + i, get(i));
    }
  }

  /**
   * Append the bytes of this value to the provided Vert.x {@link Buffer}.
   *
   * <p>
   * Note that since a Vert.x {@link Buffer} will grow as necessary, this method never fails.
   *
   * @param buffer The {@link Buffer} to which to append this value.
   */
  default void appendTo(Buffer buffer) {
    checkNotNull(buffer);
    for (int i = 0; i < size(); i++) {
      buffer.appendByte(get(i));
    }
  }

  /**
   * Append this value as a sequence of hexadecimal characters.
   *
   * @param appendable The appendable
   * @param <T> The appendable type.
   * @return The appendable.
   * @throws IOException If an IO error occurs.
   */
  default <T extends Appendable> T appendHexTo(T appendable) throws IOException {
    int size = size();
    for (int i = 0; i < size; i++) {
      byte b = get(i);
      appendable.append(AbstractBytes.HEX_CODE[b >> 4 & 15]);
      appendable.append(AbstractBytes.HEX_CODE[b & 15]);
    }
    return appendable;
  }

  /**
   * Return the number of bytes in common between this set of bytes and another.
   *
   * @param other The bytes to compare to.
   * @return The number of common bytes.
   */
  default int commonPrefixLength(Bytes other) {
    checkNotNull(other);
    int ourSize = size();
    int otherSize = other.size();
    int i = 0;
    while (i < ourSize && i < otherSize && get(i) == other.get(i)) {
      i++;
    }
    return i;
  }

  /**
   * Return a slice over the common prefix between this set of bytes and another.
   *
   * @param other The bytes to compare to.
   * @return A slice covering the common prefix.
   */
  default Bytes commonPrefix(Bytes other) {
    return slice(0, commonPrefixLength(other));
  }

  /**
   * Return a slice of representing the same value but without any leading zero bytes.
   *
   * @return {@code value} if its left-most byte is non zero, or a slice that exclude any leading zero bytes.
   */
  default Bytes trimLeadingZeros() {
    int size = size();
    for (int i = 0; i < size; i++) {
      if (get(i) != 0) {
        return slice(i);
      }
    }
    return Bytes.EMPTY;
  }

  /**
   * Update the provided message digest with the bytes of this value.
   *
   * @param digest The digest to update.
   */
  default void update(MessageDigest digest) {
    checkNotNull(digest);
    for (int i = 0; i < size(); i++) {
      digest.update(get(i));
    }
  }

  /**
   * Extract the bytes of this value into a byte array.
   *
   * @return A byte array with the same content than this value.
   */
  default byte[] toArray() {
    int size = size();
    byte[] array = new byte[size];
    for (int i = 0; i < size; i++) {
      array[i] = get(i);
    }
    return array;
  }

  /**
   * Get the bytes represented by this value as byte array.
   *
   * <p>
   * Contrarily to {@link #toArray()}, this may avoid allocating a new array and directly return the backing array of
   * this value if said value is array backed and doing so is possible. As such, modifications to the returned array may
   * or may not impact this value. As such, this method should be used with care and hence the "unsafe" moniker.
   *
   * @return A byte array with the same content than this value, which may or may not be the direct backing of this
   *         value.
   */
  default byte[] toArrayUnsafe() {
    return toArray();
  }

  /**
   * Return the hexadecimal string representation of this value.
   *
   * @return The hexadecimal representation of this value, starting with "0x".
   */
  @Override
  String toString();

  /**
   * @return This value represented as hexadecimal, starting with "0x".
   */
  default String toHexString() {
    try {
      return appendHexTo(new StringBuilder("0x")).toString();
    } catch (IOException e) {
      // not thrown
      throw new RuntimeException(e);
    }
  }

  /** @return This value represented as a minimal hexadecimal string (without any leading zero). */
  default String toShortHexString() {
    StringBuilder hex;
    try {
      hex = appendHexTo(new StringBuilder());
    } catch (IOException e) {
      // not thrown
      throw new RuntimeException(e);
    }

    int i = 0;
    while (i < hex.length() && hex.charAt(i) == '0') {
      i++;
    }
    return "0x" + hex.substring(i);
  }
}
