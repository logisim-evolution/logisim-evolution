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

public class SRFlipFlop extends AbstractFlipFlop {
  private static class SRFFHDLGeneratorFactory extends AbstractFlipFlopHDLGeneratorFactory {
    @Override
    public String ComponentName() {
      return "S-R Flip-Flop";
    }

    @Override
    public Map<String, String> GetInputMaps(NetlistComponent ComponentInfo, Netlist Nets) {
      Map<String, String> PortMap = new HashMap<>();
      PortMap.putAll(GetNetMap("S", true, ComponentInfo, 0, Nets));
      PortMap.putAll(GetNetMap("R", true, ComponentInfo, 1, Nets));
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
      ArrayList<String> Contents = new ArrayList<>();
      Contents.add("   "+HDL.assignPreamble()+"s_next_state"+HDL.assignOperator()+
          "(s_current_state_reg"+HDL.orOperator()+"S)"+HDL.andOperator()+HDL.notOperator()+"(R);");
      return Contents;
    }
  }

  public SRFlipFlop() {
    super("S-R Flip-Flop", new FlipFlopIcon(FlipFlopIcon.SR_FLIPFLOP), S.getter("srFlipFlopComponent"), 2, true);
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
