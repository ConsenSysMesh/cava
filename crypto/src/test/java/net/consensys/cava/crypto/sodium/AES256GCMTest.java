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

class AES256GCMTest {

  private static AES256GCM.Nonce nonce;

  @BeforeAll
  static void checkAvailable() {
    assumeTrue(AES256GCM.isAvailable());
    nonce = AES256GCM.Nonce.random();
  }

  @BeforeEach
  void incrementNonce() {
    nonce = nonce.increment();
  }

  @Test
  void checkCombinedEncryptDecrypt() {
    AES256GCM.Key key = AES256GCM.Key.random();

    byte[] message = "This is a test message".getBytes(Charsets.UTF_8);
    byte[] data = "123456".getBytes(Charsets.UTF_8);

    byte[] cipherText = AES256GCM.encrypt(message, data, key, nonce);
    Optional<byte[]> clearText = AES256GCM.decrypt(cipherText, data, key, nonce);

    assertTrue(clearText.isPresent());
    assertArrayEquals(message, clearText.get());

    assertFalse(AES256GCM.decrypt(cipherText, data, key, nonce.increment()).isPresent());
  }

  @Test
  void checkCombinedPrecomputedEncryptDecrypt() {
    try (AES256GCM precomputed = AES256GCM.forKey(AES256GCM.Key.random())) {
      byte[] message = "This is a test message".getBytes(Charsets.UTF_8);
      byte[] data = "123456".getBytes(Charsets.UTF_8);

      byte[] cipherText = precomputed.encrypt(message, data, nonce);
      Optional<byte[]> clearText = precomputed.decrypt(cipherText, data, nonce);

      assertTrue(clearText.isPresent());
      assertArrayEquals(message, clearText.get());

      assertFalse(precomputed.decrypt(cipherText, data, nonce.increment()).isPresent());
    }
  }

  @Test
  void checkDetachedEncryptDecrypt() {
    AES256GCM.Key key = AES256GCM.Key.random();

    byte[] message = "This is a test message".getBytes(Charsets.UTF_8);
    byte[] data = "123456".getBytes(Charsets.UTF_8);

    DetachedEncryptionResult result = AES256GCM.encryptDetached(message, data, key, nonce);
    Optional<byte[]> clearText =
        AES256GCM.decryptDetached(result.cipherTextArray(), result.macArray(), data, key, nonce);

    assertTrue(clearText.isPresent());
    assertArrayEquals(message, clearText.get());

    clearText = AES256GCM.decryptDetached(result.cipherTextArray(), result.macArray(), data, key, nonce.increment());
    assertFalse(clearText.isPresent());
  }

  @Test
  void checkDetachedPrecomputedEncryptDecrypt() {
    try (AES256GCM precomputed = AES256GCM.forKey(AES256GCM.Key.random())) {
      byte[] message = "This is a test message".getBytes(Charsets.UTF_8);
      byte[] data = "123456".getBytes(Charsets.UTF_8);

      DetachedEncryptionResult result = precomputed.encryptDetached(message, data, nonce);
      Optional<byte[]> clearText =
          precomputed.decryptDetached(result.cipherTextArray(), result.macArray(), data, nonce);

      assertTrue(clearText.isPresent());
      assertArrayEquals(message, clearText.get());

      clearText = precomputed.decryptDetached(result.cipherTextArray(), result.macArray(), data, nonce.increment());
      assertFalse(clearText.isPresent());
    }
  }
}
