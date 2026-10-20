/*
 * Logisim-evolution - digital logic design tool and simulator
 * Copyright by the Logisim-evolution developers
 *
 * https://github.com/logisim-evolution/
 *
 * This is free software released under GNU GPLv3 license
 */

package com.cburch.logisim.gui.generic;

import com.cburch.logisim.circuit.Circuit;
import com.cburch.logisim.circuit.CircuitEvent;
import com.cburch.logisim.circuit.CircuitListener;
import com.cburch.logisim.circuit.SubcircuitFactory;
import com.cburch.logisim.tools.AddTool;
import com.cburch.logisim.tools.Tool;
import com.cburch.logisim.vhdl.base.HdlModel;
import com.cburch.logisim.vhdl.base.HdlModelListener;
import com.cburch.logisim.vhdl.base.VhdlContent;
import com.cburch.logisim.vhdl.base.VhdlEntity;

/**
 * Code taken from Cornell's version of Logisim: http://www.cs.cornell.edu/courses/cs3410/2015sp/
 */
public class ProjectExplorerToolNode extends ProjectExplorerModel.Node<Tool> implements CircuitListener, HdlModelListener {

  private static final long serialVersionUID = 1L;
  private Circuit circuit;
  private VhdlContent vhdl;

  public ProjectExplorerToolNode(ProjectExplorerModel model, Tool tool) {
    super(model, tool);
    if (tool instanceof AddTool) {
      final var factory = ((AddTool) tool).getFactory();

      if (factory instanceof SubcircuitFactory sub) {
        circuit = sub.getSubcircuit();
        circuit.addCircuitListener(this);
      } else if (factory instanceof VhdlEntity vhdlEntity) {
        vhdl = vhdlEntity.getContent();
        vhdl.addHdlModelListener(this);
      }
    }
  }

  @Override
  public void contentSet(HdlModel model) {
    // fireStructureChanged();
    fireNodeChanged();
  }

  @Override
  public void aboutToSave(HdlModel source) {}

  @Override
  public void displayChanged(HdlModel source) {
    fireNodeChanged();
  }

  @Override
  public void appearanceChanged(HdlModel source) {}

  @Override
  public void circuitChanged(CircuitEvent event) {
    final var act = event.getAction();
    if (act == CircuitEvent.ACTION_SET_NAME || act == CircuitEvent.ACTION_DISPLAY_CHANGE) {
      fireNodeChanged();
    }
  }

  @Override
  ProjectExplorerToolNode create(Tool userObject) {
    return new ProjectExplorerToolNode(getModel(), userObject);
  }

  @Override
  void decommission() {
    if (circuit != null) {
      circuit.removeCircuitListener(this);
    }
    if (vhdl != null) {
      vhdl.removeHdlModelListener(this);
    }
  }
}
