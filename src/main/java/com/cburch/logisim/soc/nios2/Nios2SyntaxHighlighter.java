/**
 * This file is part of logisim-evolution.
 *
 * Logisim-evolution is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by the
 * Free Software Foundation, either version 3 of the License, or (at your
 * option) any later version.
 *
 * Logisim-evolution is distributed in the hope that it will be useful, but
 * WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 * or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
 * for more details.
 *
 * You should have received a copy of the GNU General Public License along 
 * with logisim-evolution. If not, see <http://www.gnu.org/licenses/>.
 *
 * Original code by Carl Burch (http://www.cburch.com), 2011.
 * Subsequent modifications by:
 *   + College of the Holy Cross
 *     http://www.holycross.edu
 *   + Haute École Spécialisée Bernoise/Berner Fachhochschule
 *     http://www.bfh.ch
 *   + Haute École du paysage, d'ingénierie et d'architecture de Genève
 *     http://hepia.hesge.ch/
 *   + Haute École d'Ingénierie et de Gestion du Canton de Vaud
 *     http://www.heig-vd.ch/
 */

package com.cburch.logisim.soc.nios2;

import org.fife.ui.rsyntaxtextarea.Token;
import org.fife.ui.rsyntaxtextarea.TokenMap;

import com.cburch.logisim.soc.data.AssemblerHighlighter;

public class Nios2SyntaxHighlighter extends AssemblerHighlighter {
  @Override
  public TokenMap getWordsToHighlight() {
    TokenMap map = super.getWordsToHighlight();
    for (int i = 0 ; i < Nios2State.registerABINames.length ; i++)
      map.put(Nios2State.registerABINames[i], Token.OPERATOR);
    map.put("pc", Token.OPERATOR);
    for (int i = 0 ; i < 32 ; i++) {
      map.put("r"+i, Token.OPERATOR);
      map.put("c"+i, Token.OPERATOR);
      map.put("ctl"+i, Token.OPERATOR);
    }
    for (String opcode : Nios2State.ASSEMBLER.getOpcodes())
      map.put(opcode.toLowerCase(), Token.RESERVED_WORD);
    return map;
  }
}
