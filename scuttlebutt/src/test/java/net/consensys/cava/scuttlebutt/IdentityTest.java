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
package net.consensys.cava.scuttlebutt;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import net.consensys.cava.bytes.Bytes;
import net.consensys.cava.crypto.sodium.Signer;

import org.junit.jupiter.api.Test;

class IdentityTest {

  @Test
  void testRandom() {
    Identity random1 = Identity.random();
    Identity random2 = Identity.random();
    assertNotEquals(random1, random2);
    assertNotEquals(random1.hashCode(), random2.hashCode());
  }

  @Test
  void testEquality() {
    Signer.KeyPair kp = Signer.KeyPair.random();
    Identity id1 = Identity.fromKeyPair(kp);
    Identity id2 = Identity.fromKeyPair(kp);
    assertEquals(id1, id2);
  }

  @Test
  void testHashCode() {
    Signer.KeyPair kp = Signer.KeyPair.random();
    Identity id1 = Identity.fromKeyPair(kp);
    Identity id2 = Identity.fromKeyPair(kp);
    assertEquals(id1.hashCode(), id2.hashCode());
  }

  @Test
  void testToString() throws Exception {
    Signer.KeyPair kp = Signer.KeyPair.random();
    Identity id = Identity.fromKeyPair(kp);
    System.out.println(id.toString());
    StringBuilder builder = new StringBuilder();
    builder.append("@");
    kp.publicKey().bytes().appendHexTo(builder);
    builder.append(".ed25519");
    assertEquals(builder.toString().toLowerCase(), id.toString());
  }

  @Test
  void signAndVerify() {
    Signer.KeyPair kp = Signer.KeyPair.fromSeed(
        Signer.Seed.fromBytes(Bytes.fromHexString("deadbeefdeadbeefdeadbeefdeadbeefdeadbeefdeadbeefdeadbeefdeadbeef")));
    Bytes message = Bytes.fromHexString("deadbeef");
    Identity id = Identity.fromKeyPair(kp);
    Bytes signature = id.sign(message);
    boolean verified = id.verify(signature, message);

    assertTrue(verified);
    assertFalse(id.verify(signature, Bytes.fromHexString("dea3beef")));
  }
}
