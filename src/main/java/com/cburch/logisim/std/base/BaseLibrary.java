/*
 * Logisim-evolution - digital logic design tool and simulator
 * Copyright by the Logisim-evolution developers
 *
 * https://github.com/logisim-evolution/
 *
 * This is free software released under GNU GPLv3 license
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

    tools =
        Arrays.asList(
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
    final var t = super.getTool(name);
    if (t == null && name.equals(Text._ID)) {
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
}
