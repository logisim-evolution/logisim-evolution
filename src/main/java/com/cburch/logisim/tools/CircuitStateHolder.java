/*
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

package com.cburch.logisim.tools;

import com.cburch.logisim.circuit.Circuit;
import com.cburch.logisim.circuit.CircuitListener;
import com.cburch.logisim.circuit.CircuitState;
import com.cburch.logisim.circuit.SubcircuitFactory;
import com.cburch.logisim.comp.Component;
import com.cburch.logisim.comp.ComponentListener;
import com.cburch.logisim.soc.data.SocSupport;
import java.util.ArrayList;

public interface CircuitStateHolder {
  class HierarchyInfo {
    private final Circuit mainCircuit;
    private final ArrayList<Component> components = new ArrayList<>();

    public HierarchyInfo(Circuit circ) {
      mainCircuit = circ;
    }

    public void addComponent(Component comp) {
      components.add(comp);
    }

    public void registerCircuitListener(CircuitListener l) {
      if (mainCircuit != null) mainCircuit.addCircuitListener(l);
      for (final var c : components) {
        if (c.getFactory() instanceof SubcircuitFactory) {
          final var f = (SubcircuitFactory) c.getFactory();
          f.getSubcircuit().addCircuitListener(l);
        }
      }
    }

    public void deregisterCircuitListener(CircuitListener l) {
      if (mainCircuit != null) mainCircuit.addCircuitListener(l);
      for (final var c : components) {
        if (c.getFactory() instanceof SubcircuitFactory) {
          final var f = (SubcircuitFactory) c.getFactory();
          f.getSubcircuit().removeCircuitListener(l);
        }
      }
    }

    public void registerComponentListener(ComponentListener l) {
      for (Component c : components) c.addComponentListener(l);
    }

    public void deregisterComponentListener(ComponentListener l) {
      for (Component c : components) c.addComponentListener(l);
    }

    public String getName() {
      final var s = new StringBuilder();
      if (mainCircuit != null) s.append(mainCircuit.getName());
      for (final var c : components) {
        if (s.length() != 0) s.append(":");
        s.append(SocSupport.getComponentName(c));
      }
      return s.toString();
    }

    public HierarchyInfo getCopy() {
      final var copy = new HierarchyInfo(mainCircuit);
      for (final var c : components) copy.addComponent(c);
      return copy;
    }
  }

  void setCircuitState(CircuitState state);

  void setHierarchyName(HierarchyInfo csh);
}
