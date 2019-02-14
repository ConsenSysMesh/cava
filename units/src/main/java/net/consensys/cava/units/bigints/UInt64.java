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
package net.consensys.cava.units.bigints;

import static com.google.common.base.Preconditions.checkArgument;

import net.consensys.cava.bytes.Bytes;
import net.consensys.cava.bytes.MutableBytes;

import java.math.BigInteger;
import java.util.Arrays;

/**
 * An unsigned 64-bit precision number.
 *
 * This is a raw {@link UInt64Value} - a 64-bit precision unsigned number of no particular unit.
 */
public final class UInt64 implements UInt64Value<UInt64> {
  private final static int MAX_CONSTANT = 64;
  private final static BigInteger BI_MAX_CONSTANT = BigInteger.valueOf(MAX_CONSTANT);
  private static UInt64 CONSTANTS[] = new UInt64[MAX_CONSTANT + 1];
  static {
    CONSTANTS[0] = new UInt64(Bytes.wrap(new byte[8]));
    for (int i = 1; i <= MAX_CONSTANT; ++i) {
      CONSTANTS[i] = new UInt64(i);
    }
  }

  /** The minimum value of a UInt64 */
  public final static UInt64 MIN_VALUE = valueOf(0);
  /** The maximum value of a UInt64 */
  public final static UInt64 MAX_VALUE = new UInt64(Bytes.wrap(new byte[8]).not());
  /** The value 0 */
  public final static UInt64 ZERO = valueOf(0);
  /** The value 1 */
  public final static UInt64 ONE = valueOf(1);
  /** The value 10 */
  public final static UInt64 TEN = valueOf(10);

  private static final int INTS_SIZE = 8 / 4;
  // The mask is used to obtain the value of an int as if it were unsigned.
  private static final long LONG_MASK = 0xFFFFFFFFL;
  private static final BigInteger P_2_64 = BigInteger.valueOf(2).pow(64);

  // The unsigned int components of the value
  private final int ints[];

  /**
   * Return a {@code UInt64} containing the specified value.
   *
   * @param value The value to create a {@code UInt64} for.
   * @return A {@code UInt64} containing the specified value.
   * @throws IllegalArgumentException If the value is negative.
   */
  public static UInt64 valueOf(long value) {
    checkArgument(value >= 0, "Argument must be positive");
    if (value <= MAX_CONSTANT) {
      return CONSTANTS[(int) value];
    }
    return new UInt64(value);
  }

  private UInt64(long value) {
    this.ints = new int[INTS_SIZE];
    this.ints[INTS_SIZE - 2] = (int) ((value >>> 32) & LONG_MASK);
    this.ints[INTS_SIZE - 1] = (int) (value & LONG_MASK);
  }

  /**
   * Return a {@link UInt64} containing the specified value.
   *
   * @param value The value to create a {@link UInt64} for.
   * @return A {@link UInt64} containing the specified value.
   * @throws IllegalArgumentException If the value is negative.
   */
  public static UInt64 valueOf(BigInteger value) {
    checkArgument(value.signum() >= 0, "Argument must be positive");
    if (value.compareTo(BI_MAX_CONSTANT) <= 0) {
      return CONSTANTS[value.intValue()];
    }
    int[] ints = new int[INTS_SIZE];
    for (int i = INTS_SIZE - 1; i >= 0; --i) {
      ints[i] = value.intValue();
      value = value.shiftRight(32);
    }
    return new UInt64(ints);
  }

  /**
   * Return a {@link UInt64} containing the value described by the specified bytes.
   *
   * @param bytes The bytes containing a {@link UInt64}.
   * @return A {@link UInt64} containing the specified value.
   * @throws IllegalArgumentException if {@code bytes.size() &gt; 8}.
   */
  public static UInt64 fromBytes(Bytes bytes) {
    return new UInt64(Bytes.concatenate(Bytes.wrap(new byte[8 - bytes.size()]), bytes));
  }

  /**
   * Parse a hexadecimal string into a {@link UInt64}.
   *
   * @param str The hexadecimal string to parse, which may or may not start with "0x". That representation may contain
   *        less than 8 bytes, in which case the result is left padded with zeros.
   * @return The value corresponding to {@code str}.
   * @throws IllegalArgumentException if {@code str} does not correspond to a valid hexadecimal representation or
   *         contains more than 8 bytes.
   */
  public static UInt64 fromHexString(String str) {
    return fromBytes(Bytes.fromHexStringLenient(str));
  }

  private UInt64(Bytes bytes) {
    this.ints = new int[INTS_SIZE];
    for (int i = 0, j = 0; i < INTS_SIZE; ++i, j += 4) {
      ints[i] = bytes.getInt(j);
    }
  }

  private UInt64(int ints[]) {
    this.ints = ints;
  }

  @SuppressWarnings("ReferenceEquality")
  @Override
  public boolean isZero() {
    if (this == ZERO) {
      return true;
    }
    for (int i = INTS_SIZE - 1; i >= 0; --i) {
      if (this.ints[i] != 0) {
        return false;
      }
    }
    return true;
  }

  @Override
  public UInt64 add(UInt64 value) {
    if (value.isZero()) {
      return this;
    }
    if (isZero()) {
      return value;
    }
    int result[] = new int[INTS_SIZE];
    boolean constant = true;
    long sum = (this.ints[INTS_SIZE - 1] & LONG_MASK) + (value.ints[INTS_SIZE - 1] & LONG_MASK);
    result[INTS_SIZE - 1] = (int) (sum & LONG_MASK);
    if (result[INTS_SIZE - 1] < 0 || result[INTS_SIZE - 1] > MAX_CONSTANT) {
      constant = false;
    }
    for (int i = INTS_SIZE - 2; i >= 0; --i) {
      sum = (this.ints[i] & LONG_MASK) + (value.ints[i] & LONG_MASK) + (sum >>> 32);
      result[i] = (int) (sum & LONG_MASK);
      constant &= result[i] == 0;
    }
    if (constant) {
      return CONSTANTS[result[INTS_SIZE - 1]];
    }
    return new UInt64(result);
  }

  @Override
  public UInt64 add(long value) {
    if (value == 0) {
      return this;
    }
    if (value > 0 && isZero()) {
      return UInt64.valueOf(value);
    }
    int result[] = new int[INTS_SIZE];
    boolean constant = true;
    long sum = (this.ints[INTS_SIZE - 1] & LONG_MASK) + (value & LONG_MASK);
    result[INTS_SIZE - 1] = (int) (sum & LONG_MASK);
    if (result[INTS_SIZE - 1] < 0 || result[INTS_SIZE - 1] > MAX_CONSTANT) {
      constant = false;
    }
    sum = (this.ints[INTS_SIZE - 2] & LONG_MASK) + (value >>> 32) + (sum >>> 32);
    result[INTS_SIZE - 2] = (int) (sum & LONG_MASK);
    constant &= result[INTS_SIZE - 2] == 0;
    long signExtent = (value >> 63) & LONG_MASK;
    for (int i = INTS_SIZE - 3; i >= 0; --i) {
      sum = (this.ints[i] & LONG_MASK) + signExtent + (sum >>> 32);
      result[i] = (int) (sum & LONG_MASK);
      constant &= result[i] == 0;
    }
    if (constant) {
      return CONSTANTS[result[INTS_SIZE - 1]];
    }
    return new UInt64(result);
  }

  @Override
  public UInt64 addMod(UInt64 value, UInt64 modulus) {
    if (modulus.isZero()) {
      throw new ArithmeticException("addMod with zero modulus");
    }
    return UInt64.valueOf(toBigInteger().add(value.toBigInteger()).mod(modulus.toBigInteger()));
  }

  @Override
  public UInt64 addMod(long value, UInt64 modulus) {
    if (modulus.isZero()) {
      throw new ArithmeticException("addMod with zero modulus");
    }
    return UInt64.valueOf(toBigInteger().add(BigInteger.valueOf(value)).mod(modulus.toBigInteger()));
  }

  @Override
  public UInt64 addMod(long value, long modulus) {
    if (modulus == 0) {
      throw new ArithmeticException("addMod with zero modulus");
    }
    if (modulus < 0) {
      throw new ArithmeticException("addMod unsigned with negative modulus");
    }
    return UInt64.valueOf(toBigInteger().add(BigInteger.valueOf(value)).mod(BigInteger.valueOf(modulus)));
  }

  @Override
  public UInt64 subtract(UInt64 value) {
    if (value.isZero()) {
      return this;
    }

    int result[] = new int[INTS_SIZE];
    boolean constant = true;
    long sum = (this.ints[INTS_SIZE - 1] & LONG_MASK) + ((~value.ints[INTS_SIZE - 1]) & LONG_MASK) + 1;
    result[INTS_SIZE - 1] = (int) (sum & LONG_MASK);
    if (result[INTS_SIZE - 1] < 0 || result[INTS_SIZE - 1] > MAX_CONSTANT) {
      constant = false;
    }
    for (int i = INTS_SIZE - 2; i >= 0; --i) {
      sum = (this.ints[i] & LONG_MASK) + ((~value.ints[i]) & LONG_MASK) + (sum >>> 32);
      result[i] = (int) (sum & LONG_MASK);
      constant &= result[i] == 0;
    }
    if (constant) {
      return CONSTANTS[result[INTS_SIZE - 1]];
    }
    return new UInt64(result);
  }

  @Override
  public UInt64 subtract(long value) {
    return add(-value);
  }

  @Override
  public UInt64 multiply(UInt64 value) {
    if (isZero() || value.isZero()) {
      return ZERO;
    }
    if (value.equals(UInt64.ONE)) {
      return this;
    }
    return multiply(this.ints, value.ints);
  }

  private static UInt64 multiply(int[] x, int[] y) {
    int result[] = new int[INTS_SIZE + INTS_SIZE];

    long carry = 0;
    for (int j = INTS_SIZE - 1, k = INTS_SIZE + INTS_SIZE - 1; j >= 0; j--, k--) {
      long product = (y[j] & LONG_MASK) * (x[INTS_SIZE - 1] & LONG_MASK) + carry;
      result[k] = (int) product;
      carry = product >>> 32;
    }
    result[INTS_SIZE - 1] = (int) carry;

    for (int i = INTS_SIZE - 2; i >= 0; i--) {
      carry = 0;
      for (int j = INTS_SIZE - 1, k = INTS_SIZE + i; j >= 0; j--, k--) {
        long product = (y[j] & LONG_MASK) * (x[i] & LONG_MASK) + (result[k] & LONG_MASK) + carry;

        result[k] = (int) product;
        carry = product >>> 32;
      }
      result[i] = (int) carry;
    }

    boolean constant = true;
    for (int i = INTS_SIZE; i < (INTS_SIZE + INTS_SIZE) - 2; ++i) {
      constant &= (result[i] == 0);
    }
    if (constant && result[INTS_SIZE + INTS_SIZE - 1] >= 0 && result[INTS_SIZE + INTS_SIZE - 1] <= MAX_CONSTANT) {
      return CONSTANTS[result[INTS_SIZE + INTS_SIZE - 1]];
    }
    return new UInt64(Arrays.copyOfRange(result, INTS_SIZE, INTS_SIZE + INTS_SIZE));
  }

  @Override
  public UInt64 multiply(long value) {
    if (value == 0 || isZero()) {
      return ZERO;
    }
    if (value == 1) {
      return this;
    }
    if (value < 0) {
      throw new ArithmeticException("multiply unsigned by negative");
    }
    UInt64 other = new UInt64(value);
    return multiply(this.ints, other.ints);
  }

  @Override
  public UInt64 multiplyMod(UInt64 value, UInt64 modulus) {
    if (modulus.isZero()) {
      throw new ArithmeticException("multiplyMod with zero modulus");
    }
    if (isZero() || value.isZero()) {
      return ZERO;
    }
    if (value.equals(UInt64.ONE)) {
      return mod(modulus);
    }
    return UInt64.valueOf(toBigInteger().multiply(value.toBigInteger()).mod(modulus.toBigInteger()));
  }

  @Override
  public UInt64 multiplyMod(long value, UInt64 modulus) {
    if (modulus.isZero()) {
      throw new ArithmeticException("multiplyMod with zero modulus");
    }
    if (value == 0 || isZero()) {
      return ZERO;
    }
    if (value == 1) {
      return mod(modulus);
    }
    if (value < 0) {
      throw new ArithmeticException("multiplyMod unsigned by negative");
    }
    return UInt64.valueOf(toBigInteger().multiply(BigInteger.valueOf(value)).mod(modulus.toBigInteger()));
  }

  @Override
  public UInt64 multiplyMod(long value, long modulus) {
    if (modulus == 0) {
      throw new ArithmeticException("multiplyMod with zero modulus");
    }
    if (modulus < 0) {
      throw new ArithmeticException("multiplyMod unsigned with negative modulus");
    }
    if (value == 0 || isZero()) {
      return ZERO;
    }
    if (value == 1) {
      return mod(modulus);
    }
    if (value < 0) {
      throw new ArithmeticException("multiplyMod unsigned by negative");
    }
    return UInt64.valueOf(toBigInteger().multiply(BigInteger.valueOf(value)).mod(BigInteger.valueOf(modulus)));
  }

  @Override
  public UInt64 divide(UInt64 value) {
    if (value.isZero()) {
      throw new ArithmeticException("divide by zero");
    }
    if (value.equals(UInt64.ONE)) {
      return this;
    }
    return UInt64.valueOf(toBigInteger().divide(value.toBigInteger()));
  }

  @Override
  public UInt64 divide(long value) {
    if (value == 0) {
      throw new ArithmeticException("divide by zero");
    }
    if (value < 0) {
      throw new ArithmeticException("divide unsigned by negative");
    }
    if (value == 1) {
      return this;
    }
    if (isPowerOf2(value)) {
      return shiftRight(log2(value));
    }
    return UInt64.valueOf(toBigInteger().divide(BigInteger.valueOf(value)));
  }

  @Override
  public UInt64 pow(UInt64 exponent) {
    return UInt64.valueOf(toBigInteger().modPow(exponent.toBigInteger(), P_2_64));
  }

  @Override
  public UInt64 pow(long exponent) {
    return UInt64.valueOf(toBigInteger().modPow(BigInteger.valueOf(exponent), P_2_64));
  }

  @Override
  public UInt64 mod(UInt64 modulus) {
    if (modulus.isZero()) {
      throw new ArithmeticException("mod by zero");
    }
    return UInt64.valueOf(toBigInteger().mod(modulus.toBigInteger()));
  }

  @Override
  public UInt64 mod(long modulus) {
    if (modulus == 0) {
      throw new ArithmeticException("mod by zero");
    }
    if (modulus < 0) {
      throw new ArithmeticException("mod by negative");
    }
    if (isPowerOf2(modulus)) {
      int log2 = log2(modulus);
      int d = log2 / 32;
      int s = log2 % 32;
      assert (d == 0 || d == 1);

      int[] result = new int[INTS_SIZE];
      // Mask the byte at d to only include the s right-most bits
      result[INTS_SIZE - 1 - d] = this.ints[INTS_SIZE - 1 - d] & ~(0xFFFFFFFF << s);
      if (d != 0) {
        result[INTS_SIZE - 1] = this.ints[INTS_SIZE - 1];
      }
      return new UInt64(result);
    }
    return UInt64.valueOf(toBigInteger().mod(BigInteger.valueOf(modulus)));
  }

  /**
   * Return a bit-wise AND of this value and the supplied value.
   *
   * If this value and the supplied value are different lengths, then the shorter will be zero-padded to the left.
   *
   * @param value The value to perform the operation with.
   * @return The result of a bit-wise AND.
   */
  public UInt64 and(UInt64 value) {
    int result[] = new int[INTS_SIZE];
    for (int i = INTS_SIZE - 1; i >= 0; --i) {
      result[i] = this.ints[i] & value.ints[i];
    }
    return new UInt64(result);
  }

  /**
   * Return a bit-wise AND of this value and the supplied bytes.
   *
   * If this value and the supplied value are different lengths, then the shorter will be zero-padded to the left.
   *
   * @param bytes The bytes to perform the operation with.
   * @return The result of a bit-wise AND.
   */
  public UInt64 and(Bytes bytes) {
    int result[] = new int[INTS_SIZE];
    for (int i = INTS_SIZE - 1, j = 28; i >= 0; --i, j -= 4) {
      int other = ((int) bytes.get(j) & 0xFF) << 24;
      other |= ((int) bytes.get(j + 1) & 0xFF) << 16;
      other |= ((int) bytes.get(i + 2) & 0xFF) << 8;
      other |= ((int) bytes.get(i + 3) & 0xFF);
      result[i] = this.ints[i] & other;
    }
    return new UInt64(result);
  }

  /**
   * Return a bit-wise OR of this value and the supplied value.
   *
   * If this value and the supplied value are different lengths, then the shorter will be zero-padded to the left.
   *
   * @param value The value to perform the operation with.
   * @return The result of a bit-wise OR.
   */
  public UInt64 or(UInt64 value) {
    int result[] = new int[INTS_SIZE];
    for (int i = INTS_SIZE - 1; i >= 0; --i) {
      result[i] = this.ints[i] | value.ints[i];
    }
    return new UInt64(result);
  }

  /**
   * Return a bit-wise OR of this value and the supplied bytes.
   *
   * If this value and the supplied value are different lengths, then the shorter will be zero-padded to the left.
   *
   * @param bytes The bytes to perform the operation with.
   * @return The result of a bit-wise OR.
   */
  public UInt64 or(Bytes bytes) {
    int result[] = new int[INTS_SIZE];
    for (int i = INTS_SIZE - 1, j = 28; i >= 0; --i, j -= 4) {
      result[i] = this.ints[i] | (((int) bytes.get(j) & 0xFF) << 24);
      result[i] |= ((int) bytes.get(j + 1) & 0xFF) << 16;
      result[i] |= ((int) bytes.get(j + 2) & 0xFF) << 8;
      result[i] |= ((int) bytes.get(j + 3) & 0xFF);
    }
    return new UInt64(result);
  }

  /**
   * Return a bit-wise XOR of this value and the supplied value.
   *
   * If this value and the supplied value are different lengths, then the shorter will be zero-padded to the left.
   *
   * @param value The value to perform the operation with.
   * @return The result of a bit-wise XOR.
   */
  public UInt64 xor(UInt64 value) {
    int result[] = new int[INTS_SIZE];
    for (int i = INTS_SIZE - 1; i >= 0; --i) {
      result[i] = this.ints[i] ^ value.ints[i];
    }
    return new UInt64(result);
  }

  /**
   * Return a bit-wise XOR of this value and the supplied bytes.
   *
   * If this value and the supplied value are different lengths, then the shorter will be zero-padded to the left.
   *
   * @param bytes The bytes to perform the operation with.
   * @return The result of a bit-wise XOR.
   */
  public UInt64 xor(Bytes bytes) {
    int result[] = new int[INTS_SIZE];
    for (int i = INTS_SIZE - 1, j = 28; i >= 0; --i, j -= 4) {
      result[i] = this.ints[i] ^ (((int) bytes.get(j) & 0xFF) << 24);
      result[i] ^= ((int) bytes.get(j + 1) & 0xFF) << 16;
      result[i] ^= ((int) bytes.get(j + 2) & 0xFF) << 8;
      result[i] ^= ((int) bytes.get(j + 3) & 0xFF);
    }
    return new UInt64(result);
  }

  /**
   * Return a bit-wise NOT of this value.
   *
   * @return The result of a bit-wise NOT.
   */
  public UInt64 not() {
    int result[] = new int[INTS_SIZE];
    for (int i = INTS_SIZE - 1; i >= 0; --i) {
      result[i] = ~(this.ints[i]);
    }
    return new UInt64(result);
  }

  /**
   * Shift all bits in this value to the right.
   *
   * @param distance The number of bits to shift by.
   * @return A value containing the shifted bits.
   */
  public UInt64 shiftRight(int distance) {
    if (distance == 0) {
      return this;
    }
    if (distance >= 64) {
      return ZERO;
    }
    int result[] = new int[INTS_SIZE];
    int d = distance / 32;
    int s = distance % 32;

    int resIdx = INTS_SIZE;
    if (s == 0) {
      for (int i = INTS_SIZE - d; i > 0;) {
        result[--resIdx] = this.ints[--i];
      }
    } else {
      for (int i = INTS_SIZE - 1 - d; i >= 0; i--) {
        int leftSide = this.ints[i] >>> s;
        int rightSide = (i == 0) ? 0 : this.ints[i - 1] << (32 - s);
        result[--resIdx] = (leftSide | rightSide);
      }
    }
    return new UInt64(result);
  }

  /**
   * Shift all bits in this value to the left.
   *
   * @param distance The number of bits to shift by.
   * @return A value containing the shifted bits.
   */
  public UInt64 shiftLeft(int distance) {
    if (distance == 0) {
      return this;
    }
    if (distance >= 64) {
      return ZERO;
    }
    int result[] = new int[INTS_SIZE];
    int d = distance / 32;
    int s = distance % 32;

    int resIdx = 0;
    if (s == 0) {
      for (int i = d; i < INTS_SIZE;) {
        result[resIdx++] = this.ints[i++];
      }
    } else {
      for (int i = d; i < INTS_SIZE; ++i) {
        int leftSide = this.ints[i] << s;
        int rightSide = (i == INTS_SIZE - 1) ? 0 : (this.ints[i + 1] >>> (32 - s));
        result[resIdx++] = (leftSide | rightSide);
      }
    }
    return new UInt64(result);
  }

  @Override
  public boolean equals(Object object) {
    if (object == this) {
      return true;
    }
    if (!(object instanceof UInt64)) {
      return false;
    }
    UInt64 other = (UInt64) object;
    for (int i = 0; i < INTS_SIZE; ++i) {
      if (this.ints[i] != other.ints[i]) {
        return false;
      }
    }
    return true;
  }

  @Override
  public int hashCode() {
    int result = 1;
    for (int i = 0; i < INTS_SIZE; ++i) {
      result = 31 * result + this.ints[i];
    }
    return result;
  }

  @Override
  public int compareTo(UInt64 other) {
    for (int i = 0; i < INTS_SIZE; ++i) {
      int cmp = Long.compare(((long) this.ints[i]) & LONG_MASK, ((long) other.ints[i]) & LONG_MASK);
      if (cmp != 0) {
        return cmp;
      }
    }
    return 0;
  }

  @Override
  public boolean fitsInt() {
    for (int i = 0; i < INTS_SIZE - 1; i++) {
      if (this.ints[i] != 0) {
        return false;
      }
    }
    // Lastly, the left-most byte of the int must not start with a 1.
    return this.ints[INTS_SIZE - 1] >= 0;
  }

  @Override
  public int intValue() {
    if (!fitsInt()) {
      throw new ArithmeticException("Value does not fit a 4 byte int");
    }
    return this.ints[INTS_SIZE - 1];
  }

  @Override
  public boolean fitsLong() {
    for (int i = 0; i < INTS_SIZE - 2; i++) {
      if (this.ints[i] != 0) {
        return false;
      }
    }
    // Lastly, the left-most byte of the int must not start with a 1.
    return this.ints[INTS_SIZE - 2] >= 0;
  }

  @Override
  public long toLong() {
    if (!fitsLong()) {
      throw new ArithmeticException("Value does not fit a 8 byte long");
    }
    return (((long) this.ints[INTS_SIZE - 2]) << 32) | (((long) (this.ints[INTS_SIZE - 1])) & LONG_MASK);
  }

  @Override
  public String toString() {
    return toBigInteger().toString();
  }

  @Override
  public BigInteger toBigInteger() {
    byte mag[] = new byte[8];
    for (int i = 0, j = 0; i < INTS_SIZE; ++i) {
      mag[j++] = (byte) (this.ints[i] >>> 24);
      mag[j++] = (byte) ((this.ints[i] >>> 16) & 0xFF);
      mag[j++] = (byte) ((this.ints[i] >>> 8) & 0xFF);
      mag[j++] = (byte) (this.ints[i] & 0xFF);
    }
    return new BigInteger(1, mag);
  }

  @Override
  public UInt64 toUInt64() {
    return this;
  }

  @Override
  public Bytes toBytes() {
    MutableBytes bytes = MutableBytes.create(8);
    for (int i = 0, j = 0; i < INTS_SIZE; ++i, j += 4) {
      bytes.setInt(j, this.ints[i]);
    }
    return bytes;
  }

  @Override
  public Bytes toMinimalBytes() {
    int i = 0;
    while (i < INTS_SIZE && this.ints[i] == 0) {
      ++i;
    }
    if (i == INTS_SIZE) {
      return Bytes.EMPTY;
    }
    int firstIntBytes = 4 - (Integer.numberOfLeadingZeros(this.ints[i]) / 8);
    int totalBytes = firstIntBytes + ((INTS_SIZE - (i + 1)) * 4);
    MutableBytes bytes = MutableBytes.create(totalBytes);
    int j = 0;
    switch (firstIntBytes) {
      case 4:
        bytes.set(j++, (byte) (this.ints[i] >>> 24));
        // fall through
      case 3:
        bytes.set(j++, (byte) ((this.ints[i] >>> 16) & 0xFF));
        // fall through
      case 2:
        bytes.set(j++, (byte) ((this.ints[i] >>> 8) & 0xFF));
        // fall through
      case 1:
        bytes.set(j++, (byte) (this.ints[i] & 0xFF));
    }
    ++i;
    for (; i < INTS_SIZE; ++i, j += 4) {
      bytes.setInt(j, this.ints[i]);
    }
    return bytes;
  }

  @Override
  public int numberOfLeadingZeros() {
    for (int i = 0; i < INTS_SIZE; i++) {
      if (this.ints[i] == 0) {
        continue;
      }
      return (i * 32) + Integer.numberOfLeadingZeros(this.ints[i]);
    }
    return 64;
  }

  @Override
  public int bitLength() {
    for (int i = 0; i < INTS_SIZE; i++) {
      if (this.ints[i] == 0) {
        continue;
      }
      return (INTS_SIZE * 32) - (i * 32) - Integer.numberOfLeadingZeros(this.ints[i]);
    }
    return 0;
  }

  private static boolean isPowerOf2(long n) {
    assert n > 0;
    return (n & (n - 1)) == 0;
  }

  private static int log2(long v) {
    assert v > 0;
    return 63 - Long.numberOfLeadingZeros(v);
  }
}
