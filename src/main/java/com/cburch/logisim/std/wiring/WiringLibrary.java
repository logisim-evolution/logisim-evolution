/*
 * Logisim-evolution - digital logic design tool and simulator
 * Copyright by the Logisim-evolution developers
 *
 * https://github.com/logisim-evolution/
 *
 * This is free software released under GNU GPLv3 license
 */

package com.cburch.logisim.std.wiring;

import static com.cburch.logisim.std.Strings.S;

import com.cburch.logisim.circuit.SplitterFactory;
import com.cburch.logisim.tools.AddTool;
import com.cburch.logisim.tools.FactoryDescription;
import com.cburch.logisim.tools.Library;
import com.cburch.logisim.tools.Tool;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class WiringLibrary extends Library {

  /**
   * Unique identifier of the library, used as reference in project files. Do NOT change as it will
   * prevent project files from loading.
   *
   * <p>Identifier value must MUST be unique string among all libraries.
   */
  public static final String _ID = "Wiring";

  private static final Tool[] ADD_TOOLS = {
    new AddTool(SplitterFactory.instance),
    new AddTool(Pin.FACTORY),
    new AddTool(Probe.FACTORY),
    new AddTool(Tunnel.FACTORY),
    new AddTool(PullResistor.FACTORY),
    new AddTool(Clock.FACTORY),
    new AddTool(PowerOnReset.FACTORY),
    new AddTool(Constant.FACTORY),
  };

  private static final FactoryDescription[] DESCRIPTIONS = {
    new FactoryDescription(Power.class, S.getter("powerComponent"), "power.gif"),
    new FactoryDescription(Ground.class, S.getter("groundComponent"), "ground.gif"),
    new FactoryDescription(DoNotConnect.class, S.getter("noConnectionComponent"), "noconnect.gif"),
    new FactoryDescription(Transistor.class, S.getter("transistorComponent"), "trans0.gif"),
    new FactoryDescription(
        TransmissionGate.class, S.getter("transmissionGateComponent"), "transmis.gif"),
    new FactoryDescription(BitExtender.class, S.getter("extenderComponent"), "extender.gif"),
  };

  private List<Tool> tools = null;

  @Override
  public String getDisplayName() {
    return S.get("wiringLibrary");
  }

  @Override
  public List<Tool> getTools() {
    if (tools == null) {
      List<Tool> ret = new ArrayList<>(ADD_TOOLS.length + DESCRIPTIONS.length);
      ret.addAll(Arrays.asList(ADD_TOOLS));
      ret.addAll(FactoryDescription.getTools(WiringLibrary.class, DESCRIPTIONS));
      tools = ret;
    }
    return tools;
  }
}
