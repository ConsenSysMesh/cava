package net.consensys.cava.bytes;

/**
 * An abstract {@link Bytes} value that provides implementations of {@link #equals(Object)}, {@link #hashCode()} and
 * {@link #toString()}.
 */
public abstract class AbstractBytes implements Bytes {

  static final char[] HEX_CODE = "0123456789ABCDEF".toCharArray();

  /**
   * Compare this value and the provided one for equality.
   *
   * <p>
   * Two {@link Bytes} values are equal is they have contain the exact same bytes.
   *
   * @param obj The object to test for equality with.
   * @return <tt>true</tt> if this value and {@code obj} are equal.
   */
  @Override
  public boolean equals(Object obj) {
    if (obj == this) {
      return true;
    }
    if (!(obj instanceof Bytes)) {
      return false;
    }

    Bytes other = (Bytes) obj;
    if (this.size() != other.size()) {
      return false;
    }

    for (int i = 0; i < size(); i++) {
      if (this.get(i) != other.get(i)) {
        return false;
      }
    }
    return true;
  }

  @Override
  public int hashCode() {
    int result = 1;
    for (int i = 0; i < size(); i++) {
      result = 31 * result + get(i);
    }
    return result;
  }

  @Override
  public String toString() {
    return toHexString();
  }
}
