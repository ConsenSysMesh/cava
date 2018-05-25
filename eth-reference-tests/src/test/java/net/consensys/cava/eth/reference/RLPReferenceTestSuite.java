package net.consensys.cava.eth.reference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import net.consensys.cava.bytes.Bytes;
import net.consensys.cava.rlp.RLP;
import net.consensys.cava.rlp.RLPException;
import net.consensys.cava.rlp.RLPReader;
import net.consensys.cava.rlp.RLPWriter;

import java.io.File;
import java.io.IOException;
import java.math.BigInteger;
import java.net.URL;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

class RLPReferenceTestSuite {
  private static void writePayload(RLPWriter writer, Object in) {
    if (in instanceof String && ((String) in).startsWith("#")) {
      writer.writeBigInteger(new BigInteger(((String) in).substring(1)));
    } else if (in instanceof String) {
      writer.writeString((String) in);
    } else if (in instanceof BigInteger) {
      writer.writeBigInteger((BigInteger) in);
    } else if (in instanceof Integer) {
      writer.writeValue(Bytes.minimalBytes((Integer) in));
    } else if (in instanceof List) {
      writer.writeList((listWriter) -> {
        for (Object elt : (List) in) {
          writePayload(listWriter, elt);
        }
      });
    } else {
      throw new UnsupportedOperationException();
    }
  }

  private static Object readPayload(RLPReader reader, Object in) {
    if (in instanceof List) {
      return reader.readList((listReader, list) -> {
        for (Object elt : ((List) in)) {
          list.add(readPayload(listReader, elt));
        }
      });
    } else if (in instanceof BigInteger) {
      return reader.readBigInteger();
    } else if (in instanceof String) {
      return reader.readString();
    } else if (in instanceof Integer) {
      return reader.readInt();
    } else {
      throw new UnsupportedOperationException();
    }
  }

  @ParameterizedTest(name = "{index}. write {0}")
  @MethodSource("readRLPTests")
  void testWriteRLP(String name, Object in, String out) throws IOException {
    Bytes encoded = RLP.encode((writer) -> writePayload(writer, in));
    assertEquals(Bytes.fromHexString(out), encoded, "Input was of type " + in.getClass());
  }

  @ParameterizedTest(name = "{index}. read {0}")
  @MethodSource("readRLPTests")
  void testReadRLP(String name, Object in, String out) {
    if (in instanceof String && ((String) in).startsWith("#")) {
      in = new BigInteger(((String) in).substring(1));
    }
    Object payload = in;
    Object decoded = RLP.decode(Bytes.fromHexString(out), (reader) -> readPayload(reader, payload));
    assertEquals(in, decoded);
  }

  @ParameterizedTest(name = "{index}. invalid {0}")
  @MethodSource("readInvalidRLPTests")
  void testReadInvalidRLP(String name, Object in, String out) {
    assertThrows(RLPException.class, () -> {
      if ("incorrectLengthInArray".equals(name)) {
        RLP.decodeList(Bytes.fromHexString(out), (reader, list) -> {
        });
      } else {
        RLP.decodeValue(Bytes.fromHexString(out));
      }
    });
  }

  private static Stream<Arguments> readRLPTests() throws IOException {
    return prepareRLPTests(Paths.get("RLPTests", "rlptest.json"));
  }

  private static Stream<Arguments> readInvalidRLPTests() throws IOException {
    return prepareRLPTests(Paths.get("RLPTests", "invalidRLPTest.json"));
  }

  @SuppressWarnings({"unchecked", "rawtypes"})
  private static Stream<Arguments> prepareRLPTests(Path testsPath) throws IOException {
    ObjectMapper mapper = new ObjectMapper();
    Map allTests = mapper.readerFor(Map.class).readValue(findTests(testsPath));
    return allTests.entrySet().stream().map(entry -> {
      String name = (String) ((Map.Entry) entry).getKey();
      return Arguments.of(
          name,
          ((Map) ((Map.Entry) entry).getValue()).get("in"),
          ((Map) ((Map.Entry) entry).getValue()).get("out"));
    });
  }

  private static File findTests(Path testsPath) {
    URL testFolder = RLPReferenceTestSuite.class.getClassLoader().getResource("tests");
    if (testFolder == null) {
      throw new RuntimeException("Tests folder missing. Please run git submodule --init");
    }
    return Paths.get(testFolder.getFile()).resolve(testsPath).toFile();
  }
}
