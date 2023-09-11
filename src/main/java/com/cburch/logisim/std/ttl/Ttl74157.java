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
 * TTL 74x157: Quadruple 2-line to 1-line data selector
 *
 * <p>Model based on https://www.ti.com/product/SN74LS157 datasheet.
 */
public class Ttl74157 extends AbstractTtlGate {
  /**
   * Unique identifier of the tool, used as reference in project files. Do NOT change as it will
   * prevent project files from loading.
   *
   * <p>Identifier value must MUST be unique string among all tools.
   */
  public static final String _ID = "74157";

  // IC pin indices as specified in the datasheet
  public static final byte SELECT = 1;
  public static final byte L1_A = 2;
  public static final byte L1_B = 3;
  public static final byte L1_Y = 4;
  public static final byte L2_A = 5;
  public static final byte L2_B = 6;
  public static final byte L2_Y = 7;
  public static final byte GND = 8;

  public static final byte L3_Y = 9;
  public static final byte L3_B = 10;
  public static final byte L3_A = 11;
  public static final byte L4_Y = 12;
  public static final byte L4_B = 13;
  public static final byte L4_A = 14;
  public static final byte STROBE = 15;
  public static final byte VCC = 16;

  public static final int DELAY = 1;

  // Needed for 74x158 implementation which is 74x157 with inverted output.
  protected final boolean invertOutput;

  protected static final String[] pinNames = {
    "SELECT", "1A", "1B", "1Y", "2A", "2B", "2Y",
    "3Y", "3B", "3A", "4Y", "4B", "4A", "nSTROBE (active LOW)"
  };

  private static final byte pinCount = 16;
  private static final byte[] outPins = {L1_Y, L2_Y, L3_Y, L4_Y};

  public Ttl74157() {
    super(_ID, pinCount, outPins, pinNames, null);
    invertOutput = false;
  }

  public Ttl74157(String icName, boolean invertOutput) {
    super(icName, pinCount, outPins, pinNames, null);
    this.invertOutput = invertOutput;
  }

  @Override
  public void paintInternal(InstancePainter painter, int x, int y, int height, boolean up) {
    // As tooltips can be longer than what can fit as pin name while painting IC internals,
    // we need to shorten it first to up to 4 characters to keep the diagram readable.
    final var maxLabelLength = 4;
    final var names = new ArrayList<String>();
    for (final var name : portNames) {
      final var tmp = name.split("\\s+");
      names.add((tmp[0].length() <= maxLabelLength) ? tmp[0] : tmp[0].substring(0, maxLabelLength));
    }
    super.paintBase(painter, true, false);
    Drawgates.paintPortNames(painter, x, y, height, names.toArray(new String[0]));
  }

  // Port consts are datasheet based (1-indexed), but in code we have them 0-indexed
  // with GND, VCC omitted (thus indices are shifted comparing to datasheet numbers).
  protected byte mapPort(byte dsIdx) {
    return (byte) ((dsIdx <= GND) ? dsIdx - 1 : dsIdx - 2);
  }

  protected Value computeState(InstanceState state, byte inA, byte inB) {
    final var strobe = state.getPortValue(mapPort(STROBE)) == Value.TRUE;
    final var select = state.getPortValue(mapPort(SELECT)) == Value.TRUE;
    final var A = state.getPortValue(mapPort(inA)) == Value.TRUE;
    final var B = state.getPortValue(mapPort(inB)) == Value.TRUE;

    var Y = strobe ? false : (select ? B : A);
    if (this.invertOutput) Y = !Y;

    return Y ? Value.TRUE : Value.FALSE;
  }

  @Override
  public void propagateTtl(InstanceState state) {
    state.setPort(mapPort(L1_Y), computeState(state, L1_A, L1_B), DELAY);
    state.setPort(mapPort(L2_Y), computeState(state, L2_A, L2_B), DELAY);
    state.setPort(mapPort(L3_Y), computeState(state, L3_A, L3_B), DELAY);
    state.setPort(mapPort(L4_Y), computeState(state, L4_A, L4_B), DELAY);
  }
}
