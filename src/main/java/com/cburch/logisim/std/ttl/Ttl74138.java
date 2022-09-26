/*
 * This file is part of Logisim-evolution.
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
 * with Logisim-evolution. If not, see <http://www.gnu.org/licenses/>.
 *
 */

package com.cburch.logisim.std.ttl;

import com.cburch.logisim.data.Value;
import com.cburch.logisim.instance.InstancePainter;
import com.cburch.logisim.instance.InstanceState;
import java.util.ArrayList;

/**
 * TTL 3-line to 8-line decoder
 * Model based on https://www.ti.com/product/SN74LS138 datasheet.
 */
public class Ttl74138 extends AbstractTtlGate {
  /**
   * Unique identifier of the tool, used as reference in project files.
   * Do NOT change as it will prevent project files from loading.
   *
   * Identifier value MUST be unique string among all tools.
   */
  public static final String _ID = "74138";

  // IC pin indices as specified in the datasheet
  public static final byte EN1 = 6;
  public static final byte nEN2a = 4;
  public static final byte nEN2b = 5;
  public static final byte A = 1;
  public static final byte B = 2;
  public static final byte C = 3;
  public static final byte Y0 = 15;
  public static final byte Y1 = 14;
  public static final byte Y2 = 13;
  public static final byte Y3 = 12;
  public static final byte Y4 = 11;
  public static final byte Y5 = 10;
  public static final byte Y6 = 9;
  public static final byte Y7 = 7;
  public static final byte GND = 8;
  public static final byte VCC = 16;

  public static final int DELAY = 1;

  public Ttl74138() {
    super(
            _ID,
            (byte) 16,
            new byte[] {
              Y0, Y1, Y2, Y3, Y4, Y5, Y6, Y7
            },
            new String[] {
              "A", "B", "C", "EN2a Enable (active LOW)", "EN2b Enable (active LOW)", "EN1 Enable (active HIGH)",
              "Y7", "Y6", "Y5", "Y4", "Y3", "Y2", "Y1", "Y0"
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

  // Port constants are datasheet based (1-indexed), but in code we have them 0-indexed
  // with GND, VCC omitted (thus indices are shifted comparing to datasheet numbers).
  protected byte mapPort(byte dsIdx) {
    return (byte) ((dsIdx <= GND) ? dsIdx - 1 : dsIdx - 2);
  }

  @Override
  public void propagateTtl(InstanceState state) {
    final var en1 = state.getPortValue(mapPort(EN1)) == Value.TRUE;
    final var en2a = state.getPortValue(mapPort(nEN2a)) == Value.FALSE;
    final var en2b = state.getPortValue(mapPort(nEN2b)) == Value.FALSE;
    final var enabled = en1 && en2a && en2b;
    final var a = state.getPortValue(mapPort(A)) == Value.TRUE ? (byte) 1 : 0;
    final var b = state.getPortValue(mapPort(B)) == Value.TRUE ? (byte) 2 : 0;
    final var c = state.getPortValue(mapPort(C)) == Value.TRUE ? (byte) 4 : 0;
    final var sel = a + b + c;

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

    byte[] out = {Y0, Y1, Y2, Y3, Y4, Y5, Y6, Y7};

    for (var i = 0; i < out.length; i++) {
      final var val = enabled ? (outputPortStates[sel][i] == 0 ? Value.TRUE : Value.FALSE) : Value.TRUE;
      state.setPort(mapPort(out[i]), val, DELAY);
    }
  }

}
