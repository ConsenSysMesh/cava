/*
 * Copyright 2018, ConsenSys Inc.
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
