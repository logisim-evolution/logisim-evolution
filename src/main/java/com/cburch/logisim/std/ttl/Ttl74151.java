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
import com.cburch.logisim.data.Value;
import com.cburch.logisim.fpga.designrulecheck.CorrectLabel;
import com.cburch.logisim.instance.InstancePainter;
import com.cburch.logisim.instance.InstanceState;

public class Ttl74151 extends AbstractTtlGate {

  public static final int PORT_INDEX_D3 = 0;
  public static final int PORT_INDEX_D2 = 1;
  public static final int PORT_INDEX_D1 = 2;
  public static final int PORT_INDEX_D0 = 3;
  public static final int PORT_INDEX_Y = 4;
  public static final int PORT_INDEX_W = 5;
  public static final int PORT_INDEX_nG = 6;
  public static final int PORT_INDEX_C = 7;
  public static final int PORT_INDEX_B = 8;
  public static final int PORT_INDEX_A = 9;
  public static final int PORT_INDEX_D7 = 10;
  public static final int PORT_INDEX_D6 = 11;
  public static final int PORT_INDEX_D5 = 12;
  public static final int PORT_INDEX_D4 = 13;

  public static final int[] PORT_INDICES_D = {
      PORT_INDEX_D0, PORT_INDEX_D1, PORT_INDEX_D2, PORT_INDEX_D3,
      PORT_INDEX_D4, PORT_INDEX_D5, PORT_INDEX_D6, PORT_INDEX_D7
  };

  public Ttl74151() {
    super(
        "74151",
        (byte) 16,
        new byte[] { PORT_INDEX_Y, PORT_INDEX_W },
        new String[] {
            "D3", "D2", "D1", "D0", "Y", "W", "nG",
            "C", "B", "A", "D7", "D6", "D5", "D4"
        });
  }

  @Override
  public void paintInternal(InstancePainter painter, int x, int y, int height, boolean up) {
    super.paintBase(painter, true, false);
    Drawgates.paintPortNames(painter, x, y, height, super.portnames);
  }

  @Override
  public void ttlpropagate(InstanceState state) {
    Value a = state.getPortValue(PORT_INDEX_A),
      b = state.getPortValue(PORT_INDEX_B),
      c = state.getPortValue(PORT_INDEX_C),
      nG = state.getPortValue(PORT_INDEX_nG),
      y, w;

    for (Value v : new Value[] { a, b, c, nG }) {
      if (!v.isFullyDefined()) {
        state.setPort(PORT_INDEX_Y, Value.ERROR, 1);
        state.setPort(PORT_INDEX_W, Value.ERROR, 1);
        return;
      }
    }

    if (nG == Value.TRUE) {
      y = Value.FALSE;
      w = Value.TRUE;
    } else {
      y = state.getPortValue(
        PORT_INDICES_D[(int) (
          (c.toLongValue() << 2) | (b.toLongValue() << 1) | (a.toLongValue())
        )]);
      w = y.not();
    }

    state.setPort(PORT_INDEX_Y, y, 1);
    state.setPort(PORT_INDEX_Y, w, 1);
  }

  @Override
  public String getHDLName(AttributeSet attrs) {
    return CorrectLabel.getCorrectLabel("TTL" + this.getName()).toUpperCase();
  }

  @Override
  public boolean HDLSupportedComponent(AttributeSet attrs) {
    if (MyHDLGenerator == null) MyHDLGenerator = new Ttl7485HDLGenerator();
    return MyHDLGenerator.HDLTargetSupported(attrs);
  }
}
