/*
 * Logisim-evolution - digital logic design tool and simulator
 * Copyright by the Logisim-evolution developers
 * 
 * https://github.com/logisim-evolution/
 * 
 * This is free software released under GNU GPLv3 license
 */

package com.cburch.logisim.tools;

public class CaretEvent {
  private final Caret caret;
  private final String oldtext;
  private final String newtext;

  public CaretEvent(Caret caret, String oldtext, String newtext) {
    this.caret = caret;
    this.oldtext = oldtext;
    this.newtext = newtext;
  }

  public Caret getCaret() {
    return caret;
  }

  public String getOldText() {
    return oldtext;
  }

  public String getText() {
    return newtext;
  }
}
