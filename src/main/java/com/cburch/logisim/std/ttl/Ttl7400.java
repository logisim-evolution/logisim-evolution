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

package com.cburch.logisim.std.ttl;

import com.cburch.logisim.data.AttributeSet;
import com.cburch.logisim.fpga.hdlgenerator.HDL;
import com.cburch.logisim.instance.InstancePainter;
import com.cburch.logisim.instance.InstanceState;
import java.util.ArrayList;

/**
 * TTL 74x00: quad 2-input NAND gate
 */
public class Ttl7400 extends AbstractTtlGate {
  /**
   * Unique identifier of the tool, used as reference in project files.
   * Do NOT change as it will prevent project files from loading.
   *
   * Identifier value must MUST be unique string among all tools.
   */
  public static final String _ID = "7400";

  private static class NandGateHDLGeneratorFactory extends AbstractGateHDLGenerator {

    @Override
    public String getComponentStringIdentifier() {
      return "TTL7400";
    }

    @Override
    public ArrayList<String> GetLogicFunction(int index) {
      final var contents = new ArrayList<String>();
      contents.add("   " + HDL.assignPreamble() + "gate_" + index + "_O" + HDL.assignOperator()
              + HDL.notOperator() + "(gate_" + index + "_A" + HDL.andOperator() + "gate_" + index + "B);");
      contents.add("");
      return contents;
    }
  }

  private static final byte pinCount = 14;
  private static final byte[] outPins = {3, 6, 8, 11};

  public Ttl7400() {
    super(_ID, pinCount, outPins, true);
  }

  public Ttl7400(String name) {
    super(name, pinCount, outPins, true);
  }

  @Override
  public void paintInternal(InstancePainter painter, int x, int y, int height, boolean up) {
    final var g = painter.getGraphics();
    final var portwidth = 19;
    final var portheight = 15;
    final var youtput = y + (up ? 20 : 40);
    Drawgates.paintAnd(g, x + 40, youtput, portwidth - 4, portheight, true);
    // output line
    Drawgates.paintOutputgate(g, x + 50, y, x + 44, youtput, up, height);
    // input lines
    Drawgates.paintDoubleInputgate(g, x + 30, y, x + 44 - portwidth, youtput, portheight, up, false, height);
  }

  @Override
  public void ttlpropagate(InstanceState state) {
    for (byte i = 2; i < 6; i += 3) {
      state.setPort(i, (state.getPortValue(i - 1).and(state.getPortValue(i - 2)).not()), 1);
    }
    for (byte i = 6; i < 12; i += 3) {
      state.setPort(i, (state.getPortValue(i + 1).and(state.getPortValue(i + 2)).not()), 1);
    }
  }

  @Override
  public boolean HDLSupportedComponent(AttributeSet attrs) {
    if (MyHDLGenerator == null) MyHDLGenerator = new NandGateHDLGeneratorFactory();
    return MyHDLGenerator.HDLTargetSupported(attrs);
  }
}
