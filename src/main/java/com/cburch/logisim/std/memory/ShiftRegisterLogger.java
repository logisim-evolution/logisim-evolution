/*
 * Logisim-evolution - digital logic design tool and simulator
 * Copyright by the Logisim-evolution developers
 *
 * https://github.com/logisim-evolution/
 *
 * This is free software released under GNU GPLv3 license
 */

package com.cburch.logisim.std.memory;

import static com.cburch.logisim.std.Strings.S;

import com.cburch.logisim.data.BitWidth;
import com.cburch.logisim.data.Value;
import com.cburch.logisim.instance.InstanceLogger;
import com.cburch.logisim.instance.InstanceState;
import com.cburch.logisim.instance.StdAttr;

public class ShiftRegisterLogger extends InstanceLogger {
  @Override
  public String getLogName(InstanceState state, Object option) {
    var inName = state.getAttributeValue(StdAttr.LABEL);
    if (inName == null || inName.equals("")) {
      inName = S.get("shiftRegisterComponent") + state.getInstance().getLocation();
    }
    if (option instanceof Integer) {
      return inName + "[" + option + "]";
    } else {
      return inName;
    }
  }

  @Override
  public BitWidth getBitWidth(InstanceState state, Object option) {
    return state.getAttributeValue(StdAttr.WIDTH);
  }

  @Override
  public Object[] getLogOptions(InstanceState state) {
    final var stages = state.getAttributeValue(ShiftRegister.ATTR_LENGTH);
    final var ret = new Object[stages];
    for (int i = 0; i < ret.length; i++) {
      ret[i] = i;
    }
    return ret;
  }

  @Override
  public Value getLogValue(InstanceState state, Object option) {
    var dataWidth = state.getAttributeValue(StdAttr.WIDTH);
    if (dataWidth == null) dataWidth = BitWidth.create(0);
    final var data = (ShiftRegisterData) state.getData();
    if (data == null) {
      return Value.createKnown(dataWidth, 0);
    } else {
      int index = option == null ? 0 : (Integer) option;
      return data.get(index);
    }
  }
}
