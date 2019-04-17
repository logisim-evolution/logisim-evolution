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

package com.cburch.logisim.std.gates;

import static com.cburch.logisim.std.Strings.S;

import com.cburch.logisim.tools.AddTool;
import com.cburch.logisim.tools.Library;
import com.cburch.logisim.tools.Tool;
import java.util.Arrays;
import java.util.List;

public class Gates extends Library {
  private List<Tool> tools = null;

  public Gates() {
    tools =
        Arrays.asList(
            new Tool[] {
              new AddTool(NotGate.FACTORY),
              new AddTool(Buffer.FACTORY),
              new AddTool(AndGate.FACTORY),
              new AddTool(OrGate.FACTORY),
              new AddTool(NandGate.FACTORY),
              new AddTool(NorGate.FACTORY),
              new AddTool(XorGate.FACTORY),
              new AddTool(XnorGate.FACTORY),
              new AddTool(OddParityGate.FACTORY),
              new AddTool(EvenParityGate.FACTORY),
              new AddTool(ControlledBuffer.FACTORY_BUFFER),
              new AddTool(ControlledBuffer.FACTORY_INVERTER),
              new AddTool(PLA.FACTORY)
            });
  }

  @Override
  public String getDisplayName() {
    return S.get("gatesLibrary");
  }

  @Override
  public String getName() {
    return "Gates";
  }

  @Override
  public List<Tool> getTools() {
    return tools;
  }

  public boolean removeLibrary(String Name) {
    return false;
  }
}
