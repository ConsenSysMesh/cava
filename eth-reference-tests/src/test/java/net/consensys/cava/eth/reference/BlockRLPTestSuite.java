package net.consensys.cava.eth.reference;

import static com.google.common.base.Preconditions.checkNotNull;
import static org.junit.jupiter.api.Assertions.assertEquals;

import net.consensys.cava.bytes.Bytes;
import net.consensys.cava.crypto.SECP256K1.Signature;
import net.consensys.cava.eth.domain.Address;
import net.consensys.cava.eth.domain.Block;
import net.consensys.cava.eth.domain.BlockBody;
import net.consensys.cava.eth.domain.BlockHeader;
import net.consensys.cava.eth.domain.Hash;
import net.consensys.cava.eth.domain.Transaction;
import net.consensys.cava.units.bigints.UInt256;
import net.consensys.cava.units.ethereum.Gas;
import net.consensys.cava.units.ethereum.Wei;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.Security;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.Streams;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

class BlockRLPTestSuite {

  @BeforeAll
  static void loadProvider() {
    Security.addProvider(new BouncyCastleProvider());
  }

  private static ObjectMapper mapper = new ObjectMapper();

  @ParameterizedTest(name = "{index}. block {0}/{1}/{2}[{3}]")
  @MethodSource("readBlockChainTests")
  void testBlockRLP(
      String folder,
      String fileName,
      String name,
      long blockIndex,
      Block block,
      String rlp,
      String hash) {
    Block rlpBlock = Block.fromBytes(Bytes.fromHexString(rlp));
    assertEquals(block, rlpBlock);
    assertEquals(Bytes.fromHexString(rlp), block.toBytes());
    assertEquals(Hash.fromBytes(Bytes.fromHexString(hash)), block.header().hash());
    assertEquals(Hash.fromBytes(Bytes.fromHexString(hash)), rlpBlock.header().hash());
  }

  private static Stream<Arguments> readBlockChainTests() throws IOException {
    URL testFolder = MerkleTrieTestSuite.class.getClassLoader().getResource("tests");
    if (testFolder == null) {
      throw new RuntimeException("Tests folder missing. Please run git submodule --init");
    }
    Path folderPath = Paths.get(testFolder.getFile(), "BlockchainTests");

    List<Arguments> testCases = new ArrayList<>();
    try (Stream<Path> walker = Files.walk(folderPath)) {
      walker.filter(path -> path.toString().endsWith(".json")).forEach(
          file -> testCases.addAll(readTestCase(file).collect(Collectors.toList())));
      testCases.sort(Comparator.comparing(a -> ((String) a.get()[0])));
      return testCases.stream();
    }
  }

  @SuppressWarnings({"unchecked", "rawtypes"})
  private static Stream<Arguments> readTestCase(Path testFile) {
    try {
      Map test = mapper.readerFor(Map.class).readValue(testFile.toFile());
      String name = (String) test.keySet().iterator().next();
      Map testData = (Map) test.get(name);
      List blocks = (List) testData.get("blocks");
      return Streams.mapWithIndex(
          blocks.stream().filter(block -> ((Map) block).containsKey("blockHeader")),
          (block, index) -> Arguments.of(
              testFile.getName(testFile.getNameCount() - 2).toString(),
              testFile.getName(testFile.getNameCount() - 1).toString(),
              name,
              index,
              createBlock((Map) block),
              ((Map) block).get("rlp"),
              ((Map) ((Map) block).get("blockHeader")).get("hash")));
    } catch (IOException e) {
      throw new UncheckedIOException(e);
    }
  }

  @SuppressWarnings({"unchecked", "rawtypes"})
  private static BlockHeader createBlockHeader(Map headerData) {
    checkNotNull(headerData, "" + headerData);
    BlockHeader header = new BlockHeader(
        Optional.of(Hash.fromBytes(Bytes.fromHexString((String) headerData.get("parentHash")))),
        Hash.fromBytes(Bytes.fromHexString((String) headerData.get("uncleHash"))),
        Address.fromBytes(Bytes.fromHexString((String) headerData.get("coinbase"))),
        Hash.fromBytes(Bytes.fromHexString((String) headerData.get("stateRoot"))),
        Hash.fromBytes(Bytes.fromHexString((String) headerData.get("transactionsTrie"))),
        Hash.fromBytes(Bytes.fromHexString((String) headerData.get("receiptTrie"))),
        Bytes.fromHexString((String) headerData.get("bloom")),
        UInt256.fromBytes(Bytes.fromHexString((String) headerData.get("difficulty"))),
        UInt256.fromBytes(Bytes.fromHexString((String) headerData.get("number"))),
        Gas.valueOf(UInt256.fromBytes(Bytes.fromHexString((String) headerData.get("gasLimit")))),
        Gas.valueOf(UInt256.fromBytes(Bytes.fromHexString((String) headerData.get("gasUsed")))),
        Instant.ofEpochSecond(Bytes.fromHexString((String) headerData.get("timestamp")).longValue()),
        Bytes.fromHexString((String) headerData.get("extraData")),
        Hash.fromBytes(Bytes.fromHexString((String) headerData.get("mixHash"))),
        Bytes.fromHexString((String) headerData.get("nonce")));
    return header;
  }

  @SuppressWarnings({"unchecked", "rawtypes"})
  private static Block createBlock(Map blockData) {
    Map headerData = (Map) blockData.get("blockHeader");
    BlockHeader header = createBlockHeader(headerData);
    List<Transaction> transactions = new ArrayList<>();
    for (Object txDataObj : (List) blockData.get("transactions")) {
      Map txData = (Map) txDataObj;
      transactions.add(
          new Transaction(
              UInt256.fromBytes(Bytes.fromHexString((String) txData.get("nonce"))),
              Wei.valueOf(UInt256.fromBytes(Bytes.fromHexString((String) txData.get("gasPrice")))),
              Gas.valueOf(UInt256.fromBytes(Bytes.fromHexString((String) txData.get("gasLimit")))),
              Optional
                  .ofNullable((String) txData.get("to"))
                  .map(Bytes::fromHexString)
                  .filter(bytes -> !bytes.isEmpty())
                  .map(Address::fromBytes),
              Wei.valueOf(UInt256.fromBytes(Bytes.fromHexString((String) txData.get("value")))),
              Bytes.fromHexString((String) txData.get("data")),
              Signature.create(
                  Bytes.fromHexString((String) txData.get("v")).get(0),
                  Bytes.fromHexString((String) txData.get("r")).unsignedBigIntegerValue(),
                  Bytes.fromHexString((String) txData.get("s")).unsignedBigIntegerValue())));
    }
    List<BlockHeader> ommers = new ArrayList<>();
    for (Object ommerDataObj : (List) blockData.get("uncleHeaders")) {
      ommers.add(createBlockHeader((Map) ommerDataObj));
    }

    return new Block(header, new BlockBody(transactions, ommers));
  }
}
