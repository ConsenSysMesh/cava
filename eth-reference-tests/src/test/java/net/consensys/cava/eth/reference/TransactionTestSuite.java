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
package net.consensys.cava.eth.reference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import net.consensys.cava.bytes.Bytes;
import net.consensys.cava.eth.domain.Address;
import net.consensys.cava.eth.domain.Transaction;
import net.consensys.cava.junit.BouncyCastleExtension;

import java.io.File;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.net.URL;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

@ExtendWith(BouncyCastleExtension.class)
class TransactionTestSuite {

  private static ObjectMapper mapper = new ObjectMapper();

  @ParameterizedTest(name = "{index}. tx {0}/{1}/{2}")
  @MethodSource("readTransactionTests")
  void testTransaction(String folder, String name, String milestone, String rlp, String hash, String sender) {
    if (hash == null && sender == null) {
      assertThrows(Throwable.class, () -> {
        Transaction tx = Transaction.fromBytes(Bytes.fromHexString(rlp));
        tx.sender();
      });
    } else {
      Bytes rlpBytes = Bytes.fromHexString(rlp);
      Transaction tx = Transaction.fromBytes(rlpBytes);
      assertEquals(Address.fromBytes(Bytes.fromHexString(sender)), tx.sender());
      assertEquals(rlpBytes, tx.toBytes());
      assertEquals(Bytes.fromHexString(hash), tx.hash().toBytes());
    }
  }

  private static Stream<Arguments> readTransactionTests() throws IOException {
    URL testFolder = MerkleTrieTestSuite.class.getClassLoader().getResource("tests");
    if (testFolder == null) {
      throw new RuntimeException("Tests folder missing. Please run git submodule --init");
    }
    Path folderPath = Paths.get(testFolder.getFile(), "TransactionTests");
    List<Arguments> tests = Arrays
        .stream(folderPath.toFile().listFiles(File::isDirectory))
        .map(folder -> folder.listFiles((file) -> file.getName().endsWith(".json")))
        .collect(ArrayList::new, (list, files) -> {
          for (File f : files) {
            if (!f.getName().contains("GasLimitOverflow")
                && !f.getName().contains("GasLimitxPriceOverflow")
                && !f.getName().contains("NotEnoughGas")
                && !f.getName().contains("NotEnoughGAS")
                && !f.getName().contains("EmptyTransaction")) {
              list.addAll(readTestCase(f));
            }
          }
        }, List::addAll);
    tests.sort(Comparator.comparing(a -> ((String) a.get()[0])));
    return tests.stream();
  }

  @SuppressWarnings({"unchecked", "rawtypes"})
  private static List<Arguments> readTestCase(File testFile) {
    try {
      Map test = mapper.readerFor(Map.class).readValue(testFile);
      String name = (String) test.keySet().iterator().next();
      Map testData = (Map) test.get(name);
      String rlp = (String) testData.get("rlp");
      List<Arguments> arguments = new ArrayList<>();
      for (String milestone : new String[] {
          //          "Byzantium",
          //          "Constantinople",
          //          "EIP150",
          //          "EIP158",
          "Frontier",
          "Homestead"}) {
        Map milestoneData = (Map) testData.get(milestone);
        if (!milestoneData.isEmpty()) {
          arguments.add(
              Arguments.of(
                  testFile.getParentFile().getName(),
                  name,
                  milestone,
                  rlp,
                  milestoneData.get("hash"),
                  milestoneData.get("sender")));
        }
      }
      if (arguments.isEmpty()) {
        arguments.add(Arguments.of(testFile.getParentFile().getName(), name, "(no milestone)", rlp, null, null));
      }

      return arguments;
    } catch (IOException e) {
      throw new UncheckedIOException(e);
    }
  }
}
