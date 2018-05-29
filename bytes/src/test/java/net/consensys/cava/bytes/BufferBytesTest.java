package net.consensys.cava.bytes;

import io.vertx.core.buffer.Buffer;

class BufferBytesTest extends CommonBytesTests {

  @Override
  Bytes h(String hex) {
    return Bytes.wrapBuffer(Buffer.buffer(Bytes.fromHexString(hex).toArrayUnsafe()));
  }

  @Override
  MutableBytes m(int size) {
    return new MutableBufferWrappingBytes(Buffer.buffer(new byte[size]));
  }

  @Override
  Bytes w(byte[] bytes) {
    return new MutableBufferWrappingBytes(Buffer.buffer(Bytes.of(bytes).toArray()));
  }

  @Override
  Bytes of(int... bytes) {
    return new MutableBufferWrappingBytes(Buffer.buffer(Bytes.of(bytes).toArray()));
  }
}
