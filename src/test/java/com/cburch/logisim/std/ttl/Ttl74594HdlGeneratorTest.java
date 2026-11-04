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

class Ttl74594HdlGeneratorTest {

  private final String originalHdlType = AppPreferences.HdlType.get();

  @AfterEach
  void restoreHdlType() {
    AppPreferences.HdlType.set(originalHdlType);
  }

  @Test
  void vhdlShiftsStoresAndClearsEachRegister() {
    final var hdl = functionality(HdlGeneratorFactory.VHDL);

    assertTrue(containsIgnoringCase(hdl, "QA <= storeReg(0);"));
    assertTrue(containsIgnoringCase(hdl, "QH <= storeReg(7);"));
    assertTrue(containsIgnoringCase(hdl, "QHp <= shiftReg(7);"));
    assertTrue(
        containsIgnoringCase(
            hdl, "nextShift <= shiftReg(6 DOWNTO 0) & SER WHEN tick = '1' ELSE shiftReg;"));
    assertTrue(containsIgnoringCase(hdl, "shiftReg WHEN tick2 = '1' ELSE"));
    assertTrue(containsIgnoringCase(hdl, "IF (nSRCLR = '0') THEN shiftReg <= (OTHERS => '0');"));
    assertTrue(containsIgnoringCase(hdl, "IF (nRCLR = '0') THEN storeReg <= (OTHERS => '0');"));
    assertFalse(containsIgnoringCase(hdl, "storeReg <= nextShift"));
    assertFalse(containsIgnoringCase(hdl, "'Z'"));
  }

  @Test
  void verilogShiftsStoresAndClearsEachRegister() {
    final var hdl = functionality(HdlGeneratorFactory.VERILOG);

    assertTrue(hdl.contains("assign QA = storeReg[0];"));
    assertTrue(hdl.contains("assign QH = storeReg[7];"));
    assertTrue(hdl.contains("assign QHp = shiftReg[7];"));
    assertTrue(hdl.contains("assign nextShift = (tick == 1) ? {shiftReg[6:0], SER} : shiftReg;"));
    assertTrue(
        hdl.contains(
            "assign nextStore = (tick2 == 1) ? ((nSRCLR == 0) ? 8'b0 : shiftReg) : storeReg;"));
    assertTrue(hdl.contains("if (nSRCLR == 0) shiftReg <= 8'b0;"));
    assertTrue(hdl.contains("if (nRCLR == 0) storeReg <= 8'b0;"));
    assertTrue(hdl.contains("negedge nSRCLR"));
    assertTrue(hdl.contains("negedge nRCLR"));
    assertFalse(hdl.contains("1'bZ"));
    assertFalse(hdl.contains("storeReg <= nextShift"));
  }

  @Test
  void exposedPowerPinsAreNotAnHdlTarget() {
    final var generator = new Ttl74594HdlGenerator();
    final var attrs = new Ttl74594().createAttributeSet();

    attrs.setValue(TtlLibrary.VCC_GND, false);
    assertTrue(generator.isHdlSupportedTarget(attrs));

    attrs.setValue(TtlLibrary.VCC_GND, true);
    assertFalse(generator.isHdlSupportedTarget(attrs));

    assertFalse(generator.isHdlSupportedTarget(null));
  }

  private static String functionality(String hdlType) {
    AppPreferences.HdlType.set(hdlType);
    final var attrs = new Ttl74594().createAttributeSet();
    return String.join(
        "\n", new Ttl74594HdlGenerator().getModuleFunctionality(null, attrs).get());
  }
}
