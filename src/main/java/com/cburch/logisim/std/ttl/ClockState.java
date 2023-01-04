/*
 * Logisim-evolution - digital logic design tool and simulator
 * Copyright by the Logisim-evolution developers
 *
 * https://github.com/logisim-evolution/
 *
 * This is free software released under GNU GPLv3 license
 */

package com.cburch.logisim.std.ttl;

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

  private boolean isTriggered(Value oldClock, Value newClock, Object trigger) {
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

  public boolean updateClock(Value newClock, int which, Object trigger) {
    if (lastClock.getWidth() <= which) {
      lastClock = lastClock.extendWidth(which + 1, Value.FALSE);
    }

    final var oldClock = lastClock.get(which);

    lastClock = lastClock.set(which, newClock);

    return isTriggered(oldClock, newClock, trigger);
  }

  public boolean updateClock(Value newClock, int which) {
    return updateClock(newClock, which, StdAttr.TRIG_RISING);
  }

  public boolean updateClock(Value newClock, Object trigger) {
    return updateClock(newClock, 0, trigger);
  }

  public boolean updateClock(Value newClock) {
    return updateClock(newClock, 0, StdAttr.TRIG_RISING);
  }
}
