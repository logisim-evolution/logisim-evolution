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

public class Ttl74688 extends AbstractTtlGate {

  public static final int PORT_INDEX_nOE = 0;
  public static final int PORT_INDEX_P0 = 1;
  public static final int PORT_INDEX_Q0 = 2;
  public static final int PORT_INDEX_P1 = 3;
  public static final int PORT_INDEX_Q1 = 4;
  public static final int PORT_INDEX_P2 = 5;
  public static final int PORT_INDEX_Q2 = 6;
  public static final int PORT_INDEX_P3 = 7;
  public static final int PORT_INDEX_Q3 = 8;
  public static final int PORT_INDEX_P4 = 9;
  public static final int PORT_INDEX_Q4 = 10;
  public static final int PORT_INDEX_P5 = 11;
  public static final int PORT_INDEX_Q5 = 12;
  public static final int PORT_INDEX_P6 = 13;
  public static final int PORT_INDEX_Q6 = 14;
  public static final int PORT_INDEX_P7 = 15;
  public static final int PORT_INDEX_Q7 = 16;
  public static final int PORT_INDEX_nPeqQ = 17;

  public static final int[] PORT_INDICES_P = {
      PORT_INDEX_P0, PORT_INDEX_P1, PORT_INDEX_P2, PORT_INDEX_P3,
      PORT_INDEX_P4, PORT_INDEX_P5, PORT_INDEX_P6, PORT_INDEX_P7
  };

  public static final int[] PORT_INDICES_Q = {
      PORT_INDEX_Q0, PORT_INDEX_Q1, PORT_INDEX_Q2, PORT_INDEX_Q3,
      PORT_INDEX_Q4, PORT_INDEX_Q5, PORT_INDEX_Q6, PORT_INDEX_Q7
  };

  public Ttl74688() {
    super(
        "74688",
        (byte) 20,
        new byte[] {19},
        new String[] {
            "nOE", "P0", "Q0", "P1", "Q1", "P2", "Q2", "P3", "Q3",
            "P4", "Q4", "P5", "Q5", "P6", "Q6", "P7", "Q7", "nP=Q"
        });
  }

  @Override
  public void paintInternal(InstancePainter painter, int x, int y, int height, boolean up) {
    super.paintBase(painter, true, false);
    Drawgates.paintPortNames(painter, x, y, height, super.portnames);
  }

  @Override
  public void ttlpropagate(InstanceState state) {
    Value vnOE = state.getPortValue(PORT_INDEX_nOE), result = Value.TRUE;

    if (!vnOE.isFullyDefined()) {
      result = Value.ERROR;
    } else if (vnOE == Value.TRUE) {
      result = Value.FALSE;
    } else if (vnOE == Value.FALSE) {
      for (int i = 0; i < 8; i++) {
        Value vp = state.getPortValue(PORT_INDICES_P[i]), vq = state.getPortValue(PORT_INDICES_Q[i]);

        if (!vp.isFullyDefined() || !vq.isFullyDefined()) {
          result = Value.ERROR;
          break;
        } else if (vp != vq) {
          result = Value.FALSE;
          break;
        }
      }
    }

    state.setPort(PORT_INDEX_nPeqQ, result.not(), 1);
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
