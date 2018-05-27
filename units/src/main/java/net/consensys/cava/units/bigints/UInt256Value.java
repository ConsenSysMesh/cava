package net.consensys.cava.units.bigints;


import net.consensys.cava.bytes.Bytes;
import net.consensys.cava.bytes.Bytes32;

import java.math.BigInteger;

/**
 * Represents a 256-bit (32 bytes) unsigned integer value.
 *
 * <p>
 * A {@link UInt256Value} is an unsigned integer value stored with 32 bytes, so whose value can range between 0 and
 * 2^256-1.
 *
 * <p>
 * This interface defines operations for value types with a 256-bit precision range. The methods provided by this
 * interface take parameters of the same type (and also {@code long}. This provides type safety by ensuring calculations
 * cannot mix different {@code UInt256Value} types.
 *
 * <p>
 * Where only a pure numerical 256-bit value is required, {@link UInt256} should be used.
 *
 * <p>
 * It is strongly advised to extend {@link BaseUInt256Value} rather than implementing this interface directly. Doing so
 * provides type safety in that quantities of different units cannot be mixed accidentally.
 *
 * @param <T> The concrete type of the value.
 */
public interface UInt256Value<T extends UInt256Value<T>> extends Comparable<T> {

  /**
   * @return True if this is the value 0.
   */
  default boolean isZero() {
    return toBytes().isZero();
  }

  /**
   * Returns a value that is {@code (this + value)}.
   *
   * @param value The amount to be added to this value.
   * @return {@code this + value}
   */
  T add(T value);

  /**
   * Returns a value that is {@code (this + value)}.
   *
   * @param value The amount to be added to this value.
   * @return {@code this + value}
   */
  T add(long value);

  /**
   * Returns a value equivalent to {@code ((this + value) mod modulus)}.
   *
   * @param value The amount to be added to this value.
   * @param modulus The modulus.
   * @return {@code (this + value) mod modulus}
   * @throws ArithmeticException {@code modulus} == 0.
   */
  T addMod(T value, UInt256 modulus);

  /**
   * Returns a value equivalent to {@code ((this + value) mod modulus)}.
   *
   * @param value The amount to be added to this value.
   * @param modulus The modulus.
   * @return {@code (this + value) mod modulus}
   * @throws ArithmeticException {@code modulus} == 0.
   */
  T addMod(long value, UInt256 modulus);

  /**
   * Returns a value equivalent to {@code ((this + value) mod modulus)}.
   *
   * @param value The amount to be added to this value.
   * @param modulus The modulus.
   * @return {@code (this + value) mod modulus}
   * @throws ArithmeticException {@code modulus} &le; 0.
   */
  T addMod(long value, long modulus);

  /**
   * Returns a value that is {@code (this - value)}.
   *
   * @param value The amount to be subtracted from this value.
   * @return {@code this - value}
   */
  T subtract(T value);

  /**
   * Returns a value that is {@code (this - value)}.
   *
   * @param value The amount to be subtracted from this value.
   * @return {@code this - value}
   */
  T subtract(long value);

  /**
   * Returns a value that is {@code (this * value)}.
   *
   * @param value The amount to multiply this value by.
   * @return {@code this * value}
   */
  T multiply(T value);

  /**
   * Returns a value that is {@code (this * value)}.
   *
   * @param value The amount to multiply this value by.
   * @return {@code this * value}
   * @throws ArithmeticException {@code value} &lt; 0.
   */
  T multiply(long value);

  /**
   * Returns a value that is {@code ((this * value) mod modulus)}.
   *
   * @param value The amount to multiply this value by.
   * @param modulus The modulus.
   * @return {@code (this * value) mod modulus}
   * @throws ArithmeticException {@code value} &lt; 0 or {@code modulus} == 0.
   */
  T multiplyMod(T value, UInt256 modulus);

  /**
   * Returns a value that is {@code ((this * value) mod modulus)}.
   *
   * @param value The amount to multiply this value by.
   * @param modulus The modulus.
   * @return {@code (this * value) mod modulus}
   * @throws ArithmeticException {@code value} &lt; 0 or {@code modulus} == 0.
   */
  T multiplyMod(long value, UInt256 modulus);

  /**
   * Returns a value that is {@code ((this * value) mod modulus)}.
   *
   * @param value The amount to multiply this value by.
   * @param modulus The modulus.
   * @return {@code (this * value) mod modulus}
   * @throws ArithmeticException {@code value} &lt; 0 or {@code modulus} &le; 0.
   */
  T multiplyMod(long value, long modulus);

  /**
   * Returns a value that is {@code (this / value)}.
   *
   * @param value The amount to divide this value by.
   * @return {@code this / value}
   * @throws ArithmeticException {@code value} == 0.
   */
  T divide(T value);

  /**
   * Returns a value that is {@code (this / value)}.
   *
   * @param value The amount to divide this value by.
   * @return {@code this / value}
   * @throws ArithmeticException {@code value} &le; 0.
   */
  T divide(long value);

  /**
   * Returns a value that is <tt>(this<sup>exponent</sup> mod 2<sup>256</sup>)</tt>
   *
   * <p>
   * This calculates an exponentiation over the modulus of {@code 2^256}.
   *
   * <p>
   * Note that {@code exponent} is an {@link UInt256} rather than of the type {@code T}.
   *
   * @param exponent The exponent to which this value is to be raised.
   * @return <tt>this<sup>exponent</sup> mod 2<sup>256</sup></tt>
   */
  T pow(UInt256 exponent);

  /**
   * Returns a value that is <tt>(this<sup>exponent</sup> mod 2<sup>256</sup>)</tt>
   *
   * <p>
   * This calculates an exponentiation over the modulus of {@code 2^256}.
   *
   * @param exponent The exponent to which this value is to be raised.
   * @return <tt>this<sup>exponent</sup> mod 2<sup>256</sup></tt>
   */
  T pow(long exponent);

  /**
   * Returns a value that is {@code (this mod modulus)}.
   *
   * @param modulus The modulus.
   * @return {@code this mod modulus}.
   * @throws ArithmeticException {@code modulus} == 0.
   */
  T mod(UInt256 modulus);

  /**
   * Returns a value that is {@code (this mod modulus)}.
   *
   * @param modulus The modulus.
   * @return {@code this mod modulus}.
   * @throws ArithmeticException {@code modulus} &le; 0.
   */
  T mod(long modulus);

  /**
   * @return True if this value fits a java {@code int} (i.e. is less or equal to {@code Integer.MAX_VALUE}).
   */
  default boolean fitsInt() {
    // Ints are 4 bytes, so anything but the 4 last bytes must be zeroes
    Bytes32 bytes = toBytes();
    for (int i = 0; i < Bytes32.SIZE - 4; i++) {
      if (bytes.get(i) != 0)
        return false;
    }
    // Lastly, the left-most byte of the int must not start with a 1.
    return bytes.get(Bytes32.SIZE - 4) >= 0;
  }

  /**
   * @return This value as a java {@code int} assuming it is small enough to fit an {@code int}.
   * @throws ArithmeticException If the value does not fit an {@code int}, that is if {@code
   *     !fitsInt()}.
   */
  default int intValue() {
    if (!fitsInt()) {
      throw new ArithmeticException("Value does not fit a 4 byte int");
    }
    return toBytes().getInt(Bytes32.SIZE - 4);
  }

  /**
   * @return True if this value fits a java {@code long} (i.e. is less or equal to {@code Long.MAX_VALUE}).
   */
  default boolean fitsLong() {
    // Longs are 8 bytes, so anything but the 8 last bytes must be zeroes
    for (int i = 0; i < Bytes32.SIZE - 8; i++) {
      if (toBytes().get(i) != 0)
        return false;
    }
    // Lastly, the left-most byte of the long must not start with a 1.
    return toBytes().get(Bytes32.SIZE - 8) >= 0;
  }

  /**
   * @return This value as a java {@code long} assuming it is small enough to fit a {@code long}.
   * @throws ArithmeticException If the value does not fit a {@code long}, that is if {@code
   *     !fitsLong()}.
   */
  default long longValue() {
    if (!fitsLong()) {
      throw new ArithmeticException("Value does not fit a 8 byte long");
    }
    return toBytes().getLong(Bytes32.SIZE - 8);
  }

  /**
   * @return This value as a {@link BigInteger}.
   */
  default BigInteger bigIntegerValue() {
    return toBytes().unsignedBigIntegerValue();
  }

  /**
   * This value represented as an hexadecimal string.
   *
   * <p>
   * Note that this representation includes all the 32 underlying bytes, no matter what the integer actually represents
   * (in other words, it can have many leading zeros). For a shorter representation that don't include leading zeros,
   * use {@link #toShortHexString}.
   *
   * @return This value represented as an hexadecimal string.
   */
  default String toHexString() {
    return toBytes().toHexString();
  }

  /** @return This value represented as a minimal hexadecimal string (without any leading zero). */
  default String toShortHexString() {
    return toBytes().toShortHexString();
  }

  /**
   * Type-cast this value as a {@link UInt256}.
   *
   * @return This value as a {@link UInt256}.
   */
  UInt256 uint256Value();

  /**
   * @return The value as bytes.
   */
  Bytes32 toBytes();

  /**
   * @return The value as bytes without any leading zero bytes.
   */
  Bytes toMinimalBytes();

  /**
   * @return the number of zero bits preceding the highest-order ("leftmost") one-bit in the binary representation of
   *         this value, or 256 if the value is equal to zero.
   */
  default int numberOfLeadingZeros() {
    return toBytes().numberOfLeadingZeros();
  }

  /**
   * @return The number of bits following and including the highest-order ("leftmost") one-bit in the binary
   *         representation of this value, or zero if all bits are zero.
   */
  default int bitLength() {
    return toBytes().bitLength();
  }
}