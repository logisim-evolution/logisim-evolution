/*
 * Logisim-evolution - digital logic design tool and simulator
 * Copyright by the Logisim-evolution developers
 *
 * https://github.com/logisim-evolution/
 *
 * This is free software released under GNU GPLv3 license
 */

package com.cburch.logisim.circuit.appear;

import com.cburch.logisim.circuit.Circuit;
import com.cburch.logisim.circuit.ReplacementMap;
import com.cburch.logisim.comp.Component;
import com.cburch.logisim.instance.InstanceComponent;
import java.util.HashSet;
import java.util.LinkedList;

public interface DynamicElementProvider {

  DynamicElement createDynamicElement(int x, int y, DynamicElement.Path path);

  private static HashSet<Circuit> collectAffectedCircuits(Circuit circuit) {
    final var allAffected = new HashSet<Circuit>();
    final var todo = new LinkedList<Circuit>();
    todo.add(circuit);
    while (!todo.isEmpty()) {
      final var circ = todo.remove();
      if (allAffected.contains(circ)) continue;
      allAffected.add(circ);
      for (final var other : circ.getCircuitsUsingThis())
        if (!allAffected.contains(other)) todo.add(other);
    }
    return allAffected;
  }

  static void removeDynamicElements(Circuit circuit, Component comp) {
    if (!(comp instanceof InstanceComponent)) return;
    for (final var circ : collectAffectedCircuits(circuit))
      circ.getAppearance().removeDynamicElement((InstanceComponent) comp);
  }

  static void repairDynamicElementPaths(Circuit circuit, ReplacementMap replacements) {
    if (replacements.isEmpty()) return;
    for (final var circ : collectAffectedCircuits(circuit))
      circ.getAppearance().repairDynamicElementPaths(replacements);
  }
}
