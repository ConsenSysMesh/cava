/*
 * Copyright 2018, ConsenSys Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package net.consensys.cava.rlp

import com.winterbe.expekt.should
import net.consensys.cava.bytes.Bytes
import net.consensys.cava.units.bigints.UInt256
import org.jetbrains.spek.api.Spek
import org.jetbrains.spek.api.dsl.describe
import org.jetbrains.spek.api.dsl.it
import java.math.BigInteger

class RLPWriterSpec : Spek({

  describe("A RLP writer") {

    data class SomeObject(val name: String, val number: Int, val longNumber: BigInteger)

    describe("The RLP Writer") {
      it("should allow to write and read full objects") {
        val bob = SomeObject("Bob", 4, BigInteger.valueOf(1234563434344L))
        val bytes = RLP.encode { writer ->
          writer.writeString(bob.name)
          writer.writeInt(bob.number)
          writer.writeBigInteger(bob.longNumber)
        }
        val readObject: SomeObject = RLP.decode(bytes, { reader ->
          SomeObject(reader.readString(), reader.readInt(), reader.readBigInteger())
        })
        readObject.should.equal(bob)
      }
    }

    describe("Writing integers") {
      it("should write small integers") {
        RLP.encode { writer ->
          writer.writeInt(1000)
        }.should.equal(Bytes.fromHexString("8203e8"))
      }

      it("should write 100000") {
        RLP.encode { writer ->
          writer.writeInt(100000)
        }.should.equal(Bytes.fromHexString("830186a0"))
      }

      it("should write long integers") {
        RLP.encode { writer ->
          writer.writeLong(100000)
        }.should.equal(Bytes.fromHexString("830186a0"))
      }

      it("should write uint256 integers") {
        RLP.encode { writer ->
          writer.writeUInt256(UInt256.valueOf(100000))
        }.should.equal(Bytes.fromHexString("830186a0"))
      }

      it("should write big uint256 integers") {
        RLP.encode { writer ->
          writer.writeUInt256(
            UInt256.fromHexString("0x0400000000000000000000000000000000000000000000000000f100000000ab"))
        }.should.equal(Bytes.fromHexString("a00400000000000000000000000000000000000000000000000000f100000000ab"))
      }

      it("should write big integers") {
        RLP.encode { writer ->
          writer.writeBigInteger(BigInteger.valueOf(100000))
        }.should.equal(Bytes.fromHexString("830186a0"))
      }

      it("should write big integers - very big ones") {
        RLP.encode { writer ->
          writer.writeBigInteger(BigInteger.valueOf(127).pow(16))
        }.should.equal(Bytes.fromHexString("8ee1ceefa5bbd9ed1c97f17a1df801"))
      }
    }

    describe("Writing strings") {
      it("should write empty strings") {
        RLP.encode { writer ->
          writer.writeString("")
        }.should.equal(Bytes.fromHexString("80"))
      }

      it("should write dog") {
        RLP.encode { writer ->
          writer.writeString("dog")
        }.should.equal(Bytes.fromHexString("83646f67"))
      }

      it("should write one character long strings") {
        RLP.encode { writer ->
          writer.writeString("d")
        }.should.equal(Bytes.fromHexString("64"))
      }
    }

    describe("Writing lists") {
      it("should write a short list") {
        val values = listOf("asdf", "qwer", "zxcv", "asdf", "qwer", "zxcv", "asdf", "qwer", "zxcv", "asdf", "qwer")
        RLP.encodeList { listWriter ->
          values.forEach { value ->
            listWriter.writeString(value)
          }
        /* ktlint-disable max-line-length */
        }.should.equal(Bytes.fromHexString("f784617364668471776572847a78637684617364668471776572847a78637684617364668471776572847a78637684617364668471776572"))
        /* ktlint-enable max-line-length */
      }

      it("should write nested lists") {
        val bytes = RLP.encodeList { listWriter ->
          listWriter.writeString("asdf")
          listWriter.writeString("qwer")
          for (i in 0..30) {
            listWriter.writeList { subListWriter ->
              subListWriter.writeString("zxcv")
              subListWriter.writeString("asdf")
              subListWriter.writeString("qwer")
            }
          }
        }

        RLP.decodeList(bytes, { listReader ->
          listReader.readString().should.equal("asdf")
          listReader.readString().should.equal("qwer")
          for (i in 0..30) {
            listReader.readList { subListReader ->
              subListReader.readString().should.equal("zxcv")
              subListReader.readString().should.equal("asdf")
              subListReader.readString().should.equal("qwer")
            }
          }
        })
      }

      it("should write previously encoded values") {
        val output = RLP.encode { it.writeRLP(RLP.encodeByteArray("abc".toByteArray())) }
        RLP.decodeString(output).should.equal("abc")
      }
    }
  }
})
