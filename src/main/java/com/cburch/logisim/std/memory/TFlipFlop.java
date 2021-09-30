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
import com.cburch.logisim.fpga.hdlgenerator.Hdl;
import com.cburch.logisim.gui.icons.FlipFlopIcon;
import com.cburch.logisim.instance.Port;
import com.cburch.logisim.instance.StdAttr;
import com.cburch.logisim.util.LineBuffer;
import java.util.ArrayList;
import java.util.List;

public class TFlipFlop extends AbstractFlipFlop {
  /**
   * Unique identifier of the tool, used as reference in project files.
   * Do NOT change as it will prevent project files from loading.
   *
   * Identifier value must MUST be unique string among all tools.
   */
  public static final String _ID = "T Flip-Flop";

  private static class TFFHDLGeneratorFactory extends AbstractFlipFlopHDLGeneratorFactory {

    public TFFHDLGeneratorFactory() {
      super(1, StdAttr.EDGE_TRIGGER);
      myPorts.add(Port.INPUT, "T", 1, 0);
    }

    @Override
    public List<String> getUpdateLogic() {
      return LineBuffer.getBuffer()
          .add("{{1}} s_next_state {{2}} s_current_state_reg {{3}} T;", Hdl.assignPreamble(), Hdl.assignOperator(), Hdl.xorOperator())
          .getWithIndent();
    }
  }

  public TFlipFlop() {
    super(_ID, new FlipFlopIcon(FlipFlopIcon.T_FLIPFLOP), S.getter("tFlipFlopComponent"), 1, false, new TFFHDLGeneratorFactory());
  }

  @Override
  protected Value computeValue(Value[] inputs, Value curValue) {
    if (curValue == Value.UNKNOWN) curValue = Value.FALSE;
    if (inputs[0] == Value.TRUE) {
      return curValue.not();
    } else {
      return curValue;
    }
  }

  @Override
  protected String getInputName(int index) {
    return "T";
  }
}
