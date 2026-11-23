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
 * TTL 74x05: hex inverter with open-drain outputs.
 *
 * <p>Model based on the
 * <a href="https://assets.nexperia.com/documents/data-sheet/74HC05.pdf">74HC05 datasheet</a>. Each
 * inverter drives its output low when the input is high and releases the output when the input is
 * low. A released output is {@link Value#UNKNOWN}, so a pull-up in the circuit can raise it. An
 * unknown or error input leaves the output in error. Input thresholds of the HC and HCT variants
 * are not modeled. There is no HDL model: an open-drain high-impedance output is not a push-pull
 * gate network.
 */
public class Ttl7405 extends AbstractTtlGate {
  /**
   * Unique identifier of the tool, used as reference in project files. Do NOT change as it will
   * prevent project files from loading.
   *
   * <p>Identifier value must MUST be unique string among all tools.
   */
  public static final String _ID = "7405";

  public static final int PORT_INDEX_1A = 0;
  public static final int PORT_INDEX_1Y = 1;
  public static final int PORT_INDEX_2A = 2;
  public static final int PORT_INDEX_2Y = 3;
  public static final int PORT_INDEX_3A = 4;
  public static final int PORT_INDEX_3Y = 5;
  public static final int PORT_INDEX_4Y = 6;
  public static final int PORT_INDEX_4A = 7;
  public static final int PORT_INDEX_5Y = 8;
  public static final int PORT_INDEX_5A = 9;
  public static final int PORT_INDEX_6Y = 10;
  public static final int PORT_INDEX_6A = 11;

  private static final int DELAY = 1;
  private static final byte PIN_COUNT = 14;
  private static final byte[] OUTPUT_PINS = {2, 4, 6, 8, 10, 12};
  private static final String[] PORT_NAMES = {
    "1A", "1Y", "2A", "2Y", "3A", "3Y", "4Y", "4A", "5Y", "5A", "6Y", "6A"
  };
  private static final int[][] INVERTERS = {
    {PORT_INDEX_1A, PORT_INDEX_1Y},
    {PORT_INDEX_2A, PORT_INDEX_2Y},
    {PORT_INDEX_3A, PORT_INDEX_3Y},
    {PORT_INDEX_4A, PORT_INDEX_4Y},
    {PORT_INDEX_5A, PORT_INDEX_5Y},
    {PORT_INDEX_6A, PORT_INDEX_6Y}
  };

  public Ttl7405() {
    super(_ID, PIN_COUNT, OUTPUT_PINS, null, null, PORT_NAMES, true, DEFAULT_HEIGHT, null);
  }

  @Override
  public void paintInternal(InstancePainter painter, int x, int y, int height, boolean up) {
    final var g = painter.getGraphics();
    final var portWidth = 12;
    final var portHeight = 6;
    final var yOutput = y + (up ? 20 : 40);
    Drawgates.paintNot(g, x + 26, yOutput, portWidth, portHeight);
    Drawgates.paintOpenCollector(g, x + 30, yOutput);
    Drawgates.paintOutputgate(g, x + 30, y, x + 26, yOutput, up, height);
    Drawgates.paintSingleInputgate(g, x + 10, y, x + 26 - portWidth, yOutput, up, height);
  }

  @Override
  public void propagateTtl(InstanceState state) {
    for (final var inverter : INVERTERS) {
      state.setPort(inverter[1], openDrain(state.getPortValue(inverter[0])), DELAY);
    }
  }

  /** Drives low for a high input and releases the pin for a low input. */
  private static Value openDrain(Value input) {
    if (input == Value.TRUE) return Value.FALSE;
    if (input == Value.FALSE) return Value.UNKNOWN;
    return Value.ERROR;
  }
}
