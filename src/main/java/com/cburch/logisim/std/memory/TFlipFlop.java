/**
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
import com.cburch.logisim.fpga.gui.FPGAReport;
import com.cburch.logisim.fpga.hdlgenerator.HDLGeneratorFactory;
import com.cburch.logisim.gui.icons.FlipFlopIcon;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

public class TFlipFlop extends AbstractFlipFlop {
  private class TFFHDLGeneratorFactory extends AbstractFlipFlopHDLGeneratorFactory
      implements HDLGeneratorFactory {
    @Override
    public String ComponentName() {
      return "T Flip-Flop";
    }

    @Override
    public Map<String, String> GetInputMaps(
        NetlistComponent ComponentInfo, Netlist Nets, FPGAReport Reporter, String HDLType) {
      Map<String, String> PortMap = new HashMap<String, String>();
      PortMap.putAll(GetNetMap("T", true, ComponentInfo, 0, Reporter, HDLType, Nets));
      return PortMap;
    }

    @Override
    public Map<String, Integer> GetInputPorts() {
      Map<String, Integer> Inputs = new HashMap<String, Integer>();
      Inputs.put("T", 1);
      return Inputs;
    }

    @Override
    public ArrayList<String> GetUpdateLogic(String HDLType) {
      ArrayList<String> Contents = new ArrayList<String>();
      if (HDLType.endsWith(VHDL)) Contents.add("   s_next_state <= s_current_state_reg XOR T;");
      else Contents.add("   assign s_next_state = s_current_state_reg^T;");
      return Contents;
    }
  }

  public TFlipFlop() {
    super("T Flip-Flop", new FlipFlopIcon(FlipFlopIcon.T_FLIPFLOP), S.getter("tFlipFlopComponent"), 1, false);
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
  public boolean HDLSupportedComponent(String HDLIdentifier, AttributeSet attrs) {
    if (MyHDLGenerator == null) MyHDLGenerator = new TFFHDLGeneratorFactory();
    return MyHDLGenerator.HDLTargetSupported(HDLIdentifier, attrs);
  }
}
