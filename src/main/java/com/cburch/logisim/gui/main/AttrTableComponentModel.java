/*
 * Logisim-evolution - digital logic design tool and simulator
 * Copyright by the Logisim-evolution developers
 *
 * https://github.com/logisim-evolution/
 *
 * This is free software released under GNU GPLv3 license
 */

package com.cburch.logisim.gui.main;

import static com.cburch.logisim.gui.Strings.S;

import com.cburch.logisim.circuit.Circuit;
import com.cburch.logisim.comp.Component;
import com.cburch.logisim.data.Attribute;
import com.cburch.logisim.gui.generic.AttrTableSetException;
import com.cburch.logisim.gui.generic.AttributeSetTableModel;
import com.cburch.logisim.instance.StdAttr;
import com.cburch.logisim.proj.Project;
import com.cburch.logisim.tools.SetAttributeAction;

class AttrTableComponentModel extends AttributeSetTableModel {
  final Project proj;
  final Circuit circ;
  final Component comp;

  AttrTableComponentModel(final Project proj, final Circuit circ, final Component comp) {
    super(comp.getAttributeSet());
    this.proj = proj;
    this.circ = circ;
    this.comp = comp;
    setInstance(comp.getFactory());
  }

  public Circuit getCircuit() {
    return circ;
  }

  public Component getComponent() {
    return comp;
  }

  @Override
  public String getTitle() {
    final var label = comp.getAttributeSet().getValue(StdAttr.LABEL);
    final var loc = comp.getLocation();
    var s = comp.getFactory().getDisplayName();
    if (label != null && label.length() > 0) s += " \"" + label + "\"";
    else if (loc != null) s += " " + loc;
    return s;
  }

  @Override
  public void setValueRequested(final Attribute<Object> attr, final Object value) throws AttrTableSetException {
    if (!proj.getLogisimFile().contains(circ)) {
      final var msg = S.get("cannotModifyCircuitError");
      throw new AttrTableSetException(msg);
    } else {
      final var act = new SetAttributeAction(circ, S.getter("changeAttributeAction"));
      final var compAttrSet = comp.getAttributeSet();
      if (compAttrSet != null) {
        final var mayBeChangedList = compAttrSet.attributesMayAlsoBeChanged(attr, value);
        if (mayBeChangedList != null) {
          for (final var mayChangeAttr : mayBeChangedList) {
            // mayChangeAttr is set to its current value to have it restored on undo
            act.set(comp, mayChangeAttr, compAttrSet.getValue(mayChangeAttr));
          }
        }
      }
      act.set(comp, attr, value);
      proj.doAction(act);
    }
  }
}
