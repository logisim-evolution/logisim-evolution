/*
 * Logisim-evolution - digital logic design tool and simulator
 * Copyright by the Logisim-evolution developers
 *
 * https://github.com/logisim-evolution/
 *
 * This is free software released under GNU GPLv3 license
 */

package com.cburch.logisim.fpga.gui;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.cburch.logisim.fpga.hdlgenerator.HdlGeneratorFactory;
import javax.swing.JButton;
import javax.swing.JComboBox;
import org.junit.jupiter.api.Test;

class FpgaCommanderTest {
  @Test
  void hdlControlsCanBeReenabledAfterSelectingConcreteHdl() {
    final var actionCommands = new JComboBox<>();
    final var executeButton = new JButton();

    FpgaCommander.setHdlControlsEnabled(
        actionCommands, executeButton, true, HdlGeneratorFactory.NONE);
    assertFalse(actionCommands.isEnabled());
    assertFalse(executeButton.isEnabled());

    FpgaCommander.setHdlControlsEnabled(
        actionCommands, executeButton, true, HdlGeneratorFactory.VERILOG);
    assertTrue(actionCommands.isEnabled());
    assertTrue(executeButton.isEnabled());

    FpgaCommander.setHdlControlsEnabled(
        actionCommands, executeButton, true, HdlGeneratorFactory.VHDL);
    assertTrue(actionCommands.isEnabled());
    assertTrue(executeButton.isEnabled());

    FpgaCommander.setHdlControlsEnabled(
        actionCommands, executeButton, false, HdlGeneratorFactory.VERILOG);
    assertFalse(actionCommands.isEnabled());
    assertFalse(executeButton.isEnabled());
  }
}
