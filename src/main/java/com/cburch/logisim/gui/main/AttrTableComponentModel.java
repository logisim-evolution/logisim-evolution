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
import com.cburch.logisim.data.Location;
import com.cburch.logisim.gui.generic.AttrTableSetException;
import com.cburch.logisim.gui.generic.AttributeSetTableModel;
import com.cburch.logisim.instance.StdAttr;
import com.cburch.logisim.proj.Project;
import com.cburch.logisim.tools.SetAttributeAction;

class AttrTableComponentModel extends AttributeSetTableModel {
  final Project proj;
  final Circuit circ;
  final Component comp;

  AttrTableComponentModel(Project proj, Circuit circ, Component comp) {
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
    String label = comp.getAttributeSet().getValue(StdAttr.LABEL);
    Location loc = comp.getLocation();
    String s = comp.getFactory().getDisplayName();
    if (label != null && label.length() > 0)
      s += " \"" + label + "\"";
    else if (loc != null)
      s += " " + loc;
    return s;
  }

  @Override
  public void setValueRequested(Attribute<Object> attr, Object value) throws AttrTableSetException {
    if (!proj.getLogisimFile().contains(circ)) {
      String msg = S.get("cannotModifyCircuitError");
      throw new AttrTableSetException(msg);
    } else {
      SetAttributeAction act = new SetAttributeAction(circ, S.getter("changeAttributeAction"));
      act.set(comp, attr, value);
      proj.doAction(act);
    }
  }
}
