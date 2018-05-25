package net.consensys.cava.io;

import java.io.OutputStream;

final class NullOutputStream extends OutputStream {
  static final NullOutputStream INSTANCE = new NullOutputStream();

  @Override
  public void write(int b) {
    // do nothing
  }
}
