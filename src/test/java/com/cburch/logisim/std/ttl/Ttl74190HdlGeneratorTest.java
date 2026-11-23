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

/** Generated HDL for the 74190 synchronous BCD up/down counter. */
class Ttl74190HdlGeneratorTest {

  private final String originalHdlType = AppPreferences.HdlType.get();

  @AfterEach
  void restoreHdlType() {
    AppPreferences.HdlType.set(originalHdlType);
  }

  @Test
  void vhdlLoadsWhileParallelLoadIsLowAndCountsOnTheRisingEdge() {
    final var hdl = functionality(HdlGeneratorFactory.VHDL);

    assertTrue(containsIgnoringCase(hdl, "s_up <= \"0001\" when s_count = \"0000\" else"));
    assertTrue(containsIgnoringCase(hdl, "\"0000\" when s_count = \"1001\" else"));
    assertTrue(containsIgnoringCase(hdl, "\"0100\" when s_count = \"1011\" else"));
    assertTrue(containsIgnoringCase(hdl, "s_down <= \"1001\" when s_count = \"0000\" else"));
    assertTrue(containsIgnoringCase(hdl, "\"1010\" when s_count = \"1011\" else"));
    assertTrue(containsIgnoringCase(hdl, "\"0110\";"));
    assertTrue(containsIgnoringCase(hdl, "if (nPL = '0') then"));
    assertTrue(containsIgnoringCase(hdl, "s_count <= s_data;"));
    assertTrue(containsIgnoringCase(hdl, "elsif (rising_edge(clock)) then"));
    assertTrue(containsIgnoringCase(hdl, "s_visible(0) = '1' and"));
    assertTrue(containsIgnoringCase(hdl, "s_visible(3) = '1' else '0';"));
    assertTrue(containsIgnoringCase(hdl, "s_terminal <= '1' when s_at_max = '1' or s_at_min = '1'"));
    assertTrue(
        containsIgnoringCase(
            hdl, "nRC <= '0' when s_terminal = '1' and nCE = '0' and clock = '0' else '1';"));
  }

  @Test
  void verilogLoadsWhileParallelLoadIsLowAndCountsOnTheRisingEdge() {
    final var hdl = functionality(HdlGeneratorFactory.VERILOG);

    assertTrue(hdl.contains("(s_count == 4'b0000) ? 4'b0001"));
    assertTrue(hdl.contains("(s_count == 4'b1001) ? 4'b0000"));
    assertTrue(hdl.contains("(s_count == 4'b1011) ? 4'b0100"));
    assertTrue(hdl.contains("(s_count == 4'b1011) ? 4'b1010"));
    assertTrue(hdl.contains("(s_count == 4'b1111) ? 4'b0110"));
    assertTrue(hdl.contains("assign s_visible = (nPL == 0 || s_loading == 1) ? s_data : s_count;"));
    assertTrue(hdl.contains("if (nPL == 0) begin"));
    assertTrue(hdl.contains("always @(posedge clock or nPL)"));
    assertTrue(hdl.contains("assign s_at_max = (DU == 0) && s_visible[0] && s_visible[3];"));
    assertTrue(
        hdl.contains(
            "assign nRC = ((s_terminal == 1) && (nCE == 0) && (clock == 0)) ? 1'b0 : 1'b1;"));
  }

  @Test
  void exposedPowerPinsAreNotAnHdlTarget() {
    final var generator = new Ttl74190HdlGenerator();
    final var attrs = new Ttl74190().createAttributeSet();

    attrs.setValue(TtlLibrary.VCC_GND, false);
    assertTrue(generator.isHdlSupportedTarget(attrs));

    attrs.setValue(TtlLibrary.VCC_GND, true);
    assertFalse(generator.isHdlSupportedTarget(attrs));

    assertFalse(generator.isHdlSupportedTarget(null));
  }

  private static String functionality(String hdlType) {
    AppPreferences.HdlType.set(hdlType);
    final var attrs = new Ttl74190().createAttributeSet();
    return String.join("\n", new Ttl74190HdlGenerator().getModuleFunctionality(null, attrs).get());
  }
}
