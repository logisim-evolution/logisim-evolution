/*
 * Logisim-evolution - digital logic design tool and simulator
 * Copyright by the Logisim-evolution developers
 *
 * https://github.com/logisim-evolution/
 *
 * This is free software released under GNU GPLv3 license
 */

package com.cburch.logisim.gui.main;

import com.cburch.logisim.circuit.CircuitAttributes;
import com.cburch.logisim.circuit.CircuitException;
import com.cburch.logisim.circuit.CircuitMutation;
import com.cburch.logisim.circuit.SubcircuitFactory;
import com.cburch.logisim.data.Attribute;
import com.cburch.logisim.gui.generic.AttributeSetTableModel;
import com.cburch.logisim.gui.generic.OptionPane;
import com.cburch.logisim.proj.Action;
import com.cburch.logisim.proj.Project;
import com.cburch.logisim.tools.AddTool;
import com.cburch.logisim.tools.Tool;

public class AttrTableToolModel extends AttributeSetTableModel {
  final Project proj;
  final Tool tool;

  public AttrTableToolModel(Project proj, Tool tool) {
    super(tool.getAttributeSet());
    if (tool instanceof AddTool addTool) {
      setInstance(addTool.getFactory());
      setIsTool();
    }
    this.proj = proj;
    this.tool = tool;
  }

  @Override
  public String getTitle() {
    return tool.getDisplayName();
  }

  public Tool getTool() {
    return tool;
  }

  @Override
  public void setValueRequested(Attribute<Object> attr, Object value) {
    if (tool instanceof AddTool addTool) {
      if (addTool.getFactory() instanceof SubcircuitFactory fac) {
        if (attr.equals(CircuitAttributes.NAMED_CIRCUIT_BOX_FIXED_SIZE)
            || attr.equals(CircuitAttributes.NAME_ATTR)) {
          try {
            CircuitMutation mutation = new CircuitMutation(fac.getSubcircuit());
            mutation.setForCircuit(attr, value);
            Action action = mutation.toAction(null);
            proj.doAction(action);
          } catch (CircuitException ex) {
            OptionPane.showMessageDialog(proj.getFrame(), ex.getMessage());
          }
          return;
        }
      }
    }
    proj.doAction(ToolAttributeAction.create(tool, attr, value));
  }
}
