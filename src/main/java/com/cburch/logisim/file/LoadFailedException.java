/*
 * Logisim-evolution - digital logic design tool and simulator
 * Copyright by the Logisim-evolution developers
 *
 * https://github.com/logisim-evolution/
 *
 * This is free software released under GNU GPLv3 license
 */

package com.cburch.logisim.file;

public class LoadFailedException extends Exception {
  private static final long serialVersionUID = 1L;
  private final boolean shown;

  LoadFailedException(String desc) {
    this(desc, false);
  }

  LoadFailedException(String desc, boolean shown) {
    super(desc);
    this.shown = shown;
  }

  public boolean isShown() {
    return shown;
  }
}
