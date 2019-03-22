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

package com.cburch.logisim.data;

import java.util.ArrayList;

/**
 * Code taken from Cornell's version of Logisim: http://www.cs.cornell.edu/courses/cs3410/2015sp/
 */
public class FailException extends TestException {

  private static final long serialVersionUID = 1L;
  private int column;
  private Value expected, computed;
  private ArrayList<FailException> more = new ArrayList<FailException>();

  public FailException(int column, String columnName, Value expected, Value computed) {
    super(
        columnName
            + " = "
            + computed.toDisplayString(2)
            + " (expected "
            + expected.toDisplayString(2)
            + ")");
    this.column = column;
    this.expected = expected;
    this.computed = computed;
  }

  public void add(FailException another) {
    more.add(another);
    more.addAll(another.getMore());
    another.clearMore();
  }

  public int getColumn() {
    return column;
  }

  public Value getComputed() {
    return computed;
  }

  public Value getExpected() {
    return expected;
  }

  public ArrayList<FailException> getMore() {
    return more;
  }

  public void clearMore() {
    more.clear();
  }

  public ArrayList<FailException> getAll() {
    ArrayList<FailException> ret = new ArrayList<FailException>();
    ret.add(this);
    ret.addAll(more);
    return ret;
  }
}
