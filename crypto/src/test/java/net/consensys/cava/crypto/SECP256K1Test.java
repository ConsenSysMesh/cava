/*
 * Copyright 2018 ConsenSys AG.
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
package net.consensys.cava.crypto;

import static java.nio.charset.StandardCharsets.UTF_8;
import static net.consensys.cava.bytes.Bytes.fromHexString;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import net.consensys.cava.bytes.Bytes;
import net.consensys.cava.bytes.Bytes32;
import net.consensys.cava.crypto.SECP256K1.KeyPair;
import net.consensys.cava.crypto.SECP256K1.PublicKey;
import net.consensys.cava.crypto.SECP256K1.Signature;
import net.consensys.cava.junit.BouncyCastleExtension;
import net.consensys.cava.junit.TempDirectory;
import net.consensys.cava.junit.TempDirectoryExtension;

import java.math.BigInteger;
import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

@ExtendWith(TempDirectoryExtension.class)
@ExtendWith(BouncyCastleExtension.class)
class SECP256K1Test {

  @Test
  void testCreatePrivateKey_NullEncoding() {
    assertThrows(NullPointerException.class, () -> SECP256K1.PrivateKey.create((Bytes32) null));
  }

  @Test
  void testPrivateKeyEquals() {
    SECP256K1.PrivateKey privateKey1 = SECP256K1.PrivateKey.create(BigInteger.TEN);
    SECP256K1.PrivateKey privateKey2 = SECP256K1.PrivateKey.create(BigInteger.TEN);
    assertEquals(privateKey1, privateKey2);
  }

  @Test
  void testPrivateHashCode() {
    SECP256K1.PrivateKey privateKey = SECP256K1.PrivateKey.create(BigInteger.TEN);
    assertNotEquals(0, privateKey.hashCode());
  }

  @Test
  void testCreatePublicKey_NullEncoding() {
    assertThrows(NullPointerException.class, () -> SECP256K1.PublicKey.create((Bytes) null));
  }

  @Test
  void testCreatePublicKey_EncodingTooShort() {
    assertThrows(IllegalArgumentException.class, () -> SECP256K1.PublicKey.create(Bytes.wrap(new byte[63])));
  }

  @Test
  void testCreatePublicKey_EncodingTooLong() {
    assertThrows(IllegalArgumentException.class, () -> SECP256K1.PublicKey.create(Bytes.wrap(new byte[65])));
  }

  @Test
  void testPublicKeyEquals() {
    SECP256K1.PublicKey publicKey1 = SECP256K1.PublicKey.create(
        fromHexString(
            "a0434d9e47f3c86235477c7b1ae6ae5d3442d49b1943c2b752a68e2a47e247c7893aba425419bc27a3b6c7e693a24c696f794c2ed877a1593cbee53b037368d7"));
    SECP256K1.PublicKey publicKey2 = SECP256K1.PublicKey.create(
        fromHexString(
            "a0434d9e47f3c86235477c7b1ae6ae5d3442d49b1943c2b752a68e2a47e247c7893aba425419bc27a3b6c7e693a24c696f794c2ed877a1593cbee53b037368d7"));
    assertEquals(publicKey1, publicKey2);
  }

  @Test
  void testPublicHashCode() {
    SECP256K1.PublicKey publicKey = SECP256K1.PublicKey.create(
        fromHexString(
            "a0434d9e47f3c86235477c7b1ae6ae5d3442d49b1943c2b752a68e2a47e247c7893aba425419bc27a3b6c7e693a24c696f794c2ed877a1593cbee53b037368d7"));

    assertNotEquals(0, publicKey.hashCode());
  }

  @Test
  void testCreateKeyPair_PublicKeyNull() {
    assertThrows(
        NullPointerException.class,
        () -> SECP256K1.KeyPair.create(null, SECP256K1.PublicKey.create(Bytes.wrap(new byte[64]))));
  }

  @Test
  void testCreateKeyPair_PrivateKeyNull() {
    assertThrows(
        NullPointerException.class,
        () -> SECP256K1.KeyPair.create(SECP256K1.PrivateKey.create(Bytes32.wrap(new byte[32])), null));
  }

  @Test
  void testKeyPairGeneration() {
    SECP256K1.KeyPair keyPair = SECP256K1.KeyPair.random();
    assertNotNull(keyPair);
    assertNotNull(keyPair.getPrivateKey());
    assertNotNull(keyPair.getPublicKey());
  }

  @Test
  void testKeyPairEquals() {
    SECP256K1.PrivateKey privateKey1 = SECP256K1.PrivateKey.create(BigInteger.TEN);
    SECP256K1.PrivateKey privateKey2 = SECP256K1.PrivateKey.create(BigInteger.TEN);
    SECP256K1.PublicKey publicKey1 = SECP256K1.PublicKey.create(
        fromHexString(
            "a0434d9e47f3c86235477c7b1ae6ae5d3442d49b1943c2b752a68e2a47e247c7893aba425419bc27a3b6c7e693a24c696f794c2ed877a1593cbee53b037368d7"));
    SECP256K1.PublicKey publicKey2 = SECP256K1.PublicKey.create(
        fromHexString(
            "a0434d9e47f3c86235477c7b1ae6ae5d3442d49b1943c2b752a68e2a47e247c7893aba425419bc27a3b6c7e693a24c696f794c2ed877a1593cbee53b037368d7"));

    SECP256K1.KeyPair keyPair1 = SECP256K1.KeyPair.create(privateKey1, publicKey1);
    SECP256K1.KeyPair keyPair2 = SECP256K1.KeyPair.create(privateKey2, publicKey2);

    assertEquals(keyPair1, keyPair2);
  }

  @Test
  void testKeyPairHashCode() {
    SECP256K1.KeyPair keyPair = SECP256K1.KeyPair.random();
    assertNotEquals(0, keyPair.hashCode());
  }

  @Test
  void testKeyPairGeneration_PublicKeyRecovery() {
    SECP256K1.KeyPair keyPair = SECP256K1.KeyPair.random();
    assertEquals(keyPair.getPublicKey(), SECP256K1.PublicKey.create(keyPair.getPrivateKey()));
  }

  @Test
  void testPublicKeyRecovery() {
    SECP256K1.PrivateKey privateKey = SECP256K1.PrivateKey.create(BigInteger.TEN);
    SECP256K1.PublicKey expectedPublicKey = SECP256K1.PublicKey.create(
        fromHexString(
            "a0434d9e47f3c86235477c7b1ae6ae5d3442d49b1943c2b752a68e2a47e247c7893aba425419bc27a3b6c7e693a24c696f794c2ed877a1593cbee53b037368d7"));

    SECP256K1.PublicKey publicKey = SECP256K1.PublicKey.create(privateKey);
    assertEquals(expectedPublicKey, publicKey);
  }

  @Test
  void testCreateSignature() {
    SECP256K1.Signature signature = new SECP256K1.Signature(BigInteger.ONE, BigInteger.TEN, (byte) 7);
    assertEquals(BigInteger.ONE, signature.r());
    assertEquals(BigInteger.TEN, signature.s());
    assertEquals((byte) 7, signature.v());
  }

  @Test
  void testEncodeSignature() {
    SECP256K1.Signature signature = new SECP256K1.Signature(BigInteger.ONE, BigInteger.TEN, (byte) 7);
    assertEquals(
        "0x0000000000000000000000000000000000000000000000000000000000000001000000000000000000000000000000000000000000000000000000000000000A07",
        signature.encodedBytes().toString());
  }

  @Test
  void testCreateSignatureFromEncoding() {
    SECP256K1.Signature signature = SECP256K1.Signature.create(
        fromHexString(
            "0x0000000000000000000000000000000000000000000000000000000000000001000000000000000000000000000000000000000000000000000000000000000A07"));
    assertEquals(BigInteger.ONE, signature.r());
    assertEquals(BigInteger.TEN, signature.s());
    assertEquals((byte) 7, signature.v());
  }

  @Test
  void testCreateSignatureWithNullR() {
    assertThrows(NullPointerException.class, () -> SECP256K1.Signature.create((byte) 12, null, BigInteger.ONE));
  }

  @Test
  void testCreateSignatureWithNullS() {
    assertThrows(NullPointerException.class, () -> SECP256K1.Signature.create((byte) 12, BigInteger.ONE, null));
  }

  @Test
  void testCreateSignatureWithZeroR() {
    Exception throwable = assertThrows(
        IllegalArgumentException.class,
        () -> SECP256K1.Signature.create((byte) 12, BigInteger.ZERO, BigInteger.ONE));
    assertEquals("Invalid 'r' value, should be >= 1 but got 0", throwable.getMessage());
  }

  @Test
  void testCreateSignatureWithZeroS() {
    Exception throwable = assertThrows(
        IllegalArgumentException.class,
        () -> SECP256K1.Signature.create((byte) 12, BigInteger.ONE, BigInteger.ZERO));
    assertEquals("Invalid 's' value, should be >= 1 but got 0", throwable.getMessage());
  }

  @Test
  void testCreateSignatureWithRHigherThanCurve() {
    BigInteger curveN = SECP256K1.Parameters.CURVE.getN();
    Exception throwable = assertThrows(
        IllegalArgumentException.class,
        () -> SECP256K1.Signature.create((byte) 12, curveN.add(BigInteger.ONE), BigInteger.ONE));
    assertEquals(
        "Invalid 'r' value, should be < " + curveN.toString() + " but got " + curveN.add(BigInteger.ONE),
        throwable.getMessage());
  }

  @Test
  void testCreateSignatureWithSHigherThanCurve() {
    BigInteger curveN = SECP256K1.Parameters.CURVE.getN();
    Exception throwable = assertThrows(
        IllegalArgumentException.class,
        () -> SECP256K1.Signature.create((byte) 12, BigInteger.ONE, curveN.add(BigInteger.ONE)));
    assertEquals(
        "Invalid 's' value, should be < " + curveN.toString() + " but got " + curveN.add(BigInteger.ONE),
        throwable.getMessage());
  }

  @Test
  void testRecoverPublicKeyFromSignature() {
    SECP256K1.PrivateKey privateKey = SECP256K1.PrivateKey
        .create(new BigInteger("c85ef7d79691fe79573b1a7064c19c1a9819ebdbd1faaab1a8ec92344438aaf4", 16));
    SECP256K1.KeyPair keyPair = SECP256K1.KeyPair.create(privateKey);

    Bytes data = Bytes.wrap("This is an example of a signed message.".getBytes(UTF_8));
    SECP256K1.Signature signature = SECP256K1.sign(data, keyPair);

    PublicKey recoveredPublicKey = SECP256K1.PublicKey.recoverFromSignature(data, signature);
    assertEquals(keyPair.getPublicKey().toString(), recoveredPublicKey.toString());
  }

  @Test
  void testCannotRecoverPublicKeyFromSignature() {
    SECP256K1.Signature signature = new Signature(BigInteger.ONE, BigInteger.valueOf(10), (byte) 3);

    SECP256K1KeyRecoveryException exception = assertThrows(SECP256K1KeyRecoveryException.class, () -> {
      SECP256K1.PublicKey.recoverFromSignature(Bytes.of("This is not matching data".getBytes("UTF-8")), signature);
    });
    assertEquals("Public key cannot be recovered: Invalid point compression", exception.getMessage());
  }

  @Test
  void testSignatureGeneration() {
    SECP256K1.PrivateKey privateKey = SECP256K1.PrivateKey
        .create(new BigInteger("c85ef7d79691fe79573b1a7064c19c1a9819ebdbd1faaab1a8ec92344438aaf4", 16));
    SECP256K1.KeyPair keyPair = SECP256K1.KeyPair.create(privateKey);

    Bytes data = Bytes.wrap("This is an example of a signed message.".getBytes(UTF_8));
    SECP256K1.Signature expectedSignature = new SECP256K1.Signature(
        new BigInteger("d2ce488f4da29e68f22cb05cac1b19b75df170a12b4ad1bdd4531b8e9115c6fb", 16),
        new BigInteger("75c1fe50a95e8ccffcbb5482a1e42fbbdd6324131dfe75c3b3b7f9a7c721eccb", 16),
        (byte) 28);

    SECP256K1.Signature actualSignature = SECP256K1.sign(data, keyPair);
    assertEquals(expectedSignature, actualSignature);
  }

  @Test
  void testSignatureVerification() {
    SECP256K1.PrivateKey privateKey = SECP256K1.PrivateKey
        .create(new BigInteger("c85ef7d79691fe79573b1a7064c19c1a9819ebdbd1faaab1a8ec92344438aaf4", 16));
    SECP256K1.KeyPair keyPair = SECP256K1.KeyPair.create(privateKey);

    Bytes data = Bytes.wrap("This is an example of a signed message.".getBytes(UTF_8));

    SECP256K1.Signature signature = SECP256K1.sign(data, keyPair);
    assertTrue(SECP256K1.verify(data, signature, keyPair.getPublicKey()));
  }


  @Test
  void testFileContainsValidPrivateKey(@TempDirectory Path tempDir) throws Exception {
    Path tempFile = tempDir.resolve("tempId");
    Files.write(tempFile, "000000000000000000000000000000000000000000000000000000000000000A".getBytes(UTF_8));
    SECP256K1.PrivateKey privateKey = SECP256K1.PrivateKey.load(tempFile);
    assertEquals(
        fromHexString("000000000000000000000000000000000000000000000000000000000000000A"),
        privateKey.encodedBytes());
  }

  @Test
  void testReadWritePrivateKeyString(@TempDirectory Path tempDir) throws Exception {
    SECP256K1.PrivateKey privateKey = SECP256K1.PrivateKey.create(BigInteger.TEN);
    SECP256K1.KeyPair keyPair1 = SECP256K1.KeyPair.create(privateKey);
    Path tempFile = tempDir.resolve("tempId");
    keyPair1.store(tempFile);
    SECP256K1.KeyPair keyPair2 = SECP256K1.KeyPair.load(tempFile);
    assertEquals(keyPair1, keyPair2);
  }

  @Test
  void testInvalidFileThrowsInvalidKeyPairException(@TempDirectory Path tempDir) throws Exception {
    Path tempFile = tempDir.resolve("tempId");
    Files.write(tempFile, "not valid".getBytes(UTF_8));
    assertThrows(InvalidSEC256K1PrivateKeyStoreException.class, () -> SECP256K1.PrivateKey.load(tempFile));
  }

  @Test
  void testInvalidMultiLineFileThrowsInvalidIdException(@TempDirectory Path tempDir) throws Exception {
    Path tempFile = tempDir.resolve("tempId");
    Files.write(tempFile, "not\n\nvalid".getBytes(UTF_8));
    assertThrows(InvalidSEC256K1PrivateKeyStoreException.class, () -> SECP256K1.PrivateKey.load(tempFile));
  }

  @Test
  void testEncodedBytes() {
    KeyPair kp = SECP256K1.KeyPair.random();
    Signature sig = SECP256K1.sign(Bytes.of(1, 2, 3), kp);
    assertEquals(65, sig.encodedBytes().size());
    assertTrue(sig.encodedBytes().get(64) <= 3 && sig.encodedBytes().get(64) >= 0);
  }
}
