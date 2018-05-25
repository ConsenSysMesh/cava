package net.consensys.cava.crypto.sodium;

import static org.junit.jupiter.api.Assertions.*;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

import java.util.Optional;

import com.google.common.base.Charsets;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

class SecretBoxTest {

  @BeforeAll
  static void checkAvailable() {
    assumeTrue(Sodium.isAvailable());
  }

  @Test
  void checkCombinedEncryptDecrypt() {
    SecretBox.Key key = SecretBox.Key.random();
    SecretBox.Nonce nonce = SecretBox.Nonce.random().increment();

    byte[] message = "This is a test message".getBytes(Charsets.UTF_8);

    byte[] cipherText = SecretBox.encrypt(message, key, nonce);
    Optional<byte[]> clearText = SecretBox.decrypt(cipherText, key, nonce);

    assertTrue(clearText.isPresent());
    assertArrayEquals(message, clearText.get());

    assertFalse(SecretBox.decrypt(cipherText, key, nonce.increment()).isPresent());
    SecretBox.Key otherKey = SecretBox.Key.random();
    assertFalse(SecretBox.decrypt(cipherText, otherKey, nonce).isPresent());
  }

  @Test
  void checkDetachedEncryptDecrypt() {
    SecretBox.Key key = SecretBox.Key.random();
    SecretBox.Nonce nonce = SecretBox.Nonce.random().increment();

    byte[] message = "This is a test message".getBytes(Charsets.UTF_8);

    DetachedEncryptionResult result = SecretBox.encryptDetached(message, key, nonce);
    Optional<byte[]> clearText = SecretBox.decryptDetached(result.cipherTextArray(), result.macArray(), key, nonce);

    assertTrue(clearText.isPresent());
    assertArrayEquals(message, clearText.get());

    assertFalse(
        SecretBox.decryptDetached(result.cipherTextArray(), result.macArray(), key, nonce.increment()).isPresent());
    SecretBox.Key otherKey = SecretBox.Key.random();
    assertFalse(SecretBox.decryptDetached(result.cipherTextArray(), result.macArray(), otherKey, nonce).isPresent());
  }
}
