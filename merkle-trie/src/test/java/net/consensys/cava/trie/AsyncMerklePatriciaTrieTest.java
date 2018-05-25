package net.consensys.cava.trie;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

import net.consensys.cava.bytes.Bytes;
import net.consensys.cava.bytes.Bytes32;

import java.nio.charset.Charset;
import java.security.Security;
import java.util.Optional;

import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class AsyncMerklePatriciaTrieTest {
  private AsyncMerkleTrie<Bytes, String> trie;

  @BeforeAll
  static void loadProvider() {
    Security.addProvider(new BouncyCastleProvider());
  }

  @BeforeEach
  void setup() {
    trie = new AsyncMerklePatriciaTrie<>(
        value -> (value != null) ? Bytes.wrap(value.getBytes(Charset.forName("UTF-8"))) : null);
  }

  @Test
  void testEmptyTreeReturnsEmpty() throws Exception {
    assertFalse(trie.get(Bytes.EMPTY).get().isPresent());
  }

  @Test
  void testEmptyTreeHasKnownRootHash() {
    assertEquals("0x56E81F171BCC55A6FF8345E692C0F86E5B48E01B996CADC001622FB5E363B421", trie.rootHash().toString());
  }

  @Test
  void testDeletesEntryUpdateWithNull() throws Exception {
    final Bytes key = Bytes.of(1);
    final String value1 = "value1";
    trie.put(key, value1).join();
    trie.put(key, null).join();
    assertFalse(trie.get(key).get().isPresent());
  }

  @Test
  void testReplaceSingleValue() throws Exception {
    final Bytes key = Bytes.of(1);
    final String value1 = "value1";
    trie.put(key, value1).join();
    assertEquals(Optional.of(value1), trie.get(key).get());

    final String value2 = "value2";
    trie.put(key, value2).join();
    assertEquals(Optional.of(value2), trie.get(key).get());
  }

  @Test
  void testHashChangesWhenSingleValueReplaced() throws Exception {
    final Bytes key = Bytes.of(1);
    final String value1 = "value1";
    trie.put(key, value1).join();
    final Bytes32 hash1 = trie.rootHash();

    final String value2 = "value2";
    trie.put(key, value2).join();
    final Bytes32 hash2 = trie.rootHash();

    assertNotEquals(hash2, hash1);

    trie.put(key, value1).join();
    assertEquals(hash1, trie.rootHash());
  }

  @Test
  void testReadPastLeaf() throws Exception {
    final Bytes key1 = Bytes.of(1);
    trie.put(key1, "value").join();
    final Bytes key2 = Bytes.of(1, 3);
    assertFalse(trie.get(key2).get().isPresent());
  }

  @Test
  void testBranchValue() throws Exception {
    final Bytes key1 = Bytes.of(1);
    final Bytes key2 = Bytes.of(16);

    final String value1 = "value1";
    trie.put(key1, value1).join();

    final String value2 = "value2";
    trie.put(key2, value2).join();

    assertEquals(Optional.of(value1), trie.get(key1).get());
    assertEquals(Optional.of(value2), trie.get(key2).get());
  }

  @Test
  void testReadPastBranch() throws Exception {
    final Bytes key1 = Bytes.of(12);
    final Bytes key2 = Bytes.of(12, 54);

    final String value1 = "value1";
    trie.put(key1, value1).join();
    final String value2 = "value2";
    trie.put(key2, value2).join();

    final Bytes key3 = Bytes.of(3);
    assertFalse(trie.get(key3).get().isPresent());
  }

  @Test
  void testBranchWithValue() throws Exception {
    final Bytes key1 = Bytes.of(5);
    final Bytes key2 = Bytes.EMPTY;

    final String value1 = "value1";
    trie.put(key1, value1).join();

    final String value2 = "value2";
    trie.put(key2, value2).join();

    assertEquals(Optional.of(value1), trie.get(key1).get());
    assertEquals(Optional.of(value2), trie.get(key2).get());
  }

  @Test
  void testExtendAndBranch() throws Exception {
    final Bytes key1 = Bytes.of(1, 5, 9);
    final Bytes key2 = Bytes.of(1, 5, 2);

    final String value1 = "value1";
    trie.put(key1, value1).join();

    final String value2 = "value2";
    trie.put(key2, value2).join();

    assertEquals(Optional.of(value1), trie.get(key1).get());
    assertEquals(Optional.of(value2), trie.get(key2).get());
    assertFalse(trie.get(Bytes.of(1, 4)).get().isPresent());
  }

  @Test
  void testBranchFromTopOfExtend() throws Exception {
    final Bytes key1 = Bytes.of(0xfe, 1);
    final Bytes key2 = Bytes.of(0xfe, 2);
    final Bytes key3 = Bytes.of(0xe1, 1);

    final String value1 = "value1";
    trie.put(key1, value1).join();

    final String value2 = "value2";
    trie.put(key2, value2).join();

    final String value3 = "value3";
    trie.put(key3, value3).join();

    assertEquals(Optional.of(value1), trie.get(key1).get());
    assertEquals(Optional.of(value2), trie.get(key2).get());
    assertEquals(Optional.of(value3), trie.get(key3).get());
    assertFalse(trie.get(Bytes.of(1, 4)).get().isPresent());
    assertFalse(trie.get(Bytes.of(2, 4)).get().isPresent());
    assertFalse(trie.get(Bytes.of(3)).get().isPresent());
  }

  @Test
  void testSplitBranchExtension() throws Exception {
    final Bytes key1 = Bytes.of(1, 5, 9);
    final Bytes key2 = Bytes.of(1, 5, 2);

    final String value1 = "value1";
    trie.put(key1, value1).join();

    final String value2 = "value2";
    trie.put(key2, value2).join();

    final Bytes key3 = Bytes.of(1, 9, 1);

    final String value3 = "value3";
    trie.put(key3, value3).join();

    assertEquals(Optional.of(value1), trie.get(key1).get());
    assertEquals(Optional.of(value2), trie.get(key2).get());
    assertEquals(Optional.of(value3), trie.get(key3).get());
  }

  @Test
  void testReplaceBranchChild() throws Exception {
    final Bytes key1 = Bytes.of(0);
    final Bytes key2 = Bytes.of(1);

    final String value1 = "value1";
    trie.put(key1, value1).join();
    final String value2 = "value2";
    trie.put(key2, value2).join();

    assertEquals(Optional.of(value1), trie.get(key1).get());
    assertEquals(Optional.of(value2), trie.get(key2).get());

    final String value3 = "value3";
    trie.put(key1, value3).join();

    assertEquals(Optional.of(value3), trie.get(key1).get());
    assertEquals(Optional.of(value2), trie.get(key2).get());
  }

  @Test
  void testInlineBranchInBranch() throws Exception {
    final Bytes key1 = Bytes.of(0);
    final Bytes key2 = Bytes.of(1);
    final Bytes key3 = Bytes.of(2);
    final Bytes key4 = Bytes.of(0, 0);
    final Bytes key5 = Bytes.of(0, 1);

    trie.put(key1, "value1").join();
    trie.put(key2, "value2").join();
    trie.put(key3, "value3").join();
    trie.put(key4, "value4").join();
    trie.put(key5, "value5").join();

    trie.remove(key2).join();
    trie.remove(key3).join();

    assertEquals(Optional.of("value1"), trie.get(key1).get());
    assertFalse(trie.get(key2).get().isPresent());
    assertFalse(trie.get(key3).get().isPresent());
    assertEquals(Optional.of("value4"), trie.get(key4).get());
    assertEquals(Optional.of("value5"), trie.get(key5).get());
  }

  @Test
  void testRemoveNodeInBranchExtensionHasNoEffect() throws Exception {
    final Bytes key1 = Bytes.of(1, 5, 9);
    final Bytes key2 = Bytes.of(1, 5, 2);

    final String value1 = "value1";
    trie.put(key1, value1).join();

    final String value2 = "value2";
    trie.put(key2, value2).join();

    final Bytes32 hash = trie.rootHash();

    trie.remove(Bytes.of(1, 4)).join();
    assertEquals(hash, trie.rootHash());
  }

  @Test
  void testHashChangesWhenValueChanged() throws Exception {
    final Bytes key1 = Bytes.of(1, 5, 8, 9);
    final Bytes key2 = Bytes.of(1, 6, 1, 2);
    final Bytes key3 = Bytes.of(1, 6, 1, 3);

    final String value1 = "value1";
    trie.put(key1, value1).join();
    final Bytes32 hash1 = trie.rootHash();

    final String value2 = "value2";
    trie.put(key2, value2).join();
    final String value3 = "value3";
    trie.put(key3, value3).join();
    final Bytes32 hash2 = trie.rootHash();

    assertNotEquals(hash2, hash1);

    final String value4 = "value4";
    trie.put(key1, value4).join();
    final Bytes32 hash3 = trie.rootHash();

    assertNotEquals(hash3, hash1);
    assertNotEquals(hash3, hash2);

    trie.put(key1, value1).join();
    assertEquals(hash2, trie.rootHash());

    trie.remove(key2).join();
    trie.remove(key3).join();
    assertEquals(hash1, trie.rootHash());
  }
}
