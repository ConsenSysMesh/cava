/*
 * Copyright 2019 ConsenSys AG.
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

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

import net.consensys.cava.bytes.Bytes;
import net.consensys.cava.bytes.Bytes32;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

class MessageAuthenticationCodesTest {
  @BeforeAll
  static void checkAvailable() {
    assumeTrue(Sodium.isAvailable(), "Sodium native library is not available");
  }

  @Test
  void testHmacsha256() {
    Bytes32 secret = Bytes32.random();
    Bytes authenticator = MessageAuthenticationCodes.HMACSHA256.authenticate(Bytes.fromHexString("deadbeef"), secret);
    assertTrue(MessageAuthenticationCodes.HMACSHA256.verify(authenticator, Bytes.fromHexString("deadbeef"), secret));
  }

  @Test
  void testHmacsha256InvalidAuthenticator() {
    Bytes32 secret = Bytes32.random();
    Bytes authenticator = MessageAuthenticationCodes.HMACSHA256.authenticate(Bytes.fromHexString("deadbeef"), secret);
    assertThrows(
        IllegalArgumentException.class,
        () -> MessageAuthenticationCodes.HMACSHA256
            .verify(Bytes.concatenate(authenticator, Bytes.of(1, 2, 3)), Bytes.fromHexString("deadbeef"), secret));
  }

  @Test
  void testHmacsha512() {
    Bytes32 secret = Bytes32.random();
    Bytes authenticator = MessageAuthenticationCodes.HMACSHA512.authenticate(Bytes.fromHexString("deadbeef"), secret);
    assertTrue(MessageAuthenticationCodes.HMACSHA512.verify(authenticator, Bytes.fromHexString("deadbeef"), secret));
  }

  @Test
  void testHmacsha512InvalidSecretLength() {
    Bytes secret = Bytes.random(26);
    assertThrows(
        IllegalArgumentException.class,
        () -> MessageAuthenticationCodes.HMACSHA512.authenticate(Bytes.fromHexString("deadbeef"), secret));
  }

  @Test
  void testHmacsha512256() {
    Bytes32 secret = Bytes32.random();
    Bytes authenticator =
        MessageAuthenticationCodes.HMACSHA512256.authenticate(Bytes.fromHexString("deadbeef"), secret);
    assertTrue(MessageAuthenticationCodes.HMACSHA512256.verify(authenticator, Bytes.fromHexString("deadbeef"), secret));
  }

  @Test
  void testHmacsha512256NoMatch() {
    Bytes32 secret = Bytes32.random();
    Bytes authenticator =
        MessageAuthenticationCodes.HMACSHA512256.authenticate(Bytes.fromHexString("deadbeef"), secret);
    assertFalse(
        MessageAuthenticationCodes.HMACSHA512256
            .verify(authenticator.reverse(), Bytes.fromHexString("deadbeef"), secret));
  }
}
