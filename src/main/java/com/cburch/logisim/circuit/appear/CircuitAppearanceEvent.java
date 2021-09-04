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
import lombok.Getter;

public class CircuitAppearanceEvent {
  public static final int APPEARANCE = 1;
  public static final int BOUNDS = 2;
  public static final int PORTS = 4;
  public static final int ALL_TYPES = 7;

  @Getter private final Circuit source;
  private final int affects;

  CircuitAppearanceEvent(Circuit circuit, int affects) {
    this.source = circuit;
    this.affects = affects;
  }

  public boolean isConcerning(int type) {
    return (affects & type) != 0;
  }
}
