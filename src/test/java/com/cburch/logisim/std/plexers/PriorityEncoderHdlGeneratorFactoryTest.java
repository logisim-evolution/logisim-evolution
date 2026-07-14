/*
 * Logisim-evolution - digital logic design tool and simulator
 * Copyright by the Logisim-evolution developers
 *
 * https://github.com/logisim-evolution/
 *
 * This is free software released under GNU GPLv3 license
 */

package com.cburch.logisim.std.plexers;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.cburch.logisim.data.BitWidth;
import com.cburch.logisim.fpga.hdlgenerator.HdlGeneratorFactory;
import com.cburch.logisim.prefs.AppPreferences;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

class PriorityEncoderHdlGeneratorFactoryTest {

  private final String originalHdlType = AppPreferences.HdlType.get();

  @AfterEach
  void restoreHdlType() {
    AppPreferences.HdlType.set(originalHdlType);
  }

  @Test
  void verilogCopiesFullInputVectorIntoSelectionTree() {
    final var hdl = functionality(HdlGeneratorFactory.VERILOG, 3);

    assertTrue(hdl.contains("assign s_selectVector0[63:nrOfInputBits] = 0;"));
    assertTrue(hdl.contains("assign s_selectVector0[nrOfInputBits-1:0] = inputVector;"));
    assertTrue(
        hdl.contains(
            "assign s_selectVector4 = (s_selectVector3[7:4] == 0) ? "
                + "s_selectVector3[3:0] : s_selectVector3[7:4];"));
    assertFalse(hdl.contains("assign s_selectVector0[63:nrOfSelectBits] = 0;"));
    assertFalse(hdl.contains("assign s_selectVector0[nrOfSelectBits-1:0] = inputVector;"));
    assertFalse(hdl.contains("s_selectVector2[7:4]"));
  }

  @Test
  void vhdlCopiesFullInputVectorIntoSelectionTree() {
    final var hdl = functionality(HdlGeneratorFactory.VHDL, 3);

    assertTrue(hdl.contains("s_selectVector0(63 DOWNTO nrOfInputBits)  <= (OTHERS => '0');"));
    assertTrue(hdl.contains("s_selectVector0(nrOfInputBits-1 DOWNTO 0) <= inputVector;"));
  }

  private static String functionality(String hdlType, int selectBits) {
    AppPreferences.HdlType.set(hdlType);
    final var attrs = new PriorityEncoder().createAttributeSet();
    attrs.setValue(PlexersLibrary.ATTR_SELECT, BitWidth.create(selectBits));
    return String.join(
        "\n", new PriorityEncoderHdlGeneratorFactory().getModuleFunctionality(null, attrs).get());
  }
}
