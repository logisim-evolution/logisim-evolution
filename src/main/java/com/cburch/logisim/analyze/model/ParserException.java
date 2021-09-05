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
import lombok.Getter;

public class ParserException extends Exception {
  private static final long serialVersionUID = 1L;
  @Getter private final StringGetter messageGetter;
  @Getter private final int startOffset;
  @Getter private final int endOffset;
  private final int length;

  public ParserException(StringGetter msgGetter, int startOffset) {
    this(msgGetter, startOffset, 1);
  }

  public ParserException(StringGetter msgGetter, int startOffset, int length) {
    super(msgGetter.toString());
    this.messageGetter = msgGetter;
    this.startOffset = startOffset; // start of the parser exception
    this.endOffset = startOffset + length;
    this.length = length;
  }

  @Override
  public String getMessage() {
    return messageGetter.toString();
  }
}
