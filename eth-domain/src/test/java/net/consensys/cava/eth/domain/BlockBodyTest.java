package net.consensys.cava.eth.domain;

import static net.consensys.cava.eth.domain.BlockHeaderTest.generateBlockHeader;
import static net.consensys.cava.eth.domain.TransactionTest.generateTransaction;
import static org.junit.jupiter.api.Assertions.assertEquals;

import net.consensys.cava.bytes.Bytes;

import java.security.Security;
import java.util.Arrays;

import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

public class BlockBodyTest {

  @BeforeAll
  static void loadProvider() {
    Security.addProvider(new BouncyCastleProvider());
  }

  @Test
  void testRLPRoundtrip() {
    BlockBody blockBody = new BlockBody(
        Arrays.asList(generateTransaction(), generateTransaction(), generateTransaction(), generateTransaction()),
        Arrays.asList(
            generateBlockHeader(),
            generateBlockHeader(),
            generateBlockHeader(),
            generateBlockHeader(),
            generateBlockHeader(),
            generateBlockHeader()));
    Bytes encoded = blockBody.toBytes();
    BlockBody read = BlockBody.fromBytes(encoded);
    assertEquals(blockBody, read);
  }

}
