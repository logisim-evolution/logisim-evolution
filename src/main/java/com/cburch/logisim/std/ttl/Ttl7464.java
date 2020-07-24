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

public class Ttl7464 extends AbstractTtlGate {

  public Ttl7464() {
    super(
        "7464",
        (byte) 14,
        new byte[] {8},
        new String[] {"A", "E", "F", "G", "H", "I", "Y", "J", "K", "B", "C", "D"},
        70);
  }

  @Override
  public void paintInternal(InstancePainter painter, int x, int y, int height, boolean up) {
    super.paintBase(painter, false, false);
    boolean isIEC = AppPreferences.GATE_SHAPE.get().equals(AppPreferences.SHAPE_RECTANGULAR);
    int AndOffset = isIEC ? 10 : 0;
    Graphics g = painter.getGraphics();
    Drawgates.paintOr(g, x + 125, y + 35, 10, isIEC ? 40 : 10, true, false);
    Drawgates.paintAnd(g, x + 105 + AndOffset, y + 20, 10, 10, false);
    Drawgates.paintAnd(g, x + 105 + AndOffset, y + 30, 10, 10, false);
    Drawgates.paintAnd(g, x + 105 + AndOffset, y + 40, 10, 10, false);
    Drawgates.paintAnd(g, x + 105 + AndOffset, y + 50, 10, 10, false);
    g.drawLine(x + 129, y + 35, x + 130, y + 35);
    g.drawLine(x + 130, y + 35, x + 130, y + AbstractTtlGate.pinheight);
    int[] xpos, ypos;
    for (int i = 0; i < 4; i++) {
      if (!isIEC) {
        int tmpOff = (i == 0) | (i == 3) ? 2 : 0;
        xpos = new int[] {x + 105, x + 107 + tmpOff, x + 107 + tmpOff, x + 111};
        ypos = new int[] {y + 20 + i * 10, y + 20 + i * 10, y + 32 + i * 2, y + 32 + i * 2};
        g.drawPolyline(xpos, ypos, 4);
      }
      xpos = new int[] {x + 10 + i * 20, x + 10 + i * 20, x + 95 + AndOffset};
      ypos =
          new int[] {
            i == 0 ? y + height - AbstractTtlGate.pinheight : y + AbstractTtlGate.pinheight,
            y + 33 - i * 2,
            y + 33 - i * 2
          };
      g.drawPolyline(xpos, ypos, 3);
      if (i < 2) {
        xpos = new int[] {x + 30 + i * 20, x + 30 + i * 20, x + 95 + AndOffset};
        ypos = new int[] {y + height - AbstractTtlGate.pinheight, y + 38 + i * 5, y + 38 + i * 5};
        g.drawPolyline(xpos, ypos, 3);
        xpos = new int[] {x + 70 + i * 20, x + 70 + i * 20, x + 95 + AndOffset};
        ypos = new int[] {y + height - AbstractTtlGate.pinheight, y + 47 + i * 3, y + 47 + i * 3};
        g.drawPolyline(xpos, ypos, 3);
      }
    }
    xpos = new int[] {x + 90, x + 90, x + 95 + AndOffset};
    ypos = new int[] {y + AbstractTtlGate.pinheight, y + 23, y + 23};
    g.drawPolyline(xpos, ypos, 3);
    xpos = new int[] {x + 110, x + 110, x + 93 + AndOffset, x + 93 + AndOffset, x + 95 + AndOffset};
    ypos = new int[] {y + AbstractTtlGate.pinheight, y + 12, y + 12, y + 18, y + 18};
    g.drawPolyline(xpos, ypos, 5);
    ypos =
        new int[] {
          y + height - AbstractTtlGate.pinheight, y + height - 12, y + height - 12, y + 53, y + 53
        };
    g.drawPolyline(xpos, ypos, 5);
  }

  @Override
  public void ttlpropagate(InstanceState state) {
    Value val1 = state.getPortValue(1).and(state.getPortValue(2));
    Value val2 = state.getPortValue(3).and(state.getPortValue(4).and(state.getPortValue(5)));
    Value val3 = state.getPortValue(7).and(state.getPortValue(8));
    Value val4 =
        state
            .getPortValue(9)
            .and(state.getPortValue(10).and(state.getPortValue(11).and(state.getPortValue(0))));
    Value val5 = val1.or(val2.or(val3.or(val4)));
    state.setPort(6, val5.not(), 7);
  }

  @Override
  public String getHDLName(AttributeSet attrs) {
    StringBuffer CompleteName = new StringBuffer();
    CompleteName.append(CorrectLabel.getCorrectLabel("TTL" + this.getName()).toUpperCase());
    return CompleteName.toString();
  }

  @Override
  public boolean HDLSupportedComponent(String HDLIdentifier, AttributeSet attrs) {
    if (MyHDLGenerator == null) MyHDLGenerator = new Ttl7464HDLGenerator();
    return MyHDLGenerator.HDLTargetSupported(HDLIdentifier, attrs);
  }
}
