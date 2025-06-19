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

  /**
   * Predicate which returns true if the clock has been triggered
   *
   * @param oldClock the previous state of the clock signal
   * @param newClock the actual state of the clock signal
   * @param trigger the trigger mode pos/neg edge or high/low level
   * @return true if the clock was triggered, false otherwise
   */
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

  /**
   * Update the clock state with the actual state of the clock signal
   *
   * @param newClock the actual state of the clock signal
   * @param which which of the clock signals must be updated
   * @param trigger the trigger mode pos/neg edge or high/low level
   * @return true if the clock was triggered, false otherwise
   */
  public boolean updateClock(Value newClock, int which, Object trigger) {
    if (newClock == null || newClock == Value.NIL) return false;
    if (lastClock.getWidth() <= which) {
      lastClock = lastClock.extendWidth(which + 1, Value.FALSE);
    }
    final var oldClock = lastClock.get(which);
    lastClock = lastClock.set(which, newClock);
    return isTriggered(oldClock, newClock, trigger);
  }

  /**
   * Update the clock state with the actual state of the clock signal
   * It is assumed that the clock is sensitive to the rising edge of
   * the clock signal
   *
   * @param newClock the actual state of the clock signal
   * @param which which of the clock signals must be updated
   * @return true if the clock was triggered, false otherwise
   */
  public boolean updateClock(Value newClock, int which) {
    return updateClock(newClock, which, StdAttr.TRIG_RISING);
  }

  /**
   * Update the clock state with the actual state of the clock signal
   * It is assumed that clock[0] will be updated
   *
   * @param newClock the actual state of the clock signal
   * @param trigger the trigger mode pos/neg edge or high/low level
   * @return true if clock[0] was triggered, false otherwise
   */
  public boolean updateClock(Value newClock, Object trigger) {
    return updateClock(newClock, 0, trigger);
  }

  /**
   * Update the clock state with the actual state of the clock signal
   * It is assumed that clock[0] will be updated and that clock[0] is
   * sensitive to the rising edge of the clock signal
   *
   * @param newClock the actual state of the clock signal
   * @return true if a positive edge is detected on clock[0], false otherwise
   */
  public boolean updateClock(Value newClock) {
    return updateClock(newClock, 0, StdAttr.TRIG_RISING);
  }
}
