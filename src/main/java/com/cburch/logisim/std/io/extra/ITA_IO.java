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

package com.cburch.logisim.std.io.extra;

import static com.cburch.logisim.std.Strings.S;

import com.cburch.logisim.tools.AddTool;
import com.cburch.logisim.tools.FactoryDescription;
import com.cburch.logisim.tools.Library;
import com.cburch.logisim.tools.Tool;
import java.util.ArrayList;
import java.util.List;

public class ITA_IO extends Library {

  private static FactoryDescription[] DESCRIPTIONS = {
    new FactoryDescription("Switch", S.getter("switchComponent"), "switch.gif", "Switch"),
    new FactoryDescription("Buzzer", S.getter("buzzerComponent"), "buzzer.gif", "Buzzer"),
    new FactoryDescription("Slider", S.getter("Slider"), "slider.gif", "Slider"),
    new FactoryDescription(
        "Digital Oscilloscope",
        S.getter("DigitalOscilloscopeComponent"),
        "digitaloscilloscope.gif",
        "DigitalOscilloscope"),
    new FactoryDescription("PlaRom", S.getter("PlaRomComponent"), "plarom.gif", "PlaRom"),
  };

  private List<Tool> tools = null;
  private Tool[] ADD_TOOLS = {
    new AddTool(ProgrammableGenerator.FACTORY),
  };

  @Override
  public String getName() {
    return "Input/Output-Extra";
  }

  @Override
  public boolean removeLibrary(String name) {
    // TODO Auto-generated method stub
    return false;
  }

  @Override
  public List<? extends Tool> getTools() {
    if (tools == null) {
      List<Tool> ret = new ArrayList<Tool>(ADD_TOOLS.length + DESCRIPTIONS.length);
      for (Tool a : ADD_TOOLS) ret.add(a);
      ret.addAll(FactoryDescription.getTools(ITA_IO.class, DESCRIPTIONS));
      tools = ret;
    }
    return tools;
  }
}
