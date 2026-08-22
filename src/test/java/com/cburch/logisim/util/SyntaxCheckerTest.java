/*
 * Logisim-evolution - digital logic design tool and simulator
 * Copyright by the Logisim-evolution developers
 *
 * https://github.com/logisim-evolution/
 *
 * This is free software released under GNU GPLv3 license
 */

package com.cburch.logisim.util;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.cburch.logisim.fpga.hdlgenerator.HdlGeneratorFactory;
import org.junit.jupiter.api.Test;

class SyntaxCheckerTest {
  @Test
  void noHdlAllowsNonAsciiNames() {
    assertNull(SyntaxChecker.getErrorMessage("ö", HdlGeneratorFactory.NONE));
    assertTrue(SyntaxChecker.isVariableNameAcceptable("ö", HdlGeneratorFactory.NONE, false));
  }

  @Test
  void hdlModesKeepRejectingNonAsciiNames() {
    assertFalse(SyntaxChecker.isVariableNameAcceptable("ö", HdlGeneratorFactory.VHDL, false));
    assertFalse(SyntaxChecker.isVariableNameAcceptable("ö", HdlGeneratorFactory.VERILOG, false));
  }

  @Test
  void hdlKeywordChecksFollowSelectedHdl() {
    assertFalse(SyntaxChecker.isVariableNameAcceptable("entity", HdlGeneratorFactory.VHDL, false));
    assertTrue(SyntaxChecker.isVariableNameAcceptable("entity", HdlGeneratorFactory.VERILOG, false));
    assertFalse(SyntaxChecker.isVariableNameAcceptable("module", HdlGeneratorFactory.VERILOG, false));
    assertTrue(SyntaxChecker.isVariableNameAcceptable("module", HdlGeneratorFactory.VHDL, false));
  }
}
