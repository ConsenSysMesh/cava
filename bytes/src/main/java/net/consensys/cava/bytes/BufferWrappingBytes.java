package net.consensys.cava.bytes;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkElementIndex;

import io.vertx.core.buffer.Buffer;

class BufferWrappingBytes extends AbstractBytes {

  protected final Buffer buffer;

  BufferWrappingBytes(Buffer buffer) {
    this.buffer = buffer;
  }

  BufferWrappingBytes(Buffer buffer, int offset, int length) {
    checkArgument(length >= 0, "Invalid negative length");
    int bufferLength = buffer.length();
    checkElementIndex(offset, bufferLength + 1);
    checkArgument(
        offset + length <= bufferLength,
        "Provided length %s is too big: the buffer has size %s and has only %s bytes from %s",
        length,
        bufferLength,
        bufferLength - offset,
        offset);

    if (offset == 0 && length == bufferLength) {
      this.buffer = buffer;
    } else {
      this.buffer = buffer.slice(offset, offset + length);
    }
  }

  @Override
  public int size() {
    return buffer.length();
  }

  @Override
  public byte get(int i) {
    return buffer.getByte(i);
  }

  @Override
  public int getInt(int i) {
    return buffer.getInt(i);
  }

  @Override
  public long getLong(int i) {
    return buffer.getLong(i);
  }

  @Override
  public Bytes slice(int index, int length) {
    int size = buffer.length();
    if (index == 0 && length == size) {
      return this;
    }
    if (length == 0) {
      return Bytes.EMPTY;
    }

    checkElementIndex(index, size);
    checkArgument(
        index + length <= size,
        "Provided length %s is too big: the value has size %s and has only %s bytes from %s",
        length,
        size,
        size - index,
        index);

    return new BufferWrappingBytes(buffer.slice(index, index + length));
  }

  @Override
  public boolean equals(Object obj) {
    if (obj == this) {
      return true;
    }
    if (!(obj instanceof BufferWrappingBytes)) {
      return super.equals(obj);
    }
    BufferWrappingBytes other = (BufferWrappingBytes) obj;
    return buffer.equals(other.buffer);
  }

  @Override
  public int hashCode() {
    return buffer.hashCode();
  }

  // MUST be overridden by mutable implementations
  @Override
  public Bytes copy() {
    return Bytes.wrap(toArray());
  }

  @Override
  public MutableBytes mutableCopy() {
    return MutableBytes.wrap(toArray());
  }

  @Override
  public void appendTo(Buffer buffer) {
    buffer.appendBuffer(this.buffer);
  }

  @Override
  public byte[] toArray() {
    return buffer.getBytes();
  }
}
