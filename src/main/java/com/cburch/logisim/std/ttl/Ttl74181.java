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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Arrays;

/**
 * TTL 74x181 arithmetic logic unit
 * Model based on <a href="https://www.ti.com/lit/ds/symlink/sn54s181.pdf">74LS181 datasheet</a>.
 */
public class Ttl74181 extends AbstractTtlGate {
  /**
   * Unique identifier of the tool, used as reference in project files.
   * Do NOT change as it will prevent project files from loading.
   * Identifier value MUST be unique string among all tools.
   */
  public static final String _ID = "74181";

  // IC pin indices as specified in the datasheet
  public static final byte A0 = 2;
  public static final byte A1 = 23;
  public static final byte A2 = 21;
  public static final byte A3 = 19;

  public static final byte B0 = 1;
  public static final byte B1 = 22;
  public static final byte B2 = 20;
  public static final byte B3 = 18;

  public static final byte F0 = 9;
  public static final byte F1 = 10;
  public static final byte F2 = 11;
  public static final byte F3 = 13;

  public static final byte S0 = 6;
  public static final byte S1 = 5;
  public static final byte S2 = 4;
  public static final byte S3 = 3;

  public static final byte nCi = 7;
  public static final byte nCo = 16;

  public static final byte M = 8;

  public static final byte AeqB = 14;

  public static final byte X = 15;
  public static final byte Y = 17;

  public static final byte GND = 12;
  public static final byte VCC = 24;

  public static final int DELAY = 1;

  private InstanceState _state;
  static final Logger logger = LoggerFactory.getLogger(Ttl74181.class);

  public Ttl74181() {
    super(
            _ID,
            (byte) 24,
            new byte[] {
              F0, F1, F2, F3, nCo, AeqB, X, Y
            },
            new String[] {
              "B0", "A0", "S3", "S2", "S1", "S0", "nCi", "M", "F0", "F1", "F2",
              "F3", "A=B", "X", "nCo", "Y", "B3", "A3", "B2", "A2", "B1", "A1"
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

  /**
   * Implements a "PAL32L1" device i.e. a programmable array logic device with a virtually
   * unlimited sum of products with up to 32 inputs for each product.
   *
   * @param in the inputs
   * @param products list of products, if bit n of a product is a one, then input n is part of that product
   * @return the sum of products
   */
  private boolean pal32L1(ArrayList<Boolean> in, int[] products) {
    var or = false;

    for (var i = 0; i < products.length && !or; i++) {
      var product = products[i];
      var and = true;

      for (var j = 0; j < 32 && and; j++) {
        if ((product & (1 << j)) != 0) {
          and = in.get(j);
        }
      }

      or = and;
    }

    return or;
  }

  @Override
  public void propagateTtl(InstanceState state) {
    _state = state;

    var a = new ArrayList<>(Arrays.asList(getPort(A0), getPort(A1), getPort(A2), getPort(A3)));
    var b = new ArrayList<>(Arrays.asList(getPort(B0), getPort(B1), getPort(B2), getPort(B3)));
    var bn = new ArrayList<>(b.stream().map(x -> !x).toList());
    var s = new ArrayList<>(Arrays.asList(getPort(S0), getPort(S1), getPort(S2), getPort(S3)));
    var ci = getPort(nCi);
    var m = getPort(M);

    // Level 1 PAL
    var level1 = new ArrayList<Boolean>() {
      {
        addAll(a);
        addAll(b);
        addAll(bn);
        addAll(s);
      }
    };

    var p = new ArrayList<>(Arrays.asList(
        !pal32L1(level1, new int[] { 0x00008011, 0x00004101 }),
        !pal32L1(level1, new int[] { 0x00008022, 0x00004202 }),
        !pal32L1(level1, new int[] { 0x00008044, 0x00004404 }),
        !pal32L1(level1, new int[] { 0x00008088, 0x00004808 })));

    var q = new ArrayList<>(Arrays.asList(
        !pal32L1(level1, new int[] { 0x00002100, 0x00001010, 0x00000001 }),
        !pal32L1(level1, new int[] { 0x00002200, 0x00001020, 0x00000002 }),
        !pal32L1(level1, new int[] { 0x00002400, 0x00001040, 0x00000004 }),
        !pal32L1(level1, new int[] { 0x00002800, 0x00001080, 0x00000008 })));

    // Level 2 PAL
    var level2 = new ArrayList<Boolean>() {
      {
        addAll(p);
        addAll(q);
        add(!m);
        add(ci);
      }
    };

    var r = new ArrayList<>(Arrays.asList(
        !pal32L1(level2, new int[] { 0x00000300                                     }),
        !pal32L1(level2, new int[] { 0x00000301,                         0x00000110 }),
        !pal32L1(level2, new int[] { 0x00000303,             0x00000112, 0x00000120 }),
        !pal32L1(level2, new int[] { 0x00000307, 0x00000116, 0x00000124, 0x00000140 })));

    // Determine outputs
    var x = !pal32L1(level2, new int[] { 0x00000080, 0x00000048, 0x0000002c, 0x0000001e });
    var y = !pal32L1(level2, new int[] { 0x0000000f });
    var co = !(!pal32L1(level2, new int[] { 0x0000020f }) && x);

    var eq = (a.get(0) == b.get(0)) && (a.get(1) == b.get(1)) && (a.get(2) == b.get(2) && a.get(3) == b.get(3));

    var f0 = p.get(0) ^ q.get(0) ^ r.get(0);
    var f1 = p.get(1) ^ q.get(1) ^ r.get(1);
    var f2 = p.get(2) ^ q.get(2) ^ r.get(2);
    var f3 = p.get(3) ^ q.get(3) ^ r.get(3);

    // Set outputs
    setPort(X, x);
    setPort(Y, y);
    setPort(nCo, co);
    setPort(AeqB, eq);

    setPort(F3, f3);
    setPort(F2, f2);
    setPort(F1, f1);
    setPort(F0, f0);
  }
}
