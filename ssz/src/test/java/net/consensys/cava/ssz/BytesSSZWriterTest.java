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
package net.consensys.cava.ssz;

import static java.nio.charset.StandardCharsets.UTF_8;
import static net.consensys.cava.bytes.Bytes.fromHexString;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import net.consensys.cava.bytes.Bytes;
import net.consensys.cava.units.bigints.UInt256;

import java.math.BigInteger;

import com.google.common.base.Charsets;
import org.junit.jupiter.api.Test;

class BytesSSZWriterTest {

  private static class SomeObject {
    private final String name;
    private final int number;
    private final BigInteger longNumber;

    SomeObject(String name, int number, BigInteger longNumber) {
      this.name = name;
      this.number = number;
      this.longNumber = longNumber;
    }
  }

  @Test
  void shouldWriteFullObjects() {
    SomeObject bob = new SomeObject("Bob", 4, BigInteger.valueOf(1234563434344L));
    Bytes bytes = SSZ.encode(writer -> {
      writer.writeString(bob.name);
      writer.writeInt(bob.number, 1);
      writer.writeBigInteger(bob.longNumber, 32);
    });

    assertTrue(SSZ.<Boolean>decode(bytes, reader -> {
      assertEquals("Bob", reader.readString());
      assertEquals(4, reader.readInt(1));
      assertEquals(BigInteger.valueOf(1234563434344L), reader.readBigInteger(32));
      return true;
    }));
  }

  @Test
  void shouldWriteSmallIntegers() {
    assertEquals(fromHexString("00"), SSZ.encode(writer -> writer.writeInt(0)));
    assertEquals(fromHexString("01"), SSZ.encode(writer -> writer.writeInt(1)));
    assertEquals(fromHexString("0f"), SSZ.encode(writer -> writer.writeInt(15)));
    assertEquals(fromHexString("03e8"), SSZ.encode(writer -> writer.writeInt(1000)));
    assertEquals(fromHexString("0400"), SSZ.encode(writer -> writer.writeInt(1024)));
    assertEquals(fromHexString("0186A0"), SSZ.encode(writer -> writer.writeInt(100000)));
  }

  @Test
  void shouldTakeFixedSizes() {
    assertEquals(fromHexString("00"), SSZ.encode(writer -> writer.writeInt(0, 1)));
    assertEquals(fromHexString("0000"), SSZ.encode(writer -> writer.writeInt(0, 2)));
    assertEquals(fromHexString("00000000"), SSZ.encode(writer -> writer.writeInt(0, 4)));
  }

  @Test
  void shouldWriteLongIntegers() {
    assertEquals(fromHexString("00"), SSZ.encode(writer -> writer.writeLong(0L)));
    assertEquals(fromHexString("01"), SSZ.encode(writer -> writer.writeLong(1)));
    assertEquals(fromHexString("0f"), SSZ.encode(writer -> writer.writeLong(15)));
    assertEquals(fromHexString("03e8"), SSZ.encode(writer -> writer.writeLong(1000)));
    assertEquals(fromHexString("0400"), SSZ.encode(writer -> writer.writeLong(1024)));
    assertEquals(fromHexString("0186A0"), SSZ.encode(writer -> writer.writeLong(100000L)));
  }

  @Test
  void shouldWriteUInt256Integers() {
    assertEquals(
        fromHexString("0000000000000000000000000000000000000000000000000000000000000000"),
        SSZ.encode(writer -> writer.writeUInt256(UInt256.valueOf(0L))));
    assertEquals(
        fromHexString("00000000000000000000000000000000000000000000000000000000000186a0"),
        SSZ.encode(writer -> writer.writeUInt256(UInt256.valueOf(100000L))));
    assertEquals(
        fromHexString("0400000000000000000000000000000000000000000000000000f100000000ab"),
        SSZ.encode(
            writer -> writer.writeUInt256(
                UInt256.fromHexString("0x0400000000000000000000000000000000000000000000000000f100000000ab"))));
  }

  @Test
  void shouldWriteBigIntegers() {
    assertEquals(fromHexString("0186A0"), SSZ.encode(writer -> writer.writeBigInteger(BigInteger.valueOf(100000))));
    assertEquals(
        fromHexString("e1ceefa5bbd9ed1c97f17a1df801"),
        SSZ.encode(writer -> writer.writeBigInteger(BigInteger.valueOf(127).pow(16))));
  }

  @Test
  void shouldWriteEmptyStrings() {
    assertEquals(fromHexString("00000000"), SSZ.encode(writer -> writer.writeString("")));
  }

  @Test
  void shouldWriteBooleans() {
    assertEquals(fromHexString("00"), SSZ.encode(writer -> writer.writeBoolean(false)));
    assertEquals(fromHexString("01"), SSZ.encode(writer -> writer.writeBoolean(true)));
  }

  @Test
  void shouldWriteOneCharactersStrings() {
    assertEquals(fromHexString("0000000164"), SSZ.encode(writer -> writer.writeString("d")));
  }

  @Test
  void shouldWriteStrings() {
    assertEquals(fromHexString("00000003646f67"), SSZ.encode(writer -> writer.writeString("dog")));
  }

  @Test
  void shouldWriteAddresses() {
    assertEquals(
        fromHexString("8ee1ceefa5bbd9ed1c978ee1ceefa5bbd9ed1c97"),
        SSZ.encode(writer -> writer.writeAddress(Bytes.fromHexString("8ee1ceefa5bbd9ed1c978ee1ceefa5bbd9ed1c97"))));
    assertThrows(
        IllegalArgumentException.class,
        () -> SSZ.encode(writer -> writer.writeAddress(Bytes.fromHexString("beef"))));
  }

  @Test
  void shouldWriteLists() {
    assertEquals(fromHexString("00000003030405"), SSZ.encodeList(1, 3, 4, 5));
  }

  @Test
  void shouldWriteListsOfStrings() {
    assertEquals(
        fromHexString("0000000300000003626F62000000046A616E65000000056A616E6574"),
        SSZ.encodeList("bob", "jane", "janet"));
  }

  @Test
  void shouldWriteListsOfBytes() {
    assertEquals(
        fromHexString("0000000300000003626F62000000046A616E65000000056A616E6574"),
        SSZ.encodeList(
            Bytes.wrap("bob".getBytes(Charsets.UTF_8)),
            Bytes.wrap("jane".getBytes(Charsets.UTF_8)),
            Bytes.wrap("janet".getBytes(Charsets.UTF_8))));
  }

  @Test
  void shouldWritePreviouslyEncodedValues() {
    Bytes output = SSZ.encode(writer -> writer.writeSSZ(SSZ.encodeByteArray("abc".getBytes(UTF_8))));
    assertEquals("abc", SSZ.decodeString(output));
  }
}
