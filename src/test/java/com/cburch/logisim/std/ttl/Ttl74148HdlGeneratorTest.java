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

class Ttl74148HdlGeneratorTest {

  private final String originalHdlType = AppPreferences.HdlType.get();

  @AfterEach
  void restoreHdlType() {
    AppPreferences.HdlType.set(originalHdlType);
  }

  @Test
  void vhdlResolvesThePriorityCascadeFromTheHighestNumberedInput() {
    final var hdl = functionality(HdlGeneratorFactory.VHDL);

    assertTrue(containsIgnoringCase(hdl, "\"000\" WHEN nI7 = '0' ELSE"));
    assertTrue(containsIgnoringCase(hdl, "\"011\" WHEN nI4 = '0' ELSE"));
    assertTrue(containsIgnoringCase(hdl, "\"110\" WHEN nI1 = '0' ELSE"));
    assertTrue(containsIgnoringCase(hdl, "s_code    <= s_priority WHEN nEI = '0' ELSE \"111\";"));
  }

  @Test
  void vhdlDerivesGroupSelectAndEnableOutputFromTheIdleState() {
    final var hdl = functionality(HdlGeneratorFactory.VHDL);

    assertTrue(containsIgnoringCase(hdl, "s_noInput <= nI0 AND nI1 AND nI2 AND nI3 AND"));
    assertTrue(containsIgnoringCase(hdl, "nI4 AND nI5 AND nI6 AND nI7;"));
    assertTrue(containsIgnoringCase(hdl, "nGS <= nEI OR s_noInput;"));
    assertTrue(containsIgnoringCase(hdl, "nEO <= nEI OR (NOT s_noInput);"));
  }

  @Test
  void verilogResolvesThePriorityCascadeFromTheHighestNumberedInput() {
    final var hdl = functionality(HdlGeneratorFactory.VERILOG);

    assertTrue(hdl.contains("assign s_priority = (nI7 == 0) ? 3'b000 :"));
    assertTrue(hdl.contains("(nI4 == 0) ? 3'b011 :"));
    assertTrue(hdl.contains("(nI1 == 0) ? 3'b110 :"));
    assertTrue(hdl.contains("assign s_code    = (nEI == 0) ? s_priority : 3'b111;"));
  }

  @Test
  void verilogDerivesGroupSelectAndEnableOutputFromTheIdleState() {
    final var hdl = functionality(HdlGeneratorFactory.VERILOG);

    assertTrue(
        hdl.contains("assign s_noInput = nI0 & nI1 & nI2 & nI3 & nI4 & nI5 & nI6 & nI7;"));
    assertTrue(hdl.contains("assign nGS = nEI | s_noInput;"));
    assertTrue(hdl.contains("assign nEO = nEI | ~s_noInput;"));
  }

  @Test
  void exposedPowerPinsAreNotAnHdlTarget() {
    final var generator = new Ttl74148HdlGenerator();
    final var attrs = new Ttl74148().createAttributeSet();

    attrs.setValue(TtlLibrary.VCC_GND, false);
    assertTrue(generator.isHdlSupportedTarget(attrs));

    attrs.setValue(TtlLibrary.VCC_GND, true);
    assertFalse(generator.isHdlSupportedTarget(attrs));

    assertFalse(generator.isHdlSupportedTarget(null));
  }

  private static String functionality(String hdlType) {
    AppPreferences.HdlType.set(hdlType);
    final var attrs = new Ttl74148().createAttributeSet();
    return String.join(
        "\n", new Ttl74148HdlGenerator().getModuleFunctionality(null, attrs).get());
  }
}
