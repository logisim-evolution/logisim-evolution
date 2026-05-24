/*
 * Logisim-evolution - digital logic design tool and simulator
 * Copyright by the Logisim-evolution developers
 *
 * https://github.com/logisim-evolution/
 *
 * This is free software released under GNU GPLv3 license
 */

package com.cburch.logisim.std.memory;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.cburch.logisim.data.BitWidth;
import com.cburch.logisim.fpga.hdlgenerator.HdlGeneratorFactory;
import com.cburch.logisim.instance.StdAttr;
import com.cburch.logisim.prefs.AppPreferences;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

class RandomHdlGeneratorFactoryTest {

  private final String originalHdlType = AppPreferences.HdlType.get();

  @AfterEach
  void restoreHdlType() {
    AppPreferences.HdlType.set(originalHdlType);
  }

  @Test
  void vhdlOutputAssignmentDoesNotSliceBeyondRandomState() {
    final var hdl = functionality(HdlGeneratorFactory.VHDL);

    assertTrue(hdl.contains("resize(unsigned(s_initSeed), nrOfBits)"));
    assertTrue(hdl.contains("resize(unsigned(s_currentSeed(47"));
    assertTrue(hdl.contains("12)), nrOfBits)"));
    assertFalse(hdl.contains("nrOfBits+11"));
    assertFalse(hdl.contains("s_initSeed( (nrOfBits-1)"));
  }

  @Test
  void verilogOutputAssignmentDoesNotSliceBeyondRandomState() {
    final var hdl = functionality(HdlGeneratorFactory.VERILOG);

    assertTrue(hdl.contains("s_outputReg <= s_initSeed;"));
    assertTrue(hdl.contains("s_outputReg <= s_currentSeed[47:12];"));
    assertFalse(hdl.contains("nrOfBits+11"));
    assertFalse(hdl.contains("s_initSeed[("));
  }

  private static String functionality(String hdlType) {
    AppPreferences.HdlType.set(hdlType);
    final var attrs = new Random().createAttributeSet();
    attrs.setValue(StdAttr.WIDTH, BitWidth.create(64));
    return String.join(
        "\n", new RandomHdlGeneratorFactory().getModuleFunctionality(null, attrs).get());
  }
}
