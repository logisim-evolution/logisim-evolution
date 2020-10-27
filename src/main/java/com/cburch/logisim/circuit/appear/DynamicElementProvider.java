/**
 * This file is part of logisim-evolution.
 *
 * Logisim-evolution is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by the
 * Free Software Foundation, either version 3 of the License, or (at your
 * option) any later version.
 *
 * Logisim-evolution is distributed in the hope that it will be useful, but
 * WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 * or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
 * for more details.
 *
 * You should have received a copy of the GNU General Public License along 
 * with logisim-evolution. If not, see <http://www.gnu.org/licenses/>.
 *
 * Original code by Carl Burch (http://www.cburch.com), 2011.
 * Subsequent modifications by:
 *   + College of the Holy Cross
 *     http://www.holycross.edu
 *   + Haute École Spécialisée Bernoise/Berner Fachhochschule
 *     http://www.bfh.ch
 *   + Haute École du paysage, d'ingénierie et d'architecture de Genève
 *     http://hepia.hesge.ch/
 *   + Haute École d'Ingénierie et de Gestion du Canton de Vaud
 *     http://www.heig-vd.ch/
 */

package com.cburch.logisim.circuit.appear;

import java.util.HashSet;
import java.util.LinkedList;

import com.cburch.logisim.circuit.Circuit;
import com.cburch.logisim.comp.Component;
import com.cburch.logisim.instance.InstanceComponent;

public interface DynamicElementProvider {

  public DynamicElement createDynamicElement(int x, int y, DynamicElement.Path path);
  
  public static void removeDynamicElements(Circuit circuit, Component c){
	if (!(c instanceof InstanceComponent)) return;
    HashSet<Circuit> allAffected = new HashSet<Circuit>();
    LinkedList<Circuit> todo = new LinkedList<Circuit>();
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
