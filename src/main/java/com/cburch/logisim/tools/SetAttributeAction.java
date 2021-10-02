/*
 * Logisim-evolution - digital logic design tool and simulator
 * Copyright by the Logisim-evolution developers
 *
 * https://github.com/logisim-evolution/
 *
 * This is free software released under GNU GPLv3 license
 */

package com.cburch.logisim.tools;

import com.cburch.logisim.circuit.Circuit;
import com.cburch.logisim.circuit.CircuitMutation;
import com.cburch.logisim.circuit.CircuitTransaction;
import com.cburch.logisim.comp.Component;
import com.cburch.logisim.data.Attribute;
import com.cburch.logisim.proj.Action;
import com.cburch.logisim.proj.Project;
import com.cburch.logisim.util.StringGetter;
import java.util.ArrayList;
import java.util.List;

public class SetAttributeAction extends Action {
  private final StringGetter nameGetter;
  private final Circuit circuit;
  private final List<Component> comps;
  private final List<Attribute<Object>> attrs;
  private final List<Object> values;
  private final List<Object> oldValues;
  private CircuitTransaction xnReverse;

  public SetAttributeAction(Circuit circuit, StringGetter nameGetter) {
    this.nameGetter = nameGetter;
    this.circuit = circuit;
    this.comps = new ArrayList<>();
    this.attrs = new ArrayList<>();
    this.values = new ArrayList<>();
    this.oldValues = new ArrayList<>();
  }

  @Override
  public void doIt(Project proj) {
    final var xn = new CircuitMutation(circuit);
    final var len = values.size();
    oldValues.clear();
    for (var i = 0; i < len; i++) {
      final var comp = comps.get(i);
      final var attr = attrs.get(i);
      final var value = values.get(i);
      if (circuit.contains(comp)) {
        oldValues.add(null);
        xn.set(comp, attr, value);
      } else {
        final var compAttrs = comp.getAttributeSet();
        oldValues.add(compAttrs.getValue(attr));
        compAttrs.setValue(attr, value);
      }
    }

    if (!xn.isEmpty()) {
      final var result = xn.execute();
      xnReverse = result.getReverseTransaction();
    }
  }

  @Override
  public String getName() {
    return nameGetter.toString();
  }

  public boolean isEmpty() {
    return comps.isEmpty();
  }

  public void set(Component comp, Attribute<?> attr, Object value) {
    @SuppressWarnings("unchecked")
    final var a = (Attribute<Object>) attr;
    comps.add(comp);
    attrs.add(a);
    values.add(value);
  }

  @Override
  public void undo(Project proj) {
    if (xnReverse != null) xnReverse.execute();
    for (var i = oldValues.size() - 1; i >= 0; i--) {
      final var comp = comps.get(i);
      final var attr = attrs.get(i);
      final var value = oldValues.get(i);
      if (value != null) {
        comp.getAttributeSet().setValue(attr, value);
      }
    }
  }
}
