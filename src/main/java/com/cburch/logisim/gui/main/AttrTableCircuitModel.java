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
import com.cburch.logisim.circuit.CircuitMutation;
import com.cburch.logisim.data.Attribute;
import com.cburch.logisim.gui.generic.AttrTableSetException;
import com.cburch.logisim.gui.generic.AttributeSetTableModel;
import com.cburch.logisim.proj.Project;

public class AttrTableCircuitModel extends AttributeSetTableModel {
  private final Project proj;
  private final Circuit circ;

  public AttrTableCircuitModel(final Project proj, final Circuit circ) {
    super(circ.getStaticAttributes());
    this.proj = proj;
    this.circ = circ;
  }

  @Override
  public String getTitle() {
    return S.get("circuitAttrTitle", circ.getName());
  }

  @Override
  public void setValueRequested(final Attribute<Object> attr, Object value) throws AttrTableSetException {
    if (!proj.getLogisimFile().contains(circ)) {
      final var msg = S.get("cannotModifyCircuitError");
      throw new AttrTableSetException(msg);
    } else {
      final var xn = new CircuitMutation(circ);
      xn.setForCircuit(attr, value);
      proj.doAction(xn.toAction(S.getter("changeCircuitAttrAction")));
    }
  }
}
