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
import com.cburch.logisim.instance.InstancePainter;
import com.cburch.logisim.instance.InstanceState;
import com.cburch.logisim.prefs.AppPreferences;

public class Ttl7451 extends AbstractTtlGate {
  /**
   * Unique identifier of the tool, used as reference in project files.
   * Do NOT change as it will prevent project files from loading.
   *
   * Identifier value must MUST be unique string among all tools.
   */
  public static final String _ID = "7451";

  public Ttl7451() {
    super(
        _ID,
        (byte) 14,
        new byte[] {6, 8},
        new byte[] {11, 12},
        new String[] {"A1", "A2", "B2", "C2", "D2", "Y2", "Y1", "C1", "D1", "B1"});
  }

  @Override
  public void paintInternal(InstancePainter painter, int x, int y, int height, boolean up) {
    super.paintBase(painter, false, false);
    final var gfx = painter.getGraphics();
    Drawgates.paintAnd(gfx, x + 50, y + 24, 10, 10, false);
    Drawgates.paintAnd(gfx, x + 50, y + 36, 10, 10, false);
    Drawgates.paintOr(gfx, x + 70, y + 30, 10, 10, true, false);

    Drawgates.paintAnd(gfx, x + 100, y + 24, 10, 10, false);
    Drawgates.paintAnd(gfx, x + 100, y + 36, 10, 10, false);
    Drawgates.paintOr(gfx, x + 120, y + 30, 10, 10, true, false);

    final var offset = (AppPreferences.GATE_SHAPE.get().equals(AppPreferences.SHAPE_RECTANGULAR)) ? 4 : 0;

    var posX = new int[] {x + 50, x + 53 + offset / 2, x + 53 + offset / 2, x + 56 + offset};
    var posY = new int[] {y + 24, y + 24, y + 26 + offset / 2, y + 26 + offset / 2};
    gfx.drawPolyline(posX, posY, 4);
    for (var i = 0; i < 4; i++) {
      posX[i] += 50;
    }
    gfx.drawPolyline(posX, posY, 4);
    posY[0] = posY[1] = y + 36;
    posY[2] = posY[3] = y + 34 - offset / 2;
    gfx.drawPolyline(posX, posY, 4);
    for (var i = 0; i < 4; i++) {
      posX[i] -= 50;
    }
    gfx.drawPolyline(posX, posY, 4);
    posX = new int[] {x + 10, x + 10, x + 40};
    posY = new int[] {y + height - AbstractTtlGate.PIN_HEIGHT, y + 39, y + 39};
    gfx.drawPolyline(posX, posY, 3);
    posX = new int[] {x + 30, x + 30, x + 40};
    posY = new int[] {y + AbstractTtlGate.PIN_HEIGHT, y + 33, y + 33};
    gfx.drawPolyline(posX, posY, 3);
    posX = new int[] {x + 90, x + 90, x + 33, x + 33, x + 40};
    posY = new int[] {y + AbstractTtlGate.PIN_HEIGHT, y + 10, y + 10, y + 27, y + 27};
    gfx.drawPolyline(posX, posY, 5);
    posX = new int[] {x + 110, x + 110, x + 36, x + 36, x + 40};
    posY = new int[] {y + AbstractTtlGate.PIN_HEIGHT, y + 13, y + 13, y + 21, y + 21};
    gfx.drawPolyline(posX, posY, 5);
    posX = new int[] {x + 130, x + 130, x + 75, x + 75, x + 74};
    posY = new int[] {y + AbstractTtlGate.PIN_HEIGHT, y + 16, y + 16, y + 30, y + 30};
    gfx.drawPolyline(posX, posY, 5);
    posX = new int[] {x + 30, x + 30, x + 78, x + 78, x + 90};
    posY = new int[] {y + height - AbstractTtlGate.PIN_HEIGHT, y + 44, y + 44, y + 21, y + 21};
    gfx.drawPolyline(posX, posY, 5);
    posX = new int[] {x + 50, x + 50, x + 81, x + 81, x + 90};
    posY = new int[] {y + height - AbstractTtlGate.PIN_HEIGHT, y + 47, y + 47, y + 27, y + 27};
    gfx.drawPolyline(posX, posY, 5);
    posX = new int[] {x + 70, x + 70, x + 84, x + 84, x + 90};
    posY = new int[] {y + height - AbstractTtlGate.PIN_HEIGHT, y + 50, y + 50, y + 33, y + 33};
    gfx.drawPolyline(posX, posY, 5);
    posX = new int[] {x + 90, x + 90, x + 87, x + 87, x + 90};
    posY = new int[] {y + height - AbstractTtlGate.PIN_HEIGHT, y + 50, y + 50, y + 39, y + 39};
    gfx.drawPolyline(posX, posY, 5);
    posX = new int[] {x + 110, x + 110, x + 126, x + 126, x + 124};
    posY = new int[] {y + height - AbstractTtlGate.PIN_HEIGHT, y + 40, y + 40, y + 30, y + 30};
    gfx.drawPolyline(posX, posY, 5);
  }

  @Override
  public void ttlpropagate(InstanceState state) {
    var val1 = state.getPortValue(1).and(state.getPortValue(2));
    var val2 = state.getPortValue(3).and(state.getPortValue(4));
    state.setPort(5, val1.or(val2).not(), 3);
    val1 = state.getPortValue(0).and(state.getPortValue(9));
    val2 = state.getPortValue(7).and(state.getPortValue(8));
    state.setPort(6, val1.or(val2).not(), 3);
  }

  @Override
  public boolean HDLSupportedComponent(AttributeSet attrs) {
    if (MyHDLGenerator == null) MyHDLGenerator = new Ttl7451HDLGenerator();
    return MyHDLGenerator.HDLTargetSupported(attrs);
  }
}
