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

/** HDL text for the 74HC640 octal inverting bus transceiver. */
class Ttl74640HdlGeneratorTest {

  private final String originalHdlType = AppPreferences.HdlType.get();

  @AfterEach
  void restoreHdlType() {
    AppPreferences.HdlType.set(originalHdlType);
  }

  @Test
  void vhdlInvertsTheSelectedBusAndReleasesTheOther() {
    final var hdl = functionality(HdlGeneratorFactory.VHDL);

    assertTrue(containsIgnoringCase(hdl, "B1 <= NOT A1 WHEN nOE = '0' AND DIR = '1' ELSE 'Z';"));
    assertTrue(containsIgnoringCase(hdl, "B8 <= NOT A8 WHEN nOE = '0' AND DIR = '1' ELSE 'Z';"));
    assertTrue(containsIgnoringCase(hdl, "A1 <= NOT B1 WHEN nOE = '0' AND DIR = '0' ELSE 'Z';"));
    assertTrue(containsIgnoringCase(hdl, "A8 <= NOT B8 WHEN nOE = '0' AND DIR = '0' ELSE 'Z';"));
  }

  @Test
  void verilogInvertsTheSelectedBusAndReleasesTheOther() {
    final var hdl = functionality(HdlGeneratorFactory.VERILOG);

    assertTrue(hdl.contains("assign B1 = (nOE == 0 && DIR == 1) ? ~A1 : 1'bz;"));
    assertTrue(hdl.contains("assign B8 = (nOE == 0 && DIR == 1) ? ~A8 : 1'bz;"));
    assertTrue(hdl.contains("assign A1 = (nOE == 0 && DIR == 0) ? ~B1 : 1'bz;"));
    assertTrue(hdl.contains("assign A8 = (nOE == 0 && DIR == 0) ? ~B8 : 1'bz;"));
  }

  @Test
  void exposedPowerPinsAreNotAnHdlTarget() {
    final var generator = new Ttl74640HdlGenerator();
    final var attrs = new Ttl74640().createAttributeSet();

    attrs.setValue(TtlLibrary.VCC_GND, false);
    assertTrue(generator.isHdlSupportedTarget(attrs));

    attrs.setValue(TtlLibrary.VCC_GND, true);
    assertFalse(generator.isHdlSupportedTarget(attrs));

    assertFalse(generator.isHdlSupportedTarget(null));
  }

  private static String functionality(String hdlType) {
    AppPreferences.HdlType.set(hdlType);
    final var attrs = new Ttl74640().createAttributeSet();
    return String.join(
        "\n", new Ttl74640HdlGenerator().getModuleFunctionality(null, attrs).get());
  }
}
