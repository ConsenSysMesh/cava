package net.consensys.cava.rlp

import com.winterbe.expekt.should
import net.consensys.cava.bytes.Bytes
import net.consensys.cava.bytes.Bytes.fromHexString
import org.jetbrains.spek.api.Spek
import org.jetbrains.spek.api.dsl.describe
import org.jetbrains.spek.api.dsl.it
import org.junit.jupiter.api.assertThrows
import java.math.BigInteger

/* ktlint-disable max-line-length */
val SHORT_LIST =
  fromHexString("f784617364668471776572847a78637684617364668471776572847a78637684617364668471776572847a78637684617364668471776572")
val LONG_LIST =
  fromHexString("f90200cf84617364668471776572847a786376cf84617364668471776572847a786376cf84617364668471776572847a786376cf84617364668471776572847a786376cf84617364668471776572847a786376cf84617364668471776572847a786376cf84617364668471776572847a786376cf84617364668471776572847a786376cf84617364668471776572847a786376cf84617364668471776572847a786376cf84617364668471776572847a786376cf84617364668471776572847a786376cf84617364668471776572847a786376cf84617364668471776572847a786376cf84617364668471776572847a786376cf84617364668471776572847a786376cf84617364668471776572847a786376cf84617364668471776572847a786376cf84617364668471776572847a786376cf84617364668471776572847a786376cf84617364668471776572847a786376cf84617364668471776572847a786376cf84617364668471776572847a786376cf84617364668471776572847a786376cf84617364668471776572847a786376cf84617364668471776572847a786376cf84617364668471776572847a786376cf84617364668471776572847a786376cf84617364668471776572847a786376cf84617364668471776572847a786376cf84617364668471776572847a786376cf84617364668471776572847a786376")
/* ktlint-disable max-line-length */

class RLPReaderSpec : Spek({

  describe("A RLP reader") {

    data class SomeObject(val name: String, val number: Int, val longNumber: BigInteger)

    it("should allow to parse full objects") {
      val bytes = Bytes.fromHexString("83426f620486011F71B70768")
      val readObject: SomeObject = RLP.decode(bytes, { reader ->
        SomeObject(reader.readString(), reader.readInt(), reader.readBigInteger())
      })

      readObject.name.should.equal("Bob")
      readObject.number.should.equal(4)
      readObject.longNumber.should.equal(BigInteger.valueOf(1234563434344L))
    }

    describe("Reading integers") {
      it("should read zero") {
        RLP.decode(fromHexString("80"), { reader ->
          reader.readInt().should.equal(0)
        })
      }

      it("should read one") {
        RLP.decode(fromHexString("01"), { reader ->
          reader.readInt().should.equal(1)
        })
      }

      it("should read 16") {
        RLP.decode(fromHexString("10"), { reader ->
          reader.readInt().should.equal(16)
        })
      }

      it("should read 79") {
        RLP.decode(fromHexString("4f"), { reader ->
          reader.readInt().should.equal(79)
        })
      }

      it("should read 127") {
        RLP.decode(fromHexString("7f"), { reader ->
          reader.readInt().should.equal(127)
        })
      }

      it("should read 128") {
        RLP.decode(fromHexString("8180"), { reader ->
          reader.readInt().should.equal(128)
        })
      }

      it("should read 1000") {
        RLP.decode(fromHexString("8203e8"), { reader ->
          reader.readInt().should.equal(1000)
        })
      }

      it("should read 10000") {
        RLP.decode(fromHexString("830186a0"), { reader ->
          reader.readInt().should.equal(100000)
        })
      }

      it("should throw when input exhausted") {
        assertThrows<EndOfRLPException> {
          RLP.decode(Bytes.EMPTY, { reader ->
            reader.readInt()
          })
        }
      }

      it("should throw when next item is a list") {
        val exception = assertThrows<InvalidRLPTypeException> {
          RLP.decode(SHORT_LIST, { reader ->
            reader.readInt()
          })
        }
        exception.message.should.equal("Attempted to read a value but next item is a list")
      }

      it("should throw when source is truncated") {
        val exception = assertThrows<InvalidRLPEncodingException> {
          RLP.decode(fromHexString("830186"), { reader ->
            reader.readInt()
          })
        }
        exception.message.should.equal("Insufficient bytes in RLP encoding: expected 3 but have only 2")
      }
    }

    describe("Reading strings") {
      it("should read empty strings") {
        RLP.decodeString(fromHexString("80")).should.equal("")
      }

      it("should read one byte long strings") {
        RLP.decodeString(fromHexString("00")).should.equal("\u0000")
        RLP.decodeString(fromHexString("01")).should.equal("\u0001")
        RLP.decodeString(fromHexString("7f")).should.equal("\u007F")
      }

      it("should read short strings") {
        RLP.decodeString(fromHexString("83646f67")).should.equal("dog")
      }

      it("should read long strings") {
        val content1 =
          fromHexString("b74c6f72656d20697073756d20646f6c6f722073697420616d65742c20636f6e7365637465747572206164697069736963696e6720656c69")
        RLP.decodeString(content1).should.equal(
          "Lorem ipsum dolor sit amet, consectetur adipisicing eli"
        )
        val content2 =
          fromHexString("b8384c6f72656d20697073756d20646f6c6f722073697420616d65742c20636f6e7365637465747572206164697069736963696e6720656c6974")
        RLP.decodeString(content2).should.equal(
          "Lorem ipsum dolor sit amet, consectetur adipisicing elit"
        )
        val content33 =
          fromHexString("b904004c6f72656d20697073756d20646f6c6f722073697420616d65742c20636f6e73656374657475722061646970697363696e6720656c69742e20437572616269747572206d6175726973206d61676e612c20737573636970697420736564207665686963756c61206e6f6e2c20696163756c697320666175636962757320746f72746f722e2050726f696e20737573636970697420756c74726963696573206d616c6573756164612e204475697320746f72746f7220656c69742c2064696374756d2071756973207472697374697175652065752c20756c7472696365732061742072697375732e204d6f72626920612065737420696d70657264696574206d6920756c6c616d636f7270657220616c6971756574207375736369706974206e6563206c6f72656d2e2041656e65616e2071756973206c656f206d6f6c6c69732c2076756c70757461746520656c6974207661726975732c20636f6e73657175617420656e696d2e204e756c6c6120756c74726963657320747572706973206a7573746f2c20657420706f73756572652075726e6120636f6e7365637465747572206e65632e2050726f696e206e6f6e20636f6e76616c6c6973206d657475732e20446f6e65632074656d706f7220697073756d20696e206d617572697320636f6e67756520736f6c6c696369747564696e2e20566573746962756c756d20616e746520697073756d207072696d697320696e206661756369627573206f726369206c756374757320657420756c74726963657320706f737565726520637562696c69612043757261653b2053757370656e646973736520636f6e76616c6c69732073656d2076656c206d617373612066617563696275732c2065676574206c6163696e6961206c616375732074656d706f722e204e756c6c61207175697320756c747269636965732070757275732e2050726f696e20617563746f722072686f6e637573206e69626820636f6e64696d656e74756d206d6f6c6c69732e20416c697175616d20636f6e73657175617420656e696d206174206d65747573206c75637475732c206120656c656966656e6420707572757320656765737461732e20437572616269747572206174206e696268206d657475732e204e616d20626962656e64756d2c206e6571756520617420617563746f72207472697374697175652c206c6f72656d206c696265726f20616c697175657420617263752c206e6f6e20696e74657264756d2074656c6c7573206c65637475732073697420616d65742065726f732e20437261732072686f6e6375732c206d65747573206163206f726e617265206375727375732c20646f6c6f72206a7573746f20756c747269636573206d657475732c20617420756c6c616d636f7270657220766f6c7574706174")
        RLP.decodeString(content33).should.equal(
          "Lorem ipsum dolor sit amet, consectetur adipiscing elit. Curabitur mauris magna, suscipit sed vehicula non, iaculis faucibus tortor. Proin suscipit ultricies malesuada. Duis tortor elit, dictum quis tristique eu, ultrices at risus. Morbi a est imperdiet mi ullamcorper aliquet suscipit nec lorem. Aenean quis leo mollis, vulputate elit varius, consequat enim. Nulla ultrices turpis justo, et posuere urna consectetur nec. Proin non convallis metus. Donec tempor ipsum in mauris congue sollicitudin. Vestibulum ante ipsum primis in faucibus orci luctus et ultrices posuere cubilia Curae; Suspendisse convallis sem vel massa faucibus, eget lacinia lacus tempor. Nulla quis ultricies purus. Proin auctor rhoncus nibh condimentum mollis. Aliquam consequat enim at metus luctus, a eleifend purus egestas. Curabitur at nibh metus. Nam bibendum, neque at auctor tristique, lorem libero aliquet arcu, non interdum tellus lectus sit amet eros. Cras rhoncus, metus ac ornare cursus, dolor justo ultrices metus, at ullamcorper volutpat"
        )
      }

      it("should throw when input exhausted") {
        assertThrows<EndOfRLPException> {
          RLP.decode(Bytes.EMPTY, { reader ->
            reader.readString()
          })
        }
      }

      it("should throw when next item is a list") {
        val exception = assertThrows<InvalidRLPTypeException> {
          RLP.decode(SHORT_LIST, { reader ->
            reader.readString()
          })
        }
        exception.message.should.equal("Attempted to read a value but next item is a list")
      }

      it("should throw when source is truncated") {
        val exception = assertThrows<InvalidRLPEncodingException> {
          RLP.decode(fromHexString("830186"), { reader ->
            reader.readString()
          })
        }
        exception.message.should.equal("Insufficient bytes in RLP encoding: expected 3 but have only 2")
      }

      it("should throw when the value length contains leading zero bytes") {
        val content2 =
          fromHexString("b900384c6f72656d20697073756d20646f6c6f722073697420616d65742c20636f6e7365637465747572206164697069736963696e6720656c6974")
        assertThrows<InvalidRLPEncodingException> { RLP.decodeString(content2) }
      }
    }

    describe("Reading lists") {
      it("should read a short list") {
        RLP.decodeList(SHORT_LIST, { subReader, list ->
          subReader.remaining().should.equal(11)
          for (i in 1..11) {
            list.add(subReader.readString())
          }
        }).should.equal(listOf("asdf", "qwer", "zxcv", "asdf", "qwer", "zxcv", "asdf", "qwer", "zxcv", "asdf", "qwer"))
      }

      it("should read a long list") {
        val expected = mutableListOf<List<Any>>()
        for (i in 1..31) {
          expected.add(listOf("asdf", "qwer", "zxcv"))
        }

        RLP.decodeList(LONG_LIST, { subReader, list ->
          for (i in 1..31) {
            list.add(subReader.readList({ subSubReader, subList ->
              subList.add(subSubReader.readString())
              subList.add(subSubReader.readString())
              subList.add(subSubReader.readString())
            }))
          }
        }).should.equal(expected)
      }

      it("should throw an exception if there are zero bytes in the list length") {
        assertThrows<InvalidRLPEncodingException> { RLP.decodeList(Bytes.fromHexString("0xf9000112"), { _, _ -> }) }
      }
    }
  }
})
