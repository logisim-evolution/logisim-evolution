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

package com.cburch.logisim.vhdl.base;

import static com.cburch.logisim.vhdl.Strings.S;

import com.cburch.logisim.circuit.CircuitState;
import com.cburch.logisim.comp.Component;
import com.cburch.logisim.data.Attribute;
import com.cburch.logisim.std.hdl.VhdlEntityComponent;
import com.cburch.logisim.util.StringGetter;
import com.cburch.logisim.vhdl.sim.VhdlSimulatorTop;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.LinkedList;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class VhdlSimConstants {

  public static class VhdlSimNameAttribute extends Attribute<String> {

    private VhdlSimNameAttribute(String name, StringGetter disp) {
      super(name, disp);
    }

    @Override
    public String parse(String value) {
      return value;
    }

    @Override
    public boolean isHidden() {
      return true;
    }
  }

  public static List<Component> getVhdlComponents(CircuitState s, boolean newStyle) {

    LinkedList<Component> vhdlComp = new LinkedList<Component>();

    /* Add current circuits comp */
    for (Component comp : s.getCircuit().getNonWires()) {
      if (comp.getFactory().getClass().equals(VhdlEntityComponent.class)) {
        vhdlComp.add(comp);
      }
      if (comp.getFactory().getClass().equals(VhdlEntity.class) && newStyle) {
        vhdlComp.add(comp);
      }
    }

    /* Add subcircuits comp */
    for (CircuitState sub : s.getSubstates()) {
      vhdlComp.addAll(getVhdlComponents(sub, newStyle));
    }

    return vhdlComp;
  }

  public static enum State {
    DISABLED,
    ENABLED,
    STARTING,
    RUNNING;
  }

  public static final Logger logger = LoggerFactory.getLogger(VhdlSimulatorTop.class);
  public static final Charset ENCODING = StandardCharsets.UTF_8;
  public static final String VHDL_TEMPLATES_PATH = "/resources/logisim/hdl/";
  public static final String SIM_RESOURCES_PATH = "/resources/logisim/sim/";
  public static final String SIM_PATH = System.getProperty("java.io.tmpdir") + "/logisim/sim/";
  public static final String SIM_SRC_PATH = SIM_PATH + "src/";
  public static final String SIM_COMP_PATH = SIM_PATH + "comp/";
  public static final String SIM_TOP_FILENAME = "top_sim.vhdl";
  public static final String VHDL_COMPONENT_SIM_NAME = "LogisimVhdlSimComp_";
  public static final VhdlSimNameAttribute SIM_NAME_ATTR =
      new VhdlSimNameAttribute("vhdlSimName", S.getter("vhdlSimName"));
}
