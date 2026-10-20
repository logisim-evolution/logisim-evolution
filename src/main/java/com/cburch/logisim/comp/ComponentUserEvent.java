/*
 * Logisim-evolution - digital logic design tool and simulator
 * Copyright by the Logisim-evolution developers
 *
 * https://github.com/logisim-evolution/
 *
 * This is free software released under GNU GPLv3 license
 */

package com.cburch.logisim.comp;

import com.cburch.logisim.circuit.CircuitState;
import com.cburch.logisim.gui.main.Canvas;

// NOTE: silly members' names are mostly to avoid refactoring of the whole codebase due to record's
// getters not using Bean naming convention (so i.e. `foo()` instead of `getFoo()`. We may change
// that in future, but for now it looks stupid in this file only.
public record ComponentUserEvent(Canvas getCanvas, int getX, int getY) {

  ComponentUserEvent(Canvas canvas) {
    this(canvas, 0, 0);
  }

  public CircuitState getCircuitState() {
    return getCanvas.getCircuitState();
  }

}
