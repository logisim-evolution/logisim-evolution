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

  @Override
  public void add(Circuit circuit, Component comp) {
    modified.add(circuit);
    log.add(CircuitChange.add(circuit, comp));

    final var repl = new ReplacementMap();
    repl.add(comp);
    getMap(circuit).append(repl);

    circuit.mutatorAdd(comp);
  }

  @Override
  public void clear(Circuit circuit) {
    final var comps = new HashSet<>(circuit.getNonWires());
    comps.addAll(circuit.getWires());
    if (!comps.isEmpty()) modified.add(circuit);
    log.add(CircuitChange.clear(circuit, comps));

    final var repl = new ReplacementMap();
    for (final var comp : comps) repl.remove(comp);
    getMap(circuit).append(repl);

    circuit.mutatorClear();
  }

  private ReplacementMap getMap(Circuit circuit) {
    var ret = replacements.get(circuit);
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
    final var ret = new CircuitMutation();
    final var log = this.log;
    for (var i = log.size() - 1; i >= 0; i--) {
      ret.change(log.get(i).getReverseChange());
    }
    return ret;
  }

  void markModified(Circuit circuit) {
    modified.add(circuit);
  }

  @Override
  public void remove(Circuit circuit, Component comp) {
    if (circuit.contains(comp)) {
      modified.add(circuit);
      log.add(CircuitChange.remove(circuit, comp));

      final var repl = new ReplacementMap();
      repl.remove(comp);
      getMap(circuit).append(repl);

      circuit.mutatorRemove(comp);
    }
  }

  @Override
  public void replace(Circuit circuit, Component prev, Component next) {
    replace(circuit, new ReplacementMap(prev, next));
  }

  @Override
  public void replace(Circuit circuit, ReplacementMap repl) {
    if (!repl.isEmpty()) {
      modified.add(circuit);
      log.add(CircuitChange.replace(circuit, repl));

      repl.freeze();
      getMap(circuit).append(repl);

      for (final var component : repl.getRemovals()) {
        circuit.mutatorRemove(component);
      }
      for (final var component : repl.getAdditions()) {
        circuit.mutatorAdd(component);
      }
    }
  }

  @Override
  public void set(Circuit circuit, Component comp, Attribute<?> attr, Object newValue) {
    if (circuit.contains(comp)) {
      modified.add(circuit);
      @SuppressWarnings("unchecked")
      final var a = (Attribute<Object>) attr;
      final var attrs = comp.getAttributeSet();
      final var oldValue = attrs.getValue(a);
      log.add(CircuitChange.set(circuit, comp, attr, oldValue, newValue));
      attrs.setValue(a, newValue);
    }
  }

  @Override
  public void setForCircuit(Circuit circuit, Attribute<?> attr, Object newValue) {
    @SuppressWarnings("unchecked")
    final var a = (Attribute<Object>) attr;
    final var attrs = circuit.getStaticAttributes();
    final var oldValue = attrs.getValue(a);
    log.add(CircuitChange.setForCircuit(circuit, attr, oldValue, newValue));
    attrs.setValue(a, newValue);
    if (attr == CircuitAttributes.NAME_ATTR
        || attr == CircuitAttributes.NAMED_CIRCUIT_BOX_FIXED_SIZE) {
      circuit.getAppearance().recomputeDefaultAppearance();
    }
  }
}
