/*
 * Logisim-evolution - digital logic design tool and simulator
 * Copyright by the Logisim-evolution developers
 *
 * https://github.com/logisim-evolution/
 *
 * This is free software released under GNU GPLv3 license
 */

package com.cburch.logisim.file;

// FIXME: this is unchecked exception. We most likely shall convert this to checked one.
public class LoaderException extends RuntimeException {
  private static final long serialVersionUID = 1L;
  private final boolean shown;

  LoaderException(String desc) {
    this(desc, false);
  }

  LoaderException(String desc, boolean shown) {
    super(desc);
    this.shown = shown;
  }

  public boolean isShown() {
    return shown;
  }
}
