/*
 * Logisim-evolution - digital logic design tool and simulator
 * © 2001 Logisim-evolution contributors
 *
 * https://github.com/logisim-evolution/
 *
 * This is free software released under GNU GPLv3 license
 */

package com.cburch.logisim.soc.data;

import com.cburch.logisim.circuit.CircuitState;

public interface SocBusMasterInterface {
  void initializeTransaction(SocBusTransaction trans, String busId, CircuitState cstate);
}
