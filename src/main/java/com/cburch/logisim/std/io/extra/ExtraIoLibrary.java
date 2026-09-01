/*
 * Logisim-evolution - digital logic design tool and simulator
 * Copyright by the Logisim-evolution developers
 *
 * https://github.com/logisim-evolution/
 *
 * This is free software released under GNU GPLv3 license
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
   * Unique identifier of the library, used as reference in project files. Do NOT change as it will
   * prevent project files from loading.
   *
   * <p>Identifier value must MUST be unique string among all libraries.
   */
  public static final String _ID = "Input/Output-Extra";

  private static final FactoryDescription[] DESCRIPTIONS = {
    new FactoryDescription(Switch.class, S.getter("switchComponent")),
    new FactoryDescription(Buzzer.class, S.getter("buzzerComponent")),
    new FactoryDescription(Slider.class, S.getter("Slider")),
    new FactoryDescription(DigitalOscilloscope.class, S.getter("DigitalOscilloscopeComponent")),
    new FactoryDescription(PlaRom.class, S.getter("PlaRomComponent")),
    new FactoryDescription(TwoWaySwitch.class, S.getter("twoWaySwitch")),
    new FactoryDescription(TwoPinLed.class, S.getter("twopinLEDComponent")),
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
