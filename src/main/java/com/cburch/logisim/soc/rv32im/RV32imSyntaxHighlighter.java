/*
 * Logisim-evolution - digital logic design tool and simulator
 * Copyright by the Logisim-evolution developers
 *
 * https://github.com/logisim-evolution/
 *
 * This is free software released under GNU GPLv3 license
 */

package com.cburch.logisim.soc.rv32im;

import com.cburch.logisim.soc.data.AssemblerHighlighter;
import org.fife.ui.rsyntaxtextarea.Token;
import org.fife.ui.rsyntaxtextarea.TokenMap;

public class RV32imSyntaxHighlighter extends AssemblerHighlighter {
  @Override
  public TokenMap getWordsToHighlight() {
    final var map = super.getWordsToHighlight();
    for (var i = 0; i < RV32imState.registerABINames.length; i++) {
      map.put(RV32imState.registerABINames[i], Token.OPERATOR);
    }
    for (var i = 0; i < RV32imState.implementedSprNames.length; i++) {
      map.put(RV32imState.implementedSprNames[i].toLowerCase(), Token.OPERATOR);
    }
    map.put("pc", Token.OPERATOR);
    for (var i = 0; i < 32; i++) {
      map.put("x" + i, Token.OPERATOR);
    }
    for (var opcode : RV32imState.ASSEMBLER.getOpcodes()) {
      map.put(opcode.toLowerCase(), Token.RESERVED_WORD);
    }
    return map;
  }
}
