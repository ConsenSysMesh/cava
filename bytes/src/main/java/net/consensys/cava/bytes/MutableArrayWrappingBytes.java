package net.consensys.cava.bytes;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkElementIndex;

import java.util.Arrays;

class MutableArrayWrappingBytes extends ArrayWrappingBytes implements MutableBytes {

  MutableArrayWrappingBytes(byte[] bytes, int offset, int length) {
    super(bytes, offset, length);
  }

  @Override
  public void set(int i, byte b) {
    // Check bounds because while the array access would throw, the error message would be confusing
    // for the caller.
    checkElementIndex(i, size());
    this.bytes[offset + i] = b;
  }

  @Override
  public MutableBytes mutableSlice(int i, int length) {
    if (i == 0 && length == size())
      return this;
    if (length == 0)
      return MutableBytes.EMPTY;

    checkElementIndex(i, size());
    checkArgument(
        i + length <= size(),
        "Specified length %s is too large: the value has size %s and has only %s bytes from %s",
        length,
        size(),
        size() - i,
        i);
    return length == Bytes32.SIZE ? new MutableArrayWrappingBytes32(bytes, offset + i)
        : new MutableArrayWrappingBytes(bytes, offset + i, length);
  }

  @Override
  public void fill(byte b) {
    Arrays.fill(bytes, offset, offset + length, b);
  }

  @Override
  public Bytes copy() {
    return new ArrayWrappingBytes(toArray());
  }
}
