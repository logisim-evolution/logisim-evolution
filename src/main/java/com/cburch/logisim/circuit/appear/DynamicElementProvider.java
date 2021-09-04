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
import lombok.val;

public interface DynamicElementProvider {

  DynamicElement createDynamicElement(int x, int y, DynamicElement.Path path);

  static void removeDynamicElements(Circuit circuit, Component c) {
    if (!(c instanceof InstanceComponent)) return;
    val allAffected = new HashSet<Circuit>();
    val todo = new LinkedList<Circuit>();
    todo.add(circuit);
    while (!todo.isEmpty()) {
      val circ = todo.remove();
      if (allAffected.contains(circ)) continue;
      allAffected.add(circ);
      for (val other : circ.getCircuitsUsingThis()) {
        if (!allAffected.contains(other)) todo.add(other);
      }
    }
    for (val circ : allAffected) {
      circ.getAppearance().removeDynamicElement((InstanceComponent) c);
    }
  }
}
