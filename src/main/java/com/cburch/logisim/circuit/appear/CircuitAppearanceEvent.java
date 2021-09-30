/*
 * Logisim-evolution - digital logic design tool and simulator
 * Copyright by the Logisim-evolution developers
 *
 * https://github.com/logisim-evolution/
 *
 * This is free software released under GNU GPLv3 license
 */

package com.cburch.logisim.circuit.appear;

import com.cburch.logisim.circuit.Circuit;

// NOTE: silly members' names are mostly to avoid refactoring of the whole codebase due to record's
// getters not using Bean naming convention (so i.e. `foo()` instead of `getFoo()`. We may change
// that in future, but for now it looks stupid in this file only.
public record CircuitAppearanceEvent(Circuit getCircuit, int getAffects) {

  public static final int APPEARANCE = 0b001;
  public static final int BOUNDS = 0b010;
  public static final int PORTS = 0b100;
  public static final int ALL_TYPES = APPEARANCE | BOUNDS | PORTS;

  public boolean isConcerning(int type) {
    return (getAffects() & type) != 0;
  }

}
