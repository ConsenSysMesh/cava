package net.consensys.cava.units.bigints;

import com.google.common.collect.DiscreteDomain;

/**
 * A {@link DiscreteDomain} over {@link UInt256}.
 */
public final class UInt256Domain extends DiscreteDomain<UInt256> {

  @Override
  public UInt256 next(UInt256 value) {
    return value.add(1);
  }

  @Override
  public UInt256 previous(UInt256 value) {
    return value.subtract(1);
  }

  @Override
  public long distance(UInt256 start, UInt256 end) {
    boolean negativeDistance = start.compareTo(end) < 0;
    UInt256 distance = negativeDistance ? end.subtract(start) : start.subtract(end);
    if (!distance.fitsLong()) {
      return negativeDistance ? Long.MIN_VALUE : Long.MAX_VALUE;
    }
    long distanceLong = distance.longValue();
    return negativeDistance ? -distanceLong : distanceLong;
  }

  @Override
  public UInt256 minValue() {
    return UInt256.MIN_VALUE;
  }

  @Override
  public UInt256 maxValue() {
    return UInt256.MAX_VALUE;
  }
}
