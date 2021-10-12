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

import com.cburch.logisim.data.Value;
import com.cburch.logisim.gui.icons.FlipFlopIcon;
import com.cburch.logisim.instance.Port;
import com.cburch.logisim.instance.StdAttr;
import com.cburch.logisim.util.LineBuffer;

public class JKFlipFlop extends AbstractFlipFlop {
  /**
   * Unique identifier of the tool, used as reference in project files.
   * Do NOT change as it will prevent project files from loading.
   *
   * Identifier value must MUST be unique string among all tools.
   */
  public static final String _ID = "J-K Flip-Flop";

  private static class JKFFHDLGeneratorFactory extends AbstractFlipFlopHdlGeneratorFactory {

    public JKFFHDLGeneratorFactory() {
      super(2, StdAttr.EDGE_TRIGGER);
      myPorts
          .add(Port.INPUT, "j", 1, 0)
          .add(Port.INPUT, "k", 1, 1);
    }

    @Override
    public LineBuffer getUpdateLogic() {
      final var contents = LineBuffer.getHdlBuffer();
      final var preamble = LineBuffer.formatHdl("{{assign}}s_nextState{{=}}");
      contents.add("{{1}}({{not}}(s_currentState){{and}}j){{or}}", preamble)
          .add("{{1}}(s_currentState{{and}}{{not}}(k));", " ".repeat(preamble.length()));
      return contents;
    }
  }

  public JKFlipFlop() {
    super(_ID, new FlipFlopIcon(FlipFlopIcon.JK_FLIPFLOP), S.getter("jkFlipFlopComponent"), 2, false, new JKFFHDLGeneratorFactory());
  }

  @Override
  protected Value computeValue(Value[] inputs, Value curValue) {
    if (inputs[0] == Value.FALSE) {
      if (inputs[1] == Value.FALSE) {
        return curValue;
      } else if (inputs[1] == Value.TRUE) {
        return Value.FALSE;
      }
    } else if (inputs[0] == Value.TRUE) {
      if (inputs[1] == Value.FALSE) {
        return Value.TRUE;
      } else if (inputs[1] == Value.TRUE) {
        return curValue.not();
      }
    }
    return Value.UNKNOWN;
  }

  @Override
  protected String getInputName(int index) {
    return index == 0 ? "J" : "K";
  }
}
