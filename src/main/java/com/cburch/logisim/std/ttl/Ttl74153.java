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
 * TTL 74x153: Dual 4-line to 1-line data selector
 * Model based on <a href="https://www.ti.com/lit/ds/symlink/sn74ls153.pdf">74LS153 datasheet</a>.
 */
public class Ttl74153 extends AbstractTtlGate {
  /**
   * Unique identifier of the tool, used as reference in project files.
   * Do NOT change as it will prevent project files from loading.
   * Identifier value MUST be unique string among all tools.
   */
  public static final String _ID = "74153";

  public static final int DELAY = 1;

  // IC pin indices as specified in the datasheet

  // Inputs
  public static final byte A = 14;
  public static final byte B = 2;

  public static final byte L1_G = 1;
  public static final byte L1_C0 = 6;
  public static final byte L1_C1 = 5;
  public static final byte L1_C2 = 4;
  public static final byte L1_C3 = 3;

  public static final byte L2_G = 15;
  public static final byte L2_C0 = 10;
  public static final byte L2_C1 = 11;
  public static final byte L2_C2 = 12;
  public static final byte L2_C3 = 13;

  // Outputs
  public static final byte L1_Y = 7;
  public static final byte L2_Y = 9;

  // Power supply
  public static final byte GND = 8;
  public static final byte VCC = 16;

  private InstanceState _state;

  public Ttl74153() {
    super(
            _ID,
            (byte) 16,
            new byte[] {
              L1_Y, L2_Y
            },
            new String[] {
              "n1G", "B", "1C3", "1C2", "1C1", "1C0", "1Y",
              "2Y", "2C0", "2C1", "2C2", "2C3", "A", "n2G"
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

  /** IC pin indices are datasheet based (1-indexed), but ports are 0-indexed
   *
   * @param dsPinNr datasheet pin number
   * @return port number
   */
  protected byte pinNrToPortNr(byte dsPinNr) {
    return (byte) ((dsPinNr <= GND) ? dsPinNr - 1 : dsPinNr - 2);
  }

  /** Gets the current state of the specified pin
   *
   * @param dsPinNr datasheet pin number
   * @return true if the specified pin has a logic high level
   */
  private boolean getPort(byte dsPinNr) {
    return _state.getPortValue(pinNrToPortNr(dsPinNr)) == Value.TRUE;
  }

  /** Sets the specified pin to the specified level
   *
   * @param dsPinNr datasheet pin number
   * @param b the logic level for the pin
   */
  private void setPort(byte dsPinNr, boolean b) {
    _state.setPort(pinNrToPortNr(dsPinNr), b ? Value.TRUE : Value.FALSE, DELAY);
  }

  @Override
  public void propagateTtl(InstanceState state) {
    _state = state;

    final boolean[] data1 = {getPort(L1_C0), getPort(L1_C1), getPort(L1_C2), getPort(L1_C3)};
    final boolean[] data2 = {getPort(L2_C0), getPort(L2_C1), getPort(L2_C2), getPort(L2_C3)};
    final int select = (getPort(B) ? 2 : 0) + (getPort(A) ? 1 : 0);

    setPort(L1_Y, !getPort(L1_G) && data1[select]);
    setPort(L2_Y, !getPort(L2_G) && data2[select]);
  }
}
