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

package com.cburch.logisim.std.ttl;

import com.cburch.logisim.data.AttributeSet;
import com.cburch.logisim.fpga.designrulecheck.CorrectLabel;
import com.cburch.logisim.instance.InstancePainter;
import com.cburch.logisim.instance.InstanceState;
import java.awt.Graphics;
import java.util.ArrayList;

public class Ttl74266 extends AbstractTtlGate {

  private class XNorGateHDLGeneratorFactory extends AbstractGateHDLGenerator {

    @Override
    public String getComponentStringIdentifier() {
      return "TTL74266";
    }

    @Override
    public ArrayList<String> GetLogicFunction(int index, String HDLType) {
      ArrayList<String> Contents = new ArrayList<String>();
      if (HDLType.equals(VHDL))
        Contents.add(
            "   gate_"
                + Integer.toString(index)
                + "_O <= NOT(gate_"
                + Integer.toString(index)
                + "_A"
                + " XOR gate_"
                + Integer.toString(index)
                + "_B);");
      else
        Contents.add(
            "   assign gate_"
                + Integer.toString(index)
                + "_O = ~(gate_"
                + Integer.toString(index)
                + "_A"
                + " ^ gate_"
                + Integer.toString(index)
                + "_B);");
      Contents.add("");
      return Contents;
    }
  }

  public Ttl74266() {
    super("74266", (byte) 14, new byte[] {3, 6, 8, 11}, true);
  }

  @Override
  public void paintInternal(InstancePainter painter, int x, int y, int height, boolean up) {
    Graphics g = painter.getGraphics();
    int portwidth = 18, portheight = 15;
    int youtput = y + (up ? 20 : 40);
    Drawgates.paintXor(g, x + 44, youtput, portwidth - 4, portheight, true);
    // output line
    Drawgates.paintOutputgate(g, x + 50, y, x + 48, youtput, up, height);
    // input lines
    Drawgates.paintDoubleInputgate(
        g, x + 30, y, x + 44 - portwidth, youtput, portheight, up, false, height);
  }

  @Override
  public void ttlpropagate(InstanceState state) {
    for (byte i = 2; i < 6; i += 3) {
      state.setPort(i, (state.getPortValue(i - 1).xor(state.getPortValue(i - 2)).not()), 1);
    }
    for (byte i = 6; i < 12; i += 3) {
      state.setPort(i, (state.getPortValue(i + 1).xor(state.getPortValue(i + 2)).not()), 1);
    }
  }

  @Override
  public String getHDLName(AttributeSet attrs) {
    StringBuffer CompleteName = new StringBuffer();
    CompleteName.append(CorrectLabel.getCorrectLabel("TTL" + this.getName()).toUpperCase());
    return CompleteName.toString();
  }

  @Override
  public boolean HDLSupportedComponent(String HDLIdentifier, AttributeSet attrs) {
    if (MyHDLGenerator == null) MyHDLGenerator = new XNorGateHDLGeneratorFactory();
    return MyHDLGenerator.HDLTargetSupported(HDLIdentifier, attrs);
  }
}
