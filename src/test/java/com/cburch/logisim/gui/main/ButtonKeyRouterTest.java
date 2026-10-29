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
import javax.swing.JPanel;
import javax.swing.JTextField;
import org.junit.jupiter.api.Test;

class ButtonKeyRouterTest {
  @Test
  void onlyThePokeToolAcceptsTriggerKeys() {
    assertTrue(ButtonKeyRouter.allowsTool(new PokeTool()));
    assertFalse(ButtonKeyRouter.allowsTool(new EditTool(new SelectTool(), new WiringTool())));
    assertFalse(ButtonKeyRouter.allowsTool(new TextTool()));
    assertFalse(ButtonKeyRouter.allowsTool(null));
  }

  @Test
  void onlyCanvasFocusAcceptsTriggerKeys() {
    final var canvas = new JPanel();
    final var child = new JPanel();
    canvas.add(child);
    final var properties = new JPanel();

    assertTrue(ButtonKeyRouter.allowsFocus(canvas, canvas));
    assertTrue(ButtonKeyRouter.allowsFocus(child, canvas));
    assertFalse(ButtonKeyRouter.allowsFocus(properties, canvas));
    assertFalse(ButtonKeyRouter.allowsFocus(null, canvas));
    assertFalse(ButtonKeyRouter.allowsFocus(new JTextField(), canvas));
  }
}
