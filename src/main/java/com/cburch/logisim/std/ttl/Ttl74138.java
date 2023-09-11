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

import com.cburch.logisim.data.Value;
import com.cburch.logisim.instance.InstancePainter;
import com.cburch.logisim.instance.InstanceState;
import java.util.ArrayList;

/**
 * TTL 74x138 3-line to 8-line decoders/multiplexers
 * Model based on https://www.ti.com/product/SN74LS138 datasheet.
 */
public class Ttl74138 extends AbstractTtlGate {
  /**
   * Unique identifier of the tool, used as reference in project files.
   * Do NOT change as it will prevent project files from loading.
   *
   * Identifier value must MUST be unique string among all tools.
   */
  public static final String _ID = "74138";

  // IC pin indices as specified in the datasheet
  public static final byte A = 1;
  public static final byte B = 2;
  public static final byte C = 3;
  public static final byte nEN2A = 4;
  public static final byte nEN2B = 5;
  public static final byte EN1 = 6;
  public static final byte nY7 = 7;
  public static final byte GND = 8;
  public static final byte nY6 = 9;
  public static final byte nY5 = 10;
  public static final byte nY4 = 11;
  public static final byte nY3 = 12;
  public static final byte nY2 = 13;
  public static final byte nY1 = 14;
  public static final byte nY0 = 15;
  public static final byte VCC = 16;

  public static final int DELAY = 1;

  public Ttl74138() {
    super(
        _ID,
        (byte) 16,
        new byte[] {
          nY0, nY1, nY2, nY3, nY4, nY5, nY6, nY7
        },
        new String[] {
          "A", "B", "C", "nG2A Enable (active LOW)", "nG2B Enable (active LOW)", "G1 Enable (active HIGH)", "nY7",
          "nY6", "nY5", "nY4", "nY3", "nY2", "nY1", "nY0"
        },
        null);
  }

  @Override
  public void paintInternal(InstancePainter painter, int x, int y, int height, boolean up) {
    // As tooltips can be longer than what can fit as pin name while painting IC internals,
    // we need to shorten it first to up to 4 characters to keep the diagram readable.
    final var label_len_max = 4;
    final var names = new ArrayList<String>();
    for (final var name : portNames) {
      final var tmp = name.split("\\s+");
      names.add((tmp[0].length() <= label_len_max) ? tmp[0] : tmp[0].substring(0, label_len_max));
    }
    super.paintBase(painter, true, false);
    Drawgates.paintPortNames(painter, x, y, height, names.toArray(new String[0]));
  }

  // Port consts are datasheet based (1-indexed), but in code we have them 0-indexed
  // with GND, VCC omitted (thus indices are shifted comparing to datasheet numbers).
  protected byte mapPort(byte dsIdx) {
    return (byte) ((dsIdx <= GND) ? dsIdx - 1 : dsIdx - 2);
  }

  protected void computeState(InstanceState state, byte inEn1, byte inEn2a, byte inEn2b, byte inA, byte inB, byte inC, byte[] outPorts) {
    final var enabled =
        state.getPortValue(mapPort(inEn1)) == Value.TRUE        // Active HIGH
        && state.getPortValue(mapPort(inEn2a)) == Value.FALSE   // Active LOW
        && state.getPortValue(mapPort(inEn2b)) == Value.FALSE;  // Active LOW
    final var A = state.getPortValue(mapPort(inA)) == Value.TRUE ? (byte) 1 : 0;
    final var B = state.getPortValue(mapPort(inB)) == Value.TRUE ? (byte) 2 : 0;
    final var C = state.getPortValue(mapPort(inC)) == Value.TRUE ? (byte) 4 : 0;

    final int[][] outputPortStates = {
      {1, 0, 0, 0, 0, 0, 0, 0},
      {0, 1, 0, 0, 0, 0, 0, 0},
      {0, 0, 1, 0, 0, 0, 0, 0},
      {0, 0, 0, 1, 0, 0, 0, 0},
      {0, 0, 0, 0, 1, 0, 0, 0},
      {0, 0, 0, 0, 0, 1, 0, 0},
      {0, 0, 0, 0, 0, 0, 1, 0},
      {0, 0, 0, 0, 0, 0, 0, 1},
    };

    for (var i = 0; i < 8; i++) { // Active LOW
      final var val = enabled ? (outputPortStates[A + B + C][i] == 0 ? Value.TRUE : Value.FALSE) : Value.TRUE;
      state.setPort(mapPort(outPorts[i]), val, DELAY);
    }
  }

  @Override
  public void propagateTtl(InstanceState state) {
    byte[] out = {nY0, nY1, nY2, nY3, nY4, nY5, nY6, nY7};
    computeState(state, EN1, nEN2A, nEN2B, A, B, C, out);
  }

}
