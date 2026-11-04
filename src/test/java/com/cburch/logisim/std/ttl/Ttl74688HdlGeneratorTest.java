/*
 * Logisim-evolution - digital logic design tool and simulator
 * Copyright by the Logisim-evolution developers
 *
 * https://github.com/logisim-evolution/
 *
 * This is free software released under GNU GPLv3 license
 */

package com.cburch.logisim.std.ttl;

import static com.cburch.logisim.fpga.hdlgenerator.HdlText.containsIgnoringCase;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.cburch.logisim.fpga.hdlgenerator.HdlGeneratorFactory;
import com.cburch.logisim.prefs.AppPreferences;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

class Ttl74688HdlGeneratorTest {

  private final String originalHdlType = AppPreferences.HdlType.get();

  @AfterEach
  void restoreHdlType() {
    AppPreferences.HdlType.set(originalHdlType);
  }

  @Test
  void vhdlDrivesTheEqualOutputLowOnlyWhenEnabledAndMatched() {
    final var hdl = functionality(HdlGeneratorFactory.VHDL);

    assertTrue(containsIgnoringCase(hdl, "nPQ <= nE OR s_mismatch;"));
    assertTrue(containsIgnoringCase(hdl, "(P0 XOR Q0)"));
    assertTrue(containsIgnoringCase(hdl, "(P3 XOR Q3)"));
    assertTrue(containsIgnoringCase(hdl, "(P7 XOR Q7);"));
  }

  @Test
  void verilogDrivesTheEqualOutputLowOnlyWhenEnabledAndMatched() {
    final var hdl = functionality(HdlGeneratorFactory.VERILOG);

    assertTrue(hdl.contains("assign nPQ = nE | s_mismatch;"));
    assertTrue(hdl.contains("(P0 ^ Q0)"));
    assertTrue(hdl.contains("(P3 ^ Q3)"));
    assertTrue(hdl.contains("(P7 ^ Q7);"));
  }

  @Test
  void exposedPowerPinsAreNotAnHdlTarget() {
    final var generator = new Ttl74688HdlGenerator();
    final var attrs = new Ttl74688().createAttributeSet();

    attrs.setValue(TtlLibrary.VCC_GND, false);
    assertTrue(generator.isHdlSupportedTarget(attrs));

    attrs.setValue(TtlLibrary.VCC_GND, true);
    assertFalse(generator.isHdlSupportedTarget(attrs));

    assertFalse(generator.isHdlSupportedTarget(null));
  }

  private static String functionality(String hdlType) {
    AppPreferences.HdlType.set(hdlType);
    final var attrs = new Ttl74688().createAttributeSet();
    return String.join(
        "\n", new Ttl74688HdlGenerator().getModuleFunctionality(null, attrs).get());
  }
}
