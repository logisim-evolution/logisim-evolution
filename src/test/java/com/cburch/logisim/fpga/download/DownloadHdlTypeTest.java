/*
 * Logisim-evolution - digital logic design tool and simulator
 * Copyright by the Logisim-evolution developers
 *
 * https://github.com/logisim-evolution/
 *
 * This is free software released under GNU GPLv3 license
 */

package com.cburch.logisim.fpga.download;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.cburch.logisim.fpga.hdlgenerator.HdlGeneratorFactory;
import org.junit.jupiter.api.Test;

class DownloadHdlTypeTest {
  @Test
  void hdlGenerationRequiresConcreteHdlLanguage() {
    assertTrue(DownloadBase.isHdlGenerationEnabled(HdlGeneratorFactory.VHDL));
    assertTrue(DownloadBase.isHdlGenerationEnabled(HdlGeneratorFactory.VERILOG));
    assertFalse(DownloadBase.isHdlGenerationEnabled(HdlGeneratorFactory.NONE));
  }
}
