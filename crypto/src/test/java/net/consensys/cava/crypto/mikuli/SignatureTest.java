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
package net.consensys.cava.crypto.mikuli;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import net.consensys.cava.bytes.Bytes;

import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;


class SignatureTest {

  @Test
  void testSimpleSignature() {
    KeyPair keyPair = KeyPair.random();
    byte[] message = "Hello".getBytes(UTF_8);
    SignatureAndPublicKey sigAndPubKey = BLS12381.sign(keyPair, message);

    Boolean isValid = BLS12381.verify(sigAndPubKey.publicKey(), sigAndPubKey.signature(), message);
    assertTrue(isValid);
  }

  @Test
  void testAggregatedSignature() {
    byte[] message = "Hello".getBytes(UTF_8);
    List<SignatureAndPublicKey> sigs = getSignaturesAndPublicKeys(message);
    SignatureAndPublicKey sigAndPubKey = SignatureAndPublicKey.aggregate(sigs);

    Boolean isValid = BLS12381.verify(sigAndPubKey, message);
    assertTrue(isValid);
  }

  @Test
  void testCorruptedMessage() {
    byte[] message = "Hello".getBytes(UTF_8);
    List<SignatureAndPublicKey> sigs = getSignaturesAndPublicKeys(message);
    SignatureAndPublicKey sigAndPubKey = SignatureAndPublicKey.aggregate(sigs);
    byte[] corruptedMessage = "Not Hello".getBytes(UTF_8);

    Boolean isValid = BLS12381.verify(sigAndPubKey, corruptedMessage);
    assertFalse(isValid);
  }

  @Test
  void testCorruptedSignature() {
    byte[] message = "Hello".getBytes(UTF_8);
    List<SignatureAndPublicKey> sigs = getSignaturesAndPublicKeys(message);
    KeyPair keyPair = KeyPair.random();
    byte[] notHello = "Not Hello".getBytes(UTF_8);

    SignatureAndPublicKey additionalSignature = BLS12381.sign(keyPair, notHello);
    sigs.add(additionalSignature);

    SignatureAndPublicKey sigAndPubKey = SignatureAndPublicKey.aggregate(sigs);

    Boolean isValid = BLS12381.verify(sigAndPubKey, message);
    assertFalse(isValid);
  }

  @Test
  void testSerialization() {
    KeyPair keyPair = KeyPair.random();
    byte[] message = "Hello".getBytes(UTF_8);
    Signature signature = BLS12381.sign(keyPair, message).signature();

    Bytes sigTobytes = signature.encode();
    Signature sigFromBytes = Signature.decode(sigTobytes);

    assertEquals(signature, sigFromBytes);
    assertEquals(signature.hashCode(), sigFromBytes.hashCode());

    PublicKey pubKey = keyPair.publicKey();
    byte[] pubKeyTobytes = pubKey.toByteArray();
    PublicKey pubKeyFromBytes = PublicKey.fromBytes(pubKeyTobytes);

    assertEquals(pubKey, pubKeyFromBytes);
    assertEquals(pubKey.hashCode(), pubKeyFromBytes.hashCode());
  }

  List<SignatureAndPublicKey> getSignaturesAndPublicKeys(byte[] message) {
    KeyPair keyPair1 = KeyPair.random();
    KeyPair keyPair2 = KeyPair.random();
    KeyPair keyPair3 = KeyPair.random();

    SignatureAndPublicKey sigAndPubKey1 = BLS12381.sign(keyPair1, message);
    SignatureAndPublicKey sigAndPubKey2 = BLS12381.sign(keyPair2, message);
    SignatureAndPublicKey sigAndPubKey3 = BLS12381.sign(keyPair3, message);

    List<SignatureAndPublicKey> sigs = new ArrayList<SignatureAndPublicKey>();
    sigs.add(sigAndPubKey1);
    sigs.add(sigAndPubKey2);
    sigs.add(sigAndPubKey3);

    return sigs;
  }

  @Test
  void secretKeyRoundtrip() {
    KeyPair kp = KeyPair.random();
    SecretKey key = kp.secretKey();
    Bytes bytes = key.toBytes();
    assertEquals(key, SecretKey.fromBytes(bytes));
  }
}
