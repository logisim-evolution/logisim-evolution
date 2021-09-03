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

public class TtlRegisterData extends ClockState implements InstanceData {

  private Value value;
  private final BitWidth bits;

  public TtlRegisterData(BitWidth width) {
    value =
        (AppPreferences.Memory_Startup_Unknown.get())
            ? Value.createUnknown(width)
            : Value.createKnown(width, 0);
    bits = width;
  }

  public void setValue(Value value) {
    this.value = value;
  }

  public Value getValue() {
    return value;
  }

  public BitWidth getWidth() {
    return bits;
  }
}
