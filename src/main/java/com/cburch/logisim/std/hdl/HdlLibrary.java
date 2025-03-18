/*
 * Logisim-evolution - digital logic design tool and simulator
 * Copyright by the Logisim-evolution developers
 *
 * https://github.com/logisim-evolution/
 *
 * This is free software released under GNU GPLv3 license
 */

package com.cburch.logisim.std.hdl;

import static com.cburch.logisim.vhdl.Strings.S;

import com.cburch.logisim.gui.icons.ArithmeticIcon;
import com.cburch.logisim.tools.FactoryDescription;
import com.cburch.logisim.tools.Library;
import com.cburch.logisim.tools.Tool;
import java.util.List;

public class HdlLibrary extends Library {

  /**
   * Unique identifier of the library, used as reference in project files. Do NOT change as it will
   * prevent project files from loading.
   *
   * <p>Identifier value must MUST be unique string among all libraries.
   */
  public static final String _ID = "HDL-IP";

  private static final FactoryDescription[] DESCRIPTIONS = {
      new FactoryDescription(
          VhdlEntityComponent.class, S.getter("vhdlComponent"), new ArithmeticIcon("VHDL")),
      new FactoryDescription(
          BlifCircuitComponent.class, S.getter("blifComponent"), new ArithmeticIcon("BLIF"))
  };

  private List<Tool> tools = null;

  @Override
  public String getDisplayName() {
    return S.get("hdlLibrary");
  }

  @Override
  public List<Tool> getTools() {
    if (tools == null) {
      tools = FactoryDescription.getTools(HdlLibrary.class, DESCRIPTIONS);
    }
    return tools;
  }
}
