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
import com.cburch.logisim.data.BitWidth;
import com.cburch.logisim.data.Value;
import com.cburch.logisim.instance.InstancePainter;
import com.cburch.logisim.instance.InstanceState;

public class Ttl74283 extends AbstractTtlGate {
  /**
   * Unique identifier of the tool, used as reference in project files.
   * Do NOT change as it will prevent project files from loading.
   *
   * Identifier value must MUST be unique string among all tools.
   */
  public static final String _ID = "74283";

  public Ttl74283() {
    super(
        _ID,
        (byte) 16,
        new byte[] {1, 4, 9, 10, 13},
        new String[] {
          "∑2", "B2", "A2", "∑1", "A1", "B1", "CIN", "C4", "∑4", "B4", "A4", "∑3", "A3", "B3"
        });
  }

  @Override
  public void paintInternal(InstancePainter painter, int x, int y, int height, boolean up) {
    super.paintBase(painter, true, false);
    Drawgates.paintPortNames(painter, x, y, height, super.portnames);
  }

  @Override
  public void ttlpropagate(InstanceState state) {
    final var A1 = state.getPortValue(4) == Value.TRUE ? (byte) 1 : 0;
    final var A2 = state.getPortValue(2) == Value.TRUE ? (byte) 2 : 0;
    final var A3 = state.getPortValue(12) == Value.TRUE ? (byte) 4 : 0;
    final var A4 = state.getPortValue(10) == Value.TRUE ? (byte) 8 : 0;
    final var B1 = state.getPortValue(5) == Value.TRUE ? (byte) 1 : 0;
    final var B2 = state.getPortValue(1) == Value.TRUE ? (byte) 2 : 0;
    final var B3 = state.getPortValue(13) == Value.TRUE ? (byte) 4 : 0;
    final var B4 = state.getPortValue(9) == Value.TRUE ? (byte) 8 : 0;
    final var CIN = state.getPortValue(6) == Value.TRUE ? (byte) 1 : 0;
    final var sum = (byte) (A1 + A2 + A3 + A4 + B1 + B2 + B3 + B4 + CIN);
    final var output = Value.createKnown(BitWidth.create(5), sum);
    state.setPort(3, output.get(0), 1);
    state.setPort(0, output.get(1), 1);
    state.setPort(11, output.get(2), 1);
    state.setPort(8, output.get(3), 1);
    state.setPort(7, output.get(4), 1);
  }

  @Override
  public boolean HDLSupportedComponent(AttributeSet attrs) {
    if (MyHDLGenerator == null) MyHDLGenerator = new Ttl74283HDLGenerator();
    return MyHDLGenerator.HDLTargetSupported(attrs);
  }
}
