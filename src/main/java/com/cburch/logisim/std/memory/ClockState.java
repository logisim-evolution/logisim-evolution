/*
 * Logisim-evolution - digital logic design tool and simulator
 * Copyright by the Logisim-evolution developers
 *
 * https://github.com/logisim-evolution/
 *
 * This is free software released under GNU GPLv3 license
 */

package com.cburch.logisim.std.memory;

import com.cburch.logisim.data.Value;
import com.cburch.logisim.instance.StdAttr;

public class ClockState implements Cloneable {
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
}
