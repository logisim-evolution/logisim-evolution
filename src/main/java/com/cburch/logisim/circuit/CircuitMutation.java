/*
 * Logisim-evolution - digital logic design tool and simulator
 * Copyright by the Logisim-evolution developers
 *
 * https://github.com/logisim-evolution/
 *
 * This is free software released under GNU GPLv3 license
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
  private final Circuit primary;
  private final List<CircuitChange> changes;

  CircuitMutation() {
    this(null);
  }

  public CircuitMutation(Circuit circuit) {
    this.primary = circuit;
    this.changes = new ArrayList<>();
  }

  public void add(Component comp) {
    changes.add(CircuitChange.add(primary, comp));
  }

  public void addAll(Collection<? extends Component> comps) {
    changes.add(CircuitChange.addAll(primary, new ArrayList<>(comps)));
  }

  public void change(CircuitChange change) {
    changes.add(change);
  }

  public void clear() {
    changes.add(CircuitChange.clear(primary, null));
  }

  @Override
  protected Map<Circuit, Integer> getAccessedCircuits() {
    final var accessMap = new HashMap<Circuit, Integer>();
    final var supercircsDone = new HashSet<Circuit>();
    final var vhdlDone = new HashSet<VhdlEntity>();
    final var siblingsDone = new HashSet<ComponentFactory>();
    for (final var change : changes) {
      final var circ = change.getCircuit();
      accessMap.put(circ, READ_WRITE);

      if (change.concernsSupercircuit()) {
        final var isFirstForCirc = supercircsDone.add(circ);
        if (isFirstForCirc) {
          for (final var supercirc : circ.getCircuitsUsingThis()) {
            accessMap.put(supercirc, READ_WRITE);
          }
        }
      }

      if (change.concernsSiblingComponents()) {
        final var factory = change.getComponent().getFactory();
        final var isFirstForSibling = siblingsDone.add(factory);
        if (isFirstForSibling) {
          if (factory instanceof SubcircuitFactory sub) {
            final var sibling = sub.getSubcircuit();
            final var isFirstForCirc = supercircsDone.add(sibling);
            if (isFirstForCirc) {
              for (final var supercirc : sibling.getCircuitsUsingThis()) {
                accessMap.put(supercirc, READ_WRITE);
              }
            }
          } else if (factory instanceof VhdlEntity sibling) {
            final var isFirstForVhdl = vhdlDone.add(sibling);
            if (isFirstForVhdl) {
              for (final var supercirc : sibling.getCircuitsUsingThis()) {
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
    changes.add(CircuitChange.removeAll(primary, new ArrayList<>(comps)));
  }

  public void replace(Component oldComp, Component newComp) {
    final var repl = new ReplacementMap(oldComp, newComp);
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
    for (final var change : changes) {
      final var circ = change.getCircuit();
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
