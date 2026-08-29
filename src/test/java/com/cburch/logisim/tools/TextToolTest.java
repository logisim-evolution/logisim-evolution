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
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;

import com.cburch.logisim.data.Bounds;
import com.cburch.logisim.prefs.AppPreferences;
import com.cburch.logisim.std.base.Text;
import java.awt.Color;
import java.awt.Graphics;
import java.lang.reflect.Field;
import java.util.Objects;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

class TextToolTest {
  private final int originalTextToolColor = AppPreferences.TEXT_TOOL_COLOR.get();

  @AfterEach
  void restoreTextToolColor() throws InterruptedException {
    AppPreferences.TEXT_TOOL_COLOR.set(originalTextToolColor);
    waitForTextToolPreference(originalTextToolColor);
  }

  @Test
  void usesConfiguredDefaultColor() throws InterruptedException {
    setTextToolPreference(Color.BLUE);

    final var tool = new TextTool();

    assertEquals(Color.BLUE, tool.getAttributeSet().getValue(Text.ATTR_COLOR));
  }

  @Test
  void colorFollowsPreferenceChanges() throws InterruptedException {
    final var tool = new TextTool();

    setTextToolPreference(Color.RED);

    waitForToolColor(tool, Color.RED);
  }

  @Test
  void returnsNoTextEditActionsWhenNoCaretIsActive() {
    final var tool = new TextTool();

    assertNull(tool.getTextEditActions());
  }

  @Test
  void returnsActiveCaretTextEditActions() throws Exception {
    final var tool = new TextTool();
    final var actions = new TestCaret();

    setCaret(tool, actions);

    assertSame(actions, tool.getTextEditActions());
  }

  private static void setCaret(TextTool tool, Caret caret) throws Exception {
    final Field field = TextTool.class.getDeclaredField("caret");
    field.setAccessible(true);
    field.set(tool, caret);
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

  private static void waitForToolColor(TextTool tool, Color color) throws InterruptedException {
    for (var i = 0; i < 100; i++) {
      if (color.equals(tool.getAttributeSet().getValue(Text.ATTR_COLOR))) {
        return;
      }
      Thread.sleep(10);
    }
    assertEquals(color, tool.getAttributeSet().getValue(Text.ATTR_COLOR));
  }

  private static class TestCaret implements Caret, TextEditActions {
    @Override
    public Bounds getBounds(Graphics g) {
      return Bounds.EMPTY_BOUNDS;
    }

    @Override
    public String getText() {
      return "";
    }

    @Override
    public boolean canCopy() {
      return false;
    }

    @Override
    public boolean canCut() {
      return false;
    }

    @Override
    public boolean canPaste() {
      return false;
    }

    @Override
    public boolean canRedoTextEdit() {
      return false;
    }

    @Override
    public boolean canSelectAll() {
      return false;
    }

    @Override
    public boolean canUndoTextEdit() {
      return false;
    }

    @Override
    public void copy() {}

    @Override
    public void cut() {}

    @Override
    public void paste() {}

    @Override
    public void redoTextEdit() {}

    @Override
    public void selectAll() {}

    @Override
    public void undoTextEdit() {}
  }
}
