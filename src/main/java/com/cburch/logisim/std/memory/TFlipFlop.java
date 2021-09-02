/*
 * Logisim-evolution - digital logic design tool and simulator
 * Copyright by Logisim-evolution developers
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
import com.cburch.logisim.fpga.hdlgenerator.HDL;
import com.cburch.logisim.gui.icons.FlipFlopIcon;
import com.cburch.logisim.util.LineBuffer;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

public class TFlipFlop extends AbstractFlipFlop {
  /**
   * Unique identifier of the tool, used as reference in project files.
   * Do NOT change as it will prevent project files from loading.
   *
   * Identifier value must MUST be unique string among all tools.
   */
  public static final String _ID = "T Flip-Flop";

  private static class TFFHDLGeneratorFactory extends AbstractFlipFlopHDLGeneratorFactory {
    @Override
    public String ComponentName() {
      return _ID;
    }

    @Override
    public Map<String, String> GetInputMaps(NetlistComponent ComponentInfo, Netlist nets) {
      Map<String, String> PortMap = new HashMap<>();
      PortMap.putAll(GetNetMap("T", true, ComponentInfo, 0, nets));
      return PortMap;
    }

    @Override
    public Map<String, Integer> GetInputPorts() {
      Map<String, Integer> Inputs = new HashMap<>();
      Inputs.put("T", 1);
      return Inputs;
    }

    @Override
    public ArrayList<String> GetUpdateLogic() {
      return (new LineBuffer())
          .add("{{1}} s_next_state {{2}} s_current_state_reg {{3}} T;", HDL.assignPreamble(), HDL.assignOperator(), HDL.xorOperator())
          .getWithIndent();
    }
  }

  public TFlipFlop() {
    super(_ID, new FlipFlopIcon(FlipFlopIcon.T_FLIPFLOP), S.getter("tFlipFlopComponent"), 1, false);
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

  @Override
  public boolean HDLSupportedComponent(AttributeSet attrs) {
    if (MyHDLGenerator == null) MyHDLGenerator = new TFFHDLGeneratorFactory();
    return MyHDLGenerator.HDLTargetSupported(attrs);
  }
}
