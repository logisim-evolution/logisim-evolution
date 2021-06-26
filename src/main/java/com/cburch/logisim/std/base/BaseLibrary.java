/*
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

import com.cburch.logisim.comp.ComponentFactory;
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

public class BaseLibrary extends Library {
  /**
   * Unique identifier of the tool, used as reference in project files.
   * Do NOT change as it will prevent project files from loading.
   *
   * Identifier value must MUST be unique string among all tools.
   */
  public static final String _ID = "Base";

  private final List<Tool> tools;
  private final AddTool textAdder = new AddTool(Text.FACTORY);
  private final SelectTool selectTool = new SelectTool();

  public BaseLibrary() {
    setHidden();
    WiringTool wiring = new WiringTool();

    tools = Arrays.asList(
        new PokeTool(),
        new EditTool(selectTool, wiring),
        wiring,
        new TextTool(),
        new MenuTool());
    }

  @Override
  public boolean contains(ComponentFactory querry) {
    return super.contains(querry) || (querry instanceof Text);
  }

  @Override
  public Tool getTool(String name) {
    Tool t = super.getTool(name);
    if (t == null) {
      if (name.equals(Text._ID))
        return textAdder; // needed by XmlCircuitReader
    }
    return t;
  }

  @Override
  public String getDisplayName() {
    return S.get("baseLibrary");
  }

  @Override
  public List<Tool> getTools() {
    return tools;
  }

  public boolean removeLibrary(String Name) {
    return false;
  }

}
