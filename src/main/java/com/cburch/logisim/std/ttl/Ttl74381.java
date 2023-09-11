/*
 * Logisim-evolution - digital logic design tool and simulator
 * Copyright by the Logisim-evolution developers
 *
 * https://github.com/logisim-evolution/
 *
 * This is free software released under GNU GPLv3 license
 */

package com.cburch.logisim.std.ttl;

import com.cburch.logisim.data.Value;
import com.cburch.logisim.instance.InstancePainter;
import com.cburch.logisim.instance.InstanceState;

import java.util.ArrayList;
import java.util.Arrays;

/**
 * TTL 74x381 arithmetic logic unit
 * Model based on <a href="http://bitsavers.org/components/ti/_dataBooks/1976_TI_The_TTL_Data_Book_2ed/07.pdf">74LS381 datasheet, p 7-484 to 7-486</a>.
 */
public class Ttl74381 extends AbstractTtlGate {
  /**
   * Unique identifier of the tool, used as reference in project files.
   * Do NOT change as it will prevent project files from loading.
   * Identifier value MUST be unique string among all tools.
   */
  public static final String _ID = "74381";

  public static final int DELAY = 1;

  // IC pin indices as specified in the datasheet

  // Inputs
  public static final byte A0 = 3;
  public static final byte A1 = 1;
  public static final byte A2 = 19;
  public static final byte A3 = 17;

  public static final byte B0 = 4;
  public static final byte B1 = 2;
  public static final byte B2 = 18;
  public static final byte B3 = 16;

  public static final byte F0 = 8;
  public static final byte F1 = 9;
  public static final byte F2 = 11;
  public static final byte F3 = 12;

  public static final byte S0 = 5;
  public static final byte S1 = 6;
  public static final byte S2 = 7;

  public static final byte Ci = 15;

  // Outputs
  public static final byte P = 14;
  public static final byte G = 13;

  // Power supply
  public static final byte GND = 10;
  public static final byte VCC = 20;

  private InstanceState state;

  public Ttl74381() {
    super(
            _ID,
            (byte) 20,
            new byte[] {
              F0, F1, F2, F3, P, G
            },
            new String[] {
              "A1", "B1", "A0", "B0", "S0", "S1", "S2", "F0", "F1",
              "F2", "F3", "Gn", "Pn", "Ci", "B3", "A3", "B2", "A2"
            },
            null);
  }

  @Override
  public void paintInternal(InstancePainter painter, int x, int y, int height, boolean up) {
    // As tooltips can be longer than what can fit as pin name while painting IC internals,
    // we need to shorten it first to up to 4 characters to keep the diagram readable.
    final var labelLenMax = 4;
    final var names = new ArrayList<String>();
    for (final var name : portNames) {
      final var tmp = name.split("\\s+");
      names.add((tmp[0].length() <= labelLenMax) ? tmp[0] : tmp[0].substring(0, labelLenMax));
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
    return state.getPortValue(pinNrToPortNr(dsPinNr)) == Value.TRUE;
  }

  /** Sets the specified pin to the specified level
   *
   * @param dsPinNr datasheet pin number
   * @param b the logic level for the pin
   */
  private void setPort(byte dsPinNr, boolean b) {
    state.setPort(pinNrToPortNr(dsPinNr), b ? Value.TRUE : Value.FALSE, DELAY);
  }

  /**
   * Implements a "PAL32L1" device i.e. a programmable array logic device with a virtually
   * unlimited sum of products with up to 32 inputs for each product.
   *
   * @param in the inputs
   * @param products list of products, if bit n of a product is a one, then input n is part of that product
   * @return the active-low sum of products
   */
  private boolean pal32L1(ArrayList<Boolean> in, int[] products) {
    var or = false;

    for (var i = 0; i < products.length && !or; i++) {
      final var product = products[i];
      var and = true;

      for (var j = 0; j < 32 && and; j++) {
        if ((product & (1 << j)) != 0) {
          and = in.get(j);  // will break the inner loop at the first false product term
        }
      }

      or = and; // will break the outer loop at the first true sum term
    }

    return !or; // Active low
  }

  @Override
  public void propagateTtl(InstanceState state) {
    this.state = state;

    final var a = new ArrayList<>(Arrays.asList(getPort(A0), getPort(A1), getPort(A2), getPort(A3)));
    final var an = new ArrayList<>(a.stream().map(x -> !x).toList());
    final var b = new ArrayList<>(Arrays.asList(getPort(B0), getPort(B1), getPort(B2), getPort(B3)));
    final var bn = new ArrayList<>(b.stream().map(x -> !x).toList());
    final var s = new ArrayList<>(Arrays.asList(getPort(S0), getPort(S1), getPort(S2)));
    final var ci = getPort(Ci);

    // The logic diagram in the datasheet shows two layers of AND/NOR networks
    // plus some combinatorial functions based on S.
    //
    // The combinatorial network which processes S produces six outputs, which
    // are named U, V, W, X, Y and Z (left to right).
    //
    // The first AND/NOR layer is connected to A, B, U, V, W, X and Y.
    // The second AND/NOR layer is connected to the outputs of the first layer
    // plus Z and Ci, and also has a few XOR gates.
    //
    // Output 1 of the first layer, formed by a 3-AND/1-NOR network, is called J.
    // Output 2 of the first layer, formed by a 4-AND/1-NOR network, is called K.
    //
    // The output of second layer, formed by an n-AND/1-NOR network, is called L.

    final var u =   s.get(0) ||  s.get(1);
    final var v =   s.get(1) ||  s.get(2);
    final var w =   s.get(0) || !s.get(1);
    final var x = !(s.get(0) &&  s.get(1)) ||  s.get(2);
    final var y =  (s.get(0) &&  s.get(1)) || !s.get(2);
    final var z =  !s.get(2) &&  u;

    // Level 1 PAL
    //
    // +---+---------------+-----------------------+-------------------+-----------------------+-------------------+
    // | 20| 19  18  17  16|  15    14    13    12 | 11   10   9    8  |  7     6     5     4  | 3    2    1    0  |
    // +---+---+---+---+---+-----+-----+-----+-----+----+----+----+----+-----+-----+-----+-----+----+----+----+----+
    // | U | V | W | X | Y | /B3 | /B2 | /B1 | /B0 | B3 | B2 | B1 | B0 | /A3 | /A2 | /A1 | /A0 | A3 | A2 | A1 | A0 |
    // +---+---+---+---+---+-----+-----+-----+-----+----+----+----+----+-----+-----+-----+-----+----+----+----+----+
    final var level1 = new ArrayList<Boolean>() {
      {
        addAll(a);
        addAll(an);
        addAll(b);
        addAll(bn);
        add(y);
        add(x);
        add(w);
        add(v);
        add(u);
      }
    };

    final var j = new ArrayList<>(Arrays.asList(
            pal32L1(level1, new int[] { 0x000c1010, 0x00161001, 0x000a0110 }),
            pal32L1(level1, new int[] { 0x000c2020, 0x00162002, 0x000a0220 }),
            pal32L1(level1, new int[] { 0x000c4040, 0x00164004, 0x000a0440 }),
            pal32L1(level1, new int[] { 0x000c8080, 0x00168008, 0x000a0880 })));

    final var k = new ArrayList<>(Arrays.asList(
            pal32L1(level1, new int[] { 0x00131010, 0x000c1001, 0x000c0110, 0x00120101 }),
            pal32L1(level1, new int[] { 0x00132020, 0x000c2002, 0x000c0220, 0x00120202 }),
            pal32L1(level1, new int[] { 0x00134040, 0x000c4004, 0x000c0440, 0x00120404 }),
            pal32L1(level1, new int[] { 0x00138080, 0x000c8008, 0x000c0880, 0x00120808 })));

    // Level 2 PAL
    //
    // +--------+-------------------+-------------------+
    // | 9    8 | 7    6    5    4  | 3    2    1    0  |
    // +----+---+----+----+----+----+----+----+----+----+
    // | Ci | Z | K3 | K2 | K1 | K0 | J3 | J2 | J1 | J0 |
    // +----+---+----+----+----+----+----+----+----+----+
    final var level2 = new ArrayList<Boolean>() {
      {
        addAll(j);
        addAll(k);
        add(z);
        add(ci);
      }
    };

    final var l = new ArrayList<>(Arrays.asList(
            pal32L1(level2, new int[] { 0x00000300                                     }),
            pal32L1(level2, new int[] { 0x00000301,                         0x00000111 }),
            pal32L1(level2, new int[] { 0x00000303,             0x00000113, 0x00000122 }),
            pal32L1(level2, new int[] { 0x00000307, 0x00000117, 0x00000126, 0x00000144 })));

    // Determine outputs
    final var p = pal32L1(level2, new int[] { 0x0000000f });
    final var g = pal32L1(level2, new int[] { 0x00000088, 0x0000004c, 0x0000002e, 0x0000001f });

    final var f0 = l.get(0) ^ k.get(0);
    final var f1 = l.get(1) ^ k.get(1);
    final var f2 = l.get(2) ^ k.get(2);
    final var f3 = l.get(3) ^ k.get(3);

    // Set outputs
    setPort(P, p);
    setPort(G, g);

    setPort(F3, f3);
    setPort(F2, f2);
    setPort(F1, f1);
    setPort(F0, f0);
  }
}
