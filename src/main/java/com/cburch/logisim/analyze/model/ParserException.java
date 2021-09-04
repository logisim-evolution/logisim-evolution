/*
 * Logisim-evolution - digital logic design tool and simulator
 * Copyright by the Logisim-evolution developers
 *
 * https://github.com/logisim-evolution/
 *
 * This is free software released under GNU GPLv3 license
 */

package com.cburch.logisim.analyze.model;

import com.cburch.logisim.util.StringGetter;

public class ParserException extends Exception {
  private static final long serialVersionUID = 1L;
  private final StringGetter message;
  private final int start;
  private final int length;
  
  public ParserException(StringGetter message, int start) {
    this(message, start, 1);
  }

  public ParserException(StringGetter message, int start, int length) {
    super(message.toString());
    this.message = message;
    this.start = start;
    this.length = length;
  }

  public int getEndOffset() {
    return start + length;
  }

  @Override
  public String getMessage() {
    return message.toString();
  }

  public StringGetter getMessageGetter() {
    return message;
  }

  public int getOffset() {
    return start;
  }
}
