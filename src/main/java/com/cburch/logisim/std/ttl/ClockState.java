/*
 * Logisim-evolution - digital logic design tool and simulator
 * Copyright by the Logisim-evolution developers
 *
 * https://github.com/logisim-evolution/
 *
 * This is free software released under GNU GPLv3 license
 */

package com.cburch.logisim.std.ttl;

import com.cburch.logisim.data.BitWidth;
import com.cburch.logisim.data.Value;
import com.cburch.logisim.instance.StdAttr;

class ClockState implements Cloneable {
  private Value lastClock;

  public ClockState() {
    lastClock = Value.FALSE;
  }

  @Override
  public ClockState clone() {
    try {
      return (ClockState) super.clone();
    } catch (CloneNotSupportedException e) {
      return null;
    }
  }

  public boolean updateClock(Value newClock, Object trigger) {
    final var oldClock = lastClock;
    lastClock = newClock;
    if (trigger == null || trigger == StdAttr.TRIG_RISING) {
      return oldClock == Value.FALSE && newClock == Value.TRUE;
    } else if (trigger == StdAttr.TRIG_FALLING) {
      return oldClock == Value.TRUE && newClock == Value.FALSE;
    } else if (trigger == StdAttr.TRIG_HIGH) {
      return newClock == Value.TRUE;
    } else if (trigger == StdAttr.TRIG_LOW) {
      return newClock == Value.FALSE;
    } else {
      return oldClock == Value.FALSE && newClock == Value.TRUE;
    }
  }

  public boolean updateClock(Value newClock) {
    final var oldClock = lastClock;
    lastClock = newClock;
    return oldClock == Value.FALSE && newClock == Value.TRUE;
  }

  public boolean updateClock(Value newClock, int which) {
    var values = lastClock.getAll();
    if (values.length <= which) {
      final var nvalue = (Value.createKnown(BitWidth.create(which + 1), 0)).getAll();
      System.arraycopy(values, 0, nvalue, 0, values.length);
      values = nvalue;
    }
    final var oldClock = values[which];
    values[which] = newClock;
    lastClock = Value.create(values);
    return oldClock == Value.FALSE && newClock == Value.TRUE;
  }
}
