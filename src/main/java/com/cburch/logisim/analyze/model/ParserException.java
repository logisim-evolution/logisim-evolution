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

package com.cburch.logisim.analyze.model;

import com.cburch.logisim.util.StringGetter;

public class ParserException extends Exception {
  private static final long serialVersionUID = 1L;
  private StringGetter message;
  private int start;
  private int length;
  
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
