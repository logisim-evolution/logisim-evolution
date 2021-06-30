/*
 * This file is part of logisim-evolution.
 *
 * Logisim-evolution is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by the
 * Free Software Foundation, either version 3 of the License, or (at your
 * option) any later version.
 *
 * Logisim-evolution is distributed in the hope that it will be useful, but
 * WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 * or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
 * for more details.
 *
 * You should have received a copy of the GNU General Public License along
 * with logisim-evolution. If not, see <http://www.gnu.org/licenses/>.
 *
 * Original code by Carl Burch (http://www.cburch.com), 2011.
 * Subsequent modifications by:
 *   + College of the Holy Cross
 *     http://www.holycross.edu
 *   + Haute École Spécialisée Bernoise/Berner Fachhochschule
 *     http://www.bfh.ch
 *   + Haute École du paysage, d'ingénierie et d'architecture de Genève
 *     http://hepia.hesge.ch/
 *   + Haute École d'Ingénierie et de Gestion du Canton de Vaud
 *     http://www.heig-vd.ch/
 */

package com.cburch.logisim.std.memory;

import static com.cburch.logisim.std.Strings.S;

import com.cburch.logisim.data.AttributeSet;
import com.cburch.logisim.data.Value;
import com.cburch.logisim.fpga.designrulecheck.Netlist;
import com.cburch.logisim.fpga.designrulecheck.NetlistComponent;
import com.cburch.logisim.fpga.hdlgenerator.HDL;
import com.cburch.logisim.gui.icons.FlipFlopIcon;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

public class JKFlipFlop extends AbstractFlipFlop {
  /**
   * Unique identifier of the tool, used as reference in project files.
   * Do NOT change as it will prevent project files from loading.
   *
   * Identifier value must MUST be unique string among all tools.
   */
  public static final String _ID = "J-K Flip-Flop";

  private static class JKFFHDLGeneratorFactory extends AbstractFlipFlopHDLGeneratorFactory {
    @Override
    public String ComponentName() {
      return _ID;
    }

    @Override
    public Map<String, String> GetInputMaps(NetlistComponent ComponentInfo, Netlist nets) {
      final var portMap = new HashMap<String, String>();
      portMap.putAll(GetNetMap("J", true, ComponentInfo, 0, nets));
      portMap.putAll(GetNetMap("K", true, ComponentInfo, 1, nets));
      return portMap;
    }

    @Override
    public Map<String, Integer> GetInputPorts() {
      final var inputs = new HashMap<String, Integer>();
      inputs.put("J", 1);
      inputs.put("K", 1);
      return inputs;
    }

    @Override
    public ArrayList<String> GetUpdateLogic() {
      final var contents = new ArrayList<String>();
      contents.add("   " + HDL.assignPreamble() + "s_next_state" + HDL.assignOperator()
              + "(" + HDL.notOperator() + "(s_current_state_reg)" + HDL.andOperator() + "J)" + HDL.orOperator());
      contents.add("         (s_current_state_reg" + HDL.andOperator() + HDL.notOperator() + "(K));");
      return contents;
    }
  }

  public JKFlipFlop() {
    super(_ID, new FlipFlopIcon(FlipFlopIcon.JK_FLIPFLOP), S.getter("jkFlipFlopComponent"), 2, false);
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

  @Override
  public boolean HDLSupportedComponent(AttributeSet attrs) {
    if (MyHDLGenerator == null) MyHDLGenerator = new JKFFHDLGeneratorFactory();
    return MyHDLGenerator.HDLTargetSupported(attrs);
  }
}
