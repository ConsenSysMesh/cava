package net.consensys.cava.trie;

import static com.google.common.base.Preconditions.checkArgument;

import net.consensys.cava.bytes.Bytes;
import net.consensys.cava.bytes.MutableBytes;

final class CompactEncoding {
  private CompactEncoding() {}

  static final byte LEAF_TERMINATOR = 0x10;

  static Bytes bytesToPath(Bytes bytes) {
    MutableBytes path = MutableBytes.create(bytes.size() * 2 + 1);
    int j = 0;
    for (int i = 0; i < bytes.size(); i += 1, j += 2) {
      byte b = bytes.get(i);
      path.set(j, (byte) ((b >>> 4) & 0x0f));
      path.set(j + 1, (byte) (b & 0x0f));
    }
    path.set(j, LEAF_TERMINATOR);
    return path;
  }

  static Bytes encode(Bytes path) {
    int size = path.size();
    boolean isLeaf = size > 0 && path.get(size - 1) == LEAF_TERMINATOR;
    if (isLeaf) {
      size = size - 1;
    }

    MutableBytes encoded = MutableBytes.create((size + 2) / 2);
    int i = 0;
    int j = 0;

    if (size % 2 == 1) {
      // add first nibble to magic
      byte high = (byte) (isLeaf ? 0x03 : 0x01);
      byte low = path.get(i++);
      if ((low & 0xf0) != 0) {
        throw new IllegalArgumentException("Invalid path: contains elements larger than a nibble");
      }
      encoded.set(j++, (byte) (high << 4 | low));
    } else {
      byte high = (byte) (isLeaf ? 0x02 : 0x00);
      encoded.set(j++, (byte) (high << 4));
    }

    while (i < size) {
      byte high = path.get(i++);
      byte low = path.get(i++);
      if ((high & 0xf0) != 0 || (low & 0xf0) != 0) {
        throw new IllegalArgumentException("Invalid path: contains elements larger than a nibble");
      }
      encoded.set(j++, (byte) (high << 4 | low));
    }

    return encoded;
  }

  public static Bytes decode(Bytes encoded) {
    int size = encoded.size();
    checkArgument(size > 0);
    byte magic = encoded.get(0);
    checkArgument((magic & 0xc0) == 0, "Invalid compact encoding");

    boolean isLeaf = (magic & 0x20) != 0;

    int pathLength = ((size - 1) * 2) + (isLeaf ? 1 : 0);
    MutableBytes path;
    int i = 0;

    if ((magic & 0x10) != 0) {
      // need to use lower nibble of magic
      path = MutableBytes.create(pathLength + 1);
      path.set(i++, (byte) (magic & 0x0f));
    } else {
      path = MutableBytes.create(pathLength);
    }

    for (int j = 1; j < size; j++) {
      byte b = encoded.get(j);
      path.set(i++, (byte) ((b >>> 4) & 0x0f));
      path.set(i++, (byte) (b & 0x0f));
    }

    if (isLeaf) {
      path.set(i, LEAF_TERMINATOR);
    }

    return path;
  }
}
