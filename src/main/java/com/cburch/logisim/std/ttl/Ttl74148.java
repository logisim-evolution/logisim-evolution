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
 * TTL 74x148: 8-line to 3-line octal priority encoder
 * Model based on <a href="https://www.ti.com/lit/ds/symlink/sn74hc148.pdf">74HC148 datasheet</a>.
 *
 * <p>Every data, control and code pin of this device is active LOW, which the pin names denote with
 * an "n" prefix. The code outputs carry the complement of the binary number of the highest-priority
 * asserted data input, so an asserted nI0 and "no input asserted at all" produce the same code and
 * are told apart by nGS only.
 *
 * <p>A pin is treated as asserted only when it reads exactly {@link Value#FALSE}; unknown and error
 * values are treated as the inactive HIGH level. An unconnected device therefore behaves as if it
 * were disabled.
 */
public class Ttl74148 extends AbstractTtlGate {
  /**
   * Unique identifier of the tool, used as reference in project files.
   * Do NOT change as it will prevent project files from loading.
   * Identifier value MUST be unique string among all tools.
   */
  public static final String _ID = "74148";

  public static final int DELAY = 1;

  // IC pin indices as specified in the datasheet

  // Inputs
  public static final byte I4 = 1;
  public static final byte I5 = 2;
  public static final byte I6 = 3;
  public static final byte I7 = 4;

  public static final byte EI = 5;

  public static final byte I0 = 10;
  public static final byte I1 = 11;
  public static final byte I2 = 12;
  public static final byte I3 = 13;

  // Outputs
  public static final byte A2 = 6;
  public static final byte A1 = 7;
  public static final byte A0 = 9;

  public static final byte GS = 14;
  public static final byte EO = 15;

  // Power supply
  public static final byte GND = 8;
  public static final byte VCC = 16;

  /** Data inputs ordered by the code they encode, so the last entry has the highest priority. */
  private static final byte[] INPUTS = new byte[] {I0, I1, I2, I3, I4, I5, I6, I7};

  private static final byte[] OUTPUTS = new byte[] {A2, A1, A0, GS, EO};

  private static final String[] PORT_NAMES = {
    "nI4 Data input 4",
    "nI5 Data input 5",
    "nI6 Data input 6",
    "nI7 Data input 7 (highest priority)",
    "nEI Enable input",
    "nA2 Code output (MSB)",
    "nA1 Code output",
    "nA0 Code output (LSB)",
    "nI0 Data input 0 (lowest priority)",
    "nI1 Data input 1",
    "nI2 Data input 2",
    "nI3 Data input 3",
    "nGS Group select output",
    "nEO Enable output"
  };

  /** Creates a 74148 8-line to 3-line priority encoder. */
  public Ttl74148() {
    super(_ID, (byte) 16, OUTPUTS, PORT_NAMES, new Ttl74148HdlGenerator());
  }

  /**
   * Converts a 1-based datasheet pin number to a 0-based Logisim port index.
   *
   * <p>Power pins are omitted from the port list.
   *
   * @param dsPinNr datasheet pin number
   * @return port number
   */
  static byte pinNrToPortNr(byte dsPinNr) {
    return (byte) ((dsPinNr <= GND) ? dsPinNr - 1 : dsPinNr - 2);
  }

  @Override
  public void paintInternal(InstancePainter painter, int x, int y, int height, boolean up) {
    super.paintBase(painter, true, false);
    Drawgates.paintPortNamesByPin(
        painter,
        x,
        y,
        height,
        new String[] {
          "nI4", "nI5", "nI6", "nI7", "nEI", "nA2", "nA1", null,
          "nA0", "nI0", "nI1", "nI2", "nI3", "nGS", "nEO", null
        });
  }

  private static boolean isAsserted(InstanceState state, byte dsPinNr) {
    return state.getPortValue(pinNrToPortNr(dsPinNr)) == Value.FALSE;
  }

  private static void setAsserted(InstanceState state, byte dsPinNr, boolean asserted) {
    state.setPort(pinNrToPortNr(dsPinNr), asserted ? Value.FALSE : Value.TRUE, DELAY);
  }

  /**
   * Finds the code of the highest-priority asserted data input.
   *
   * @return the code to encode, or -1 when the device is disabled or no input is asserted
   */
  private static int selectedInput(InstanceState state) {
    if (!isAsserted(state, EI)) {
      return -1;
    }
    for (var code = INPUTS.length - 1; code >= 0; code--) {
      if (isAsserted(state, INPUTS[code])) {
        return code;
      }
    }
    return -1;
  }

  @Override
  public void propagateTtl(InstanceState state) {
    final var code = selectedInput(state);
    final var encoding = code >= 0;

    setAsserted(state, A0, encoding && (code & 1) != 0);
    setAsserted(state, A1, encoding && (code & 2) != 0);
    setAsserted(state, A2, encoding && (code & 4) != 0);
    setAsserted(state, GS, encoding);
    setAsserted(state, EO, isAsserted(state, EI) && !encoding);
  }
}
