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

import static java.util.Objects.requireNonNull;
import static net.consensys.cava.ssz.SSZ.encodeByteArray;
import static net.consensys.cava.ssz.SSZ.encodeNumber;

import net.consensys.cava.bytes.Bytes;

import java.util.ArrayDeque;
import java.util.Deque;

final class AccumulatingSSZWriter implements SSZWriter {

  private static final int COMBINE_THRESHOLD = 32;

  private ArrayDeque<byte[]> values = new ArrayDeque<>();

  Deque<byte[]> values() {
    return values;
  }

  @Override
  public void writeSSZ(Bytes value) {
    requireNonNull(value);
    appendBytes(value.toArrayUnsafe());
  }

  @Override
  public void writeValue(Bytes value) {
    requireNonNull(value);
    writeByteArray(value.toArrayUnsafe());
  }

  @Override
  public void writeByteArray(byte[] value) {
    encodeByteArray(value, this::appendBytes);
  }

  @Override
  public void writeByte(byte value) {
    encodeByteArray(new byte[] {value}, this::appendBytes);
  }

  @Override
  public void writeLong(long value) {
    appendBytes(encodeNumber(value));
  }

  @Override
  public void writeLong(long value, int size) {
    appendBytes(encodeNumber(value, size));
  }

  private void appendBytes(byte[] bytes) {
    if (bytes.length < COMBINE_THRESHOLD) {
      if (!values.isEmpty()) {
        byte[] last = values.getLast();
        if (last.length <= (COMBINE_THRESHOLD - bytes.length)) {
          byte[] combined = new byte[last.length + bytes.length];
          System.arraycopy(last, 0, combined, 0, last.length);
          System.arraycopy(bytes, 0, combined, last.length, bytes.length);
          values.pollLast();
          values.add(combined);
          return;
        }
      }
    }
    values.add(bytes);
  }
}
