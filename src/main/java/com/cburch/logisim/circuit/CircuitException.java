/*
 * Logisim-evolution - digital logic design tool and simulator
 * © 2001 Logisim-evolution contributors
 *
 * https://github.com/logisim-evolution/
 *
 * This is free software released under GNU GPLv3 license
 */

package com.cburch.logisim.circuit;

public class CircuitException extends RuntimeException {
  private static final long serialVersionUID = 1L;

  public CircuitException(String msg) {
    super(msg);
  }
}
