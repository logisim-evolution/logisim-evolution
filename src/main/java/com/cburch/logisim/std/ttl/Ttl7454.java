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

public class Ttl7454 extends AbstractTtlGate {

  public Ttl7454() {
    super(
        "7454",
        (byte) 14,
        new byte[] {8},
        new byte[] {6, 11, 12},
        new String[] {"A", "C", "D", "E", "F", "Y", "G", "H", "B"});
  }

  @Override
  public void paintInternal(InstancePainter painter, int x, int y, int height, boolean up) {
    super.paintBase(painter, false, false);
    Graphics g = painter.getGraphics();
    Drawgates.paintOr(g, x + 125, y + 30, 10, 10, true, false);
    Drawgates.paintAnd(g, x + 105, y + 20, 10, 10, false);
    Drawgates.paintAnd(g, x + 105, y + 40, 10, 10, false);
    Drawgates.paintAnd(g, x + 65, y + 20, 10, 10, false);
    Drawgates.paintAnd(g, x + 65, y + 40, 10, 10, false);
    // TODO Auto-generated method stub
    int offset = (AppPreferences.GATE_SHAPE.get().equals(AppPreferences.SHAPE_RECTANGULAR)) ? 4 : 0;
    int[] xpos = new int[] {x + 105, x + 108, x + 108, x + 111 + offset};
    int[] ypos = new int[] {y + 20, y + 20, y + 27, y + 27};
    g.drawPolyline(xpos, ypos, 4);
    xpos = new int[] {x + 65, x + 68, x + 68, x + 111 + offset};
    ypos = new int[] {y + 20, y + 20, y + 29, y + 29};
    g.drawPolyline(xpos, ypos, 4);
    ypos = new int[] {y + 40, y + 40, y + 31, y + 31};
    g.drawPolyline(xpos, ypos, 4);
    xpos = new int[] {x + 105, x + 108, x + 108, x + 111 + offset};
    ypos = new int[] {y + 40, y + 40, y + 33, y + 33};
    g.drawPolyline(xpos, ypos, 4);
    xpos = new int[] {x + 129, x + 130, x + 130};
    ypos = new int[] {y + 30, y + 30, y + AbstractTtlGate.pinheight};
    g.drawPolyline(xpos, ypos, 3);
    xpos = new int[] {x + 30, x + 30, x + 55};
    ypos = new int[] {y + AbstractTtlGate.pinheight, y + 17, y + 17};
    g.drawPolyline(xpos, ypos, 3);
    xpos = new int[] {x + 10, x + 10, x + 55};
    ypos = new int[] {y + height - AbstractTtlGate.pinheight, y + 23, y + 23};
    g.drawPolyline(xpos, ypos, 3);
    xpos = new int[] {x + 30, x + 30, x + 55};
    ypos = new int[] {y + height - AbstractTtlGate.pinheight, y + 37, y + 37};
    g.drawPolyline(xpos, ypos, 3);
    xpos = new int[] {x + 50, x + 50, x + 55};
    ypos = new int[] {y + height - AbstractTtlGate.pinheight, y + 43, y + 43};
    g.drawPolyline(xpos, ypos, 3);
    xpos = new int[] {x + 70, x + 70, x + 95};
    ypos = new int[] {y + height - AbstractTtlGate.pinheight, y + 37, y + 37};
    g.drawPolyline(xpos, ypos, 3);
    xpos = new int[] {x + 90, x + 90, x + 95};
    ypos = new int[] {y + height - AbstractTtlGate.pinheight, y + 43, y + 43};
    g.drawPolyline(xpos, ypos, 3);
    xpos = new int[] {x + 90, x + 90, x + 95};
    ypos = new int[] {y + AbstractTtlGate.pinheight, y + 23, y + 23};
    g.drawPolyline(xpos, ypos, 3);
    xpos = new int[] {x + 110, x + 110, x + 93, x + 93, x + 95};
    ypos = new int[] {y + AbstractTtlGate.pinheight, y + 10, y + 10, y + 17, y + 17};
    g.drawPolyline(xpos, ypos, 5);
  }

  @Override
  public void ttlpropagate(InstanceState state) {
    Value val1 = state.getPortValue(0).and(state.getPortValue(8));
    Value val2 = state.getPortValue(1).and(state.getPortValue(2));
    Value val3 = state.getPortValue(3).and(state.getPortValue(4));
    Value val4 = state.getPortValue(6).and(state.getPortValue(7));
    state.setPort(5, val1.or(val2.or(val3.or(val4))).not(), 3);
  }

  @Override
  public String getHDLName(AttributeSet attrs) {
    StringBuffer CompleteName = new StringBuffer();
    CompleteName.append(CorrectLabel.getCorrectLabel("TTL" + this.getName()).toUpperCase());
    return CompleteName.toString();
  }

  @Override
  public boolean HDLSupportedComponent(String HDLIdentifier, AttributeSet attrs) {
    if (MyHDLGenerator == null) MyHDLGenerator = new Ttl7454HDLGenerator();
    return MyHDLGenerator.HDLTargetSupported(HDLIdentifier, attrs);
  }
}
