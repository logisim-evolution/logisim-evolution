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

package com.cburch.logisim.std.wiring;

import static com.cburch.logisim.std.Strings.S;

import com.cburch.logisim.circuit.SplitterFactory;
import com.cburch.logisim.data.Attribute;
import com.cburch.logisim.data.AttributeOption;
import com.cburch.logisim.data.Attributes;
import com.cburch.logisim.tools.AddTool;
import com.cburch.logisim.tools.FactoryDescription;
import com.cburch.logisim.tools.Library;
import com.cburch.logisim.tools.Tool;
import java.util.ArrayList;
import java.util.List;

public class Wiring extends Library {

  static final AttributeOption GATE_TOP_LEFT =
      new AttributeOption("tl", S.getter("wiringGateTopLeftOption"));
  static final AttributeOption GATE_BOTTOM_RIGHT =
      new AttributeOption("br", S.getter("wiringGateBottomRightOption"));
  static final Attribute<AttributeOption> ATTR_GATE =
      Attributes.forOption(
          "gate",
          S.getter("wiringGateAttr"),
          new AttributeOption[] {GATE_TOP_LEFT, GATE_BOTTOM_RIGHT});

  private static Tool[] ADD_TOOLS = {
    new AddTool(SplitterFactory.instance),
    new AddTool(Pin.FACTORY),
    new AddTool(Probe.FACTORY),
    new AddTool(Tunnel.FACTORY),
    new AddTool(PullResistor.FACTORY),
    new AddTool(Clock.FACTORY),
    new AddTool(PowerOnReset.FACTORY),
    new AddTool(Constant.FACTORY),
  };

  private static FactoryDescription[] DESCRIPTIONS = {
    new FactoryDescription("Power", S.getter("powerComponent"), "power.gif", "Power"),
    new FactoryDescription("Ground", S.getter("groundComponent"), "ground.gif", "Ground"),
    new FactoryDescription("NoConnect",S.getter("noConnectionComponent"),"noconnect.gif","DoNotConnect"),
    new FactoryDescription("Transistor", S.getter("transistorComponent"), "trans0.gif", "Transistor"),
    new FactoryDescription("Transmission Gate",S.getter("transmissionGateComponent"),"transmis.gif","TransmissionGate"),
    new FactoryDescription("Bit Extender", S.getter("extenderComponent"), "extender.gif", "BitExtender"),
  };

  private List<Tool> tools = null;

  public Wiring() {}

  @Override
  public String getDisplayName() {
    return S.get("wiringLibrary");
  }

  @Override
  public String getName() {
    return "Wiring";
  }

  @Override
  public List<Tool> getTools() {
    if (tools == null) {
      List<Tool> ret = new ArrayList<Tool>(ADD_TOOLS.length + DESCRIPTIONS.length);
      for (Tool a : ADD_TOOLS) {
        ret.add(a);
      }
      ret.addAll(FactoryDescription.getTools(Wiring.class, DESCRIPTIONS));
      tools = ret;
    }
    return tools;
  }

  public boolean removeLibrary(String Name) {
    return false;
  }
}
