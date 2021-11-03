/*
 * Logisim-evolution - digital logic design tool and simulator
 * © 2001 Logisim-evolution contributors
 *
 * https://github.com/logisim-evolution/
 *
 * This is free software released under GNU GPLv3 license
 */

package com.cburch.logisim.gui.menu;

import com.cburch.logisim.circuit.CircuitState;
import com.cburch.logisim.circuit.Simulator;

public interface SimulateListener {
  void stateChangeRequested(Simulator sim, CircuitState state);
}
