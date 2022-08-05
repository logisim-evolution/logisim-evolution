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

public class DFlipFlop extends AbstractFlipFlop {
  /**
   * Unique identifier of the tool, used as reference in project files. Do NOT change as it will
   * prevent project files from loading.
   *
   * <p>Identifier value must MUST be unique string among all tools.
   */
  public static final String _ID = "D Flip-Flop";

  private static class DFFHDLGeneratorFactory extends AbstractFlipFlopHdlGeneratorFactory {

    public DFFHDLGeneratorFactory() {
      super(1, StdAttr.TRIGGER);
      myPorts.add(Port.INPUT, "d", 1, 0);
    }

    @Override
    public LineBuffer getUpdateLogic() {
      return LineBuffer.getHdlBuffer().add("{{assign}}s_nextState {{=}} d;");
    }
  }

  public DFlipFlop() {
    super(
        _ID,
        new FlipFlopIcon(FlipFlopIcon.D_FLIPFLOP),
        S.getter("dFlipFlopComponent"),
        1,
        true,
        new DFFHDLGeneratorFactory());
  }

  @Override
  protected Value computeValue(Value[] inputs, Value curValue) {
    return inputs[0];
  }

  @Override
  protected String getInputName(int index) {
    return "D";
  }
}
