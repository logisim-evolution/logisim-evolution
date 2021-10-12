/*
 * Logisim-evolution - digital logic design tool and simulator
 * Copyright by the Logisim-evolution developers
 *
 * https://github.com/logisim-evolution/
 *
 * This is free software released under GNU GPLv3 license
 */

package com.cburch.logisim.tools.key;

import com.cburch.logisim.data.Attribute;
import com.cburch.logisim.data.AttributeSet;

public abstract class NumericConfigurator<V> implements KeyConfigurator, Cloneable {
  private static final int maxTimeKeyLasts = 800;

  private final Attribute<V> attr;
  private final long minValue;
  private final long maxValue;
  private long curValue;
  private final int radix;
  private final int modsEx;
  private long whenTyped;

  public NumericConfigurator(Attribute<V> attr, long min, long max, int modifiersEx) {
    this(attr, min, max, modifiersEx, 10);
  }

  public NumericConfigurator(Attribute<V> attr, long min, long max, int modifiersEx, int radix) {
    this.attr = attr;
    this.minValue = min;
    this.maxValue = max;
    this.radix = radix;
    this.modsEx = modifiersEx;
    this.curValue = 0;
    this.whenTyped = 0;
  }

  @Override
  public NumericConfigurator<V> clone() {
    try {
      @SuppressWarnings("unchecked")
      NumericConfigurator<V> ret = (NumericConfigurator<V>) super.clone();
      ret.whenTyped = 0;
      ret.curValue = 0;
      return ret;
    } catch (CloneNotSupportedException e) {
      e.printStackTrace();
      return null;
    }
  }

  protected abstract V createValue(long value);

  protected long getMaximumValue(AttributeSet attrs) {
    return maxValue;
  }

  protected long getMinimumValue(AttributeSet attrs) {
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
        var val = 0L;
        if (sinceLast < maxTimeKeyLasts) {
          val = radix * curValue;
          if (val > max) {
            val = 0;
          }
        }
        val += digit;
        if (val > max) {
          val = digit;
          if (val > max) {
            return null;
          }
        }
        event.consume();
        whenTyped = now;
        curValue = val;

        if (val >= min) {
          final Object valObj = createValue(val);
          return new KeyConfigurationResult(event, attr, valObj);
        }
      }
    }
    return null;
  }
}
