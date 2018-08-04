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
package net.consensys.cava.eth.domain;

import static org.junit.jupiter.api.Assertions.assertEquals;

import net.consensys.cava.bytes.Bytes;
import net.consensys.cava.crypto.SECP256K1.Signature;
import net.consensys.cava.junit.BouncyCastleExtension;
import net.consensys.cava.units.bigints.UInt256;
import net.consensys.cava.units.ethereum.Gas;
import net.consensys.cava.units.ethereum.Wei;

import java.math.BigInteger;
import java.security.SecureRandom;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

@ExtendWith(BouncyCastleExtension.class)
class TransactionTest {

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
        Address.fromBytes(Bytes.fromHexString("0x0102030405060708091011121314151617181920")),
        Wei.valueOf(10L),
        Bytes.of(1, 2, 3, 4),
        Signature.fromBytes(randomBytes(65)));
  }

  @Test
  void testRLPRoundtrip() {
    Transaction tx = generateTransaction();
    Bytes encoded = tx.toBytes();
    Transaction read = Transaction.fromBytes(encoded);
    assertEquals(tx, read);
  }
}
