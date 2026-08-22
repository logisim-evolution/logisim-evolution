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

/**
 * TTL 74x38 quad dual-input NAND gate with open collector output
 * Model based on <a href="https://www.ti.com/lit/ds/symlink/sn74ls38.pdf">7438 datasheet</a>.
 */
public class Ttl7438 extends AbstractTtlGate {
  /**
   * Unique identifier of the tool, used as reference in project files.
   * Do NOT change as it will prevent project files from loading.
   * Identifier value MUST be unique string among all tools.
   */
  public static final String _ID = "7438";

  public static final int DELAY = 1;

  // IC pin indices as specified in the datasheet

  // Inputs
  public static final byte A1 = 1;
  public static final byte B1 = 2;

  public static final byte A2 = 4;
  public static final byte B2 = 5;

  public static final byte A3 = 9;
  public static final byte B3 = 10;

  public static final byte A4 = 12;
  public static final byte B4 = 13;

  // Outputs
  public static final byte Y1 = 3;
  public static final byte Y2 = 6;
  public static final byte Y3 = 8;
  public static final byte Y4 = 11;

  // Power supply
  public static final byte GND = 7;
  public static final byte VCC = 14;

  public Ttl7438() {
    super(
        _ID,
        (byte) 14,
        new byte[] {
            Y1, Y2, Y3, Y4
        },
        null,
        null,
        new String[] {
            "1A", "1B", "1Y", "2A", "2B", "2Y",
            "3Y", "3A", "3B", "4Y", "4A", "4B"
        },
        true,
        DEFAULT_HEIGHT,
        null);
  }

  @Override
  public void paintInternal(InstancePainter painter, int x, int y, int height, boolean up) {
    super.paintBase(painter, false, false);

    final var g = painter.getGraphics();

    final var portwidth = 19;
    final var portheight = 15;
    final var youtput = y + (up ? 20 : 40);

    Drawgates.paintAnd(g, x + 40, youtput, portwidth - 4, portheight, true);
    Drawgates.paintOutputgate(g, x + 50, y, x + 44, youtput, up, height);
    Drawgates.paintDoubleInputgate(g, x + 30, y, x + 44 - portwidth, youtput, portheight, up, false, height);
  }

  private record LogicScope(InstanceState state) {
    /**
     * IC pin indices are datasheet based (1-indexed), but ports are 0-indexed
     *
     * @param dsPinNr datasheet pin number
     * @return port number
     */
    private byte pinNrToPortNr(byte dsPinNr) {
      return (byte) ((dsPinNr <= GND) ? dsPinNr - 1 : dsPinNr - 2);
    }

    /**
     * Gets the current state of the specified pin
     *
     * @param dsPinNr datasheet pin number
     * @return true if the specified pin has a logic high level
     */
    private boolean getPort(byte dsPinNr) {
      return state.getPortValue(pinNrToPortNr(dsPinNr)) == Value.TRUE;
    }

    /**
     * Sets the specified open-collector pin to the specified level
     *
     * @param dsPinNr datasheet pin number
     * @param b       the logic level for the pin
     */
    private void setOcPort(byte dsPinNr, boolean b) {
      state.setPort(pinNrToPortNr(dsPinNr), b ? Value.UNKNOWN : Value.FALSE, DELAY);
    }

    /**
     * Sets the output for the specified NAND gate.
     *
     * @param A datasheet pin number for input A
     * @param B datasheet pin number for input B
     * @param Y datasheet pin number for output Y
     */
    private void setGate(byte A, byte B, byte Y) {
      final var a = getPort(A);
      final var b = getPort(B);

      setOcPort(Y, !(a && b));
    }

    public void propagate() {
      // Set outputs
      setGate(A1, B1, Y1);
      setGate(A2, B2, Y2);
      setGate(A3, B3, Y3);
      setGate(A4, B4, Y4);
    }
  }

  @Override
  public void propagateTtl(InstanceState state) {
    new LogicScope(state).propagate();
  }
}
