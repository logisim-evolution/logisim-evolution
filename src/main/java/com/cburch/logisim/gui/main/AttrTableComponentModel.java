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
import lombok.Getter;
import lombok.val;

class AttrTableComponentModel extends AttributeSetTableModel {
  final Project proj;

  @Getter
  final Circuit circuit;
  @Getter
  final Component component;

  AttrTableComponentModel(Project proj, Circuit circ, Component comp) {
    super(comp.getAttributeSet());
    this.proj = proj;
    this.circuit = circ;
    this.component = comp;
    setInstance(comp.getFactory());
  }

  @Override
  public String getTitle() {
    val label = component.getAttributeSet().getValue(StdAttr.LABEL);
    val loc = component.getLocation();
    var s = component.getFactory().getDisplayName();
    if (label != null && label.length() > 0)
      s += " \"" + label + "\"";
    else if (loc != null)
      s += " " + loc;
    return s;
  }

  @Override
  public void setValueRequested(Attribute<Object> attr, Object value) throws AttrTableSetException {
    if (!proj.getLogisimFile().contains(circuit)) {
      throw new AttrTableSetException(S.get("cannotModifyCircuitError"));
    } else {
      val act = new SetAttributeAction(circuit, S.getter("changeAttributeAction"));
      act.set(component, attr, value);
      proj.doAction(act);
    }
  }
}
