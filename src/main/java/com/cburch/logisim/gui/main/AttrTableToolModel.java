/**
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

package com.cburch.logisim.gui.main;

import static com.cburch.logisim.gui.Strings.S;

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
  Project proj;
  Tool tool;

  public AttrTableToolModel(Project proj, Tool tool) {
    super(tool.getAttributeSet());
    if (tool instanceof AddTool) {
      AddTool mytool = (AddTool) tool;
      SetInstance(mytool.getFactory());
      SetIsTool();
    }
    this.proj = proj;
    this.tool = tool;
  }

  @Override
  public String getTitle() {
    return S.fmt("toolAttrTitle", tool.getDisplayName());
  }

  public Tool getTool() {
    return tool;
  }

  @Override
  public void setValueRequested(Attribute<Object> attr, Object value) {
    if (tool instanceof AddTool) {
      AddTool mytool = (AddTool) tool;
      if (mytool.getFactory() instanceof SubcircuitFactory) {
        SubcircuitFactory fac = (SubcircuitFactory) mytool.getFactory();
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
