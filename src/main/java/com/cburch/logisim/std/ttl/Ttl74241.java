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
 * TTL 74x241 octal buffers and line drivers with three-state outputs and complementary enables
 * Model based on https://www.ti.com/product/SN74LS241 datasheet.
 */
public class Ttl74241 extends AbstractOctalBuffers {
  /**
   * Unique identifier of the tool, used as reference in project files.
   * Do NOT change as it will prevent project files from loading.
   *
   * Identifier value must MUST be unique string among all tools.
   */
  public static final String _ID = "74241";

  public Ttl74241() {
    super(
        _ID,
        (byte) 20,
        new byte[] {3, 5, 7, 9, 12, 14, 16, 18},
        new String[] {
          "n1G", "1A1", "2Y4", "1A2", "2Y3", "1A3", "2Y2", "1A4", "2Y1",
          "2A1", "1Y4", "2A2", "1Y3", "2A3", "1Y2", "2A4", "1Y1", "2G",
        },
        null);
    super.setOutputInverted(false, false);
    super.setEnableInverted(true, false);
  }
}
