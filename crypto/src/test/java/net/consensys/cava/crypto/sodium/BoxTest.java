package net.consensys.cava.crypto.sodium;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

import java.util.Optional;

import com.google.common.base.Charsets;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class BoxTest {

  private static Box.Seed seed;
  private static Box.Nonce nonce;

  @BeforeAll
  static void checkAvailable() {
    assumeTrue(Sodium.isAvailable());
    nonce = Box.Nonce.random();
    // @formatter:off
    seed = Box.Seed.forBytes(new byte[] {
        0x00, 0x01, 0x02, 0x03, 0x04, 0x05, 0x06, 0x07,
        0x08, 0x09, 0x0a, 0x0b, 0x0c, 0x0d, 0x0e, 0x0f,
        0x10, 0x11, 0x12, 0x13, 0x14, 0x15, 0x16, 0x17,
        0x18, 0x19, 0x1a, 0x1b, 0x1c, 0x1d, 0x1e, 0x1f
    });
    // @formatter:on
  }

  @BeforeEach
  void incrementNonce() {
    nonce = nonce.increment();
  }

  @Test
  void checkCombinedEncryptDecrypt() {
    Box.KeyPair aliceKeyPair = Box.KeyPair.random();
    Box.KeyPair bobKeyPair = Box.KeyPair.fromSeed(seed);

    byte[] message = "This is a test message".getBytes(Charsets.UTF_8);

    byte[] cipherText = Box.encrypt(message, aliceKeyPair.publicKey(), bobKeyPair.secretKey(), nonce);
    Optional<byte[]> clearText = Box.decrypt(cipherText, bobKeyPair.publicKey(), aliceKeyPair.secretKey(), nonce);

    assertTrue(clearText.isPresent());
    assertArrayEquals(message, clearText.get());

    clearText = Box.decrypt(cipherText, bobKeyPair.publicKey(), aliceKeyPair.secretKey(), nonce.increment());
    assertFalse(clearText.isPresent());

    Box.KeyPair otherKeyPair = Box.KeyPair.random();
    clearText = Box.decrypt(cipherText, otherKeyPair.publicKey(), bobKeyPair.secretKey(), nonce);
    assertFalse(clearText.isPresent());
  }

  @Test
  void checkCombinedPrecomputedEncryptDecrypt() {
    Box.KeyPair aliceKeyPair = Box.KeyPair.random();
    Box.KeyPair bobKeyPair = Box.KeyPair.random();

    byte[] message = "This is a test message".getBytes(Charsets.UTF_8);
    byte[] cipherText;

    try (Box precomputed = Box.forKeys(aliceKeyPair.publicKey(), bobKeyPair.secretKey())) {
      cipherText = precomputed.encrypt(message, nonce);
    }

    Optional<byte[]> clearText = Box.decrypt(cipherText, bobKeyPair.publicKey(), aliceKeyPair.secretKey(), nonce);

    assertTrue(clearText.isPresent());
    assertArrayEquals(message, clearText.get());

    try (Box precomputed = Box.forKeys(bobKeyPair.publicKey(), aliceKeyPair.secretKey())) {
      clearText = precomputed.decrypt(cipherText, nonce);

      assertTrue(clearText.isPresent());
      assertArrayEquals(message, clearText.get());

      assertFalse(precomputed.decrypt(cipherText, nonce.increment()).isPresent());
    }

    Box.KeyPair otherKeyPair = Box.KeyPair.random();
    try (Box precomputed = Box.forKeys(otherKeyPair.publicKey(), bobKeyPair.secretKey())) {
      assertFalse(precomputed.decrypt(cipherText, nonce).isPresent());
    }
  }

  @Test
  void checkDetachedEncryptDecrypt() {
    Box.KeyPair aliceKeyPair = Box.KeyPair.random();
    Box.KeyPair bobKeyPair = Box.KeyPair.random();

    byte[] message = "This is a test message".getBytes(Charsets.UTF_8);

    DetachedEncryptionResult result =
        Box.encryptDetached(message, aliceKeyPair.publicKey(), bobKeyPair.secretKey(), nonce);
    Optional<byte[]> clearText = Box.decryptDetached(
        result.cipherTextArray(),
        result.macArray(),
        bobKeyPair.publicKey(),
        aliceKeyPair.secretKey(),
        nonce);

    assertTrue(clearText.isPresent());
    assertArrayEquals(message, clearText.get());

    clearText = Box.decryptDetached(
        result.cipherTextArray(),
        result.macArray(),
        bobKeyPair.publicKey(),
        aliceKeyPair.secretKey(),
        nonce.increment());
    assertFalse(clearText.isPresent());

    Box.KeyPair otherKeyPair = Box.KeyPair.random();
    clearText = Box.decryptDetached(
        result.cipherTextArray(),
        result.macArray(),
        otherKeyPair.publicKey(),
        bobKeyPair.secretKey(),
        nonce);
    assertFalse(clearText.isPresent());
  }

  @Test
  void checkDetachedPrecomputedEncryptDecrypt() {
    Box.KeyPair aliceKeyPair = Box.KeyPair.random();
    Box.KeyPair bobKeyPair = Box.KeyPair.random();

    byte[] message = "This is a test message".getBytes(Charsets.UTF_8);
    DetachedEncryptionResult result;

    try (Box precomputed = Box.forKeys(aliceKeyPair.publicKey(), bobKeyPair.secretKey())) {
      result = precomputed.encryptDetached(message, nonce);
    }

    Optional<byte[]> clearText = Box.decryptDetached(
        result.cipherTextArray(),
        result.macArray(),
        bobKeyPair.publicKey(),
        aliceKeyPair.secretKey(),
        nonce);

    assertTrue(clearText.isPresent());
    assertArrayEquals(message, clearText.get());

    try (Box precomputed = Box.forKeys(bobKeyPair.publicKey(), aliceKeyPair.secretKey())) {
      clearText = precomputed.decryptDetached(result.cipherTextArray(), result.macArray(), nonce);

      assertTrue(clearText.isPresent());
      assertArrayEquals(message, clearText.get());

      assertFalse(
          precomputed.decryptDetached(result.cipherTextArray(), result.macArray(), nonce.increment()).isPresent());
    }

    Box.KeyPair otherKeyPair = Box.KeyPair.random();
    try (Box precomputed = Box.forKeys(otherKeyPair.publicKey(), bobKeyPair.secretKey())) {
      assertFalse(precomputed.decryptDetached(result.cipherTextArray(), result.macArray(), nonce).isPresent());
    }
  }
}
