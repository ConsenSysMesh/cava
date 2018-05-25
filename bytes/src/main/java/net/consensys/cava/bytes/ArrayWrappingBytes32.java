package net.consensys.cava.bytes;

import static com.google.common.base.Preconditions.checkArgument;

final class ArrayWrappingBytes32 extends ArrayWrappingBytes implements Bytes32 {

  ArrayWrappingBytes32(byte[] bytes) {
    this(checkLength(bytes), 0);
  }

  ArrayWrappingBytes32(byte[] bytes, int offset) {
    super(checkLength(bytes, offset), offset, SIZE);
  }

  // Ensures a proper error message.
  private static byte[] checkLength(byte[] bytes) {
    checkArgument(bytes.length == SIZE, "Expected %s bytes but got %s", SIZE, bytes.length);
    return bytes;
  }

  // Ensures a proper error message.
  private static byte[] checkLength(byte[] bytes, int offset) {
    checkArgument(
        bytes.length - offset >= SIZE,
        "Expected at least %s bytes from offset %s but got only %s",
        SIZE,
        offset,
        bytes.length - offset);
    return bytes;
  }

  @Override
  public Bytes32 copy() {
    if (offset == 0 && length == bytes.length) {
      return this;
    }
    return new ArrayWrappingBytes32(toArray());
  }

  @Override
  public MutableBytes32 mutableCopy() {
    return new MutableArrayWrappingBytes32(toArray());
  }
}
