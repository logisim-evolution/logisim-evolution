/*
 * Logisim-evolution - digital logic design tool and simulator
 * Copyright by the Logisim-evolution developers
 *
 * https://github.com/logisim-evolution/
 *
 * This is free software released under GNU GPLv3 license
 */

package com.cburch.logisim.circuit;

import com.cburch.logisim.comp.Component;
import com.cburch.logisim.data.Attribute;

public interface CircuitMutator {
  void add(Circuit circuit, Component comp);

  void clear(Circuit circuit);

  void remove(Circuit circuit, Component comp);

  void replace(Circuit circuit, Component oldComponent, Component newComponent);

  void replace(Circuit circuit, ReplacementMap replacements);

  void set(Circuit circuit, Component comp, Attribute<?> attr, Object value);

  void setForCircuit(Circuit circuit, Attribute<?> attr, Object value);
}
