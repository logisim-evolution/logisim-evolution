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

package com.cburch.logisim.std.io.extra;

import static com.cburch.logisim.std.Strings.S;

import com.cburch.logisim.tools.FactoryDescription;
import com.cburch.logisim.tools.Library;
import com.cburch.logisim.tools.Tool;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class ExtraIoLibrary extends Library {
  /**
   * Unique identifier of the library, used as reference in project files.
   * Do NOT change as it will prevent project files from loading.
   *
   * Identifier value must MUST be unique string among all libraries.
   */
  public static final String _ID = "Input/Output-Extra";

  private static final FactoryDescription[] DESCRIPTIONS = {
    new FactoryDescription(Switch.class, S.getter("switchComponent"), "switch.gif"),
    new FactoryDescription(Buzzer.class, S.getter("buzzerComponent"), "buzzer.gif"),
    new FactoryDescription(Slider.class, S.getter("Slider"), "slider.gif"),
    new FactoryDescription(DigitalOscilloscope.class, S.getter("DigitalOscilloscopeComponent"), "digitaloscilloscope.gif"),
    new FactoryDescription(PlaRom.class, S.getter("PlaRomComponent"), "plarom.gif"),
  };

  private List<Tool> tools = null;
  private final Tool[] ADD_TOOLS = {
    // new AddTool(ProgrammableGenerator.FACTORY), /* TODO: Broken component, fix */
  };

  @Override
  public String getDisplayName() {
    return S.get("input.output.extra");
  }

  @Override
  public boolean removeLibrary(String name) {
    // TODO Auto-generated method stub
    return false;
  }

  @Override
  public List<? extends Tool> getTools() {
    if (tools == null) {
      List<Tool> ret = new ArrayList<>(ADD_TOOLS.length + DESCRIPTIONS.length);
      ret.addAll(Arrays.asList(ADD_TOOLS));
      ret.addAll(FactoryDescription.getTools(ExtraIoLibrary.class, DESCRIPTIONS));
      tools = ret;
    }
    return tools;
  }
}
