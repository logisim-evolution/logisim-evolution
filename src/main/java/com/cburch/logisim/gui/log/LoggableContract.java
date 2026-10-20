/*
 * Logisim-evolution - digital logic design tool and simulator
 * Copyright by the Logisim-evolution developers
 *
 * https://github.com/logisim-evolution/
 *
 * This is free software released under GNU GPLv3 license
 */

package com.cburch.logisim.gui.log;

import com.cburch.logisim.circuit.CircuitState;
import com.cburch.logisim.data.BitWidth;
import com.cburch.logisim.data.Value;

public interface LoggableContract {
  String getLogName(Object option);

  BitWidth getBitWidth(Object option);

  Object[] getLogOptions();

  Value getLogValue(CircuitState state, Object option);

  boolean isInput(Object option);
}
