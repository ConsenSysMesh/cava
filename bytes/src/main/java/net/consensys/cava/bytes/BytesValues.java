package net.consensys.cava.bytes;

import static com.google.common.base.Preconditions.checkArgument;

final class BytesValues {
  private BytesValues() {}

  static final int MAX_UNSIGNED_SHORT = (1 << 16) - 1;
  static final long MAX_UNSIGNED_INT = (1L << 32) - 1;

  static Bytes fromHexString(String str, int destSize, boolean lenient) {
    return Bytes.wrap(fromRawHexString(str, destSize, lenient));
  }

  static byte[] fromRawHexString(String str, int destSize, boolean lenient) {
    String hex = str;
    if (str.startsWith("0x")) {
      hex = str.substring(2);
    }

    int len = hex.length();
    int idxShift = 0;
    if (len % 2 != 0) {
      if (!lenient) {
        throw new IllegalArgumentException("Invalid odd-length hex binary representation '" + str + "'");
      }

      hex = "0" + hex;
      len += 1;
      idxShift = 1;
    }

    int size = len / 2;
    if (destSize < 0) {
      destSize = size;
    } else {
      checkArgument(
          size <= destSize,
          "Hex value %s is too big: expected at most %s bytes but got %s",
          str,
          destSize,
          size);
    }

    byte[] out = new byte[destSize];

    int destOffset = (destSize - size);
    for (int i = 0; i < len; i += 2) {
      int h = hexToBin(hex.charAt(i));
      int l = hexToBin(hex.charAt(i + 1));
      if (h == -1) {
        throw new IllegalArgumentException(
            String.format(
                "Illegal character '%c' found at index %d in hex binary representation '%s'",
                hex.charAt(i),
                i - idxShift,
                str));
      }
      if (l == -1) {
        throw new IllegalArgumentException(
            String.format(
                "Illegal character '%c' found at index %d in hex binary representation '%s'",
                hex.charAt(i + 1),
                i + 1 - idxShift,
                str));
      }

      out[destOffset + (i / 2)] = (byte) (h * 16 + l);
    }
    return out;
  }

  private static int hexToBin(char ch) {
    if ('0' <= ch && ch <= '9') {
      return ch - 48;
    } else if ('A' <= ch && ch <= 'F') {
      return ch - 65 + 10;
    } else {
      return 'a' <= ch && ch <= 'f' ? ch - 97 + 10 : -1;
    }
  }
}
