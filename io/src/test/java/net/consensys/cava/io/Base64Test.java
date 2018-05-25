package net.consensys.cava.io;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;

import net.consensys.cava.bytes.Bytes;

import org.junit.jupiter.api.Test;

class Base64Test {

  @Test
  void shouldEncodeByteArray() {
    String s = Base64.encodeBytes(new byte[] {1, 2, 3, 4, 5, 6, 7, 8});
    assertEquals("AQIDBAUGBwg=", s);
  }

  @Test
  void shouldEncodeBytesValue() {
    String s = Base64.encode(Bytes.of(1, 2, 3, 4, 5, 6, 7, 8));
    assertEquals("AQIDBAUGBwg=", s);
  }

  @Test
  void shouldDecodeToByteArray() {
    byte[] bytes = Base64.decodeBytes("AQIDBAUGBwg=");
    assertArrayEquals(new byte[] {1, 2, 3, 4, 5, 6, 7, 8}, bytes);
  }

  @Test
  void shouldDecodeToBytesValue() {
    Bytes bytes = Base64.decode("AQIDBAUGBwg=");
    assertEquals(Bytes.of(1, 2, 3, 4, 5, 6, 7, 8), bytes);
  }
}
