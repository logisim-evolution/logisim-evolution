/*
 * Logisim-evolution - digital logic design tool and simulator
 * Copyright by Logisim-evolution developers
 * 
 * https://github.com/logisim-evolution/
 * 
 * This is free software released under GNU GPLv3 license
 */

package com.cburch.logisim.comp;

import com.cburch.logisim.circuit.CircuitState;
import com.cburch.logisim.gui.main.Canvas;

public class ComponentUserEvent {
  private final Canvas canvas;
  private int x = 0;
  private int y = 0;

  ComponentUserEvent(Canvas canvas) {
    this.canvas = canvas;
  }

  public ComponentUserEvent(Canvas canvas, int x, int y) {
    this.canvas = canvas;
    this.x = x;
    this.y = y;
  }

  public Canvas getCanvas() {
    return canvas;
  }

  public CircuitState getCircuitState() {
    return canvas.getCircuitState();
  }

  public int getX() {
    return x;
  }

  public int getY() {
    return y;
  }
}
