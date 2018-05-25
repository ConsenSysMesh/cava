package net.consensys.cava.eth.domain;

import static net.consensys.cava.eth.domain.TransactionTest.randomBytes;
import static org.junit.jupiter.api.Assertions.assertEquals;

import net.consensys.cava.bytes.Bytes;
import net.consensys.cava.units.bigints.UInt256;
import net.consensys.cava.units.ethereum.Gas;

import java.security.Security;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Optional;

import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

class BlockHeaderTest {

  @BeforeAll
  static void loadProvider() {
    Security.addProvider(new BouncyCastleProvider());
  }

  static BlockHeader generateBlockHeader() {
    return new BlockHeader(
        Optional.of(Hash.fromBytes(randomBytes(32))),
        Hash.fromBytes(randomBytes(32)),
        Address.fromBytes(Bytes.fromHexString("0x0102030405060708091011121314151617181920")),
        Hash.fromBytes(randomBytes(32)),
        Hash.fromBytes(randomBytes(32)),
        Hash.fromBytes(randomBytes(32)),
        randomBytes(8),
        UInt256.fromBytes(randomBytes(32)),
        UInt256.fromBytes(randomBytes(32)),
        Gas.valueOf(UInt256.fromBytes(randomBytes(6))),
        Gas.valueOf(UInt256.fromBytes(randomBytes(6))),
        Instant.now().truncatedTo(ChronoUnit.SECONDS),
        randomBytes(22),
        Hash.fromBytes(randomBytes(32)),
        randomBytes(8));
  }

  @Test
  void rlpRoundtrip() {
    BlockHeader blockHeader = generateBlockHeader();
    BlockHeader read = BlockHeader.fromBytes(blockHeader.toBytes());
    assertEquals(blockHeader, read);
  }

}
