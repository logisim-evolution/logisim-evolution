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
 * Original code by Marcin Orlowski (http://MarcinOrlowski.com), 2021
 */

package com.cburch.logisim.std.ttl;

import com.cburch.logisim.fpga.hdlgenerator.HDL;
import com.cburch.logisim.instance.InstancePainter;
import com.cburch.logisim.instance.InstanceState;
import java.util.ArrayList;

/**
 * TTL 74x34: hex buffer gate
 */
public class Ttl7434 extends AbstractTtlGate {
  /**
   * Unique identifier of the tool, used as reference in project files.
   * Do NOT change as it will prevent project files from loading.
   *
   * Identifier value must MUST be unique string among all tools.
   */
  public static final String _ID = "7434";

  private static class BufferGateHDLGeneratorFactory extends AbstractGateHDLGenerator {

    public BufferGateHDLGeneratorFactory() {
      super("TTL7434");
    }

    @Override
    public ArrayList<String> GetLogicFunction(int index) {
      final var contents = new ArrayList<String>();
      contents.add("   " + HDL.assignPreamble() + "gate_" + index + "_O" + HDL.assignOperator()
                  + "(gate_" + index + "_A);");
      contents.add("");
      return contents;
    }
  }

  private static final byte pinCount = 14;
  private static final byte[] outPins = {2, 4, 6, 8, 10, 12};

  public Ttl7434() {
    super(_ID, pinCount, outPins, true, new BufferGateHDLGeneratorFactory());
  }

  public Ttl7434(String name) {
    super(name, pinCount, outPins, true, new BufferGateHDLGeneratorFactory());
  }

  @Override
  public void paintInternal(InstancePainter painter, int x, int y, int height, boolean isUpOriented) {
    final var g = painter.getGraphics();
    final var portWidth = 16;
    final var portHeight = 6;
    final var yOutput = y + (isUpOriented ? 20 : 40);
    Drawgates.paintBuffer(g, x + 30, yOutput, portWidth, portHeight);
    Drawgates.paintOutputgate(g, x + 30, y, x + 26, yOutput, isUpOriented, height);
    Drawgates.paintSingleInputgate(g, x + 10, y, x + 30 - portWidth, yOutput, isUpOriented, height);
  }

  @Override
  public void ttlpropagate(InstanceState state) {
    for (byte i = 1; i < 6; i += 2) {
      state.setPort(i, state.getPortValue(i - 1), 1);
    }
    for (byte i = 6; i < 12; i += 2) {
      state.setPort(i, state.getPortValue(i + 1), 1);
    }
  }
}
