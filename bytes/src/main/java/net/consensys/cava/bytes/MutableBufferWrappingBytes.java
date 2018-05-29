package net.consensys.cava.bytes;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkElementIndex;

import io.vertx.core.buffer.Buffer;

final class MutableBufferWrappingBytes extends BufferWrappingBytes implements MutableBytes {

  MutableBufferWrappingBytes(Buffer buffer) {
    super(buffer);
  }

  MutableBufferWrappingBytes(Buffer buffer, int offset, int length) {
    super(buffer, offset, length);
  }

  @Override
  public void set(int i, byte b) {
    buffer.setByte(i, b);
  }

  @Override
  public void setInt(int i, int value) {
    buffer.setInt(i, value);
  }

  @Override
  public void setLong(int i, long value) {
    buffer.setLong(i, value);
  }

  @Override
  public MutableBytes mutableSlice(int index, int length) {
    if (index == 0 && length == size()) {
      return this;
    }
    if (length == 0) {
      return MutableBytes.EMPTY;
    }

    checkElementIndex(index, size());
    checkArgument(
        index + length <= size(),
        "Provided length %s is too big: the value has size %s and has only %s bytes from %s",
        length,
        size(),
        size() - index,
        index);

    return new MutableBufferWrappingBytes(buffer);
  }

  @Override
  public Bytes copy() {
    return Bytes.wrap(toArray());
  }

  @Override
  public MutableBytes mutableCopy() {
    return MutableBytes.wrap(toArray());
  }
}
