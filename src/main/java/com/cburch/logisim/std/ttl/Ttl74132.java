/*
 * Logisim-evolution - digital logic design tool and simulator
 * Copyright by the Logisim-evolution developers
 *
 * https://github.com/logisim-evolution/
 *
 * This is free software released under GNU GPLv3 license
 */

package com.cburch.logisim.std.ttl;

/**
 * TTL 74x132: quad 2-input NAND gate with Schmitt-trigger inputs.
 *
 * <p>Model based on the
 * <a href="https://assets.nexperia.com/documents/data-sheet/74HC_HCT132.pdf">Nexperia
 * 74HC132/74HCT132 datasheet</a>. Each output is the NAND of its two inputs, and the DIP-14 pinout
 * matches the 7400. Schmitt-trigger thresholds and hysteresis are analog and are not modeled; a
 * valid digital input is treated as a logic level, as with the existing 7414 and 7424 models.
 * Drawing and HDL generation are inherited from {@link Ttl7400}.
 */
public class Ttl74132 extends Ttl7400 {
  /**
   * Unique identifier of the tool, used as reference in project files. Do NOT change as it will
   * prevent project files from loading.
   *
   * <p>Identifier value must MUST be unique string among all tools.
   */
  public static final String _ID = "74132";

  public static final int PORT_INDEX_1A = 0;
  public static final int PORT_INDEX_1B = 1;
  public static final int PORT_INDEX_1Y = 2;
  public static final int PORT_INDEX_2A = 3;
  public static final int PORT_INDEX_2B = 4;
  public static final int PORT_INDEX_2Y = 5;
  public static final int PORT_INDEX_3Y = 6;
  public static final int PORT_INDEX_3A = 7;
  public static final int PORT_INDEX_3B = 8;
  public static final int PORT_INDEX_4Y = 9;
  public static final int PORT_INDEX_4A = 10;
  public static final int PORT_INDEX_4B = 11;

  public Ttl74132() {
    super(_ID);
  }
}
