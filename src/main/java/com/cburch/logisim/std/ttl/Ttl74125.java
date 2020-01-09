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

import com.cburch.logisim.data.Value;
import com.cburch.logisim.instance.InstancePainter;
import com.cburch.logisim.instance.InstanceState;
import java.awt.Graphics;

public class Ttl74125 extends AbstractTtlGate {

  public Ttl74125() {
    super("74125", (byte) 14, new byte[] {3, 6, 8, 11}, true);
  }

  @Override
  public void paintInternal(InstancePainter painter, int x, int y, int height, boolean up) {
    Graphics g = painter.getGraphics();
    int portwidth = 15, portheight = 8;
    int youtput = y + (up ? 20 : 40);
    Drawgates.paintBuffer(g, x + 50, youtput, portwidth, portheight);
    // output line
    Drawgates.paintOutputgate(g, x + 50, y, x + 45, youtput, up, height);
    // input line
    Drawgates.paintSingleInputgate(g, x + 30, y, x + 35, youtput, up, height);
    // enable line
    if (!up) {
      Drawgates.paintSingleInputgate(g, x + 10, y, x + 41, youtput - 7, up, height);
      g.drawLine(x + 41, youtput - 5, x + 41, youtput - 7);
      g.drawOval(x + 40, youtput - 5, 3, 3);
    } else {
      Drawgates.paintSingleInputgate(g, x + 10, y, x + 41, youtput + 7, up, height);
      g.drawLine(x + 41, youtput + 5, x + 41, youtput + 7);
      g.drawOval(x + 40, youtput + 2, 3, 3);
    }
  }

  @Override
  public void ttlpropagate(InstanceState state) {
    for (byte i = 2; i < 6; i += 3) {

      if (state.getPortValue(i - 2) == Value.TRUE) state.setPort(i, Value.UNKNOWN, 1);
      else state.setPort(i, state.getPortValue(i - 1), 1);
    }
    for (byte i = 6; i < 11; i += 3) {
      if (state.getPortValue(i + 2) == Value.TRUE) state.setPort(i, Value.UNKNOWN, 1);
      else state.setPort(i, state.getPortValue(i + 1), 1);
    }
  }
}
