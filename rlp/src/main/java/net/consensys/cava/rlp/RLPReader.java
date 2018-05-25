package net.consensys.cava.rlp;

import static java.util.Objects.requireNonNull;

import net.consensys.cava.bytes.Bytes;
import net.consensys.cava.units.bigints.UInt256;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;
import java.util.function.BiConsumer;
import java.util.function.Function;

import com.google.common.base.Charsets;

/**
 * A reader for consuming values from an RLP encoded source.
 */
public interface RLPReader {

  /**
   * Read the next value from the RLP source.
   *
   * @return The bytes for the next value.
   * @throws InvalidRLPEncodingException If there is an error decoding the RLP source.
   * @throws EndOfRLPException If there are no more RLP values to read.
   */
  Bytes readValue();

  /**
   * Read an integer value from the RLP source.
   *
   * @return An integer.
   * @throws InvalidRLPEncodingException If there is an error decoding the RLP source.
   * @throws InvalidRLPTypeException If the next RLP value cannot be represented as an integer.
   * @throws EndOfRLPException If there are no more RLP values to read.
   */
  default int readInt() {
    Bytes bytes = readValue();
    try {
      return bytes.intValue();
    } catch (IllegalArgumentException e) {
      throw new InvalidRLPTypeException("Next value is too large to be represented as an int");
    }
  }

  /**
   * Read a long value from the RLP source.
   *
   * @return A long.
   * @throws InvalidRLPEncodingException If there is an error decoding the RLP source.
   * @throws InvalidRLPTypeException If the next RLP value cannot be represented as a long.
   * @throws EndOfRLPException If there are no more RLP values to read.
   */
  default long readLong() {
    Bytes bytes = readValue();
    try {
      return bytes.longValue();
    } catch (IllegalArgumentException e) {
      throw new InvalidRLPTypeException("Next value is too large to be represented as a long");
    }
  }

  /**
   * Read a {@link UInt256} value from the RLP source.
   *
   * @return A {@link UInt256} value.
   * @throws InvalidRLPEncodingException If there is an error decoding the RLP source.
   * @throws InvalidRLPTypeException If the next RLP value cannot be represented as a long.
   * @throws EndOfRLPException If there are no more RLP values to read.
   */
  default UInt256 readUInt256() {
    Bytes bytes = readValue();
    try {
      return UInt256.fromBytes(bytes);
    } catch (IllegalArgumentException e) {
      throw new InvalidRLPTypeException("Next value is too large to be represented as a UInt256");
    }
  }

  /**
   * Read a big integer value from the RLP source.
   *
   * @return A big integer.
   * @throws InvalidRLPEncodingException If there is an error decoding the RLP source.
   * @throws InvalidRLPTypeException If the next RLP value cannot be represented as a big integer.
   * @throws EndOfRLPException If there are no more RLP values to read.
   */
  default BigInteger readBigInteger() {
    return readValue().unsignedBigIntegerValue();
  }

  /**
   * Read a string value from the RLP source.
   *
   * @return A string.
   * @throws InvalidRLPEncodingException If there is an error decoding the RLP source.
   * @throws InvalidRLPTypeException If the next RLP value cannot be represented as a string.
   * @throws EndOfRLPException If there are no more RLP values to read.
   */
  default String readString() {
    return new String(readValue().toArrayUnsafe(), Charsets.UTF_8);
  }

  /**
   * Check if the next item to be read is a list.
   *
   * @return <tt>true</tt> if the next item to be read is a list.
   * @throws EndOfRLPException If there are no more RLP values to read.
   */
  boolean nextIsList();

  /**
   * Check if the next item to be read is empty.
   *
   * @return <tt>true</tt> if the next item to be read is empty.
   * @throws EndOfRLPException If there are no more RLP values to read.
   */
  boolean nextIsEmpty();

  /**
   * Read a list of values from the RLP source.
   *
   * @param fn A function that will be provided a {@link RLPReader}.
   * @param <T> The result type of the reading function.
   * @return The result from the reading function.
   * @throws InvalidRLPEncodingException If there is an error decoding the RLP source.
   * @throws InvalidRLPTypeException If the next RLP value is not a list.
   * @throws EndOfRLPException If there are no more RLP values to read.
   */
  <T> T readList(Function<RLPReader, T> fn);

  /**
   * Read a list of values from the RLP source, populating a mutable output list.
   *
   * @param fn A function that will be provided with a {@link RLPReader} and a mutable output list.
   * @return The list supplied to {@code fn}.
   * @throws InvalidRLPEncodingException If there is an error decoding the RLP source.
   * @throws InvalidRLPTypeException If the next RLP value is not a list.
   * @throws EndOfRLPException If there are no more RLP values to read.
   */
  default List<Object> readList(BiConsumer<RLPReader, List<Object>> fn) {
    requireNonNull(fn);
    return readList(reader -> {
      List<Object> list = new ArrayList<>();
      fn.accept(reader, list);
      return list;
    });
  }

  /**
   * Skip the next value or list in the RLP source.
   *
   * @throws InvalidRLPEncodingException If there is an error decoding the RLP source.
   * @throws EndOfRLPException If there are no more RLP values to read.
   */
  void skipNext();

  /**
   * The number of remaining values to read.
   *
   * @return The number of remaining values to read.
   */
  int remaining();

  /**
   * Check if all values have been read.
   *
   * @return <tt>true</tt> if all values have been read.
   */
  boolean isComplete();
}
