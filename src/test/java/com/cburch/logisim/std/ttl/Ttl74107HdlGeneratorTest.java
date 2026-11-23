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

/** Generated HDL for the 74107 dual negative-edge J-K flip-flop. */
class Ttl74107HdlGeneratorTest {

  private final String originalHdlType = AppPreferences.HdlType.get();

  @AfterEach
  void restoreHdlType() {
    AppPreferences.HdlType.set(originalHdlType);
  }

  @Test
  void vhdlResetsOnLowAndUpdatesOnTheFallingEdge() {
    final var hdl = functionality(HdlGeneratorFactory.VHDL);

    assertTrue(containsIgnoringCase(hdl, "nQ1 <= not(state1);"));
    assertTrue(containsIgnoringCase(hdl, "nQ2 <= not(state2);"));
    assertTrue(
        containsIgnoringCase(
            hdl, "next1 <= (J1 and not(state1)) or (not(K1) and state1);"));
    assertTrue(
        containsIgnoringCase(
            hdl, "next2 <= (J2 and not(state2)) or (not(K2) and state2);"));
    assertTrue(containsIgnoringCase(hdl, "if (nR1 = '0') then state1 <= '0';"));
    assertTrue(containsIgnoringCase(hdl, "if (nR2 = '0') then state2 <= '0';"));
    assertTrue(containsIgnoringCase(hdl, "elsif (falling_edge(clock)) then"));
    assertTrue(containsIgnoringCase(hdl, "elsif (falling_edge(clock2)) then"));
    assertTrue(containsIgnoringCase(hdl, "if (tick = '1') then state1 <= next1;"));
    assertTrue(containsIgnoringCase(hdl, "if (tick2 = '1') then state2 <= next2;"));
  }

  @Test
  void verilogResetsOnLowAndUpdatesOnTheFallingEdge() {
    final var hdl = functionality(HdlGeneratorFactory.VERILOG);

    assertTrue(hdl.contains("assign nQ1 = ~state1;"));
    assertTrue(hdl.contains("assign nQ2 = ~state2;"));
    assertTrue(hdl.contains("assign next1 = (J1 & ~state1) | (~K1 & state1);"));
    assertTrue(hdl.contains("assign next2 = (J2 & ~state2) | (~K2 & state2);"));
    assertTrue(hdl.contains("always @(negedge clock or negedge nR1)"));
    assertTrue(hdl.contains("always @(negedge clock2 or negedge nR2)"));
    assertTrue(hdl.contains("if (nR1 == 0) state1 <= 0;"));
    assertTrue(hdl.contains("if (nR2 == 0) state2 <= 0;"));
    assertTrue(hdl.contains("else if (tick == 1) state1 <= next1;"));
    assertTrue(hdl.contains("else if (tick2 == 1) state2 <= next2;"));
  }

  @Test
  void exposedPowerPinsAreNotAnHdlTarget() {
    final var generator = new Ttl74107HdlGenerator();
    final var attrs = new Ttl74107().createAttributeSet();

    attrs.setValue(TtlLibrary.VCC_GND, false);
    assertTrue(generator.isHdlSupportedTarget(attrs));

    attrs.setValue(TtlLibrary.VCC_GND, true);
    assertFalse(generator.isHdlSupportedTarget(attrs));

    assertFalse(generator.isHdlSupportedTarget(null));
  }

  private static String functionality(String hdlType) {
    AppPreferences.HdlType.set(hdlType);
    final var attrs = new Ttl74107().createAttributeSet();
    return String.join("\n", new Ttl74107HdlGenerator().getModuleFunctionality(null, attrs).get());
  }
}
