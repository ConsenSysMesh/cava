package net.consensys.cava.bytes;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkElementIndex;

import java.nio.ByteBuffer;

public class MutableByteBufferWrappingBytes extends ByteBufferWrappingBytes implements MutableBytes {

  MutableByteBufferWrappingBytes(ByteBuffer byteBuffer) {
    super(byteBuffer);
  }

  MutableByteBufferWrappingBytes(ByteBuffer byteBuffer, int offset, int length) {
    super(byteBuffer, offset, length);
  }

  @Override
  public void setInt(int i, int value) {
    byteBuffer.putInt(offset + i, value);
  }

  @Override
  public void setLong(int i, long value) {
    byteBuffer.putLong(offset + i, value);
  }

  @Override
  public void set(int i, byte b) {
    byteBuffer.put(offset + i, b);
  }

  @Override
  public MutableBytes mutableSlice(int i, int length) {
    if (i == 0 && length == this.length) {
      return this;
    }
    if (length == 0) {
      return MutableBytes.EMPTY;
    }

    checkElementIndex(i, this.length);
    checkArgument(
        i + length <= this.length,
        "Provided length %s is too big: the value has size %s and has only %s bytes from %s",
        length,
        this.length,
        this.length - i,
        i);

    return new MutableByteBufferWrappingBytes(byteBuffer, offset + i, length);
  }

  @Override
  public Bytes copy() {
    return new ArrayWrappingBytes(toArray());
  }
}
