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
   * Two {@link Bytes} values are equal is they have the same time and contain the exact same bytes in order.
   *
   * @param other The other value to test for equality.
   * @return Whether this value and {@code other} are equal.
   */
  @Override
  public boolean equals(Object other) {
    if (!(other instanceof Bytes))
      return false;

    Bytes that = (Bytes) other;
    if (this.size() != that.size())
      return false;

    for (int i = 0; i < size(); i++) {
      if (this.get(i) != that.get(i))
        return false;
    }
    return true;
  }

  @Override
  public int hashCode() {
    int result = 1;
    for (int i = 0; i < size(); i++)
      result = 31 * result + get(i);
    return result;
  }

  @Override
  public String toString() {
    return toHexString();
  }
}
