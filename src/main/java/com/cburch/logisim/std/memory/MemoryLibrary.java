/*
 * Logisim-evolution - digital logic design tool and simulator
 * Copyright by the Logisim-evolution developers
 *
 * https://github.com/logisim-evolution/
 *
 * This is free software released under GNU GPLv3 license
 */

package com.cburch.logisim.std.memory;

import static com.cburch.logisim.std.Strings.S;

import com.cburch.logisim.tools.FactoryDescription;
import com.cburch.logisim.tools.Library;
import com.cburch.logisim.tools.Tool;
import java.util.List;

public class MemoryLibrary extends Library {
  /**
   * Unique identifier of the library, used as reference in project files.
   * Do NOT change as it will prevent project files from loading.
   *
   * Identifier value must MUST be unique string among all libraries.
   */
  public static final String _ID = "Memory";

  protected static final int DELAY = 5;

  private static final FactoryDescription[] DESCRIPTIONS = {
    new FactoryDescription(DFlipFlop.class, S.getter("dFlipFlopComponent"), "dFlipFlop.gif"),
    new FactoryDescription(TFlipFlop.class, S.getter("tFlipFlopComponent"), "tFlipFlop.gif"),
    new FactoryDescription(JKFlipFlop.class, S.getter("jkFlipFlopComponent"), "jkFlipFlop.gif"),
    new FactoryDescription(SRFlipFlop.class, S.getter("srFlipFlopComponent"), "srFlipFlop.gif"),
    new FactoryDescription(Register.class, S.getter("registerComponent"), "register.gif"),
    new FactoryDescription(Counter.class, S.getter("counterComponent"), "counter.gif"),
    new FactoryDescription(ShiftRegister.class, S.getter("shiftRegisterComponent"), "shiftreg.gif"),
    new FactoryDescription(Random.class, S.getter("randomComponent"), "random.gif"),
    new FactoryDescription(Ram.class, S.getter("ramComponent"), "ram.gif"),
    new FactoryDescription(Rom.class, S.getter("romComponent"), "rom.gif"),
  };

  private List<Tool> tools = null;

  @Override
  public String getDisplayName() {
    return S.get("memoryLibrary");
  }

  @Override
  public List<Tool> getTools() {
    if (tools == null) {
      tools = FactoryDescription.getTools(MemoryLibrary.class, DESCRIPTIONS);
    }
    return tools;
  }
}
