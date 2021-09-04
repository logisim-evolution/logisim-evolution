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
import com.cburch.logisim.comp.Component;
import com.cburch.logisim.instance.InstanceComponent;
import java.util.HashSet;
import java.util.LinkedList;

public interface DynamicElementProvider {

  DynamicElement createDynamicElement(int x, int y, DynamicElement.Path path);

  static void removeDynamicElements(Circuit circuit, Component c) {
    if (!(c instanceof InstanceComponent)) return;
    HashSet<Circuit> allAffected = new HashSet<>();
    LinkedList<Circuit> todo = new LinkedList<>();
    todo.add(circuit);
    while (!todo.isEmpty()) {
      Circuit circ = todo.remove();
      if (allAffected.contains(circ)) continue;
      allAffected.add(circ);
      for (Circuit other : circ.getCircuitsUsingThis())
        if (!allAffected.contains(other)) todo.add(other);
    }
    for (Circuit circ : allAffected)
      circ.getAppearance().removeDynamicElement((InstanceComponent) c);
  }
}
