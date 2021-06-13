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
 * Original code by Marcin Orlowski (http://MarcinOrlowski.com), 2021.
 */

package com.cburch.logisim.std.ttl;

import com.cburch.logisim.data.AttributeSet;
import com.cburch.logisim.data.Value;
import com.cburch.logisim.fpga.designrulecheck.CorrectLabel;
import com.cburch.logisim.instance.InstancePainter;
import com.cburch.logisim.instance.InstanceState;

import java.util.ArrayList;

/**
 * TTL 74x139 Dual 2-line to 4-line decoders/multiplexers
 * Model based on https://www.ti.com/product/SN74LS139A datasheet.
 */
public class Ttl74139 extends AbstractTtlGate {

  // IC pin indices as specified in the datasheet
  public static final byte L1_nEN = 1;
  public static final byte L1_A = 2;
  public static final byte L1_B = 3;
  public static final byte L1_Y0 = 4;
  public static final byte L1_Y1 = 5;
  public static final byte L1_Y2 = 6;
  public static final byte L1_Y3 = 7;
  public static final byte GND = 8;

  public static final byte L2_Y3 = 9;
  public static final byte L2_Y2 = 10;
  public static final byte L2_Y1 = 11;
  public static final byte L2_Y0 = 12;
  public static final byte L2_B = 13;
  public static final byte L2_A = 14;
  public static final byte L2_nEN = 15;
  public static final byte VCC = 16;

  public static final int DELAY = 1;

  public Ttl74139() {
    super(
        "74139",
        (byte) 16,

        new byte[] {
          L1_Y0, L1_Y1, L1_Y2, L1_Y3,
          L2_Y0, L2_Y1, L2_Y2, L2_Y3
        },
        new String[] {
          "1nG Enable (active LOW)", "1A", "1B", "1Y0", "1Y1", "1Y2", "1Y3",
          "2Y3", "2Y2", "2Y1", "2Y0", "2B", "2A", "1nG Enable (active LOW)"
        });
  }

  @Override
  public void paintInternal(InstancePainter painter, int x, int y, int height, boolean up) {
    // As tooltips can be longer than what can fit as pin name while painting IC internals,
    // we need to shorten it first to up to 4 characters to keep the diagram readable.
    final int label_len_max = 4;
    ArrayList<String> names = new ArrayList<>();
    for (String name: portnames) {
      String[] tmp = name.split("\\s+");
      names.add((tmp[0].length() <= label_len_max) ? tmp[0] : tmp[0].substring(0,label_len_max));
    }
    super.paintBase(painter, true, false);
    Drawgates.paintPortNames(painter, x, y, height, names.toArray(new String[0]));
  }

  // Port consts are datasheet based (1-indexed), but in code we have them 0-indexed
  // with GND, VCC omitted (thus indices are shifted comparing to datasheet numbers).
  protected byte mapPort(byte dsIdx) {
    return (byte)((dsIdx <= GND) ? dsIdx - 1 : dsIdx - 2);
  }

  protected void computeState(InstanceState state, byte inEn, byte inA, byte inB, byte[] outPorts) {
    final boolean enabled = state.getPortValue(mapPort(inEn)) == Value.FALSE; // Active LOW
    final byte A = state.getPortValue(mapPort(inA)) == Value.TRUE ? (byte) 1 : 0;
    final byte B = state.getPortValue(mapPort(inB)) == Value.TRUE ? (byte) 2 : 0;

    final int[][] outputPortStates = {
      {1, 0, 0, 0},
      {0, 1, 0, 0},
      {0, 0, 1, 0},
      {0, 0, 0, 1},
    };

    for (int i = 0; i < 4; i++) {
      Value val = enabled ? (outputPortStates[A + B][i] == 1 ? Value.TRUE : Value.FALSE) : Value.TRUE;
      state.setPort(mapPort(outPorts[i]), val, DELAY);
    }
  }

  @Override
  public void ttlpropagate(InstanceState state) {
    byte[] out1 = {L1_Y0, L1_Y1, L1_Y2, L1_Y3};
    computeState(state, L1_nEN, L1_A, L1_B, out1);
    byte[] out2 = {L2_Y0, L2_Y1, L2_Y2, L2_Y3};
    computeState(state, L2_nEN, L2_A, L2_B, out2);
  }

}
