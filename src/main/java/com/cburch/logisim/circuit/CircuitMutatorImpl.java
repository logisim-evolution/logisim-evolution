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

package com.cburch.logisim.circuit;

import com.cburch.logisim.comp.Component;
import com.cburch.logisim.data.Attribute;
import com.cburch.logisim.data.AttributeSet;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;

class CircuitMutatorImpl implements CircuitMutator {
  private final ArrayList<CircuitChange> log;
  private final HashMap<Circuit, ReplacementMap> replacements;
  private final HashSet<Circuit> modified;

  public CircuitMutatorImpl() {
    log = new ArrayList<>();
    replacements = new HashMap<>();
    modified = new HashSet<>();
  }

  public void add(Circuit circuit, Component comp) {
    modified.add(circuit);
    log.add(CircuitChange.add(circuit, comp));

    ReplacementMap repl = new ReplacementMap();
    repl.add(comp);
    getMap(circuit).append(repl);

    circuit.mutatorAdd(comp);
  }

  public void clear(Circuit circuit) {
    HashSet<Component> comps = new HashSet<>(circuit.getNonWires());
    comps.addAll(circuit.getWires());
    if (!comps.isEmpty()) modified.add(circuit);
    log.add(CircuitChange.clear(circuit, comps));

    ReplacementMap repl = new ReplacementMap();
    for (Component comp : comps) repl.remove(comp);
    getMap(circuit).append(repl);

    circuit.mutatorClear();
  }

  private ReplacementMap getMap(Circuit circuit) {
    ReplacementMap ret = replacements.get(circuit);
    if (ret == null) {
      ret = new ReplacementMap();
      replacements.put(circuit, ret);
    }
    return ret;
  }

  Collection<Circuit> getModifiedCircuits() {
    return Collections.unmodifiableSet(modified);
  }

  ReplacementMap getReplacementMap(Circuit circuit) {
    return replacements.get(circuit);
  }

  CircuitTransaction getReverseTransaction() {
    CircuitMutation ret = new CircuitMutation();
    ArrayList<CircuitChange> log = this.log;
    for (int i = log.size() - 1; i >= 0; i--) {
      ret.change(log.get(i).getReverseChange());
    }
    return ret;
  }

  void markModified(Circuit circuit) {
    modified.add(circuit);
  }

  public void remove(Circuit circuit, Component comp) {
    if (circuit.contains(comp)) {
      modified.add(circuit);
      log.add(CircuitChange.remove(circuit, comp));

      ReplacementMap repl = new ReplacementMap();
      repl.remove(comp);
      getMap(circuit).append(repl);

      circuit.mutatorRemove(comp);
    }
  }

  public void replace(Circuit circuit, Component prev, Component next) {
    replace(circuit, new ReplacementMap(prev, next));
  }

  public void replace(Circuit circuit, ReplacementMap repl) {
    if (!repl.isEmpty()) {
      modified.add(circuit);
      log.add(CircuitChange.replace(circuit, repl));

      repl.freeze();
      getMap(circuit).append(repl);

      for (Component c : repl.getRemovals()) {
        circuit.mutatorRemove(c);
      }
      for (Component c : repl.getAdditions()) {
        circuit.mutatorAdd(c);
      }
    }
  }

  public void set(Circuit circuit, Component comp, Attribute<?> attr, Object newValue) {
    if (circuit.contains(comp)) {
      modified.add(circuit);
      @SuppressWarnings("unchecked")
      Attribute<Object> a = (Attribute<Object>) attr;
      AttributeSet attrs = comp.getAttributeSet();
      Object oldValue = attrs.getValue(a);
      log.add(CircuitChange.set(circuit, comp, attr, oldValue, newValue));
      attrs.setValue(a, newValue);
    }
  }

  public void setForCircuit(Circuit circuit, Attribute<?> attr, Object newValue) {
    @SuppressWarnings("unchecked")
    Attribute<Object> a = (Attribute<Object>) attr;
    AttributeSet attrs = circuit.getStaticAttributes();
    Object oldValue = attrs.getValue(a);
    log.add(CircuitChange.setForCircuit(circuit, attr, oldValue, newValue));
    attrs.setValue(a, newValue);
    if (attr == CircuitAttributes.NAME_ATTR
        || attr == CircuitAttributes.NAMED_CIRCUIT_BOX_FIXED_SIZE) {
      circuit.getAppearance().recomputeDefaultAppearance();
    }
  }
}
