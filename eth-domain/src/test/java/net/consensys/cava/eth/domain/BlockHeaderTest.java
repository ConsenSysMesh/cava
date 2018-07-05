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

import static net.consensys.cava.eth.domain.TransactionTest.randomBytes;
import static org.junit.jupiter.api.Assertions.assertEquals;

import net.consensys.cava.bytes.Bytes;
import net.consensys.cava.junit.BouncyCastleExtension;
import net.consensys.cava.units.bigints.UInt256;
import net.consensys.cava.units.ethereum.Gas;

import java.time.Instant;
import java.time.temporal.ChronoUnit;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

@ExtendWith(BouncyCastleExtension.class)
class BlockHeaderTest {

  static BlockHeader generateBlockHeader() {
    return new BlockHeader(
        Hash.fromBytes(randomBytes(32)),
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
