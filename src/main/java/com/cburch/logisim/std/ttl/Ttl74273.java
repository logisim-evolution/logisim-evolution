/*
 * Logisim-evolution - digital logic design tool and simulator
 * © 2001 Logisim-evolution contributors
 *
 * https://github.com/logisim-evolution/
 *
 * This is free software released under GNU GPLv3 license
 */

package com.cburch.logisim.std.ttl;

public class Ttl74273 extends AbstractOctalFlops {
  /**
   * Unique identifier of the tool, used as reference in project files.
   * Do NOT change as it will prevent project files from loading.
   *
   * Identifier value must MUST be unique string among all tools.
   */
  public static final String _ID = "74273";

  public Ttl74273() {
    super(
        _ID,
        (byte) 20,
        new byte[] {2, 5, 6, 9, 12, 15, 16, 19},
        new String[] {
          "nCLR", "Q1", "D1", "D2", "Q2", "Q3", "D3", "D4", "Q4", "CLK", "Q5", "D5", "D6", "Q6",
          "Q7", "D7", "D8", "Q8"
        },
        new AbstractOctalFlopsHdlGenerator(false));
    super.setWe(false);
  }
}
