package net.consensys.cava.units.bigints;

import java.util.function.Function;

import com.google.common.collect.DiscreteDomain;

/**
 * A {@link DiscreteDomain} over a {@link UInt256Value}.
 */
public final class UInt256ValueDomain<T extends UInt256Value<T>> extends DiscreteDomain<T> {

  private final T minValue;
  private final T maxValue;

  /**
   * @param ctor The constructor for the {@link UInt256Value} type.
   */
  public UInt256ValueDomain(Function<UInt256, T> ctor) {
    this.minValue = ctor.apply(UInt256.MIN_VALUE);
    this.maxValue = ctor.apply(UInt256.MAX_VALUE);
  }

  @Override
  public T next(T value) {
    return value.add(1);
  }

  @Override
  public T previous(T value) {
    return value.subtract(1);
  }

  @Override
  public long distance(T start, T end) {
    boolean negativeDistance = start.compareTo(end) < 0;
    T distance = negativeDistance ? end.subtract(start) : start.subtract(end);
    if (!distance.fitsLong()) {
      return negativeDistance ? Long.MIN_VALUE : Long.MAX_VALUE;
    }
    long distanceLong = distance.longValue();
    return negativeDistance ? -distanceLong : distanceLong;
  }

  @Override
  public T minValue() {
    return minValue;
  }

  @Override
  public T maxValue() {
    return maxValue;
  }
}
