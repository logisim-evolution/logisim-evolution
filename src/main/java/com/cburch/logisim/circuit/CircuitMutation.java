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

package com.cburch.logisim.circuit;

import static com.cburch.logisim.circuit.Strings.S;

import com.cburch.logisim.comp.Component;
import com.cburch.logisim.comp.ComponentFactory;
import com.cburch.logisim.data.Attribute;
import com.cburch.logisim.proj.Action;
import com.cburch.logisim.util.StringGetter;
import com.cburch.logisim.vhdl.base.VhdlEntity;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;

public final class CircuitMutation extends CircuitTransaction {
  private Circuit primary;
  private List<CircuitChange> changes;

  CircuitMutation() {
    this(null);
  }

  public CircuitMutation(Circuit circuit) {
    this.primary = circuit;
    this.changes = new ArrayList<CircuitChange>();
  }

  public void add(Component comp) {
    changes.add(CircuitChange.add(primary, comp));
  }

  public void addAll(Collection<? extends Component> comps) {
    changes.add(CircuitChange.addAll(primary, new ArrayList<Component>(comps)));
  }

  public void change(CircuitChange change) {
    changes.add(change);
  }

  public void clear() {
    changes.add(CircuitChange.clear(primary, null));
  }

  @Override
  protected Map<Circuit, Integer> getAccessedCircuits() {
    HashMap<Circuit, Integer> accessMap = new HashMap<>();
    HashSet<Circuit> supercircsDone = new HashSet<Circuit>();
    HashSet<VhdlEntity> vhdlDone = new HashSet<>();
    HashSet<ComponentFactory> siblingsDone = new HashSet<>();
    for (CircuitChange change : changes) {
      Circuit circ = change.getCircuit();
      accessMap.put(circ, READ_WRITE);

      if (change.concernsSupercircuit()) {
        boolean isFirstForCirc = supercircsDone.add(circ);
        if (isFirstForCirc) {
          for (Circuit supercirc : circ.getCircuitsUsingThis()) {
            accessMap.put(supercirc, READ_WRITE);
          }
        }
      }

      if (change.concernsSiblingComponents()) {
        ComponentFactory factory = change.getComponent().getFactory();
        boolean isFirstForSibling = siblingsDone.add(factory);
        if (isFirstForSibling) {
          if (factory instanceof SubcircuitFactory) {
            Circuit sibling = ((SubcircuitFactory) factory).getSubcircuit();
            boolean isFirstForCirc = supercircsDone.add(sibling);
            if (isFirstForCirc) {
              for (Circuit supercirc : sibling.getCircuitsUsingThis()) {
                accessMap.put(supercirc, READ_WRITE);
              }
            }
          } else if (factory instanceof VhdlEntity) {
            VhdlEntity sibling = (VhdlEntity) factory;
            boolean isFirstForVhdl = vhdlDone.add(sibling);
            if (isFirstForVhdl) {
              for (Circuit supercirc : sibling.getCircuitsUsingThis()) {
                accessMap.put(supercirc, READ_WRITE);
              }
            }
          }
        }
      }
    }
    return accessMap;
  }

  public boolean isEmpty() {
    return changes.isEmpty();
  }

  public void remove(Component comp) {
    changes.add(CircuitChange.remove(primary, comp));
  }

  public void removeAll(Collection<? extends Component> comps) {
    changes.add(CircuitChange.removeAll(primary, new ArrayList<Component>(comps)));
  }

  public void replace(Component oldComp, Component newComp) {
    ReplacementMap repl = new ReplacementMap(oldComp, newComp);
    changes.add(CircuitChange.replace(primary, repl));
  }

  public void replace(ReplacementMap replacements) {
    if (!replacements.isEmpty()) {
      replacements.freeze();
      changes.add(CircuitChange.replace(primary, replacements));
    }
  }

  @Override
  protected void run(CircuitMutator mutator) {
    Circuit curCircuit = null;
    ReplacementMap curReplacements = null;
    for (CircuitChange change : changes) {
      Circuit circ = change.getCircuit();
      if (circ != curCircuit) {
        if (curCircuit != null) {
          mutator.replace(curCircuit, curReplacements);
        }
        curCircuit = circ;
        curReplacements = new ReplacementMap();
      }
      change.execute(mutator, curReplacements);
    }
    if (curCircuit != null) {
      mutator.replace(curCircuit, curReplacements);
    }
  }

  public void set(Component comp, Attribute<?> attr, Object value) {
    changes.add(CircuitChange.set(primary, comp, attr, value));
  }

  public void setForCircuit(Attribute<?> attr, Object value) {
    changes.add(CircuitChange.setForCircuit(primary, attr, value));
  }

  public Action toAction(StringGetter name) {
    if (name == null) name = S.getter("unknownChangeAction");
    return new CircuitAction(name, this);
  }
}
