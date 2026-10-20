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

public class SRFlipFlop extends AbstractFlipFlop {
  /**
   * Unique identifier of the tool, used as reference in project files. Do NOT change as it will
   * prevent project files from loading.
   *
   * <p>Identifier value must MUST be unique string among all tools.
   */
  public static final String _ID = "S-R Flip-Flop";

  private static class SRFFHDLGeneratorFactory extends AbstractFlipFlopHdlGeneratorFactory {

    public SRFFHDLGeneratorFactory() {
      super(2, StdAttr.TRIGGER);
      myPorts.add(Port.INPUT, "s", 1, 0).add(Port.INPUT, "r", 1, 1);
    }

    @Override
    public LineBuffer getUpdateLogic() {
      return LineBuffer.getHdlBuffer()
          .add("{{assign}} s_nextState{{=}}(s_currentState{{and}}s){{or}}({{not}}(r){{and}}s){{or}}(s_currentState{{and}}{{not}}(r));");
    }
  }

  public SRFlipFlop() {
    super(
        _ID,
        new FlipFlopIcon(FlipFlopIcon.SR_FLIPFLOP),
        S.getter("srFlipFlopComponent"),
        2,
        true,
        new SRFFHDLGeneratorFactory());
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
        return Value.ERROR;
      }
    }
    return Value.UNKNOWN;
  }

  @Override
  protected String getInputName(int index) {
    return index == 0 ? "S" : "R";
  }
}
