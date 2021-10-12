/*
 * Logisim-evolution - digital logic design tool and simulator
 * Copyright by the Logisim-evolution developers
 *
 * https://github.com/logisim-evolution/
 *
 * This is free software released under GNU GPLv3 license
 */

package com.cburch.logisim.soc.nios2;

import com.cburch.logisim.soc.data.AssemblerHighlighter;
import org.fife.ui.rsyntaxtextarea.Token;
import org.fife.ui.rsyntaxtextarea.TokenMap;

// FIXME: this class seems to be unused
public class Nios2SyntaxHighlighter extends AssemblerHighlighter {
  @Override
  public TokenMap getWordsToHighlight() {
    TokenMap map = super.getWordsToHighlight();
    for (int i = 0; i < Nios2State.registerABINames.length; i++)
      map.put(Nios2State.registerABINames[i], Token.OPERATOR);
    map.put("pc", Token.OPERATOR);
    for (int i = 0; i < 32; i++) {
      map.put("r" + i, Token.OPERATOR);
      map.put("c" + i, Token.OPERATOR);
      map.put("ctl" + i, Token.OPERATOR);
    }
    for (String opcode : Nios2State.ASSEMBLER.getOpcodes())
      map.put(opcode.toLowerCase(), Token.RESERVED_WORD);
    return map;
  }
}
