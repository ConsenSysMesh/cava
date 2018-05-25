package net.consensys.cava.crypto.sodium;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

import net.consensys.cava.bytes.Bytes;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

class KeyDerivationTest {

  @BeforeAll
  static void checkAvailable() {
    assumeTrue(Sodium.isAvailable());
  }

  @Test
  void differentIdsShouldGenerateDifferentKeys() {
    KeyDerivation.Key masterKey = KeyDerivation.Key.random();

    Bytes subKey1 = KeyDerivation.deriveKey(40, 1, "abcdefg", masterKey);
    assertEquals(subKey1, KeyDerivation.deriveKey(40, 1, "abcdefg", masterKey));

    assertNotEquals(subKey1, KeyDerivation.deriveKey(40, 2, "abcdefg", masterKey));
    assertNotEquals(subKey1, KeyDerivation.deriveKey(40, 1, new byte[KeyDerivation.contextLength()], masterKey));
  }
}
