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

public class Ttl74279 extends AbstractTtlGate {

  public static final int PORT_INDEX_n1R = 0;
  public static final int PORT_INDEX_n1S1 = 1;
  public static final int PORT_INDEX_n1S2 = 2;
  public static final int PORT_INDEX_1Q = 3;
  public static final int PORT_INDEX_n2R = 4;
  public static final int PORT_INDEX_n2S = 5;
  public static final int PORT_INDEX_2Q = 6;
  public static final int PORT_INDEX_3Q = 7;
  public static final int PORT_INDEX_n3R = 8;
  public static final int PORT_INDEX_n3S1 = 9;
  public static final int PORT_INDEX_n3S2 = 10;
  public static final int PORT_INDEX_4Q = 11;
  public static final int PORT_INDEX_n4R = 12;
  public static final int PORT_INDEX_n4S = 13;

  public Ttl74279() {
    super(
        "74279",
        (byte) 16,
        new byte[] { PORT_INDEX_1Q, PORT_INDEX_2Q, PORT_INDEX_3Q, PORT_INDEX_4Q },
        new String[] {
            "n1R", "n1S1", "n1S2", "1Q", "n2R", "n2S", "2Q",
            "3Q", "n3R", "n3S1", "n3S2", "4Q" ,"n4R", "n4S"
        });
  }

  @Override
  public void paintInternal(InstancePainter painter, int x, int y, int height, boolean up) {
    super.paintBase(painter, true, false);
    Drawgates.paintPortNames(painter, x, y, height, super.portnames);
  }

  private void computeSR(InstanceState state, int portNS1, int portNS2, int portNR1, int portQ) {
    Value ns = state.getPortValue(portNS1).and(state.getPortValue(portNS2)),
      nr = state.getPortValue(portNR1),
      q0 = state.getPortValue(portQ),
      q = q0.isFullyDefined() ? q0 : Value.FALSE;

    if (!ns.isFullyDefined() || !nr.isFullyDefined()) {
      q = Value.ERROR;
    } else if (ns == Value.FALSE && nr == Value.TRUE) {
      q = Value.TRUE;
    } else if (ns == Value.TRUE && nr == Value.FALSE) {
      q = Value.FALSE;
    } else if (ns == Value.FALSE && nr == Value.FALSE) {
      q = Value.TRUE;
    }

    state.setPort(portQ, q, 1);
  }

  @Override
  public void ttlpropagate(InstanceState state) {
    this.computeSR(state, PORT_INDEX_n1S1, PORT_INDEX_n1S2, PORT_INDEX_n1R, PORT_INDEX_1Q);
    this.computeSR(state, PORT_INDEX_n2S, PORT_INDEX_n2S, PORT_INDEX_n2R, PORT_INDEX_2Q);
    this.computeSR(state, PORT_INDEX_n3S1, PORT_INDEX_n3S2, PORT_INDEX_n3R, PORT_INDEX_3Q);
    this.computeSR(state, PORT_INDEX_n4S, PORT_INDEX_n4S, PORT_INDEX_n4R, PORT_INDEX_4Q);
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
