/*
 * Logisim-evolution - digital logic design tool and simulator
 * Copyright by the Logisim-evolution developers
 *
 * https://github.com/logisim-evolution/
 *
 * This is free software released under GNU GPLv3 license
 */

package com.cburch.logisim.comp;

public class TextFieldEvent {
  private final TextField field;
  private final String oldval;
  private final String newval;

  public TextFieldEvent(TextField field, String old, String val) {
    this.field = field;
    this.oldval = old;
    this.newval = val;
  }

  public String getOldText() {
    return oldval;
  }

  public String getText() {
    return newval;
  }

  public TextField getTextField() {
    return field;
  }
}
