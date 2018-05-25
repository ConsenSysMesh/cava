package net.consensys.cava.bytes;

import static net.consensys.cava.bytes.Bytes.concatenate;
import static net.consensys.cava.bytes.Bytes.fromHexString;
import static net.consensys.cava.bytes.Bytes.fromHexStringLenient;
import static net.consensys.cava.bytes.Bytes.minimalBytes;
import static net.consensys.cava.bytes.Bytes.of;
import static net.consensys.cava.bytes.Bytes.ofUnsignedShort;
import static net.consensys.cava.bytes.Bytes.wrap;
import static net.consensys.cava.bytes.Bytes.wrapBuffer;
import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.math.BigInteger;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;
import java.util.function.Function;

import io.vertx.core.buffer.Buffer;
import org.junit.jupiter.api.Test;

class BytesTest {

  private static Bytes h(String hex) {
    return fromHexString(hex);
  }

  private static Buffer b(String hex) {
    return Buffer.buffer(fromHexString(hex).toArrayUnsafe());
  }

  private static BigInteger bi(String decimal) {
    return new BigInteger(decimal);
  }

  @Test
  void testWrap() {
    Bytes wrap = wrap(new byte[0]);
    assertEquals(Bytes.EMPTY, wrap);

    testWrap(new byte[10]);
    testWrap(new byte[] {1});
    testWrap(new byte[] {1, 2, 3, 4});
    testWrap(new byte[] {-1, 127, -128});
  }

  private static void testWrap(byte[] bytes) {
    Bytes value = wrap(bytes);
    assertEquals(bytes.length, value.size());
    assertArrayEquals(value.toArray(), bytes);
  }

  @Test
  void testWrapNull() {
    assertThrows(NullPointerException.class, () -> wrap((byte[]) null));
  }

  /**
   * Checks that modifying a wrapped array modifies the value itself.
   */
  @Test
  void testWrapReflectsUpdates() {
    byte[] bytes = new byte[] {1, 2, 3, 4, 5};
    Bytes value = wrap(bytes);

    assertEquals(bytes.length, value.size());
    assertArrayEquals(value.toArray(), bytes);

    bytes[1] = 127;
    bytes[3] = 127;

    assertEquals(bytes.length, value.size());
    assertArrayEquals(value.toArray(), bytes);
  }

  @Test
  void testWrapSlice() {
    Bytes wrap = wrap(new byte[0], 0, 0);
    assertEquals(Bytes.EMPTY, wrap);
    assertEquals(Bytes.EMPTY, wrap(new byte[] {1, 2, 3}, 0, 0));
    assertEquals(Bytes.EMPTY, wrap(new byte[] {1, 2, 3}, 2, 0));

    testWrapSlice(new byte[] {1, 2, 3, 4}, 0, 4);
    testWrapSlice(new byte[] {1, 2, 3, 4}, 0, 2);
    testWrapSlice(new byte[] {1, 2, 3, 4}, 2, 1);
    testWrapSlice(new byte[] {1, 2, 3, 4}, 2, 2);
  }

  private static void testWrapSlice(byte[] bytes, int offset, int length) {
    Bytes value = wrap(bytes, offset, length);
    assertEquals(length, value.size());
    assertArrayEquals(value.toArray(), Arrays.copyOfRange(bytes, offset, offset + length));
  }

  @Test
  void testWrapSliceNull() {
    assertThrows(NullPointerException.class, () -> wrap(null, 0, 2));
  }

  @Test
  void testWrapSliceNegativeOffset() {
    assertThrows(IndexOutOfBoundsException.class, () -> testWrapSlice(new byte[] {1, 2, 3, 4}, -1, 4));
  }

  @Test
  void testWrapSliceOutOfBoundOffset() {
    assertThrows(IndexOutOfBoundsException.class, () -> testWrapSlice(new byte[] {1, 2, 3, 4}, 5, 1));
  }

  @Test
  void testWrapSliceNegativeLength() {
    Throwable exception =
        assertThrows(IllegalArgumentException.class, () -> testWrapSlice(new byte[] {1, 2, 3, 4}, 0, -2));
    assertEquals("Invalid negative length", exception.getMessage());
  }

  @Test
  void testWrapSliceTooBig() {
    Throwable exception =
        assertThrows(IllegalArgumentException.class, () -> testWrapSlice(new byte[] {1, 2, 3, 4}, 2, 3));
    assertEquals("Provided length 3 is too big: the value has only 2 bytes from offset 2", exception.getMessage());
  }

  /**
   * Checks that modifying a wrapped array modifies the value itself, but only if within the wrapped slice.
   */
  @Test
  void testWrapSliceReflectsUpdates() {
    byte[] bytes = new byte[] {1, 2, 3, 4, 5};
    testWrapSlice(bytes, 2, 2);
    bytes[2] = 127;
    bytes[3] = 127;
    testWrapSlice(bytes, 2, 2);

    Bytes wrapped = wrap(bytes, 2, 2);
    Bytes copy = wrapped.copy();

    // Modify the bytes outside of the wrapped slice and check this doesn't affect the value (that
    // it is still equal to the copy from before the updates)
    bytes[0] = 127;
    assertEquals(copy, wrapped);

    // Sanity check for copy(): modify within the wrapped slice and check the copy differs now.
    bytes[2] = 42;
    assertNotEquals(copy, wrapped);
  }

  @Test
  void testConcatenatedWrap() {
    testConcatenatedWrap(new byte[] {}, new byte[] {});
    testConcatenatedWrap(new byte[] {}, new byte[] {1, 2, 3});
    testConcatenatedWrap(new byte[] {1, 2, 3}, new byte[] {});
    testConcatenatedWrap(new byte[] {1, 2, 3}, new byte[] {4, 5});
  }

  private static void testConcatenatedWrap(byte[] first, byte[] second) {
    byte[] res = wrap(wrap(first), wrap(second)).toArray();
    assertArrayEquals(Arrays.copyOfRange(res, 0, first.length), first);
    assertArrayEquals(Arrays.copyOfRange(res, first.length, res.length), second);
  }

  @Test
  void testConcatenatedWrapReflectsUpdates() {
    byte[] first = new byte[] {1, 2, 3};
    byte[] second = new byte[] {4, 5};
    byte[] expected1 = new byte[] {1, 2, 3, 4, 5};
    Bytes res = wrap(wrap(first), wrap(second));
    assertArrayEquals(res.toArray(), expected1);

    first[1] = 42;
    second[0] = 42;
    byte[] expected2 = new byte[] {1, 42, 3, 42, 5};
    assertArrayEquals(res.toArray(), expected2);
  }

  @Test
  void testConcatenate() {
    assertEquals(h("0x"), concatenate(h("0x"), h("0x")));

    assertEquals(h("0x1234"), concatenate(h("0x1234"), h("0x")));
    assertEquals(h("0x1234"), concatenate(h("0x"), h("0x1234")));

    assertEquals(h("0x12345678"), concatenate(h("0x1234"), h("0x5678")));

    int valCount = 10;
    Bytes[] values = new Bytes[valCount];
    StringBuilder res = new StringBuilder();
    for (int i = 0; i < valCount; i++) {
      String hex = "1234";
      values[i] = h(hex);
      res.append(hex);
    }
    assertEquals(h(res.toString()), concatenate(values));
  }

  @Test
  void testWrapBuffer() {
    assertEquals(Bytes.EMPTY, wrapBuffer(Buffer.buffer()));

    testWrapBuffer(new byte[10]);
    testWrapBuffer(new byte[] {1});
    testWrapBuffer(new byte[] {1, 2, 3, 4});
    testWrapBuffer(new byte[] {-1, 127, -128});
  }

  private static void testWrapBuffer(byte[] bytes) {
    Buffer buffer = Buffer.buffer(bytes);
    Bytes value = wrapBuffer(buffer);
    assertEquals(buffer.length(), value.size());
    assertArrayEquals(value.toArray(), buffer.getBytes());
  }

  @Test
  void testWrapBufferNull() {
    assertThrows(NullPointerException.class, () -> wrapBuffer(null));
  }

  /**
   * Checks that modifying a wrapped buffer modifies the value itself.
   */
  @Test
  void testWrapBufferReflectsUpdates() {
    Buffer buffer = Buffer.buffer(new byte[] {1, 2, 3, 4, 5});
    Bytes value = wrapBuffer(buffer);

    assertEquals(buffer.length(), value.size());
    assertArrayEquals(value.toArray(), buffer.getBytes());

    buffer.setByte(1, (byte) 127);
    buffer.setByte(3, (byte) 127);

    assertEquals(buffer.length(), value.size());
    assertArrayEquals(value.toArray(), buffer.getBytes());
  }

  @Test
  void testWrapBufferSlice() {
    Bytes wrap = wrapBuffer(Buffer.buffer(new byte[0]), 0, 0);
    assertEquals(Bytes.EMPTY, wrap);
    assertEquals(Bytes.EMPTY, wrapBuffer(Buffer.buffer(new byte[] {1, 2, 3}), 0, 0));
    assertEquals(Bytes.EMPTY, wrapBuffer(Buffer.buffer(new byte[] {1, 2, 3}), 2, 0));

    testWrapBufferSlice(new byte[] {1, 2, 3, 4}, 0, 4);
    testWrapBufferSlice(new byte[] {1, 2, 3, 4}, 0, 2);
    testWrapBufferSlice(new byte[] {1, 2, 3, 4}, 2, 1);
    testWrapBufferSlice(new byte[] {1, 2, 3, 4}, 2, 2);
  }

  private static void testWrapBufferSlice(byte[] bytes, int offset, int length) {
    Buffer buffer = Buffer.buffer(bytes);
    Bytes value = wrapBuffer(buffer, offset, length);
    assertEquals(length, value.size());
    assertArrayEquals(value.toArray(), Arrays.copyOfRange(bytes, offset, offset + length));
  }

  @Test
  void testWrapBufferSliceNull() {
    assertThrows(NullPointerException.class, () -> wrapBuffer(null, 0, 2));
  }

  @Test
  void testWrapBufferSliceNegativeOffset() {
    assertThrows(IndexOutOfBoundsException.class, () -> testWrapBufferSlice(new byte[] {1, 2, 3, 4}, -1, 4));
  }

  @Test
  void testWrapBufferSliceOutOfBoundOffset() {
    assertThrows(IndexOutOfBoundsException.class, () -> testWrapBufferSlice(new byte[] {1, 2, 3, 4}, 5, 1));
  }

  @Test
  void testWrapBufferSliceNegativeLength() {
    Throwable exception =
        assertThrows(IllegalArgumentException.class, () -> testWrapBufferSlice(new byte[] {1, 2, 3, 4}, 0, -2));
    assertEquals("Invalid negative length", exception.getMessage());
  }

  @Test
  void testWrapBufferSliceTooBig() {
    Throwable exception =
        assertThrows(IllegalArgumentException.class, () -> testWrapBufferSlice(new byte[] {1, 2, 3, 4}, 2, 3));
    assertEquals(
        "Provided length 3 is too big: the buffer has size 4 and has only 2 bytes from 2",
        exception.getMessage());
  }

  /**
   * Checks that modifying a wrapped array modifies the value itself, but only if within the wrapped slice.
   */
  @Test
  void testWrapBufferSliceReflectsUpdates() {
    Buffer buffer = Buffer.buffer(new byte[] {1, 2, 3, 4, 5});
    Bytes value = wrapBuffer(buffer, 2, 2);
    Bytes copy = value.copy();

    assertEquals(2, value.size());
    assertArrayEquals(value.toArray(), Arrays.copyOfRange(buffer.getBytes(), 2, 4));

    // Modify within the wrapped slice. This should have modified the wrapped value but not its copy
    buffer.setByte(2, (byte) 127);
    buffer.setByte(3, (byte) 127);

    assertArrayEquals(value.toArray(), Arrays.copyOfRange(buffer.getBytes(), 2, 4));
    assertFalse(Arrays.equals(Arrays.copyOfRange(buffer.getBytes(), 2, 4), copy.toArray()));

    Bytes newCopy = value.copy();

    // Modify the bytes outside of the wrapped slice and check this doesn't affect the value (that
    // it is still equal to the copy from before the updates)
    buffer.setByte(0, (byte) 127);
    assertEquals(newCopy, value);
  }

  @Test
  void testOfBytes() {
    assertArrayEquals(of().toArray(), new byte[] {});
    assertArrayEquals(of((byte) 1, (byte) 2).toArray(), new byte[] {1, 2});
    assertArrayEquals(of((byte) 1, (byte) 2, (byte) 3, (byte) 4, (byte) 5).toArray(), new byte[] {1, 2, 3, 4, 5});

    assertArrayEquals(of((byte) -1, (byte) 2, (byte) -3).toArray(), new byte[] {-1, 2, -3});
  }

  @Test
  void testOfInts() {
    assertArrayEquals(of(1, 2).toArray(), new byte[] {1, 2});
    assertArrayEquals(of(1, 2, 3, 4, 5).toArray(), new byte[] {1, 2, 3, 4, 5});
    assertArrayEquals(of(0xff, 0x7f, 0x80).toArray(), new byte[] {-1, 127, -128});
  }

  @Test
  void testOfIntsTooBig() {
    Throwable exception = assertThrows(IllegalArgumentException.class, () -> of(2, 3, 256));
    assertEquals("3th value 256 does not fit a byte", exception.getMessage());
  }

  @Test
  void testOfIntsTooLow() {
    Throwable exception = assertThrows(IllegalArgumentException.class, () -> of(2, -1, 3));
    assertEquals("2th value -1 does not fit a byte", exception.getMessage());
  }

  @Test
  void testToMinimalBytes() {
    assertEquals(h("0x"), minimalBytes(0));

    assertEquals(h("0x01"), minimalBytes(1));
    assertEquals(h("0x04"), minimalBytes(4));
    assertEquals(h("0x10"), minimalBytes(16));
    assertEquals(h("0xFF"), minimalBytes(255));

    assertEquals(h("0x0100"), minimalBytes(256));
    assertEquals(h("0x0200"), minimalBytes(512));

    assertEquals(h("0x010000"), minimalBytes(1L << 16));
    assertEquals(h("0x01000000"), minimalBytes(1L << 24));
    assertEquals(h("0x0100000000"), minimalBytes(1L << 32));
    assertEquals(h("0x010000000000"), minimalBytes(1L << 40));
    assertEquals(h("0x01000000000000"), minimalBytes(1L << 48));
    assertEquals(h("0x0100000000000000"), minimalBytes(1L << 56));
    assertEquals(h("0xFFFFFFFFFFFFFFFF"), minimalBytes(-1L));
  }

  @Test
  void testOfUnsignedShort() {
    assertEquals(h("0x0000"), ofUnsignedShort(0));
    assertEquals(h("0x0001"), ofUnsignedShort(1));

    assertEquals(h("0x0100"), ofUnsignedShort(256));
    assertEquals(h("0xFFFF"), ofUnsignedShort(65535));
  }

  @Test
  void testOfUnsignedShortNegative() {
    Throwable exception = assertThrows(IllegalArgumentException.class, () -> ofUnsignedShort(-1));
    assertEquals(
        "Value -1 cannot be represented as an unsigned short (it is negative or too big)",
        exception.getMessage());
  }

  @Test
  void testOfUnsignedShortTooBig() {
    Throwable exception = assertThrows(IllegalArgumentException.class, () -> ofUnsignedShort(65536));
    assertEquals(
        "Value 65536 cannot be represented as an unsigned short (it is negative or too big)",
        exception.getMessage());
  }

  @Test
  void testAsUnsignedBigInteger() {
    assertEquals(bi("0"), Bytes.EMPTY.unsignedBigIntegerValue());
    assertEquals(bi("1"), Bytes.of(1).unsignedBigIntegerValue());

    // Make sure things are interpreted unsigned.
    assertEquals(bi("255"), h("0xFF").unsignedBigIntegerValue());

    // Try 2^100 + Long.MAX_VALUE, as an easy to define a big not too special big integer.
    BigInteger expected = BigInteger.valueOf(2).pow(100).add(BigInteger.valueOf(Long.MAX_VALUE));

    // 2^100 is a one followed by 100 zeros, that's 12 bytes of zeros (=96) plus 4 more zeros (so
    // 0x10 == 16).
    MutableBytes v = MutableBytes.create(13);
    v.set(0, (byte) 16);
    v.setLong(v.size() - 8, Long.MAX_VALUE);
    assertEquals(expected, v.unsignedBigIntegerValue());
  }

  @Test
  void testAsSignedBigInteger() {
    assertEquals(bi("0"), Bytes.EMPTY.bigIntegerValue());
    assertEquals(bi("1"), Bytes.of(1).bigIntegerValue());

    // Make sure things are interpreted signed.
    assertEquals(bi("-1"), h("0xFF").bigIntegerValue());

    // Try 2^100 + Long.MAX_VALUE, as an easy to define a big but not too special big integer.
    BigInteger expected = BigInteger.valueOf(2).pow(100).add(BigInteger.valueOf(Long.MAX_VALUE));

    // 2^100 is a one followed by 100 zeros, that's 12 bytes of zeros (=96) plus 4 more zeros (so
    // 0x10 == 16).
    MutableBytes v = MutableBytes.create(13);
    v.set(0, (byte) 16);
    v.setLong(v.size() - 8, Long.MAX_VALUE);
    assertEquals(expected, v.bigIntegerValue());

    // And for a large negative one, we use -(2^100 + Long.MAX_VALUE), which is:
    //  2^100 + Long.MAX_VALUE = 0x10(4 bytes of 0)7F(  7 bytes of 1)
    //                 inverse = 0xEF(4 bytes of 1)80(  7 bytes of 0)
    //                      +1 = 0xEF(4 bytes of 1)80(6 bytes of 0)01
    expected = expected.negate();
    v = MutableBytes.create(13);
    v.set(0, (byte) 0xEF);
    for (int i = 1; i < 5; i++) {
      v.set(i, (byte) 0xFF);
    }
    v.set(5, (byte) 0x80);
    // 6 bytes of 0
    v.set(12, (byte) 1);
    assertEquals(expected, v.bigIntegerValue());
  }

  @Test
  void testFromHexStringLenient() {
    assertEquals(of(), fromHexStringLenient(""));
    assertEquals(of(), fromHexStringLenient("0x"));

    assertEquals(of(0), fromHexStringLenient("0"));
    assertEquals(of(0), fromHexStringLenient("0x0"));
    assertEquals(of(0), fromHexStringLenient("00"));
    assertEquals(of(0), fromHexStringLenient("0x00"));
    assertEquals(of(1), fromHexStringLenient("0x1"));
    assertEquals(of(1), fromHexStringLenient("0x01"));

    assertEquals(of(0x01, 0xff, 0x2a), fromHexStringLenient("1FF2A"));
    assertEquals(of(0x01, 0xff, 0x2a), fromHexStringLenient("0x1FF2A"));
    assertEquals(of(0x01, 0xff, 0x2a), fromHexStringLenient("0x1ff2a"));
    assertEquals(of(0x01, 0xff, 0x2a), fromHexStringLenient("0x1fF2a"));
    assertEquals(of(0x01, 0xff, 0x2a), fromHexStringLenient("01FF2A"));
    assertEquals(of(0x01, 0xff, 0x2a), fromHexStringLenient("0x01FF2A"));
    assertEquals(of(0x01, 0xff, 0x2a), fromHexStringLenient("0x01ff2A"));
  }

  @Test
  void testFromHexStringLenientInvalidInput() {
    Throwable exception = assertThrows(IllegalArgumentException.class, () -> fromHexStringLenient("foo"));
    assertEquals("Illegal character 'o' found at index 1 in hex binary representation 'foo'", exception.getMessage());
  }

  @Test
  void testFromHexStringLenientLeftPadding() {
    assertEquals(of(), fromHexStringLenient("", 0));
    assertEquals(of(0), fromHexStringLenient("", 1));
    assertEquals(of(0, 0), fromHexStringLenient("", 2));
    assertEquals(of(0, 0), fromHexStringLenient("0x", 2));

    assertEquals(of(0, 0, 0), fromHexStringLenient("0", 3));
    assertEquals(of(0, 0, 0), fromHexStringLenient("0x0", 3));
    assertEquals(of(0, 0, 0), fromHexStringLenient("00", 3));
    assertEquals(of(0, 0, 0), fromHexStringLenient("0x00", 3));
    assertEquals(of(0, 0, 1), fromHexStringLenient("0x1", 3));
    assertEquals(of(0, 0, 1), fromHexStringLenient("0x01", 3));

    assertEquals(of(0x01, 0xff, 0x2a), fromHexStringLenient("1FF2A", 3));
    assertEquals(of(0x00, 0x01, 0xff, 0x2a), fromHexStringLenient("0x1FF2A", 4));
    assertEquals(of(0x00, 0x00, 0x01, 0xff, 0x2a), fromHexStringLenient("0x1ff2a", 5));
    assertEquals(of(0x00, 0x01, 0xff, 0x2a), fromHexStringLenient("0x1fF2a", 4));
    assertEquals(of(0x00, 0x01, 0xff, 0x2a), fromHexStringLenient("01FF2A", 4));
    assertEquals(of(0x01, 0xff, 0x2a), fromHexStringLenient("0x01FF2A", 3));
    assertEquals(of(0x01, 0xff, 0x2a), fromHexStringLenient("0x01ff2A", 3));
  }

  @Test
  void testFromHexStringLenientLeftPaddingInvalidInput() {
    Throwable exception = assertThrows(IllegalArgumentException.class, () -> fromHexStringLenient("foo", 10));
    assertEquals("Illegal character 'o' found at index 1 in hex binary representation 'foo'", exception.getMessage());
  }

  @Test
  void testFromHexStringLenientLeftPaddingInvalidSize() {
    Throwable exception = assertThrows(IllegalArgumentException.class, () -> fromHexStringLenient("0x001F34", 2));
    assertEquals("Hex value 0x001F34 is too big: expected at most 2 bytes but got 3", exception.getMessage());
  }

  @Test
  void testFromHexString() {
    assertEquals(of(), fromHexString("0x"));

    assertEquals(of(0), fromHexString("00"));
    assertEquals(of(0), fromHexString("0x00"));
    assertEquals(of(1), fromHexString("0x01"));

    assertEquals(of(1, 0xff, 0x2a), fromHexString("01FF2A"));
    assertEquals(of(1, 0xff, 0x2a), fromHexString("0x01FF2A"));
    assertEquals(of(1, 0xff, 0x2a), fromHexString("0x01ff2a"));
    assertEquals(of(1, 0xff, 0x2a), fromHexString("0x01fF2a"));
  }

  @Test
  void testFromHexStringInvalidInput() {
    Throwable exception = assertThrows(IllegalArgumentException.class, () -> fromHexString("fooo"));
    assertEquals("Illegal character 'o' found at index 1 in hex binary representation 'fooo'", exception.getMessage());
  }

  @Test
  void testFromHexStringNotLenient() {
    Throwable exception = assertThrows(IllegalArgumentException.class, () -> fromHexString("0x100"));
    assertEquals("Invalid odd-length hex binary representation '0x100'", exception.getMessage());
  }

  @Test
  void testFromHexStringLeftPadding() {
    assertEquals(of(), fromHexString("0x", 0));
    assertEquals(of(0, 0), fromHexString("0x", 2));
    assertEquals(of(0, 0, 0, 0), fromHexString("0x", 4));

    assertEquals(of(0, 0), fromHexString("00", 2));
    assertEquals(of(0, 0), fromHexString("0x00", 2));
    assertEquals(of(0, 0, 1), fromHexString("0x01", 3));

    assertEquals(of(0x00, 0x01, 0xff, 0x2a), fromHexString("01FF2A", 4));
    assertEquals(of(0x01, 0xff, 0x2a), fromHexString("0x01FF2A", 3));
    assertEquals(of(0x00, 0x00, 0x01, 0xff, 0x2a), fromHexString("0x01ff2a", 5));
    assertEquals(of(0x00, 0x00, 0x01, 0xff, 0x2a), fromHexString("0x01fF2a", 5));
  }

  @Test
  void testFromHexStringLeftPaddingInvalidInput() {
    Throwable exception = assertThrows(IllegalArgumentException.class, () -> fromHexString("fooo", 4));
    assertEquals("Illegal character 'o' found at index 1 in hex binary representation 'fooo'", exception.getMessage());
  }

  @Test
  void testFromHexStringLeftPaddingNotLenient() {
    Throwable exception = assertThrows(IllegalArgumentException.class, () -> fromHexString("0x100", 4));
    assertEquals("Invalid odd-length hex binary representation '0x100'", exception.getMessage());
  }

  @Test
  void testFromHexStringLeftPaddingInvalidSize() {
    Throwable exception = assertThrows(IllegalArgumentException.class, () -> fromHexStringLenient("0x001F34", 2));
    assertEquals("Hex value 0x001F34 is too big: expected at most 2 bytes but got 3", exception.getMessage());
  }

  @Test
  void testSize() {
    assertEquals(0, wrap(new byte[0]).size());
    assertEquals(1, wrap(new byte[1]).size());
    assertEquals(10, wrap(new byte[10]).size());
  }

  @Test
  void testGet() {
    Bytes v = wrap(new byte[] {1, 2, 3, 4});
    assertEquals((int) (byte) 1, (int) v.get(0));
    assertEquals((int) (byte) 2, (int) v.get(1));
    assertEquals((int) (byte) 3, (int) v.get(2));
    assertEquals((int) (byte) 4, (int) v.get(3));
  }

  @Test
  void testGetNegativeIndex() {
    assertThrows(IndexOutOfBoundsException.class, () -> wrap(new byte[] {1, 2, 3, 4}).get(-1));
  }

  @Test
  void testGetOutOfBound() {
    assertThrows(IndexOutOfBoundsException.class, () -> wrap(new byte[] {1, 2, 3, 4}).get(4));
  }

  @Test
  void testGetInt() {
    Bytes value = wrap(new byte[] {0, 0, 1, 0, -1, -1, -1, -1});

    // 0x00000100 = 256
    assertEquals(256, value.getInt(0));
    // 0x000100FF = 65536 + 255 = 65791
    assertEquals(65791, value.getInt(1));
    // 0x0100FFFF = 16777216 (2^24) + (65536 - 1) = 16842751
    assertEquals(16842751, value.getInt(2));
    // 0xFFFFFFFF = -1
    assertEquals(-1, value.getInt(4));
  }

  @Test
  void testGetIntNegativeIndex() {
    assertThrows(IndexOutOfBoundsException.class, () -> wrap(new byte[] {1, 2, 3, 4}).getInt(-1));
  }

  @Test
  void testGetIntOutOfBound() {
    assertThrows(IndexOutOfBoundsException.class, () -> wrap(new byte[] {1, 2, 3, 4}).getInt(4));
  }

  @Test
  void testGetIntNotEnoughBytes() {
    Throwable exception = assertThrows(IllegalArgumentException.class, () -> wrap(new byte[] {1, 2, 3, 4}).getInt(1));
    assertEquals("Value of size 4 has not enough bytes to read a 4 bytes int from index 1", exception.getMessage());
  }

  @Test
  void testAsInt() {
    assertEquals(0, Bytes.EMPTY.intValue());
    Bytes value1 = wrap(new byte[] {0, 0, 1, 0});
    // 0x00000100 = 256
    assertEquals(256, value1.intValue());
    assertEquals(256, value1.slice(2).intValue());

    Bytes value2 = wrap(new byte[] {0, 1, 0, -1});
    // 0x000100FF = 65536 + 255 = 65791
    assertEquals(65791, value2.intValue());
    assertEquals(65791, value2.slice(1).intValue());

    Bytes value3 = wrap(new byte[] {1, 0, -1, -1});
    // 0x0100FFFF = 16777216 (2^24) + (65536 - 1) = 16842751
    assertEquals(16842751, value3.intValue());

    Bytes value4 = wrap(new byte[] {-1, -1, -1, -1});
    // 0xFFFFFFFF = -1
    assertEquals(-1, value4.intValue());
  }

  @Test
  void testAsIntTooManyBytes() {
    Throwable exception =
        assertThrows(IllegalArgumentException.class, () -> wrap(new byte[] {1, 2, 3, 4, 5}).intValue());
    assertEquals("Value of size 5 has more than 4 bytes", exception.getMessage());
  }

  @Test
  void testGetLong() {
    Bytes value1 = wrap(new byte[] {0, 0, 1, 0, -1, -1, -1, -1, 0, 0});
    // 0x00000100FFFFFFFF = (2^40) + (2^32) - 1 = 1103806595071
    assertEquals(1103806595071L, value1.getLong(0));
    // 0x 000100FFFFFFFF00 = (2^48) + (2^40) - 1 - 255 = 282574488338176
    assertEquals(282574488338176L, value1.getLong(1));

    Bytes value2 = wrap(new byte[] {-1, -1, -1, -1, -1, -1, -1, -1});
    assertEquals(-1L, value2.getLong(0));
  }

  @Test
  void testGetLongNegativeIndex() {
    assertThrows(IndexOutOfBoundsException.class, () -> wrap(new byte[] {1, 2, 3, 4, 5, 6, 7, 8}).getLong(-1));
  }

  @Test
  void testGetLongOutOfBound() {
    assertThrows(IndexOutOfBoundsException.class, () -> wrap(new byte[] {1, 2, 3, 4, 5, 6, 7, 8}).getLong(8));
  }

  @Test
  void testGetLongNotEnoughBytes() {
    Throwable exception = assertThrows(IllegalArgumentException.class, () -> wrap(new byte[] {1, 2, 3, 4}).getLong(0));
    assertEquals("Value of size 4 has not enough bytes to read a 8 bytes long from index 0", exception.getMessage());
  }

  @Test
  void testAsLong() {
    assertEquals(0, Bytes.EMPTY.longValue());
    Bytes value1 = wrap(new byte[] {0, 0, 1, 0, -1, -1, -1, -1});
    // 0x00000100FFFFFFFF = (2^40) + (2^32) - 1 = 1103806595071
    assertEquals(1103806595071L, value1.longValue());
    assertEquals(1103806595071L, value1.slice(2).longValue());
    Bytes value2 = wrap(new byte[] {0, 1, 0, -1, -1, -1, -1, 0});
    // 0x000100FFFFFFFF00 = (2^48) + (2^40) - 1 - 255 = 282574488338176
    assertEquals(282574488338176L, value2.longValue());
    assertEquals(282574488338176L, value2.slice(1).longValue());

    Bytes value3 = wrap(new byte[] {-1, -1, -1, -1, -1, -1, -1, -1});
    assertEquals(-1L, value3.longValue());
  }

  @Test
  void testAsLongTooManyBytes() {
    Throwable exception =
        assertThrows(IllegalArgumentException.class, () -> wrap(new byte[] {1, 2, 3, 4, 5, 6, 7, 8, 9}).longValue());
    assertEquals("Value of size 9 has more than 8 bytes", exception.getMessage());
  }

  @Test
  void testSlice() {
    assertEquals(h("0x"), h("0x0123456789").slice(0, 0));
    assertEquals(h("0x"), h("0x0123456789").slice(2, 0));
    assertEquals(h("0x01"), h("0x0123456789").slice(0, 1));
    assertEquals(h("0x0123"), h("0x0123456789").slice(0, 2));

    assertEquals(h("0x4567"), h("0x0123456789").slice(2, 2));
    assertEquals(h("0x23456789"), h("0x0123456789").slice(1, 4));
  }

  @Test
  void testSliceNegativeOffset() {
    assertThrows(IndexOutOfBoundsException.class, () -> h("0x012345").slice(-1, 2));
  }

  @Test
  void testSliceOffsetOutOfBound() {
    assertThrows(IndexOutOfBoundsException.class, () -> h("0x012345").slice(3, 2));
  }

  @Test
  void testSliceTooLong() {
    Throwable exception = assertThrows(IllegalArgumentException.class, () -> h("0x012345").slice(1, 3));
    assertEquals(
        "Provided length 3 is too big: the value has size 3 and has only 2 bytes from 1",
        exception.getMessage());
  }

  @Test
  void testMutableCopy() {
    Bytes v = h("0x012345");
    MutableBytes mutableCopy = v.mutableCopy();

    // Initially, copy must be equal.
    assertEquals(mutableCopy, v);

    // Upon modification, original should not have been modified.
    mutableCopy.set(0, (byte) -1);
    assertNotEquals(mutableCopy, v);
    assertEquals(h("0x012345"), v);
    assertEquals(h("0xFF2345"), mutableCopy);
  }

  @Test
  void testCopyTo() {
    MutableBytes dest;

    // The follow does nothing, but simply making sure it doesn't throw.
    dest = MutableBytes.EMPTY;
    Bytes.EMPTY.copyTo(dest);
    assertEquals(Bytes.EMPTY, dest);

    dest = MutableBytes.create(1);
    of(1).copyTo(dest);
    assertEquals(h("0x01"), dest);

    dest = MutableBytes.create(1);
    of(10).copyTo(dest);
    assertEquals(h("0x0A"), dest);

    dest = MutableBytes.create(2);
    of(0xff, 0x03).copyTo(dest);
    assertEquals(h("0xFF03"), dest);

    dest = MutableBytes.create(4);
    of(0xff, 0x03).copyTo(dest.mutableSlice(1, 2));
    assertEquals(h("0x00FF0300"), dest);
  }

  @Test
  void testCopyToTooSmall() {
    Throwable exception =
        assertThrows(IllegalArgumentException.class, () -> of(1, 2, 3).copyTo(MutableBytes.create(2)));
    assertEquals("Cannot copy 3 bytes to destination of non-equal size 2", exception.getMessage());
  }

  @Test
  void testCopyToTooBig() {
    Throwable exception =
        assertThrows(IllegalArgumentException.class, () -> of(1, 2, 3).copyTo(MutableBytes.create(4)));
    assertEquals("Cannot copy 3 bytes to destination of non-equal size 4", exception.getMessage());
  }

  @Test
  void testCopyToWithOffset() {
    MutableBytes dest;

    dest = MutableBytes.wrap(new byte[] {1, 2, 3});
    Bytes.EMPTY.copyTo(dest, 0);
    assertEquals(h("0x010203"), dest);

    dest = MutableBytes.wrap(new byte[] {1, 2, 3});
    of(1).copyTo(dest, 1);
    assertEquals(h("0x010103"), dest);

    dest = MutableBytes.wrap(new byte[] {1, 2, 3});
    of(2).copyTo(dest, 0);
    assertEquals(h("0x020203"), dest);

    dest = MutableBytes.wrap(new byte[] {1, 2, 3});
    of(1, 1).copyTo(dest, 1);
    assertEquals(h("0x010101"), dest);

    dest = MutableBytes.create(4);
    of(0xff, 0x03).copyTo(dest, 1);
    assertEquals(h("0x00FF0300"), dest);
  }

  @Test
  void testCopyToWithOffsetTooSmall() {
    Throwable exception =
        assertThrows(IllegalArgumentException.class, () -> of(1, 2, 3).copyTo(MutableBytes.create(4), 2));
    assertEquals("Cannot copy 3 bytes, destination has only 2 bytes from index 2", exception.getMessage());
  }

  @Test
  void testCopyToWithNegativeOffset() {
    assertThrows(IndexOutOfBoundsException.class, () -> of(1, 2, 3).copyTo(MutableBytes.create(10), -1));
  }

  @Test
  void testCopyToWithOutOfBoundIndex() {
    assertThrows(IndexOutOfBoundsException.class, () -> of(1, 2, 3).copyTo(MutableBytes.create(10), 10));
  }

  @Test
  void testAppendTo() {
    testAppendTo(Bytes.EMPTY, Buffer.buffer(), Bytes.EMPTY);
    testAppendTo(Bytes.EMPTY, b("0x1234"), h("0x1234"));
    testAppendTo(h("0x1234"), Buffer.buffer(), h("0x1234"));
    testAppendTo(h("0x5678"), b("0x1234"), h("0x12345678"));
  }

  private void testAppendTo(Bytes toAppend, Buffer buffer, Bytes expected) {
    toAppend.appendTo(buffer);
    assertEquals(expected, Bytes.wrap(buffer.getBytes()));
  }

  @Test
  void testIsZero() {
    assertTrue(Bytes.EMPTY.isZero());
    assertTrue(Bytes.of(0).isZero());
    assertTrue(Bytes.of(0, 0, 0).isZero());

    assertFalse(Bytes.of(1).isZero());
    assertFalse(Bytes.of(1, 0, 0).isZero());
    assertFalse(Bytes.of(0, 0, 1).isZero());
    assertFalse(Bytes.of(0, 0, 1, 0, 0).isZero());
  }

  @Test
  void testIsEmpty() {
    assertTrue(Bytes.EMPTY.isEmpty());

    assertFalse(Bytes.of(0).isEmpty());
    assertFalse(Bytes.of(0, 0, 0).isEmpty());
    assertFalse(Bytes.of(1).isEmpty());
  }

  @Test
  void findsCommonPrefix() {
    Bytes v = Bytes.of(1, 2, 3, 4, 5, 6, 7);
    Bytes o = Bytes.of(1, 2, 3, 4, 4, 3, 2);
    assertEquals(4, v.commonPrefixLength(o));
    assertEquals(Bytes.of(1, 2, 3, 4), v.commonPrefix(o));
  }

  @Test
  void findsCommonPrefixOfShorter() {
    Bytes v = Bytes.of(1, 2, 3, 4, 5, 6, 7);
    Bytes o = Bytes.of(1, 2, 3, 4);
    assertEquals(4, v.commonPrefixLength(o));
    assertEquals(Bytes.of(1, 2, 3, 4), v.commonPrefix(o));
  }

  @Test
  void findsCommonPrefixOfLonger() {
    Bytes v = Bytes.of(1, 2, 3, 4);
    Bytes o = Bytes.of(1, 2, 3, 4, 4, 3, 2);
    assertEquals(4, v.commonPrefixLength(o));
    assertEquals(Bytes.of(1, 2, 3, 4), v.commonPrefix(o));
  }

  @Test
  void findsCommonPrefixOfSliced() {
    Bytes v = Bytes.of(1, 2, 3, 4).slice(2, 2);
    Bytes o = Bytes.of(3, 4, 3, 3, 2).slice(3, 2);
    assertEquals(1, v.commonPrefixLength(o));
    assertEquals(Bytes.of(3), v.commonPrefix(o));
  }

  @Test
  void testTrimLeadingZeroes() {
    assertEquals(h("0x"), h("0x").trimLeadingZeros());
    assertEquals(h("0x"), h("0x00").trimLeadingZeros());
    assertEquals(h("0x"), h("0x00000000").trimLeadingZeros());

    assertEquals(h("0x01"), h("0x01").trimLeadingZeros());
    assertEquals(h("0x01"), h("0x00000001").trimLeadingZeros());

    assertEquals(h("0x3010"), h("0x3010").trimLeadingZeros());
    assertEquals(h("0x3010"), h("0x00003010").trimLeadingZeros());

    assertEquals(h("0xFFFFFFFF"), h("0xFFFFFFFF").trimLeadingZeros());
    assertEquals(h("0xFFFFFFFF"), h("0x000000000000FFFFFFFF").trimLeadingZeros());
  }

  @Test
  void slideToEnd() {
    assertEquals(Bytes.of(1, 2, 3, 4), Bytes.of(1, 2, 3, 4).slice(0));
    assertEquals(Bytes.of(2, 3, 4), Bytes.of(1, 2, 3, 4).slice(1));
    assertEquals(Bytes.of(3, 4), Bytes.of(1, 2, 3, 4).slice(2));
    assertEquals(Bytes.of(4), Bytes.of(1, 2, 3, 4).slice(3));
  }

  @Test
  void slicePastEndReturnsEmpty() {
    assertEquals(Bytes.EMPTY, Bytes.of(1, 2, 3, 4).slice(4));
    assertEquals(Bytes.EMPTY, Bytes.of(1, 2, 3, 4).slice(5));
  }

  @Test
  void testUpdate() throws NoSuchAlgorithmException {
    // Digest the same byte array in 4 ways:
    //  1) directly from the array
    //  2) after wrapped using the update() method
    //  3) after wrapped and copied using the update() method
    //  4) after wrapped but getting the byte manually
    // and check all compute the same digest.
    MessageDigest md1 = MessageDigest.getInstance("SHA-1");
    MessageDigest md2 = MessageDigest.getInstance("SHA-1");
    MessageDigest md3 = MessageDigest.getInstance("SHA-1");
    MessageDigest md4 = MessageDigest.getInstance("SHA-1");

    byte[] toDigest = new BigInteger("12324029423415041783577517238472017314").toByteArray();
    Bytes wrapped = wrap(toDigest);

    byte[] digest1 = md1.digest(toDigest);

    wrapped.update(md2);
    byte[] digest2 = md2.digest();

    wrapped.copy().update(md3);
    byte[] digest3 = md3.digest();

    for (int i = 0; i < wrapped.size(); i++)
      md4.update(wrapped.get(i));
    byte[] digest4 = md4.digest();

    assertArrayEquals(digest2, digest1);
    assertArrayEquals(digest3, digest1);
    assertArrayEquals(digest4, digest1);
  }

  @Test
  void testArrayExtraction() {
    // extractArray() and getArrayUnsafe() have essentially the same contract...
    testArrayExtraction(Bytes::toArray);
    testArrayExtraction(Bytes::toArrayUnsafe);

    // But on top of the basic, extractArray() guarantees modifying the returned array is safe from
    // impacting the original value (not that getArrayUnsafe makes no guarantees here one way or
    // another, so there is nothing to test).
    byte[] orig = new byte[] {1, 2, 3, 4};
    Bytes value = wrap(orig);
    byte[] extracted = value.toArray();
    assertArrayEquals(orig, extracted);
    Arrays.fill(extracted, (byte) -1);
    assertArrayEquals(extracted, new byte[] {-1, -1, -1, -1});
    assertArrayEquals(orig, new byte[] {1, 2, 3, 4});
    assertEquals(of(1, 2, 3, 4), value);
  }

  private void testArrayExtraction(Function<Bytes, byte[]> extractor) {
    byte[] bytes = new byte[0];
    assertArrayEquals(extractor.apply(Bytes.EMPTY), bytes);

    byte[][] toTest = new byte[][] {new byte[] {1}, new byte[] {1, 2, 3, 4, 5, 6}, new byte[] {-1, -1, 0, -1}};
    for (byte[] array : toTest) {
      assertArrayEquals(extractor.apply(wrap(array)), array);
    }

    // Test slightly more complex interactions
    assertArrayEquals(extractor.apply(wrap(new byte[] {1, 2, 3, 4, 5}).slice(2, 2)), new byte[] {3, 4});
    assertArrayEquals(extractor.apply(wrap(new byte[] {1, 2, 3, 4, 5}).slice(2, 0)), new byte[] {});
  }

  @Test
  void testToString() {
    assertEquals("0x", Bytes.EMPTY.toString());

    assertEquals("0x01", of(1).toString());
    assertEquals("0x0AFF03", of(0x0a, 0xff, 0x03).toString());
  }

  @Test
  void testHasLeadingZeroByte() {
    assertFalse(Bytes.fromHexString("0x").hasLeadingZeroByte());
    assertTrue(Bytes.fromHexString("0x0012").hasLeadingZeroByte());
    assertFalse(Bytes.fromHexString("0x120012").hasLeadingZeroByte());
  }

  @Test
  void testHasLeadingZeroBit() {
    assertFalse(Bytes.fromHexString("0x").hasLeadingZero());
    assertTrue(Bytes.fromHexString("0x01").hasLeadingZero());
    assertFalse(Bytes.fromHexString("0xFF0012").hasLeadingZero());
  }
}
