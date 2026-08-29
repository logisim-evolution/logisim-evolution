/*
 * Logisim-evolution - digital logic design tool and simulator
 * Copyright by the Logisim-evolution developers
 *
 * https://github.com/logisim-evolution/
 *
 * This is free software released under GNU GPLv3 license
 */

package com.cburch.logisim.tools;

public interface TextEditActions {
  boolean canCopy();

  boolean canCut();

  boolean canPaste();

  boolean canRedoTextEdit();

  boolean canSelectAll();

  boolean canUndoTextEdit();

  void copy();

  void cut();

  void paste();

  void redoTextEdit();

  void selectAll();

  void undoTextEdit();
}
