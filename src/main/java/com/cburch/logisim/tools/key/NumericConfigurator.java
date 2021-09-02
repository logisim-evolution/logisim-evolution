/*
 * Logisim-evolution - digital logic design tool and simulator
 * Copyright by Logisim-evolution developers
 * 
 * https://github.com/logisim-evolution/
 * 
 * This is free software released under GNU GPLv3 license
 */

package com.cburch.logisim.tools.key;

import com.cburch.logisim.data.Attribute;
import com.cburch.logisim.data.AttributeSet;
import java.awt.event.KeyEvent;

public abstract class NumericConfigurator<V> implements KeyConfigurator, Cloneable {
  private static final int MAX_TIME_KEY_LASTS = 800;

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

  public KeyConfigurationResult keyEventReceived(KeyConfigurationEvent event) {
    if (event.getType() == KeyConfigurationEvent.KEY_TYPED) {
      KeyEvent e = event.getKeyEvent();
      int digit = Character.digit(e.getKeyChar(), radix);
      if (digit >= 0 && e.getModifiersEx() == modsEx) {
        long now = System.currentTimeMillis();
        long sinceLast = now - whenTyped;
        AttributeSet attrs = event.getAttributeSet();
        long min = getMinimumValue(attrs);
        long max = getMaximumValue(attrs);
        long val = 0;
        if (sinceLast < MAX_TIME_KEY_LASTS) {
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
          Object valObj = createValue(val);
          return new KeyConfigurationResult(event, attr, valObj);
        }
      }
    }
    return null;
  }
}
