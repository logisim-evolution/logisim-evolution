/*
 * Logisim-evolution - digital logic design tool and simulator
 * Copyright by the Logisim-evolution developers
 *
 * https://github.com/logisim-evolution/
 *
 * This is free software released under GNU GPLv3 license
 */

package com.cburch.logisim.vhdl.base;

import static com.cburch.logisim.vhdl.Strings.S;

import com.cburch.logisim.circuit.CircuitState;
import com.cburch.logisim.comp.Component;
import com.cburch.logisim.data.Attribute;
import com.cburch.logisim.std.hdl.VhdlEntityComponent;
import com.cburch.logisim.util.StringGetter;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.LinkedList;
import java.util.List;

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

    LinkedList<Component> vhdlComp = new LinkedList<>();

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

  public enum State {
    DISABLED,
    ENABLED,
    STARTING,
    RUNNING
  }

  public static final Charset ENCODING = StandardCharsets.UTF_8;
  public static final String VHDL_TEMPLATES_PATH = "/resources/logisim/hdl/";
  public static final String SIM_RESOURCES_PATH = "/resources/logisim/sim/";
  public static final String SIM_PATH = System.getProperty("java.io.tmpdir") + "/logisim/sim/";
  public static final String SIM_SRC_PATH = SIM_PATH + "src/";
  public static final String SIM_COMP_PATH = SIM_PATH + "comp/";
  public static final String SIM_TOP_FILENAME = "top_sim.vhdl";
  public static final String VHDL_COMPONENT_SIM_NAME = "LogisimVhdlSimComp_";
  // FIXME: hardcoded path. The "../src/" asks for troubles!
  public static final String VHDL_COMPILE_COMMAND = "vcom -reportprogress 300 -work work ../src/";
  public static final VhdlSimNameAttribute SIM_NAME_ATTR =
      new VhdlSimNameAttribute("vhdlSimName", S.getter("vhdlSimName"));
}
