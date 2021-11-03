/*
 * Logisim-evolution - digital logic design tool and simulator
 * © 2001 Logisim-evolution contributors
 *
 * https://github.com/logisim-evolution/
 *
 * This is free software released under GNU GPLv3 license
 */

package com.cburch.logisim.file;

import java.util.Collections;
import java.util.List;

class XmlReaderException extends Exception {
  private static final long serialVersionUID = 1L;
  private final List<String> messages;

  public XmlReaderException(List<String> messages) {
    this.messages = messages;
  }

  public XmlReaderException(String message) {
    this(Collections.singletonList(message));
  }

  @Override
  public String getMessage() {
    return messages.get(0);
  }

  public List<String> getMessages() {
    return messages;
  }
}
