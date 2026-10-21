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

/** HDL text for the 74HC393 dual 4-bit binary ripple counter. */
class Ttl74393HdlGeneratorTest {

  private final String originalHdlType = AppPreferences.HdlType.get();

  @AfterEach
  void restoreHdlType() {
    AppPreferences.HdlType.set(originalHdlType);
  }

  @Test
  void vhdlCountsOnTheFallingEdgeAndClearsWhileResetIsHigh() {
    final var hdl = functionality(HdlGeneratorFactory.VHDL);

    assertTrue(containsIgnoringCase(hdl, "Q1_0 <= state1(0);"));
    assertTrue(containsIgnoringCase(hdl, "Q1_3 <= state1(3);"));
    assertTrue(containsIgnoringCase(hdl, "Q2_0 <= state2(0);"));
    assertTrue(containsIgnoringCase(hdl, "Q2_3 <= state2(3);"));
    assertTrue(containsIgnoringCase(hdl, "IF (MR1 = '1') THEN state1 <= \"0000\";"));
    assertTrue(containsIgnoringCase(hdl, "ELSIF (falling_edge(clock)) THEN"));
    assertTrue(
        containsIgnoringCase(
            hdl, "IF (tick = '1') THEN state1 <= std_logic_vector(unsigned(state1) + 1);"));
    assertTrue(containsIgnoringCase(hdl, "IF (MR2 = '1') THEN state2 <= \"0000\";"));
    assertTrue(containsIgnoringCase(hdl, "ELSIF (falling_edge(clock2)) THEN"));
    assertTrue(
        containsIgnoringCase(
            hdl, "IF (tick2 = '1') THEN state2 <= std_logic_vector(unsigned(state2) + 1);"));
  }

  @Test
  void verilogCountsOnTheFallingEdgeAndClearsWhileResetIsHigh() {
    final var hdl = functionality(HdlGeneratorFactory.VERILOG);

    assertTrue(hdl.contains("assign Q1_0 = state1[0];"));
    assertTrue(hdl.contains("assign Q1_3 = state1[3];"));
    assertTrue(hdl.contains("assign Q2_0 = state2[0];"));
    assertTrue(hdl.contains("assign Q2_3 = state2[3];"));
    assertTrue(hdl.contains("always @(negedge clock or posedge MR1)"));
    assertTrue(hdl.contains("if (MR1 == 1) state1 <= 0;"));
    assertTrue(hdl.contains("else if (tick == 1) state1 <= state1 + 1;"));
    assertTrue(hdl.contains("always @(negedge clock2 or posedge MR2)"));
    assertTrue(hdl.contains("if (MR2 == 1) state2 <= 0;"));
    assertTrue(hdl.contains("else if (tick2 == 1) state2 <= state2 + 1;"));
  }

  @Test
  void exposedPowerPinsAreNotAnHdlTarget() {
    final var generator = new Ttl74393HdlGenerator();
    final var attrs = new Ttl74393().createAttributeSet();

    attrs.setValue(TtlLibrary.VCC_GND, false);
    assertTrue(generator.isHdlSupportedTarget(attrs));

    attrs.setValue(TtlLibrary.VCC_GND, true);
    assertFalse(generator.isHdlSupportedTarget(attrs));

    assertFalse(generator.isHdlSupportedTarget(null));
  }

  private static String functionality(String hdlType) {
    AppPreferences.HdlType.set(hdlType);
    final var attrs = new Ttl74393().createAttributeSet();
    return String.join(
        "\n", new Ttl74393HdlGenerator().getModuleFunctionality(null, attrs).get());
  }
}
