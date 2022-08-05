/*
 * Logisim-evolution - digital logic design tool and simulator
 * Copyright by the Logisim-evolution developers
 *
 * https://github.com/logisim-evolution/
 *
 * This is free software released under GNU GPLv3 license
 */

package com.cburch.logisim.tools;

import com.cburch.logisim.util.LocaleManager;

public class Strings {

  private Strings() {
    throw new IllegalStateException("Utility class. No instantiation allowed.");
  }

  public static final LocaleManager S = new LocaleManager("resources/logisim", "tools");
}
