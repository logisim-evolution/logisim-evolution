/*
 * Logisim-evolution - digital logic design tool and simulator
 * © 2001 Logisim-evolution contributors
 *
 * https://github.com/logisim-evolution/
 *
 * This is free software released under GNU GPLv3 license
 */

package com.cburch.logisim.util;

public class Strings {

  private Strings() {
    throw new IllegalStateException("Utility class. No instantiation allowed.");
  }

  public static final LocaleManager S = new LocaleManager("resources/logisim", "util");
}
