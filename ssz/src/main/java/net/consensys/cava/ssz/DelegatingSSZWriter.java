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

import net.consensys.cava.bytes.Bytes;
import net.consensys.cava.units.bigints.UInt256;

import java.math.BigInteger;

class DelegatingSSZWriter<T extends AccumulatingSSZWriter> implements SSZWriter {

  T delegate;

  DelegatingSSZWriter(T delegate) {
    this.delegate = delegate;
  }

  @Override
  public void writeSSZ(Bytes value) {
    delegate.writeSSZ(value);
  }

  @Override
  public void writeValue(Bytes value) {
    delegate.writeValue(value);
  }

  @Override
  public void writeByteArray(byte[] value) {
    delegate.writeByteArray(value);
  }

  @Override
  public void writeByte(byte value) {
    delegate.writeByte(value);
  }

  @Override
  public void writeInt(int value) {
    delegate.writeInt(value);
  }

  @Override
  public void writeInt(int value, int size) {
    delegate.writeInt(value, size);
  }

  @Override
  public void writeLong(long value) {
    delegate.writeLong(value);
  }

  @Override
  public void writeLong(long value, int size) {
    delegate.writeLong(value, size);
  }

  @Override
  public void writeUInt256(UInt256 value) {
    delegate.writeUInt256(value);
  }

  @Override
  public void writeBigInteger(BigInteger value) {
    delegate.writeBigInteger(value);
  }

  @Override
  public void writeBigInteger(BigInteger value, int size) {
    delegate.writeBigInteger(value, size);
  }

  @Override
  public void writeString(String str) {
    delegate.writeString(str);
  }

  @Override
  public void writeList(String... elements) {
    delegate.writeList(elements);

  }

  @Override
  public void writeList(int size, int... elements) {
    delegate.writeList(size, elements);
  }

  @Override
  public void writeList(int size, long... elements) {
    delegate.writeList(size, elements);
  }

  @Override
  public void writeList(int size, BigInteger... elements) {
    delegate.writeList(size, elements);
  }

  @Override
  public void writeList(boolean... elements) {
    delegate.writeList(elements);
  }

  @Override
  public void writeList(UInt256... elements) {
    delegate.writeList(elements);
  }

  @Override
  public void writeListOfAddresses(Bytes... addresses) {
    delegate.writeListOfAddresses(addresses);
  }

  @Override
  public void writeListOfHashes(Bytes... hashes) {
    delegate.writeListOfHashes(hashes);
  }

  @Override
  public void writeAddress(Bytes address) {
    delegate.writeAddress(address);
  }

  @Override
  public void writeHash(Bytes hash) {
    delegate.writeHash(hash);
  }

  @Override
  public void writeBoolean(Boolean bool) {
    delegate.writeBoolean(bool);
  }


}
