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

import net.consensys.cava.bytes.Bytes;
import net.consensys.cava.units.bigints.UInt256;

import java.math.BigInteger;
import java.util.function.Consumer;

import com.google.common.base.Charsets;

/**
 * A writer for encoding values to RLP.
 */
public interface RLPWriter {

  /**
   * Append an already RLP encoded value.
   *
   * <p>
   * Note that this method <b>may not</b> validate that {@code value} is a valid RLP sequence. Appending an invalid RLP
   * sequence will cause the entire RLP encoding produced by this writer to also be invalid.
   *
   * @param value The RLP encoded bytes to append.
   */
  void writeRLP(Bytes value);

  /**
   * Encode a {@link Bytes} value to RLP.
   *
   * @param value The byte array to encode.
   */
  void writeValue(Bytes value);

  /**
   * Encode a byte array to RLP.
   *
   * @param value The byte array to encode.
   */
  default void writeByteArray(byte[] value) {
    writeValue(Bytes.wrap(value));
  }

  /**
   * Write an integer to the output.
   *
   * @param value The integer to write.
   */
  default void writeInt(int value) {
    writeLong(value);
  }

  /**
   * Write a long to the output.
   *
   * @param value The long value to write.
   */
  void writeLong(long value);

  /**
   * Write a {@link UInt256} to the output.
   *
   * @param value The {@link UInt256} value to write.
   */
  default void writeUInt256(UInt256 value) {
    writeValue(value.toMinimalBytes());
  }

  /**
   * Write a big integer to the output.
   *
   * @param value The integer to write.
   */
  default void writeBigInteger(BigInteger value) {
    if (value.signum() == 0) {
      writeInt(0);
      return;
    }
    byte[] byteArray = value.toByteArray();
    if (byteArray[0] == 0) {
      writeValue(Bytes.wrap(byteArray).slice(1));
    } else {
      writeByteArray(byteArray);
    }
  }

  /**
   * Write a string to the output.
   *
   * @param str The string to write.
   */
  default void writeString(String str) {
    writeByteArray(str.getBytes(Charsets.UTF_8));
  }

  /**
   * Write a list of values.
   *
   * @param fn A consumer that will be provided with a {@link RLPWriter} that can consume values.
   */
  void writeList(Consumer<RLPWriter> fn);
}
