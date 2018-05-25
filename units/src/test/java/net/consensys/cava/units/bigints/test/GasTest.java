package net.consensys.cava.units.bigints.test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;

import net.consensys.cava.units.ethereum.Gas;
import net.consensys.cava.units.ethereum.Wei;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;

class GasTest {

  @Test
  void testOverflowThroughAddition() {
    Gas max = Gas.valueOf(Long.MAX_VALUE);
    assertThrows(ArithmeticException.class, () -> {
      max.add(Gas.valueOf(1L));
    });
  }

  @Test
  void testOverflow() {
    assertThrows(IllegalArgumentException.class, () -> {
      Gas.valueOf(BigInteger.valueOf(Long.MAX_VALUE).add(BigInteger.ONE));
    });
  }

  @Test
  void testGetWeiPrice() {
    Gas gas = Gas.valueOf(5L);
    Wei result = gas.priceFor(Wei.valueOf(3L));
    assertEquals(15, result.intValue());
  }

  @Test
  void testReuseConstants() {
    List<Gas> oneTime = new ArrayList<>();
    for (int i = 0; i < 128; i++) {
      oneTime.add(Gas.valueOf((long) i));
    }
    List<Gas> secondTime = new ArrayList<>();
    for (int i = 0; i < 128; i++) {
      secondTime.add(Gas.valueOf((long) i));
    }
    for (int i = 0; i < 128; i++) {
      Gas first = oneTime.get(i);
      Gas second = secondTime.get(i);
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
      Gas.valueOf(-1L);
    });
  }

  @Test
  void testNegativeBigInteger() {
    assertThrows(IllegalArgumentException.class, () -> {
      Gas.valueOf(BigInteger.valueOf(-123L));
    });
  }

}
