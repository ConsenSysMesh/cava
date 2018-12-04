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

import static com.google.common.base.Preconditions.checkArgument;

import net.consensys.cava.bytes.Bytes;
import net.consensys.cava.units.bigints.UInt256;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

final class BytesSSZReader implements SSZReader {

  private final Bytes content;
  private int index = 0;

  BytesSSZReader(Bytes content) {
    this.content = content;
  }

  @Override
  public Bytes readBytes(int limit) {
    int byteLength = 4;
    ensureBytes(byteLength, () -> "SSZ encoded data is not a byte array");
    int size;
    try {
      size = content.getInt(index);
    } catch (IndexOutOfBoundsException e) {
      throw new EndOfSSZException();
    }
    if (size < 0 || size > limit) {
      throw new InvalidSSZTypeException("length of bytes would exceed limit");
    }
    index += 4;
    if (content.size() - index - size < 0) {
      throw new InvalidSSZTypeException("SSZ encoded data has insufficient bytes for decoded byte array length");
    }
    return consumeBytes(size);
  }

  @Override
  public int readInt(int bitLength) {
    checkArgument(bitLength % 8 == 0, "bitLength must be a multiple of 8");
    int byteLength = bitLength / 8;
    ensureBytes(byteLength, () -> "SSZ encoded data has insufficient length to read a " + bitLength + "-bit integer");
    Bytes bytes = content.slice(index, byteLength);
    int zeroBytes = bytes.numberOfLeadingZeroBytes();
    if ((byteLength - zeroBytes) > 4) {
      throw new InvalidSSZTypeException("decoded integer is too large for an int");
    }
    index += byteLength;
    return bytes.slice(zeroBytes).toInt();
  }

  @Override
  public long readLong(int bitLength) {
    checkArgument(bitLength % 8 == 0, "bitLength must be a multiple of 8");
    int byteLength = bitLength / 8;
    ensureBytes(byteLength, () -> "SSZ encoded data has insufficient length to read a " + bitLength + "-bit integer");
    Bytes bytes = content.slice(index, byteLength);
    int zeroBytes = content.numberOfLeadingZeroBytes();
    if ((byteLength - zeroBytes) > 8) {
      throw new InvalidSSZTypeException("decoded integer is too large for an int");
    }
    index += byteLength;
    return bytes.slice(zeroBytes).toLong();
  }

  @Override
  public BigInteger readBigInteger(int bitLength) {
    checkArgument(bitLength % 8 == 0, "bitLength must be a multiple of 8");
    int byteLength = bitLength / 8;
    ensureBytes(byteLength, () -> "SSZ encoded data has insufficient length to read a " + bitLength + "-bit integer");
    return consumeBytes(byteLength).toBigInteger();
  }

  @Override
  public BigInteger readUnsignedBigInteger(int bitLength) {
    checkArgument(bitLength % 8 == 0, "bitLength must be a multiple of 8");
    int byteLength = bitLength / 8;
    ensureBytes(byteLength, () -> "SSZ encoded data has insufficient length to read a " + bitLength + "-bit integer");
    return consumeBytes(byteLength).toUnsignedBigInteger();
  }

  @Override
  public UInt256 readUInt256() {
    ensureBytes(32, () -> "SSZ encoded data has insufficient length to read a 256-bit integer");
    return UInt256.fromBytes(consumeBytes(32));
  }

  @Override
  public Bytes readAddress() {
    ensureBytes(20, () -> "SSZ encoded data has insufficient length to read a 20-byte address");
    return consumeBytes(20);
  }

  @Override
  public Bytes readHash(int hashLength) {
    ensureBytes(hashLength, () -> "SSZ encoded data has insufficient length to read a " + hashLength + "-byte hash");
    return consumeBytes(hashLength);
  }

  @Override
  public List<Bytes> readBytesList(int limit) {
    return readList(() -> readBytes(limit));
  }

  @Override
  public List<String> readStringList(int limit) {
    return readList(() -> readString(limit));
  }

  @Override
  public List<Integer> readIntList(int bitLength) {
    return readList(() -> readInt(bitLength));
  }

  @Override
  public List<Long> readLongIntList(int bitLength) {
    return readList(() -> readLong(bitLength));
  }

  @Override
  public List<BigInteger> readBigIntegerList(int bitLength) {
    return readList(() -> readBigInteger(bitLength));
  }

  @Override
  public List<BigInteger> readUnsignedBigIntegerList(int bitLength) {
    return readList(() -> readUnsignedBigInteger(bitLength));
  }

  @Override
  public List<UInt256> readUInt256List() {
    return readList(this::readUInt256);
  }

  @Override
  public List<Bytes> readAddressList() {
    return readList(this::readAddress);
  }

  @Override
  public List<Bytes> readHashList(int hashLength) {
    return readList(() -> readHash(hashLength));
  }

  @Override
  public List<Boolean> readBooleanList() {
    return readList(this::readBoolean);
  }

  @Override
  public boolean isComplete() {
    return index >= content.size();
  }

  private void ensureBytes(int byteLength, Supplier<String> message) {
    if (index == content.size()) {
      throw new EndOfSSZException();
    }
    if (content.size() - index - byteLength < 0) {
      throw new InvalidSSZTypeException(message.get());
    }
  }

  private Bytes consumeBytes(int size) {
    Bytes bytes = content.slice(index, size);
    index += size;
    return bytes;
  }

  private <T> List<T> readList(Supplier<T> elementSupplier) {
    ensureBytes(4, () -> "SSZ encoded data is not a list");
    int originalIndex = this.index;
    List<T> bytesList;
    try {
      int size = consumeBytes(4).toInt();
      bytesList = new ArrayList<>(size);
      for (int i = 0; i < size; ++i) {
        bytesList.add(elementSupplier.get());
      }
    } catch (Exception e) {
      this.index = originalIndex;
      throw e;
    }
    return bytesList;
  }
}
