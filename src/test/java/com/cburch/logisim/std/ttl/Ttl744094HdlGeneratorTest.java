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

class Ttl744094HdlGeneratorTest {

  private final String originalHdlType = AppPreferences.HdlType.get();

  @AfterEach
  void restoreHdlType() {
    AppPreferences.HdlType.set(originalHdlType);
  }

  @Test
  void vhdlShiftsOnTheRisingEdgeAndDelaysTheCascadeOutput() {
    final var hdl = functionality(HdlGeneratorFactory.VHDL);

    assertTrue(containsIgnoringCase(hdl, "QP0 <= storeReg(0) WHEN OE = '1' ELSE 'Z';"));
    assertTrue(containsIgnoringCase(hdl, "QP7 <= storeReg(7) WHEN OE = '1' ELSE 'Z';"));
    assertTrue(containsIgnoringCase(hdl, "QS1 <= shiftReg(7);"));
    assertTrue(containsIgnoringCase(hdl, "QS2 <= qs2Reg;"));
    assertTrue(containsIgnoringCase(hdl, "IF (rising_edge(CP)) THEN"));
    assertTrue(containsIgnoringCase(hdl, "shiftReg <= shiftReg(6 DOWNTO 0) & D;"));
    assertTrue(containsIgnoringCase(hdl, "IF (falling_edge(CP)) THEN"));
    assertTrue(containsIgnoringCase(hdl, "qs2Reg <= shiftReg(7);"));
    assertTrue(containsIgnoringCase(hdl, "IF (STR = '1') THEN storeReg <= shiftReg;"));
  }

  @Test
  void verilogShiftsOnTheRisingEdgeAndDelaysTheCascadeOutput() {
    final var hdl = functionality(HdlGeneratorFactory.VERILOG);

    assertTrue(hdl.contains("assign QP0 = (OE == 1) ? storeReg[0] : 1'bZ;"));
    assertTrue(hdl.contains("assign QP7 = (OE == 1) ? storeReg[7] : 1'bZ;"));
    assertTrue(hdl.contains("assign QS1 = shiftReg[7];"));
    assertTrue(hdl.contains("assign QS2 = qs2Reg;"));
    assertTrue(hdl.contains("always @(posedge CP)"));
    assertTrue(hdl.contains("shiftReg <= {shiftReg[6:0], D};"));
    assertTrue(hdl.contains("always @(negedge CP)"));
    assertTrue(hdl.contains("qs2Reg <= shiftReg[7];"));
    assertTrue(hdl.contains("if (STR == 1) storeReg <= shiftReg;"));
  }

  @Test
  void exposedPowerPinsAreNotAnHdlTarget() {
    final var generator = new Ttl744094HdlGenerator();
    final var attrs = new Ttl744094().createAttributeSet();

    attrs.setValue(TtlLibrary.VCC_GND, false);
    assertTrue(generator.isHdlSupportedTarget(attrs));

    attrs.setValue(TtlLibrary.VCC_GND, true);
    assertFalse(generator.isHdlSupportedTarget(attrs));

    assertFalse(generator.isHdlSupportedTarget(null));
  }

  private static String functionality(String hdlType) {
    AppPreferences.HdlType.set(hdlType);
    final var attrs = new Ttl744094().createAttributeSet();
    return String.join(
        "\n", new Ttl744094HdlGenerator().getModuleFunctionality(null, attrs).get());
  }
}
