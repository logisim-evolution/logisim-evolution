/*
 * Logisim-evolution - digital logic design tool and simulator
 * Copyright by the Logisim-evolution developers
 *
 * https://github.com/logisim-evolution/
 *
 * This is free software released under GNU GPLv3 license
 */

package com.cburch.logisim.comp;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.awt.Canvas;
import java.awt.Component;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.Test;

class TextFieldCaretTest {
  private static final Component EVENT_SOURCE = new Canvas();

  @Test
  void metaMenuShortcutSelectsAllText() {
    final var caret = caret("abc", 0, true);

    caret.keyPressed(keyPressed(KeyEvent.VK_A, InputEvent.META_DOWN_MASK));
    caret.keyTyped(keyTyped('x'));

    assertEquals("x", caret.getText());
  }

  @Test
  void metaMenuShortcutIsIgnoredWhenDisabled() {
    final var caret = caret("abc", 0, false);

    caret.keyPressed(keyPressed(KeyEvent.VK_A, InputEvent.META_DOWN_MASK));
    caret.keyTyped(keyTyped('x'));

    assertEquals("xabc", caret.getText());
  }

  @Test
  void menuShortcutZUndoesInProgressTextEdit() {
    final var caret = caret("abc", 3);

    caret.keyTyped(keyTyped('d'));
    caret.keyPressed(keyPressed(KeyEvent.VK_Z, InputEvent.CTRL_DOWN_MASK));

    assertEquals("abc", caret.getText());
  }

  @Test
  void menuShortcutYRedoesInProgressTextEdit() {
    final var caret = caret("abc", 3);

    caret.keyTyped(keyTyped('d'));
    caret.keyPressed(keyPressed(KeyEvent.VK_Z, InputEvent.CTRL_DOWN_MASK));
    assertEquals("abc", caret.getText());
    caret.keyPressed(keyPressed(KeyEvent.VK_Y, InputEvent.CTRL_DOWN_MASK));

    assertEquals("abcd", caret.getText());
  }

  @Test
  void menuShortcutShiftZRedoesInProgressTextEdit() {
    final var caret = caret("abc", 3);

    caret.keyTyped(keyTyped('d'));
    caret.keyPressed(keyPressed(KeyEvent.VK_Z, InputEvent.CTRL_DOWN_MASK));
    assertEquals("abc", caret.getText());
    caret.keyPressed(
        keyPressed(KeyEvent.VK_Z, InputEvent.CTRL_DOWN_MASK | InputEvent.SHIFT_DOWN_MASK));

    assertEquals("abcd", caret.getText());
  }

  @Test
  void stopEditingNotifiesCaretListenersBeforeUpdatingTextField() {
    final var field = new TextField(0, 0, TextField.H_LEFT, TextField.V_BASELINE);
    field.setText("abc");
    final var mirroredText = new AtomicReference<>(field.getText());
    field.addTextFieldListener(e -> mirroredText.set(e.getText()));
    final var caret = new TextFieldCaret(field, null, 3, false);
    final var mirroredTextWhenCommitted = new AtomicReference<String>();

    caret.addCaretListener(
        new com.cburch.logisim.tools.CaretListener() {
          @Override
          public void editingCanceled(com.cburch.logisim.tools.CaretEvent e) {}

          @Override
          public void editingStopped(com.cburch.logisim.tools.CaretEvent e) {
            mirroredTextWhenCommitted.set(mirroredText.get());
          }
        });

    caret.keyTyped(keyTyped('d'));
    caret.stopEditing();

    assertEquals("abc", mirroredTextWhenCommitted.get());
  }

  private static TextFieldCaret caret(String text, int pos) {
    return caret(text, pos, false);
  }

  private static TextFieldCaret caret(String text, int pos, boolean metaMenuShortcutEnabled) {
    final var field = new TextField(0, 0, TextField.H_LEFT, TextField.V_BASELINE);
    field.setText(text);
    return new TextFieldCaret(field, null, pos, metaMenuShortcutEnabled);
  }

  private static KeyEvent keyPressed(int keyCode, int modifiers) {
    return new KeyEvent(
        EVENT_SOURCE, KeyEvent.KEY_PRESSED, 0, modifiers, keyCode, KeyEvent.CHAR_UNDEFINED);
  }

  private static KeyEvent keyTyped(char keyChar) {
    return new KeyEvent(
        EVENT_SOURCE, KeyEvent.KEY_TYPED, 0, 0, KeyEvent.VK_UNDEFINED, keyChar);
  }
}
