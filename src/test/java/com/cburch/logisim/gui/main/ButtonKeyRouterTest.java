/*
 * Logisim-evolution - digital logic design tool and simulator
 * Copyright by the Logisim-evolution developers
 *
 * https://github.com/logisim-evolution/
 *
 * This is free software released under GNU GPLv3 license
 */

package com.cburch.logisim.gui.main;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.cburch.logisim.tools.EditTool;
import com.cburch.logisim.tools.PokeTool;
import com.cburch.logisim.tools.SelectTool;
import com.cburch.logisim.tools.TextTool;
import com.cburch.logisim.tools.WiringTool;
import javax.swing.JComboBox;
import javax.swing.JPanel;
import javax.swing.JTextField;
import org.junit.jupiter.api.Test;

class ButtonKeyRouterTest {
  @Test
  void pointerAndPokeAcceptTriggerKeys() {
    assertTrue(ButtonKeyRouter.allowsTool(new EditTool(new SelectTool(), new WiringTool())));
    assertTrue(ButtonKeyRouter.allowsTool(new PokeTool()));
  }

  @Test
  void textToolDoesNotAcceptTriggerKeys() {
    assertFalse(ButtonKeyRouter.allowsTool(new TextTool()));
  }

  @Test
  void propertiesTableAcceptsTriggerKeysButTextFieldsDoNot() {
    assertTrue(ButtonKeyRouter.allowsFocus(new JPanel()));
    assertTrue(ButtonKeyRouter.allowsFocus(null));
    assertFalse(ButtonKeyRouter.allowsFocus(new JTextField()));
  }

  @Test
  void closedComboAcceptsTriggerKeys() {
    assertTrue(ButtonKeyRouter.allowsFocus(new JComboBox<>(new String[] {"1", "2"})));
  }
}
