package net.consensys.cava.eth.domain;

import static org.junit.jupiter.api.Assertions.assertEquals;

import net.consensys.cava.bytes.Bytes;
import net.consensys.cava.crypto.SECP256K1.Signature;
import net.consensys.cava.units.bigints.UInt256;
import net.consensys.cava.units.ethereum.Gas;
import net.consensys.cava.units.ethereum.Wei;

import java.math.BigInteger;
import java.security.SecureRandom;
import java.security.Security;
import java.util.Optional;

import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

class TransactionTest {

  @BeforeAll
  static void loadProvider() {
    Security.addProvider(new BouncyCastleProvider());
  }

  static Bytes randomBytes(int length) {
    SecureRandom random = new SecureRandom();
    byte[] bytes = new byte[length];
    random.nextBytes(bytes);
    return Bytes.wrap(bytes);
  }

  static Transaction generateTransaction() {
    return new Transaction(
        UInt256.valueOf(0),
        Wei.valueOf(BigInteger.valueOf(5L)),
        Gas.valueOf(10L),
        Optional.of(Address.fromBytes(Bytes.fromHexString("0x0102030405060708091011121314151617181920"))),
        Wei.valueOf(10L),
        Bytes.of(1, 2, 3, 4),
        Signature.create(randomBytes(65)));
  }

  @Test
  void testRLPRoundtrip() {
    Transaction tx = generateTransaction();
    Bytes encoded = tx.toBytes();
    Transaction read = Transaction.fromBytes(encoded);
    assertEquals(tx, read);
  }
}
