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

/**
 * TTL 74x182 look-ahead carry generator
 * Model based on <a href="https://www.ti.com/lit/ds/symlink/sn54s182.pdf">74LS182 datasheet</a>.
 */
public class Ttl74182 extends AbstractTtlGate {
  /**
   * Unique identifier of the tool, used as reference in project files.
   * Do NOT change as it will prevent project files from loading.
   * Identifier value MUST be unique string among all tools.
   */
  public static final String _ID = "74182";

  // IC pin indices as specified in the datasheet
  public static final byte nG0 = 3;
  public static final byte nG1 = 1;
  public static final byte nG2 = 14;
  public static final byte nG3 = 5;

  public static final byte nP0 = 4;
  public static final byte nP1 = 2;
  public static final byte nP2 = 15;
  public static final byte nP3 = 6;

  public static final byte Cn = 13;

  public static final byte nP = 7;
  public static final byte nG = 10;

  public static final byte Cnx = 12;
  public static final byte Cny = 11;
  public static final byte Cnz = 9;

  public static final byte GND = 8;
  public static final byte VCC = 16;

  public static final int DELAY = 1;

  private InstanceState _state;

  public Ttl74182() {
    super(
            _ID,
            (byte) 16,
            new byte[] {
              nP, nG, Cnx, Cny, Cnz
            },
            new String[] {
              "G1", "P1", "G0", "P0", "G3", "P3", "P",
              "Cnz", "G", "Cny", "Cnx", "Cn", "G2", "P2"
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

    var p = new boolean[] { !getPort(nP0), !getPort(nP1), !getPort(nP2), !getPort(nP3) }; // Active low
    var g = new boolean[] { !getPort(nG0), !getPort(nG1), !getPort(nG2), !getPort(nG3) }; // Active low
    var ci = getPort(Cn);

    // Determine outputs
    var po = p[3] && p[2] && p[1] && p[0];
    var go = (g[3])
          || (p[3] && g[2])
          || (p[3] && p[2] && g[1])
          || (p[3] && p[2] && p[1] && g[0]);
    var cx = (g[0])
          || (p[0] && ci);
    var cy = (g[1])
          || (p[1] && g[0])
          || (p[1] && p[0] && ci);
    var cz = (g[2])
          || (p[2] && g[1])
          || (p[2] && p[1] && g[0])
          || (p[2] && p[1] && p[0] && ci);

    // Set outputs
    setPort(nP, !po); // Active low
    setPort(nG, !go); // Active low
    setPort(Cnx, cx);
    setPort(Cny, cy);
    setPort(Cnz, cz);
  }
}
