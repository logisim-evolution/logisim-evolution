/*
 * Logisim-evolution - digital logic design tool and simulator
 * Copyright by the Logisim-evolution developers
 *
 * https://github.com/logisim-evolution/
 *
 * This is free software released under GNU GPLv3 license
 */

package com.cburch.logisim.util;

public interface LocaleListener {
  /**
   * Reacts to locale changes by setting the corresponding preference value.
   */
  void localeChanged();
}
