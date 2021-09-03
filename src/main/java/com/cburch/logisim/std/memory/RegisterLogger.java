/*
 * Logisim-evolution - digital logic design tool and simulator
 * Copyright by the Logisim-evolution developers
 *
 * https://github.com/logisim-evolution/
 *
 * This is free software released under GNU GPLv3 license
 */

package com.cburch.logisim.std.memory;

import com.cburch.logisim.data.BitWidth;
import com.cburch.logisim.data.Value;
import com.cburch.logisim.instance.InstanceLogger;
import com.cburch.logisim.instance.InstanceState;
import com.cburch.logisim.instance.StdAttr;

public class RegisterLogger extends InstanceLogger {
  @Override
  public String getLogName(InstanceState state, Object option) {
    final var ret = state.getAttributeValue(StdAttr.LABEL);
    return ret != null && !ret.equals("") ? ret : null;
  }

  @Override
  public BitWidth getBitWidth(InstanceState state, Object option) {
    return state.getAttributeValue(StdAttr.WIDTH);
  }

  @Override
  public Value getLogValue(InstanceState state, Object option) {
    var dataWidth = state.getAttributeValue(StdAttr.WIDTH);
    if (dataWidth == null) dataWidth = BitWidth.create(0);
    final var data = (RegisterData) state.getData();
    if (data == null) return Value.createKnown(dataWidth, 0);
    return data.value;
  }
}
