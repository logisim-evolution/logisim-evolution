/*
 * Logisim-evolution - digital logic design tool and simulator
 * Copyright by the Logisim-evolution developers
 *
 * https://github.com/logisim-evolution/
 *
 * This is free software released under GNU GPLv3 license
 */

package com.cburch.logisim.std.bfh;

import static com.cburch.logisim.std.Strings.S;

import com.cburch.logisim.tools.FactoryDescription;
import com.cburch.logisim.tools.Library;
import com.cburch.logisim.tools.Tool;
import java.util.List;

public class BfhLibrary extends Library {
  /**
   * Unique identifier of the library, used as reference in project files.
   * Do NOT change as it will prevent project files from loading.
   *
   * Identifier value must MUST be unique string among all libraries.
   */
  public static final String _ID = "BFH-Praktika";

  private static final FactoryDescription[] DESCRIPTIONS = {
    new FactoryDescription(BinToBcd.class, S.getter("Bin2BCD")),
    new FactoryDescription(BcdToSevenSegmentDisplay.class, S.getter("BCD2SevenSegment")),
  };

  private List<Tool> tools = null;

  @Override
  public String getDisplayName() {
    return S.get("BFHMegaFunctions");
  }

  @Override
  public List<Tool> getTools() {
    if (tools == null) {
      tools = FactoryDescription.getTools(BfhLibrary.class, DESCRIPTIONS);
    }
    return tools;
  }
}
