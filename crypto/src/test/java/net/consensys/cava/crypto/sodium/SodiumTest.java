package net.consensys.cava.crypto.sodium;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

import com.google.common.base.Charsets;
import jnr.ffi.Pointer;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

class SodiumTest {

  @BeforeAll
  static void checkSodium() {
    assumeTrue(Sodium.isAvailable());
  }

  @Test
  void checkBasicConstants() {
    assertEquals(12, Sodium.crypto_aead_aes256gcm_npubbytes());
    assertEquals(64, Sodium.crypto_auth_hmacsha512_bytes());
    assertEquals(32, Sodium.crypto_generichash_bytes());
  }

  @Test
  void checkCryptoHashSha512MultiPart() {
    byte[] message = "This is a test message".getBytes(Charsets.UTF_8);
    byte[] hash = new byte[(int) Sodium.crypto_hash_sha512_bytes()];
    int rc = Sodium.crypto_hash_sha512(hash, message, message.length);
    assertEquals(0, rc);

    Pointer state = Sodium.sodium_malloc(Sodium.crypto_hash_sha512_statebytes());
    try {
      rc = Sodium.crypto_hash_sha512_init(state);
      assertEquals(0, rc);

      byte[] message1 = "This is ".getBytes(Charsets.UTF_8);
      Sodium.crypto_hash_sha512_update(state, message1, message1.length);
      byte[] message2 = "a test message".getBytes(Charsets.UTF_8);
      Sodium.crypto_hash_sha512_update(state, message2, message2.length);

      byte[] hash2 = new byte[(int) Sodium.crypto_hash_sha512_bytes()];
      Sodium.crypto_hash_sha512_final(state, hash2);

      assertArrayEquals(hash, hash2);
    } finally {
      Sodium.sodium_free(state);
    }
  }

  @Test
  void checkCryptoHashSha256MultiPart() {
    byte[] message = "This is a test message".getBytes(Charsets.UTF_8);
    byte[] hash = new byte[(int) Sodium.crypto_hash_sha256_bytes()];
    int rc = Sodium.crypto_hash_sha256(hash, message, message.length);
    assertEquals(0, rc);

    Pointer state = Sodium.sodium_malloc(Sodium.crypto_hash_sha256_statebytes());
    try {
      rc = Sodium.crypto_hash_sha256_init(state);
      assertEquals(0, rc);

      byte[] message1 = "This is ".getBytes(Charsets.UTF_8);
      Sodium.crypto_hash_sha256_update(state, message1, message1.length);
      byte[] message2 = "a test message".getBytes(Charsets.UTF_8);
      Sodium.crypto_hash_sha256_update(state, message2, message2.length);

      byte[] hash2 = new byte[(int) Sodium.crypto_hash_sha256_bytes()];
      Sodium.crypto_hash_sha256_final(state, hash2);

      assertArrayEquals(hash, hash2);
    } finally {
      Sodium.sodium_free(state);
    }
  }
}
