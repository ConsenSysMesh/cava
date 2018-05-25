package net.consensys.cava.units.bigints.test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;

import net.consensys.cava.units.ethereum.Wei;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;

class WeiTest {

  @Test
  void testReuseConstants() {
    List<Wei> oneTime = new ArrayList<>();
    for (int i = 0; i < 128; i++) {
      oneTime.add(Wei.valueOf((long) i));
    }
    List<Wei> secondTime = new ArrayList<>();
    for (int i = 0; i < 128; i++) {
      secondTime.add(Wei.valueOf((long) i));
    }
    for (int i = 0; i < 128; i++) {
      Wei first = oneTime.get(i);
      Wei second = secondTime.get(i);
      if (i <= 64) {
        assertSame(first, second);
      } else {
        assertNotSame(first, second);
        assertEquals(first, second);
      }
    }
  }

  @Test
  void testNegativeLong() {
    assertThrows(IllegalArgumentException.class, () -> {
      Wei.valueOf(-1L);
    });
  }

  @Test
  void testNegativeBigInteger() {
    assertThrows(IllegalArgumentException.class, () -> {
      Wei.valueOf(BigInteger.valueOf(-123L));
    });
  }
}
