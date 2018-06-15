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
package net.consensys.cava.eth.reference;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.junit.jupiter.api.Assertions.assertEquals;

import net.consensys.cava.bytes.Bytes;
import net.consensys.cava.junit.BouncyCastleExtension;
import net.consensys.cava.trie.experimental.MerklePatriciaTrie;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Stream;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

@ExtendWith(BouncyCastleExtension.class)
class MerkleTrieTestSuite {

  private Bytes readFromString(String value) {
    if (value.startsWith("0x")) {
      return Bytes.fromHexString(value);
    } else {
      return Bytes.wrap(value.getBytes(UTF_8));
    }
  }

  @ParameterizedTest(name = "{index}. {0}")
  @MethodSource("readAnyOrderTrieTests")
  @SuppressWarnings({"unchecked", "rawtypes"})
  void testAnyOrderTrieTrees(String name, Map input, String root) throws Exception {
    MerklePatriciaTrie<String> trie = new MerklePatriciaTrie<>((Function<String, Bytes>) this::readFromString);
    for (Object entry : input.entrySet()) {
      Map.Entry keyValue = (Map.Entry) entry;
      trie.putAsync(readFromString((String) keyValue.getKey()), (String) keyValue.getValue()).join();
    }
    assertEquals(Bytes.fromHexString(root), trie.rootHash());
  }

  @ParameterizedTest(name = "{index}. {0}")
  @MethodSource("readTrieTests")
  @SuppressWarnings({"unchecked", "rawtypes"})
  void testTrieTrees(String name, List input, String root) throws Exception {
    MerklePatriciaTrie<String> trie = new MerklePatriciaTrie<>((Function<String, Bytes>) this::readFromString);
    for (Object entry : input) {
      List keyValue = (List) entry;
      trie.putAsync(readFromString((String) keyValue.get(0)), (String) keyValue.get(1)).join();
    }
    assertEquals(Bytes.fromHexString(root), trie.rootHash());
  }

  private static Stream<Arguments> readTrieTests() throws IOException {
    return prepareTests(Paths.get("TrieTests", "trietest.json"));
  }

  private static Stream<Arguments> readAnyOrderTrieTests() throws IOException {
    return prepareTests(Paths.get("TrieTests", "trieanyorder.json"));
  }

  @SuppressWarnings({"unchecked", "rawtypes"})
  private static Stream<Arguments> prepareTests(Path path) throws IOException {
    ObjectMapper mapper = new ObjectMapper();
    Map allTests = mapper.readerFor(Map.class).readValue(findTests(path));
    return allTests.entrySet().stream().map(entry -> {
      String name = (String) ((Map.Entry) entry).getKey();
      return Arguments.of(
          name,
          ((Map) ((Map.Entry) entry).getValue()).get("in"),
          ((Map) ((Map.Entry) entry).getValue()).get("root"));
    });
  }

  private static File findTests(Path testsPath) {
    URL testFolder = MerkleTrieTestSuite.class.getClassLoader().getResource("tests");
    if (testFolder == null) {
      throw new RuntimeException("Tests folder missing. Please run git submodule --init");
    }
    return Paths.get(testFolder.getFile()).resolve(testsPath).toFile();
  }
}
