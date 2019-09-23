package com.cburch.logisim.soc.rv32im;

import org.fife.ui.rsyntaxtextarea.Token;
import org.fife.ui.rsyntaxtextarea.TokenMap;

import com.cburch.logisim.soc.data.AssemblerHighlighter;

public class RV32imSyntaxHighlighter extends AssemblerHighlighter {

  @Override
  public TokenMap getWordsToHighlight() {
    TokenMap map = super.getWordsToHighlight();
    for (int i = 0 ; i < RV32im_state.registerABINames.length ; i++)
      map.put(RV32im_state.registerABINames[i], Token.OPERATOR);
    map.put("pc", Token.OPERATOR);
    for (int i = 0 ; i < 32 ; i++) map.put("x"+i, Token.OPERATOR);
    for (String opcode : RV32im_state.ASSEMBLER.getOpcodes())
      map.put(opcode.toLowerCase(), Token.RESERVED_WORD);
    return map;
  }
}
