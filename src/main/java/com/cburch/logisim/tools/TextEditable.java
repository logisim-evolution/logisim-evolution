/*
 * Logisim-evolution - digital logic design tool and simulator
 * Copyright by the Logisim-evolution developers
 *
 * https://github.com/logisim-evolution/
 *
 * This is free software released under GNU GPLv3 license
 */

package com.cburch.logisim.tools;

import com.cburch.logisim.circuit.Circuit;
import com.cburch.logisim.comp.ComponentUserEvent;
import com.cburch.logisim.proj.Action;

public interface TextEditable {
  Action getCommitAction(Circuit circuit, String oldText, String newText);

  Caret getTextCaret(ComponentUserEvent event);
}
