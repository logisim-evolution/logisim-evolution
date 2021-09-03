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

public class CircuitAppearanceEvent {
  public static final int APPEARANCE = 1;
  public static final int BOUNDS = 2;
  public static final int PORTS = 4;
  public static final int ALL_TYPES = 7;

  private final Circuit circuit;
  private final int affects;

  CircuitAppearanceEvent(Circuit circuit, int affects) {
    this.circuit = circuit;
    this.affects = affects;
  }

  public Circuit getSource() {
    return circuit;
  }

  public boolean isConcerning(int type) {
    return (affects & type) != 0;
  }
}
