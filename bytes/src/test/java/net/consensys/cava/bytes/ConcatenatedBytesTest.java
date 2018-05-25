package net.consensys.cava.bytes;

import static net.consensys.cava.bytes.Bytes.fromHexString;
import static net.consensys.cava.bytes.Bytes.wrap;
import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class ConcatenatedBytesTest {

  @Test
  void shouldReadConcatenatedValue() {
    Bytes bytes = wrap(fromHexString("0x01234567"), fromHexString("0x89ABCDEF"));
    assertEquals(8, bytes.size());
    assertEquals("0x0123456789ABCDEF", bytes.toHexString());
  }

  @Test
  void shouldSliceConcatenatedValue() {
    Bytes bytes = wrap(
        fromHexString("0x01234567"),
        fromHexString("0x89ABCDEF"),
        fromHexString("0x01234567"),
        fromHexString("0x89ABCDEF"));
    assertEquals("0x", bytes.slice(4, 0).toHexString());
    assertEquals("0x0123456789ABCDEF0123456789ABCDEF", bytes.slice(0, 16).toHexString());
    assertEquals("0x01234567", bytes.slice(0, 4).toHexString());
    assertEquals("0x0123", bytes.slice(0, 2).toHexString());
    assertEquals("0x6789", bytes.slice(3, 2).toHexString());
    assertEquals("0x89ABCDEF", bytes.slice(4, 4).toHexString());
    assertEquals("0xABCD", bytes.slice(5, 2).toHexString());
    assertEquals("0xEF012345", bytes.slice(7, 4).toHexString());
    assertEquals("0x01234567", bytes.slice(8, 4).toHexString());
    assertEquals("0x456789ABCDEF", bytes.slice(10, 6).toHexString());
    assertEquals("0x89ABCDEF", bytes.slice(12, 4).toHexString());
  }

  @Test
  void shouldReadDeepConcatenatedValue() {
    Bytes bytes = wrap(
        wrap(fromHexString("0x01234567"), fromHexString("0x89ABCDEF")),
        wrap(fromHexString("0x01234567"), fromHexString("0x89ABCDEF")),
        fromHexString("0x01234567"),
        fromHexString("0x89ABCDEF"));
    assertEquals(24, bytes.size());
    assertEquals("0x0123456789ABCDEF0123456789ABCDEF0123456789ABCDEF", bytes.toHexString());
  }
}
