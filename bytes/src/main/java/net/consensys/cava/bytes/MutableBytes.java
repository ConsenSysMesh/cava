package net.consensys.cava.bytes;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkElementIndex;
import static com.google.common.base.Preconditions.checkNotNull;

import io.vertx.core.buffer.Buffer;

/**
 * A mutable {@link Bytes} value.
 */
public interface MutableBytes extends Bytes {

  /**
   * The empty value (with 0 bytes).
   */
  MutableBytes EMPTY = wrap(new byte[0]);

  /**
   * Create a new mutable byte value.
   *
   * @param size The size of the returned value.
   * @return A {@link MutableBytes} value.
   */
  static MutableBytes create(int size) {
    if (size == 32) {
      return MutableBytes32.create();
    }
    return new MutableArrayWrappingBytes(new byte[size]);
  }

  /**
   * Wrap a byte array in a {@link MutableBytes} value.
   *
   * @param value The value to wrap.
   * @return A {@link MutableBytes} value wrapping {@code value}.
   */
  static MutableBytes wrap(byte[] value) {
    checkNotNull(value);
    return new MutableArrayWrappingBytes(value);
  }

  /**
   * Wrap a slice of a byte array as a {@link MutableBytes} value.
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
  static MutableBytes wrap(byte[] value, int offset, int length) {
    checkNotNull(value);
    if (length == 32) {
      return new MutableArrayWrappingBytes32(value, offset);
    }
    return new MutableArrayWrappingBytes(value, offset, length);
  }

  /**
   * Wrap a full Vert.x {@link Buffer} as a {@link MutableBytes} value.
   *
   * <p>
   * Note that any change to the content of the buffer may be reflected in the returned value.
   *
   * @param buffer The buffer to wrap.
   * @return A {@link MutableBytes} value.
   */
  static MutableBytes wrapBuffer(Buffer buffer) {
    return wrapBuffer(buffer, 0, buffer.length());
  }

  /**
   * Wrap a slice of a Vert.x {@link Buffer} as a {@link MutableBytes} value.
   *
   * <p>
   * Note that any change to the content of the buffer may be reflected in the returned value, and any change to the
   * returned value will be reflected in the buffer.
   *
   * @param buffer The buffer to wrap.
   * @param offset The offset in {@code buffer} from which to expose the bytes in the returned value. That is,
   *        {@code wrapBuffer(buffer, i, 1).get(0) == buffer.getByte(i)}.
   * @param size The size of the returned value.
   * @return A {@link MutableBytes} value.
   */
  static MutableBytes wrapBuffer(Buffer buffer, int offset, int size) {
    checkNotNull(buffer);
    if (size == 0) {
      return EMPTY;
    }
    return new MutableBufferWrappingBytes(buffer, offset, size);
  }

  /**
   * Set a byte in this value.
   *
   * @param i The index of the byte to set.
   * @param b The value to set that byte to.
   * @throws IndexOutOfBoundsException if {@code i < 0} or {i &gt;= size()}.
   */
  void set(int i, byte b);

  /**
   * Set the 4 bytes starting at the specified index to the specified integer value.
   *
   * @param i The index, which must less than or equal to {@code size() - 4}.
   * @param value The integer value.
   * @throws IndexOutOfBoundsException if {@code i &lt; 0} or {i &gt;= size()}.
   * @throws IllegalArgumentException if {@code i &gt; size() - 4}.
   */
  default void setInt(int i, int value) {
    checkElementIndex(i, size());
    checkArgument(
        i <= size() - 4,
        "Value of size %s does not have enough bytes to write a 4 bytes int from index %s",
        size(),
        i);

    set(i++, (byte) (value >>> 24));
    set(i++, (byte) ((value >>> 16) & 0xFF));
    set(i++, (byte) ((value >>> 8) & 0xFF));
    set(i, (byte) (value & 0xFF));
  }

  /**
   * Set the 8 bytes starting at the specified index to the specified long value.
   *
   * @param i The index , which must less than or equal to {@code size() - 8}.
   * @param value The long value.
   * @throws IndexOutOfBoundsException if {@code i &lt; 0} or {i &gt;= size()}.
   * @throws IllegalArgumentException if {@code i &gt; size() - 8}.
   */
  default void setLong(int i, long value) {
    checkElementIndex(i, size());
    checkArgument(
        i <= size() - 8,
        "Value of size %s has not enough bytes to write a 8 bytes long from index %s",
        size(),
        i);

    set(i++, (byte) (value >>> 56));
    set(i++, (byte) ((value >>> 48) & 0xFF));
    set(i++, (byte) ((value >>> 40) & 0xFF));
    set(i++, (byte) ((value >>> 32) & 0xFF));
    set(i++, (byte) ((value >>> 24) & 0xFF));
    set(i++, (byte) ((value >>> 16) & 0xFF));
    set(i++, (byte) ((value >>> 8) & 0xFF));
    set(i, (byte) (value & 0xFF));
  }

  /**
   * Create a mutable slice of the bytes of this value.
   *
   * <p>
   * Note: the resulting slice is only a view over the original value. Holding a reference to the returned slice may
   * hold more memory than the slide represents. Use {@link #copy} on the returned slice to avoid this.
   *
   * @param i The start index for the slice.
   * @param length The length of the resulting value.
   * @return A new mutable view over the bytes of this value from index {@code i} (included) to index {@code i + length}
   *         (excluded).
   * @throws IllegalArgumentException if {@code length &lt; 0}.
   * @throws IndexOutOfBoundsException if {@code i &lt; 0} or {i &gt;= size()} or {i + length &gt; size()} .
   */
  MutableBytes mutableSlice(int i, int length);

  /**
   * Fill all the bytes of this value with the specified byte.
   *
   * @param b The byte to use to fill the value.
   */
  default void fill(byte b) {
    for (int i = 0; i < size(); i++) {
      set(i, b);
    }
  }

  /**
   * Set all bytes in this value to 0.
   */
  default void clear() {
    fill((byte) 0);
  }
}
