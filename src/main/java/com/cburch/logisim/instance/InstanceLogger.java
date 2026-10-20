/*
 * Logisim-evolution - digital logic design tool and simulator
 * Copyright by the Logisim-evolution developers
 *
 * https://github.com/logisim-evolution/
 *
 * This is free software released under GNU GPLv3 license
 */

package com.cburch.logisim.instance;

import com.cburch.logisim.data.BitWidth;
import com.cburch.logisim.data.Value;

public abstract class InstanceLogger {
  public abstract String getLogName(InstanceState state, Object option);

  public abstract BitWidth getBitWidth(InstanceState state, Object option);

  public Object[] getLogOptions(InstanceState state) {
    return null;
  }

  public abstract Value getLogValue(InstanceState state, Object option);

  public boolean isInput(InstanceState state, Object option) {
    return false;
  }
}
