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

public class Ttl7485 extends AbstractTtlGate {

  public Ttl7485() {
    super(
        "7485",
        (byte) 16,
        new byte[] {5, 6, 7},
        new String[] {
          "B3", "A<B", "A=B", "A>B", "A>B", "A=B", "A<B", "B0", "A0", "B1", "A1", "A2", "B2", "A3"
        });
  }

  @Override
  public void paintInternal(InstancePainter painter, int x, int y, int height, boolean up) {
    super.paintBase(painter, true, false);
    Drawgates.paintPortNames(painter, x, y, height, super.portnames);
  }

  @Override
  public void ttlpropagate(InstanceState state) {
    byte A0 = state.getPortValue(8) == Value.TRUE ? (byte) 1 : 0;
    byte A1 = state.getPortValue(10) == Value.TRUE ? (byte) 2 : 0;
    byte A2 = state.getPortValue(11) == Value.TRUE ? (byte) 4 : 0;
    byte A3 = state.getPortValue(13) == Value.TRUE ? (byte) 8 : 0;
    byte B0 = state.getPortValue(7) == Value.TRUE ? (byte) 1 : 0;
    byte B1 = state.getPortValue(9) == Value.TRUE ? (byte) 2 : 0;
    byte B2 = state.getPortValue(12) == Value.TRUE ? (byte) 4 : 0;
    byte B3 = state.getPortValue(0) == Value.TRUE ? (byte) 8 : 0;
    byte A = (byte) (A3 + A2 + A1 + A0);
    byte B = (byte) (B3 + B2 + B1 + B0);
    if (A > B) {
      state.setPort(4, Value.TRUE, 1);
      state.setPort(5, Value.FALSE, 1);
      state.setPort(6, Value.FALSE, 1);
    } else if (A < B) {
      state.setPort(4, Value.FALSE, 1);
      state.setPort(5, Value.FALSE, 1);
      state.setPort(6, Value.TRUE, 1);
    } else {
      if (state.getPortValue(2) == Value.TRUE) {
        state.setPort(4, Value.FALSE, 1);
        state.setPort(5, Value.TRUE, 1);
        state.setPort(6, Value.FALSE, 1);
      } else if (state.getPortValue(1) == Value.TRUE && state.getPortValue(3) == Value.TRUE) {
        state.setPort(4, Value.FALSE, 1);
        state.setPort(5, Value.FALSE, 1);
        state.setPort(6, Value.FALSE, 1);
      } else if (state.getPortValue(1) == Value.TRUE) {
        state.setPort(4, Value.FALSE, 1);
        state.setPort(5, Value.FALSE, 1);
        state.setPort(6, Value.TRUE, 1);
      } else if (state.getPortValue(3) == Value.TRUE) {
        state.setPort(4, Value.TRUE, 1);
        state.setPort(5, Value.FALSE, 1);
        state.setPort(6, Value.FALSE, 1);
      } else {
        state.setPort(4, Value.TRUE, 1);
        state.setPort(5, Value.FALSE, 1);
        state.setPort(6, Value.TRUE, 1);
      }
    }
  }

  @Override
  public String getHDLName(AttributeSet attrs) {
    StringBuffer CompleteName = new StringBuffer();
    CompleteName.append(CorrectLabel.getCorrectLabel("TTL" + this.getName()).toUpperCase());
    return CompleteName.toString();
  }

  @Override
  public boolean HDLSupportedComponent(String HDLIdentifier, AttributeSet attrs) {
    if (MyHDLGenerator == null) MyHDLGenerator = new Ttl7485HDLGenerator();
    return MyHDLGenerator.HDLTargetSupported(HDLIdentifier, attrs);
  }
}
