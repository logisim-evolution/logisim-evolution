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

import com.cburch.logisim.data.AttributeSet;
import com.cburch.logisim.data.Value;
import com.cburch.logisim.fpga.designrulecheck.Netlist;
import com.cburch.logisim.fpga.designrulecheck.NetlistComponent;
import com.cburch.logisim.gui.icons.FlipFlopIcon;
import com.cburch.logisim.util.LineBuffer;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

public class SRFlipFlop extends AbstractFlipFlop {
  /**
   * Unique identifier of the tool, used as reference in project files.
   * Do NOT change as it will prevent project files from loading.
   *
   * Identifier value must MUST be unique string among all tools.
   */
  public static final String _ID = "S-R Flip-Flop";

  private static class SRFFHDLGeneratorFactory extends AbstractFlipFlopHDLGeneratorFactory {
    @Override
    public String ComponentName() {
      return _ID;
    }

    @Override
    public Map<String, String> GetInputMaps(NetlistComponent ComponentInfo, Netlist nets) {
      Map<String, String> PortMap = new HashMap<>();
      PortMap.putAll(GetNetMap("S", true, ComponentInfo, 0, nets));
      PortMap.putAll(GetNetMap("R", true, ComponentInfo, 1, nets));
      return PortMap;
    }

    @Override
    public Map<String, Integer> GetInputPorts() {
      Map<String, Integer> Inputs = new HashMap<>();
      Inputs.put("S", 1);
      Inputs.put("R", 1);
      return Inputs;
    }

    @Override
    public ArrayList<String> GetUpdateLogic() {
      return (new LineBuffer())
          .addHdlPairs()
          .add("{{assign}} s_next_state {{=}} (s_current_state_reg {{or}} S) {{and}} {{not}}(R);")
          .getWithIndent();
    }
  }

  public SRFlipFlop() {
    super(_ID, new FlipFlopIcon(FlipFlopIcon.SR_FLIPFLOP), S.getter("srFlipFlopComponent"), 2, true);
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

  @Override
  public boolean HDLSupportedComponent(AttributeSet attrs) {
    if (MyHDLGenerator == null) MyHDLGenerator = new SRFFHDLGeneratorFactory();
    return MyHDLGenerator.HDLTargetSupported(attrs);
  }
}
