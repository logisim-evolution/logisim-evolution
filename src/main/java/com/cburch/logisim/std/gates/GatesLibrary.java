/*
 * Logisim-evolution - digital logic design tool and simulator
 * Copyright by the Logisim-evolution developers
 *
 * https://github.com/logisim-evolution/
 *
 * This is free software released under GNU GPLv3 license
 */

package com.cburch.logisim.std.gates;

import static com.cburch.logisim.std.Strings.S;

import com.cburch.logisim.tools.AddTool;
import com.cburch.logisim.tools.Library;
import com.cburch.logisim.tools.Tool;
import java.util.Arrays;
import java.util.List;

public class GatesLibrary extends Library {

  /**
   * Unique identifier of the tool, used as reference in project files.
   * Do NOT change as it will prevent project files from loading.
   *
   * Identifier value must MUST be unique string among all tools.
   */
  public static final String _ID = "Gates";

  private List<Tool> tools = null;

  public GatesLibrary() {
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
              new AddTool(Pla.FACTORY)
            });
  }

  @Override
  public String getDisplayName() {
    return S.get("gatesLibrary");
  }

  @Override
  public List<Tool> getTools() {
    return tools;
  }
}
