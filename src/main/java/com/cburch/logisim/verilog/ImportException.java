/*
 * Logisim-evolution - digital logic design tool and simulator
 * Copyright by the Logisim-evolution developers
 * https://github.com/logisim-evolution/
 * This is free software released under GNU GPLv3 license
 */
package com.cburch.logisim.verilog;

/** A diagnostic for HDL constructs that cannot be represented by the importer. */
public class ImportException extends IllegalArgumentException {
  private static final long serialVersionUID = 1L;

  public ImportException(String message) {
    super(message);
  }
}
