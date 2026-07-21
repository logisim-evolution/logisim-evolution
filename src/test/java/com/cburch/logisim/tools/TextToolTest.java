/*
 * Logisim-evolution - digital logic design tool and simulator
 * Copyright by the Logisim-evolution developers
 *
 * https://github.com/logisim-evolution/
 *
 * This is free software released under GNU GPLv3 license
 */

package com.cburch.logisim.tools;

import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;

import com.cburch.logisim.data.Bounds;
import java.awt.Graphics;
import java.lang.reflect.Field;
import org.junit.jupiter.api.Test;

class TextToolTest {
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
