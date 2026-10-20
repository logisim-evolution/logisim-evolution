/*
 * Logisim-evolution - digital logic design tool and simulator
 * Copyright by the Logisim-evolution developers
 *
 * https://github.com/logisim-evolution/
 *
 * This is free software released under GNU GPLv3 license
 */

package com.cburch.logisim.tools;

import com.cburch.logisim.data.Bounds;
import java.awt.Graphics;
import java.awt.event.KeyEvent;
import java.awt.event.MouseEvent;

public interface Caret {
  Bounds getBounds(Graphics g);

  // query/Graphics methods
  String getText();

  // listener methods
  default void addCaretListener(CaretListener e) {
    // no-op implementation
  }

  default void cancelEditing() {
    // no-op implementation
  }

  // finishing
  default void commitText(String text) {
    // no-op implementation
  }

  default void draw(Graphics g) {
    // no-op implementation
  }

  default void keyPressed(KeyEvent e) {
    // no-op implementation
  }

  default void keyReleased(KeyEvent e) {
    // no-op implementation
  }

  default void keyTyped(KeyEvent e) {
    // no-op implementation
  }

  default void mouseDragged(MouseEvent e) {
    // no-op implementation
  }

  // events to handle
  default void mousePressed(MouseEvent e) {
    // no-op implementation
  }

  default void mouseReleased(MouseEvent e) {
    // no-op implementation
  }

  default void removeCaretListener(CaretListener e) {
    // no-op implementation
  }

  default void stopEditing() {
    // no-op implementation
  }
}
