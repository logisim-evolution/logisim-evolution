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

/** HDL text for the 74HC374 octal positive-edge D flip-flop. */
class Ttl74374HdlGeneratorTest {

  private final String originalHdlType = AppPreferences.HdlType.get();

  @AfterEach
  void restoreHdlType() {
    AppPreferences.HdlType.set(originalHdlType);
  }

  @Test
  void vhdlLoadsOnTheRisingEdgeAndReleasesWhenDisabled() {
    final var hdl = functionality(HdlGeneratorFactory.VHDL);

    assertTrue(containsIgnoringCase(hdl, "next <= D8&D7&D6&D5&D4&D3&D2&D1 WHEN tick = '1' ELSE state;"));
    assertTrue(containsIgnoringCase(hdl, "Q1   <= state(0) WHEN nOE = '0' ELSE 'Z';"));
    assertTrue(containsIgnoringCase(hdl, "Q8   <= state(7) WHEN nOE = '0' ELSE 'Z';"));
    assertTrue(containsIgnoringCase(hdl, "IF (rising_edge(clock)) THEN state <= next;"));
    assertFalse(containsIgnoringCase(hdl, "nCLR"));
  }

  @Test
  void verilogLoadsOnTheRisingEdgeAndReleasesWhenDisabled() {
    final var hdl = functionality(HdlGeneratorFactory.VERILOG);

    assertTrue(hdl.contains("assign next = (tick == 1) ? {D8, D7, D6, D5, D4, D3, D2, D1} : state;"));
    assertTrue(hdl.contains("assign Q1 = (nOE == 0) ? state[0] : 1'bz;"));
    assertTrue(hdl.contains("assign Q8 = (nOE == 0) ? state[7] : 1'bz;"));
    assertTrue(hdl.contains("always @(posedge clock)"));
    assertTrue(hdl.contains("state <= next;"));
    assertFalse(hdl.contains("nCLR"));
  }

  @Test
  void exposedPowerPinsAreNotAnHdlTarget() {
    final var generator = new Ttl74374HdlGenerator();
    final var attrs = new Ttl74374().createAttributeSet();

    attrs.setValue(TtlLibrary.VCC_GND, false);
    assertTrue(generator.isHdlSupportedTarget(attrs));

    attrs.setValue(TtlLibrary.VCC_GND, true);
    assertFalse(generator.isHdlSupportedTarget(attrs));

    assertFalse(generator.isHdlSupportedTarget(null));
  }

  private static String functionality(String hdlType) {
    AppPreferences.HdlType.set(hdlType);
    final var attrs = new Ttl74374().createAttributeSet();
    return String.join(
        "\n", new Ttl74374HdlGenerator().getModuleFunctionality(null, attrs).get());
  }
}
