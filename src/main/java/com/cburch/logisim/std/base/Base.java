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

package com.cburch.logisim.std.base;

import static com.cburch.logisim.std.Strings.S;

import com.cburch.logisim.tools.AddTool;
import com.cburch.logisim.tools.EditTool;
import com.cburch.logisim.tools.Library;
import com.cburch.logisim.tools.MenuTool;
import com.cburch.logisim.tools.PokeTool;
import com.cburch.logisim.tools.SelectTool;
import com.cburch.logisim.tools.TextTool;
import com.cburch.logisim.tools.Tool;
import com.cburch.logisim.tools.WiringTool;
import java.util.Arrays;
import java.util.List;

public class Base extends Library {
  private List<Tool> tools;

  public Base() {
    setHidden();
    SelectTool select = new SelectTool();
    WiringTool wiring = new WiringTool();

    tools =
        Arrays.asList(
            new Tool[] {
              new PokeTool(),
              new EditTool(select, wiring),
              select,
              wiring,
              new TextTool(),
              new MenuTool(),
              new AddTool(Text.FACTORY),
            });
  }

  @Override
  public String getDisplayName() {
    return S.get("baseLibrary");
  }

  @Override
  public String getName() {
    return "Base";
  }

  @Override
  public List<Tool> getTools() {
    return tools;
  }

  public boolean removeLibrary(String Name) {
    return false;
  }

}
