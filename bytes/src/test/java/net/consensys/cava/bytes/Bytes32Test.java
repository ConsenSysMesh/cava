package net.consensys.cava.bytes;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;

class Bytes32Test {

  @Test
  void failsWhenWrappingArraySmallerThan32() {
    Throwable exception = assertThrows(IllegalArgumentException.class, () -> Bytes32.wrap(new byte[31]));
    assertEquals("Expected 32 bytes but got 31", exception.getMessage());
  }

  @Test
  void failsWhenWrappingArrayLargerThan32() {
    Throwable exception = assertThrows(IllegalArgumentException.class, () -> Bytes32.wrap(new byte[33]));
    assertEquals("Expected 32 bytes but got 33", exception.getMessage());
  }

  @Test
  void leftPadAValueToBytes32() {
    Bytes32 b32 = Bytes32.leftPad(Bytes.of(1, 2, 3));
    assertEquals(32, b32.size());
    for (int i = 0; i < 28; ++i) {
      assertEquals((byte) 0, b32.get(i));
    }
    assertEquals((byte) 1, b32.get(29));
    assertEquals((byte) 2, b32.get(30));
    assertEquals((byte) 3, b32.get(31));
  }

  @Test
  void failsWhenLeftPaddingValueLargerThan32() {
    Throwable exception = assertThrows(IllegalArgumentException.class, () -> Bytes32.leftPad(MutableBytes.create(33)));
    assertEquals("Expected at most 32 bytes but got 33", exception.getMessage());
  }
}
