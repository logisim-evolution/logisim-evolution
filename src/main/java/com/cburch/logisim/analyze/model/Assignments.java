/*
 * Logisim-evolution - digital logic design tool and simulator
 * © 2001 Logisim-evolution contributors
 *
 * https://github.com/logisim-evolution/
 *
 * This is free software released under GNU GPLv3 license
 */

package com.cburch.logisim.analyze.model;

import java.util.HashMap;
import java.util.Map;

class Assignments {
  private final Map<String, Boolean> map = new HashMap<>();

  public boolean get(String variable) {
    final var value = map.get(variable);
    return value != null && value;
  }

  public void put(String variable, boolean value) {
    map.put(variable, value);
  }
}
