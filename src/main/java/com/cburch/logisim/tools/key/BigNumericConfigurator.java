/*
 * Logisim-evolution - digital logic design tool and simulator
 * Copyright by the Logisim-evolution developers
 *
 * https://github.com/logisim-evolution/
 *
 * This is free software released under GNU GPLv3 license
 */

package com.cburch.logisim.tools.key;

import java.math.BigInteger;

import com.cburch.logisim.data.Attribute;
import com.cburch.logisim.data.AttributeSet;

public abstract class BigNumericConfigurator<V> implements KeyConfigurator, Cloneable {
  private static final int maxTimeKeyLasts = 1000;

  private final Attribute<V> attr;
  private final BigInteger minValue;
  private final BigInteger maxValue;
  private BigInteger curValue;
  private final int radix;
  private final int modsEx;
  private long whenTyped;

  public BigNumericConfigurator(Attribute<V> attr, BigInteger min, BigInteger max, int modifiersEx) {
    this(attr, min, max, modifiersEx, 10);
  }

  public BigNumericConfigurator(Attribute<V> attr, BigInteger min, BigInteger max, int modifiersEx, int radix) {
    this.attr = attr;
    this.minValue = min;
    this.maxValue = max;
    this.radix = radix;
    this.modsEx = modifiersEx;
    this.curValue = BigInteger.ZERO;
    this.whenTyped = 0;
  }

  @Override
  public BigNumericConfigurator<V> clone() {
    try {
      @SuppressWarnings("unchecked")
      BigNumericConfigurator<V> ret = (BigNumericConfigurator<V>) super.clone();
      ret.whenTyped = 0;
      ret.curValue = BigInteger.ZERO;
      return ret;
    } catch (CloneNotSupportedException e) {
      e.printStackTrace();
      return null;
    }
  }

  protected abstract V createValue(BigInteger value);

  protected BigInteger getMaximumValue(AttributeSet attrs) {
    return maxValue;
  }

  protected BigInteger getMinimumValue(AttributeSet attrs) {
    return minValue;
  }

  @Override
  public KeyConfigurationResult keyEventReceived(KeyConfigurationEvent event) {
    if (event.getType() == KeyConfigurationEvent.KEY_TYPED) {
      final var e = event.getKeyEvent();
      final var digit = Character.digit(e.getKeyChar(), radix);
      if (digit >= 0 && e.getModifiersEx() == modsEx) {
        final var now = System.currentTimeMillis();
        final var sinceLast = now - whenTyped;
        final var attrs = event.getAttributeSet();
        final var min = getMinimumValue(attrs);
        final var max = getMaximumValue(attrs);
        var val = BigInteger.ZERO;
        if (sinceLast < maxTimeKeyLasts) {
          val = curValue.multiply(BigInteger.valueOf(radix));
          if (val.compareTo(max) > 0) {
            val = max;
          }
        }
        var bigDigit = BigInteger.valueOf(digit);
        val = val.add(bigDigit);
        if (val.compareTo(max) > 0) {
          val = bigDigit;
          if (val.compareTo(max) > 0) {
            return null;
          }
        }
        event.consume();
        whenTyped = now;
        curValue = val;

        if (val.compareTo(min) >= 0) {
          final Object valObj = createValue(val);
          return new KeyConfigurationResult(event, attr, valObj);
        }
      }
    }
    return null;
  }
}
