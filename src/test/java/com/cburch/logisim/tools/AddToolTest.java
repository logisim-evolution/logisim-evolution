/*
 * Logisim-evolution - digital logic design tool and simulator
 * Copyright by the Logisim-evolution developers
 *
 * https://github.com/logisim-evolution/
 *
 * This is free software released under GNU GPLv3 license
 */

package com.cburch.logisim.tools;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.cburch.logisim.prefs.AppPreferences;
import com.cburch.logisim.std.base.Text;
import java.awt.Color;
import java.util.Objects;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

class AddToolTest {
  private final int originalTextToolColor = AppPreferences.TEXT_TOOL_COLOR.get();

  @AfterEach
  void restoreTextToolColor() throws InterruptedException {
    AppPreferences.TEXT_TOOL_COLOR.set(originalTextToolColor);
    waitForTextToolPreference(originalTextToolColor);
  }

  @Test
  void textToolUsesConfiguredDefaultColor() throws InterruptedException {
    setTextToolPreference(Color.BLUE);

    final var tool = new AddTool(Text.FACTORY);

    assertEquals(Color.BLUE, tool.getAttributeSet().getValue(Text.ATTR_COLOR));
  }

  @Test
  void textToolColorFollowsPreferenceChanges() throws InterruptedException {
    final var tool = new AddTool(Text.FACTORY);

    setTextToolPreference(Color.RED);

    waitForToolColor(tool, Color.RED);
  }

  @Test
  void textFactoryDefaultColorStaysBlack() throws InterruptedException {
    setTextToolPreference(Color.RED);

    assertEquals(Color.BLACK, Text.FACTORY.createAttributeSet().getValue(Text.ATTR_COLOR));
  }

  private static void setTextToolPreference(Color color) throws InterruptedException {
    final var rgb = color.getRGB();
    AppPreferences.TEXT_TOOL_COLOR.set(rgb);
    waitForTextToolPreference(rgb);
  }

  private static void waitForTextToolPreference(int rgb) throws InterruptedException {
    for (var i = 0; i < 100; i++) {
      if (Objects.equals(AppPreferences.TEXT_TOOL_COLOR.get(), rgb)) {
        return;
      }
      Thread.sleep(10);
    }
    assertEquals(rgb, AppPreferences.TEXT_TOOL_COLOR.get());
  }

  private static void waitForToolColor(AddTool tool, Color color) throws InterruptedException {
    for (var i = 0; i < 100; i++) {
      if (color.equals(tool.getAttributeSet().getValue(Text.ATTR_COLOR))) {
        return;
      }
      Thread.sleep(10);
    }
    assertEquals(color, tool.getAttributeSet().getValue(Text.ATTR_COLOR));
  }
}
