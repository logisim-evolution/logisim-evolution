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

public class Ttl7447 extends AbstractTtlGate {
  /**
   * Unique identifier of the tool, used as reference in project files.
   * Do NOT change as it will prevent project files from loading.
   *
   * Identifier value must MUST be unique string among all tools.
   */
  public static final String _ID = "7447";

  public Ttl7447() {
    super(
        _ID,
        (byte) 16,
        new byte[] {9, 10, 11, 12, 13, 14, 15},
        new String[] {"B", "C", "LT", "BI", "RBI", "D", "A", "e", "d", "c", "b", "a", "g", "f"});
  }

  @Override
  public void paintInternal(InstancePainter painter, int x, int y, int height, boolean up) {
    super.paintBase(painter, true, false);
    Drawgates.paintPortNames(painter, x, y, height, super.portnames);
  }

  @Override
  public void ttlpropagate(InstanceState state) {
    DisplayDecoder.ComputeDisplayDecoderOutputs(
        state,
        DisplayDecoder.getdecval(state, false, 0, 6, 0, 1, 5),
        11,
        10,
        9,
        8,
        7,
        13,
        12,
        2,
        3,
        4);
  }

  @Override
  public boolean HDLSupportedComponent(AttributeSet attrs) {
    if (MyHDLGenerator == null) MyHDLGenerator = new Ttl7447HDLGenerator();
    return MyHDLGenerator.HDLTargetSupported(attrs);
  }
}
