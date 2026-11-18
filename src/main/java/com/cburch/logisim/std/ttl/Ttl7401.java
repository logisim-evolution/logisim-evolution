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
 * TTL 74x01: quad 2-input NAND gate with open-collector outputs.
 *
 * <p>Model based on the
 * <a href="https://www.ti.com/lit/ds/symlink/sn74ls01.pdf">74LS01 datasheet</a>. Each gate drives
 * its output low when both inputs are high and releases the output otherwise. A released output is
 * {@link Value#UNKNOWN}, so a pull-up in the circuit can raise it. An unknown or error input that
 * could still decide the output leaves it in error. Unlike the 7400 and the open-collector 7438,
 * the pinout follows the 7402: each output is the first pin of its gate. There is no HDL model: an
 * open-collector high-impedance output is not a push-pull gate network.
 */
public class Ttl7401 extends AbstractTtlGate {
  /**
   * Unique identifier of the tool, used as reference in project files. Do NOT change as it will
   * prevent project files from loading.
   *
   * <p>Identifier value must MUST be unique string among all tools.
   */
  public static final String _ID = "7401";

  public static final int PORT_INDEX_1Y = 0;
  public static final int PORT_INDEX_1A = 1;
  public static final int PORT_INDEX_1B = 2;
  public static final int PORT_INDEX_2Y = 3;
  public static final int PORT_INDEX_2A = 4;
  public static final int PORT_INDEX_2B = 5;
  public static final int PORT_INDEX_3A = 6;
  public static final int PORT_INDEX_3B = 7;
  public static final int PORT_INDEX_3Y = 8;
  public static final int PORT_INDEX_4A = 9;
  public static final int PORT_INDEX_4B = 10;
  public static final int PORT_INDEX_4Y = 11;

  private static final int DELAY = 1;
  private static final byte PIN_COUNT = 14;
  private static final byte[] OUTPUT_PINS = {1, 4, 10, 13};
  private static final String[] PORT_NAMES = {
    "1Y", "1A", "1B", "2Y", "2A", "2B", "3A", "3B", "3Y", "4A", "4B", "4Y"
  };
  /** Port indexes of {input A, input B, output Y} for each gate. */
  private static final int[][] GATES = {
    {PORT_INDEX_1A, PORT_INDEX_1B, PORT_INDEX_1Y},
    {PORT_INDEX_2A, PORT_INDEX_2B, PORT_INDEX_2Y},
    {PORT_INDEX_3A, PORT_INDEX_3B, PORT_INDEX_3Y},
    {PORT_INDEX_4A, PORT_INDEX_4B, PORT_INDEX_4Y}
  };

  public Ttl7401() {
    super(_ID, PIN_COUNT, OUTPUT_PINS, null, null, PORT_NAMES, true, DEFAULT_HEIGHT, null);
  }

  @Override
  public void paintInternal(InstancePainter painter, int x, int y, int height, boolean up) {
    final var g = painter.getGraphics();
    final var portWidth = 19;
    final var portHeight = 15;
    final var yOutput = y + (up ? 20 : 40);
    // Right-to-left like the 7402: the output pin is on the left of each gate.
    Drawgates.paintAnd(g, x + 20, yOutput, portWidth - 4, portHeight, true, true);
    Drawgates.paintOpenCollector(g, x + 4, yOutput);
    Drawgates.paintOutputgate(g, x + 10, y, x + 16, yOutput, up, height);
    Drawgates.paintDoubleInputgate(
        g, x + 50, y, x + 16 + portWidth, yOutput, portHeight, up, true, height);
  }

  @Override
  public void propagateTtl(InstanceState state) {
    for (final var gate : GATES) {
      final var nand = state.getPortValue(gate[0]).and(state.getPortValue(gate[1])).not();
      state.setPort(gate[2], openCollector(nand), DELAY);
    }
  }

  /** Drives low when the NAND result is low and releases the pin when it is high. */
  private static Value openCollector(Value nand) {
    if (nand == Value.FALSE) return Value.FALSE;
    if (nand == Value.TRUE) return Value.UNKNOWN;
    return Value.ERROR;
  }
}
