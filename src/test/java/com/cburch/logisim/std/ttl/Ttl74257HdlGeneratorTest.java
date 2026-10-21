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

class Ttl74257HdlGeneratorTest {

  private final String originalHdlType = AppPreferences.HdlType.get();

  @AfterEach
  void restoreHdlType() {
    AppPreferences.HdlType.set(originalHdlType);
  }

  @Test
  void vhdlSelectsASourceOrReleasesTheOutput() {
    final var hdl = functionality(HdlGeneratorFactory.VHDL);

    assertTrue(containsIgnoringCase(
        hdl, "Y1 <= 'Z' WHEN nOE = '1' ELSE I1_1 WHEN S = '1' ELSE I0_1;"));
    assertTrue(containsIgnoringCase(
        hdl, "Y2 <= 'Z' WHEN nOE = '1' ELSE I1_2 WHEN S = '1' ELSE I0_2;"));
    assertTrue(containsIgnoringCase(
        hdl, "Y3 <= 'Z' WHEN nOE = '1' ELSE I1_3 WHEN S = '1' ELSE I0_3;"));
    assertTrue(containsIgnoringCase(
        hdl, "Y4 <= 'Z' WHEN nOE = '1' ELSE I1_4 WHEN S = '1' ELSE I0_4;"));
  }

  @Test
  void verilogSelectsASourceOrReleasesTheOutput() {
    final var hdl = functionality(HdlGeneratorFactory.VERILOG);

    assertTrue(hdl.contains("assign Y1 = (nOE == 1) ? 1'bZ : ((S == 1) ? I1_1 : I0_1);"));
    assertTrue(hdl.contains("assign Y2 = (nOE == 1) ? 1'bZ : ((S == 1) ? I1_2 : I0_2);"));
    assertTrue(hdl.contains("assign Y3 = (nOE == 1) ? 1'bZ : ((S == 1) ? I1_3 : I0_3);"));
    assertTrue(hdl.contains("assign Y4 = (nOE == 1) ? 1'bZ : ((S == 1) ? I1_4 : I0_4);"));
  }

  @Test
  void exposedPowerPinsAreNotAnHdlTarget() {
    final var generator = new Ttl74257HdlGenerator();
    final var attrs = new Ttl74257().createAttributeSet();

    attrs.setValue(TtlLibrary.VCC_GND, false);
    assertTrue(generator.isHdlSupportedTarget(attrs));

    attrs.setValue(TtlLibrary.VCC_GND, true);
    assertFalse(generator.isHdlSupportedTarget(attrs));

    assertFalse(generator.isHdlSupportedTarget(null));
  }

  private static String functionality(String hdlType) {
    AppPreferences.HdlType.set(hdlType);
    final var attrs = new Ttl74257().createAttributeSet();
    return String.join(
        "\n", new Ttl74257HdlGenerator().getModuleFunctionality(null, attrs).get());
  }
}
