/*
 * Logisim-evolution - digital logic design tool and simulator
 * Copyright by the Logisim-evolution developers
 *
 * https://github.com/logisim-evolution/
 *
 * This is free software released under GNU GPLv3 license
 */

package com.cburch.logisim.fpga.hdlgenerator;

import java.util.Locale;

/**
 * Test support for asserting on generated HDL.
 */
public final class HdlText {

  private HdlText() {
    // utility class
  }

  /**
   * Returns whether hdl contains expected value ignoring case. VHDL keywords are
   * emitted in upper or lower case depending on the persisted "Use upper case for
   * VHDL keywords" preference. Tests that describe the generated logic rather
   * than its presentation must not depend on that setting, so they compare
   * case-insensitively and leave the casing itself to the preference's own
   * coverage. And it seems that preference pinning is not proper approach here
   * as preference's set() updates cached data asynchronously, so `set()`/`get()`
   * condition race can occur therefore @BeforeEach/@AfterEach approach looks
   * not reliable.
   */
  public static boolean containsIgnoringCase(String hdl, String expected) {
    return hdl.toLowerCase(Locale.ROOT).contains(expected.toLowerCase(Locale.ROOT));
  }
}
