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
 * TTL 74x240 octal buffers and line drivers with three-state inverted outputs
 * Model based on https://www.ti.com/product/SN74LS240 datasheet.
 */
public class Ttl74240 extends AbstractOctalBuffers {
  /**
   * Unique identifier of the tool, used as reference in project files.
   * Do NOT change as it will prevent project files from loading.
   *
   * Identifier value must MUST be unique string among all tools.
   */
  public static final String _ID = "74240";

  public Ttl74240() {
    super(
        _ID,
        (byte) 20,
        new byte[] {3, 5, 7, 9, 12, 14, 16, 18},
        new String[] {
          "n1G", "1A1", "n2Y4", "1A2", "n2Y3", "1A3", "n2Y2", "1A4", "n2Y1",
          "2A1", "n1Y4", "2A2", "n1Y3", "2A3", "n1Y2", "2A4", "n1Y1", "n2G",
        },
        null);
    super.setOutputInverted(true, true);
    super.setEnableInverted(true, true);
  }
}
