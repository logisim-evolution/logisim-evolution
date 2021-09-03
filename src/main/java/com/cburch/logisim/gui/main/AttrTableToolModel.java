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
import com.cburch.logisim.proj.Project;
import com.cburch.logisim.tools.AddTool;
import com.cburch.logisim.tools.Tool;
import lombok.Getter;
import lombok.val;

public class AttrTableToolModel extends AttributeSetTableModel {
  final Project proj;
  @Getter final Tool tool;

  public AttrTableToolModel(Project proj, Tool tool) {
    super(tool.getAttributeSet());
    if (tool instanceof AddTool) {
      val mytool = (AddTool) tool;
      setInstance(mytool.getFactory());
      setIsTool();
    }
    this.proj = proj;
    this.tool = tool;
  }

  @Override
  public String getTitle() {
    return tool.getDisplayName();
  }

  @Override
  public void setValueRequested(Attribute<Object> attr, Object value) {
    if (tool instanceof AddTool) {
      val mytool = (AddTool) tool;
      if (mytool.getFactory() instanceof SubcircuitFactory) {
        val fac = (SubcircuitFactory) mytool.getFactory();
        if (attr.equals(CircuitAttributes.NAMED_CIRCUIT_BOX_FIXED_SIZE)
            || attr.equals(CircuitAttributes.NAME_ATTR)) {
          try {
            val mutation = new CircuitMutation(fac.getSubcircuit());
            mutation.setForCircuit(attr, value);
            val action = mutation.toAction(null);
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
