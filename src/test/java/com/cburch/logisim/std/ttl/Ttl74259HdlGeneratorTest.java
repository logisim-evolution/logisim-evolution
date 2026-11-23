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

/** HDL text for the 74HC259 8-bit addressable latch. */
class Ttl74259HdlGeneratorTest {

  private final String originalHdlType = AppPreferences.HdlType.get();

  @AfterEach
  void restoreHdlType() {
    AppPreferences.HdlType.set(originalHdlType);
  }

  @Test
  void vhdlClearsResetsAndFollowsTheAddressedLatch() {
    final var hdl = functionality(HdlGeneratorFactory.VHDL);

    assertTrue(containsIgnoringCase(hdl, "Q0 <= state(0);"));
    assertTrue(containsIgnoringCase(hdl, "Q7 <= state(7);"));
    assertTrue(containsIgnoringCase(hdl, "IF (MR = '0' AND LE = '1') THEN"));
    assertTrue(containsIgnoringCase(hdl, "state <= \"00000000\";"));
    assertTrue(
        containsIgnoringCase(
            hdl,
            "IF (A2 = '0' AND A1 = '0' AND A0 = '0') THEN state(0) <= D; ELSE state(0) <= '0'; END IF;"));
    assertTrue(
        containsIgnoringCase(
            hdl, "IF (A2 = '1' AND A1 = '1' AND A0 = '1') THEN state(7) <= D; END IF;"));
    assertFalse(containsIgnoringCase(hdl, "rising_edge"));
  }

  @Test
  void verilogClearsResetsAndFollowsTheAddressedLatch() {
    final var hdl = functionality(HdlGeneratorFactory.VERILOG);

    assertTrue(hdl.contains("assign Q0 = state[0];"));
    assertTrue(hdl.contains("assign Q7 = state[7];"));
    assertTrue(hdl.contains("always @(*)"));
    assertTrue(hdl.contains("if (MR == 0 && LE == 1) state = 8'b00000000;"));
    assertTrue(hdl.contains("state[0] = (A2 == 0 && A1 == 0 && A0 == 0) ? D : 1'b0;"));
    assertTrue(hdl.contains("if (A2 == 1 && A1 == 1 && A0 == 1) state[7] = D;"));
    assertFalse(hdl.contains("posedge"));
  }

  @Test
  void exposedPowerPinsAreNotAnHdlTarget() {
    final var generator = new Ttl74259HdlGenerator();
    final var attrs = new Ttl74259().createAttributeSet();

    attrs.setValue(TtlLibrary.VCC_GND, false);
    assertTrue(generator.isHdlSupportedTarget(attrs));

    attrs.setValue(TtlLibrary.VCC_GND, true);
    assertFalse(generator.isHdlSupportedTarget(attrs));

    assertFalse(generator.isHdlSupportedTarget(null));
  }

  private static String functionality(String hdlType) {
    AppPreferences.HdlType.set(hdlType);
    final var attrs = new Ttl74259().createAttributeSet();
    return String.join("\n", new Ttl74259HdlGenerator().getModuleFunctionality(null, attrs).get());
  }
}
