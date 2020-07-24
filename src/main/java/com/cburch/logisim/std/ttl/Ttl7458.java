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
import com.cburch.logisim.data.Value;
import com.cburch.logisim.fpga.designrulecheck.CorrectLabel;
import com.cburch.logisim.instance.InstancePainter;
import com.cburch.logisim.instance.InstanceState;
import com.cburch.logisim.prefs.AppPreferences;
import java.awt.Graphics;

public class Ttl7458 extends AbstractTtlGate {

  public Ttl7458() {
    super(
        "7458",
        (byte) 14,
        new byte[] {6, 8},
        new String[] {"A0", "A1", "B1", "C1", "D1", "Y1", "Y0", "D0", "E0", "F0", "B0", "C0"});
  }

  @Override
  public void paintInternal(InstancePainter painter, int x, int y, int height, boolean up) {
    super.paintBase(painter, false, false);
    Graphics g = painter.getGraphics();
    Drawgates.paintOr(g, x + 107, y + 39, 10, 10, false, false);
    Drawgates.paintAnd(g, x + 86, y + 34, 10, 10, false);
    Drawgates.paintAnd(g, x + 86, y + 44, 10, 10, false);
    int OrOffset =
        (AppPreferences.GATE_SHAPE.get().equals(AppPreferences.SHAPE_RECTANGULAR)) ? 4 : 0;
    int[] xpos = new int[] {x + 86, x + 90, x + 90, x + 93 + OrOffset};
    int[] ypos = new int[] {y + 34, y + 34, y + 36, y + 36};
    g.drawPolyline(xpos, ypos, 4);
    ypos = new int[] {y + 44, y + 44, y + 42, y + 42};
    g.drawPolyline(xpos, ypos, 4);
    xpos = new int[] {x + 107, x + 110, x + 110};
    ypos = new int[] {y + 39, y + 39, y + height - AbstractTtlGate.pinheight};
    g.drawPolyline(xpos, ypos, 3);
    for (int i = 0; i < 3; i++) {
      g.drawLine(
          x + 30 + i * 20, y + 32 + i * 5, x + 30 + i * 20, y + height - AbstractTtlGate.pinheight);
      g.drawLine(x + 30 + i * 20, y + 32 + i * 5, x + 76, y + 32 + i * 5);
    }
    xpos = new int[] {x + 76, x + 73, x + 73, x + 90, x + 90};
    ypos = new int[] {y + 47, y + 47, y + 51, y + 51, y + height - AbstractTtlGate.pinheight};
    g.drawPolyline(xpos, ypos, 5);

    Drawgates.paintOr(g, x + 127, y + 21, 10, 10, false, false);
    Drawgates.paintAnd(g, x + 106, y + 16, 10, 10, false);
    Drawgates.paintAnd(g, x + 106, y + 26, 10, 10, false);
    xpos = new int[] {x + 106, x + 110, x + 110, x + 113 + OrOffset};
    ypos = new int[] {y + 16, y + 16, y + 18, y + 18};
    g.drawPolyline(xpos, ypos, 4);
    ypos = new int[] {y + 26, y + 26, y + 24, y + 24};
    g.drawPolyline(xpos, ypos, 4);
    xpos = new int[] {x + 127, x + 130, x + 130};
    ypos = new int[] {y + 21, y + 21, y + AbstractTtlGate.pinheight};
    g.drawPolyline(xpos, ypos, 3);
    for (int i = 0; i < 5; i++) {
      xpos = new int[] {x + 10 + i * 20, x + 10 + i * 20, x + 95};
      ypos =
          new int[] {
            i == 0 ? y + height - AbstractTtlGate.pinheight : y + AbstractTtlGate.pinheight,
            y + 28 - i * 3,
            y + 28 - i * 3
          };
      g.drawPolyline(xpos, ypos, 3);
    }
    xpos = new int[] {x + 96, x + 93, x + 93, x + 110, x + 110};
    ypos = new int[] {y + 13, y + 13, y + 9, y + 9, y + AbstractTtlGate.pinheight};
    g.drawPolyline(xpos, ypos, 5);
  }

  @Override
  public void ttlpropagate(InstanceState state) {
    Value val1 = state.getPortValue(1).and(state.getPortValue(2));
    Value val2 = state.getPortValue(3).and(state.getPortValue(4));
    state.setPort(5, val1.or(val2), 5);
    val1 = state.getPortValue(0).and(state.getPortValue(11).and(state.getPortValue(10)));
    val2 = state.getPortValue(9).and(state.getPortValue(8).and(state.getPortValue(7)));
    state.setPort(6, val1.or(val2), 5);
  }

  @Override
  public String getHDLName(AttributeSet attrs) {
    StringBuffer CompleteName = new StringBuffer();
    CompleteName.append(CorrectLabel.getCorrectLabel("TTL" + this.getName()).toUpperCase());
    return CompleteName.toString();
  }

  @Override
  public boolean HDLSupportedComponent(String HDLIdentifier, AttributeSet attrs) {
    if (MyHDLGenerator == null) MyHDLGenerator = new Ttl7458HDLGenerator();
    return MyHDLGenerator.HDLTargetSupported(HDLIdentifier, attrs);
  }
}
