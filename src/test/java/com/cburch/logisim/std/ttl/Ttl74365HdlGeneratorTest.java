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

class Ttl74365HdlGeneratorTest {

  private final String originalHdlType = AppPreferences.HdlType.get();

  @AfterEach
  void restoreHdlType() {
    AppPreferences.HdlType.set(originalHdlType);
  }

  @Test
  void vhdlCopiesEachInputOnlyWhileBothEnablesAreLow() {
    final var hdl = functionality(HdlGeneratorFactory.VHDL);

    assertTrue(containsIgnoringCase(hdl, "s_enabled <= (NOT nOE1) AND (NOT nOE2);"));
    assertTrue(containsIgnoringCase(hdl, "Y1 <= A1 WHEN s_enabled = '1' ELSE 'Z';"));
    assertTrue(containsIgnoringCase(hdl, "Y4 <= A4 WHEN s_enabled = '1' ELSE 'Z';"));
    assertTrue(containsIgnoringCase(hdl, "Y6 <= A6 WHEN s_enabled = '1' ELSE 'Z';"));
  }

  @Test
  void verilogCopiesEachInputOnlyWhileBothEnablesAreLow() {
    final var hdl = functionality(HdlGeneratorFactory.VERILOG);

    assertTrue(hdl.contains("assign s_enabled = ~nOE1 & ~nOE2;"));
    assertTrue(hdl.contains("assign Y1 = s_enabled ? A1 : 1'bZ;"));
    assertTrue(hdl.contains("assign Y4 = s_enabled ? A4 : 1'bZ;"));
    assertTrue(hdl.contains("assign Y6 = s_enabled ? A6 : 1'bZ;"));
  }

  @Test
  void exposedPowerPinsAreNotAnHdlTarget() {
    final var generator = new Ttl74365HdlGenerator();
    final var attrs = new Ttl74365().createAttributeSet();

    attrs.setValue(TtlLibrary.VCC_GND, false);
    assertTrue(generator.isHdlSupportedTarget(attrs));

    attrs.setValue(TtlLibrary.VCC_GND, true);
    assertFalse(generator.isHdlSupportedTarget(attrs));

    assertFalse(generator.isHdlSupportedTarget(null));
  }

  private static String functionality(String hdlType) {
    AppPreferences.HdlType.set(hdlType);
    final var attrs = new Ttl74365().createAttributeSet();
    return String.join(
        "\n", new Ttl74365HdlGenerator().getModuleFunctionality(null, attrs).get());
  }
}
