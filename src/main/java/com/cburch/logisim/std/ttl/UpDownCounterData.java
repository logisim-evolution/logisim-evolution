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
import com.cburch.logisim.instance.InstanceData;
import com.cburch.logisim.prefs.AppPreferences;

public class UpDownCounterData extends ClockState implements InstanceData {

  private static final BitWidth WIDTH = BitWidth.create(4);

  private Value value;
  private Value downPrev;
  private Value upPrev;
  private Value carry;
  private Value borrow;

  public UpDownCounterData() {
    value =
        (AppPreferences.Memory_Startup_Unknown.get())
            ? Value.createUnknown(WIDTH)
            : Value.createKnown(WIDTH, 0);
    downPrev = Value.FALSE;
    upPrev = Value.FALSE;
    carry = Value.TRUE;
    borrow = Value.TRUE;
  }

  public void setAll(Value value, Value carry, Value borrow, Value down, Value up) {
    this.value = value;
    this.carry = carry;
    this.borrow = borrow;
    this.upPrev = up;
    this.downPrev = down;
  }

  public Value getValue() {
    return value;
  }

  public Value getCarry() {
    return carry;
  }

  public Value getBorrow() {
    return borrow;
  }

  public Value getDownPrev() {
    return downPrev;
  }

  public Value getUpPrev() {
    return upPrev;
  }
}
