/*
 * Copyright 2018, ConsenSys Inc.
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
package net.consensys.cava.rlp;

import static java.util.Objects.requireNonNull;

import net.consensys.cava.bytes.Bytes;

import java.nio.ByteBuffer;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.function.Consumer;

final class BytesValueRLPWriter implements RLPWriter {

  private static final byte[] EMPTY_VALUE = new byte[] {(byte) 0x80};
  private static final int COMBINE_THRESHOLD = 32;

  private ArrayDeque<byte[]> values = new ArrayDeque<>();

  Bytes toBytes() {
    if (values.isEmpty()) {
      return Bytes.EMPTY;
    }
    return Bytes.wrap(values.stream().map(Bytes::wrap).toArray(Bytes[]::new));
  }

  @Override
  public void writeRLP(Bytes value) {
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

  static Bytes encodeValue(byte[] value) {
    int maxSize = value.length + 5;
    ByteBuffer buffer = ByteBuffer.allocate(maxSize);
    encodeByteArray(value, buffer::put);
    return Bytes.wrap(buffer.array(), 0, buffer.position());
  }

  private static void encodeByteArray(byte[] value, Consumer<byte[]> appender) {
    requireNonNull(value);
    int size = value.length;
    if (size == 0) {
      appender.accept(EMPTY_VALUE);
      return;
    }
    if (size == 1) {
      byte b = value[0];
      if ((b & 0xFF) <= 0x7f) {
        appender.accept(value);
        return;
      }
    }
    appender.accept(encodeLength(size, 0x80));
    appender.accept(value);
  }

  @Override
  public void writeLong(long value) {
    appendBytes(encodeNumber(value));
  }

  static Bytes encodeLong(long value) {
    return Bytes.wrap(encodeNumber(value));
  }

  @Override
  public void writeList(Consumer<RLPWriter> fn) {
    requireNonNull(fn);
    BytesValueRLPWriter listWriter = new BytesValueRLPWriter();
    fn.accept(listWriter);
    writeEncodedValuesAsList(listWriter.values);
  }

  private void writeEncodedValuesAsList(Deque<byte[]> values) {
    int totalSize = 0;
    for (byte[] value : values) {
      try {
        totalSize = Math.addExact(totalSize, value.length);
      } catch (ArithmeticException e) {
        throw new IllegalArgumentException("Combined length of values is too long (> Integer.MAX_VALUE)");
      }
    }
    appendBytes(encodeLength(totalSize, 0xc0));
    this.values.addAll(values);
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

  private static byte[] encodeNumber(long value) {
    if (value <= 0x7f) {
      return new byte[] {(byte) (value & 0xFF)};
    }
    return encodeLongBytes(value, 0x80);
  }

  private static byte[] encodeLength(int length, int offset) {
    if (length <= 55) {
      return new byte[] {(byte) ((offset + length) & 0xFF)};
    }
    return encodeLongBytes(length, offset + 55);
  }

  private static byte[] encodeLongBytes(long value, int offset) {
    int zeros = Long.numberOfLeadingZeros(value);
    int resultBytes = 8 - (zeros / 8);

    byte[] encoded = new byte[resultBytes + 1];
    encoded[0] = (byte) ((offset + resultBytes) & 0xFF);

    int shift = 0;
    for (int i = 0; i < resultBytes; i++) {
      encoded[resultBytes - i] = (byte) ((value >> shift) & 0xFF);
      shift += 8;
    }
    return encoded;
  }
}
