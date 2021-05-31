/*
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

public class VariableListEvent {
  public static final int ALL_REPLACED = 0;
  public static final int ADD = 1;
  public static final int REMOVE = 2;
  public static final int MOVE = 3;
  public static final int REPLACE = 4;

  private final VariableList source;
  private final int type;
  private final Var variable;
  private final Integer index;
  private final Integer bitIndex;

  public VariableListEvent(
      VariableList source, int type, Var variable, Integer index, Integer bitIndex) {
    this.source = source;
    this.type = type;
    this.variable = variable;
    this.index = index;
    this.bitIndex = bitIndex;
  }

  public VariableList getSource() {
    return source;
  }

  public Integer getIndex() {
    return index;
  }

  public Integer getBitIndex() {
    return bitIndex;
  }

  public int getType() {
    return type;
  }

  public Var getVariable() {
    return variable;
  }
}
