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

package com.cburch.logisim.circuit;

public class CircuitEvent {
  public static final int ACTION_SET_NAME = 0; // name changed
  public static final int ACTION_ADD = 1; // component added
  public static final int ACTION_REMOVE = 2; // component removed
//  public static final int ACTION_CHANGE = 3; // component changed
  public static final int ACTION_INVALIDATE = 4; // component invalidated (pin types changed)
  public static final int ACTION_CLEAR = 5; // entire circuit cleared
  public static final int TRANSACTION_DONE = 6;
  public static final int CHANGE_DEFAULT_BOX_APPEARANCE = 7;
  public static final int ACTION_CHECK_NAME = 8;
  public static final int ACTION_DISPLAY_CHANGE = 9; // viewed/haloed status change

  private final int action;
  private final Circuit circuit;
  private final Object data;

  CircuitEvent(int action, Circuit circuit, Object data) {
    this.action = action;
    this.circuit = circuit;
    this.data = data;
  }

  // access methods
  public int getAction() {
    return action;
  }

  public Circuit getCircuit() {
    return circuit;
  }

  public Object getData() {
    return data;
  }

  public CircuitTransactionResult getResult() {
    return (CircuitTransactionResult) data;
  }

  public String toString() {
    String s;
    switch (action) {
    case ACTION_SET_NAME : s = "ACTION_SET_NAME"; break;
    case ACTION_ADD : s = "ACTION_ADD"; break;
    case ACTION_REMOVE : s = "ACTION_REMOVE"; break;
    case ACTION_INVALIDATE : s = "ACTION_INVALIDATE"; break;
    case ACTION_CLEAR : s = "ACTION_CLEAR"; break;
    case TRANSACTION_DONE : s = "TRANSACTION_DONE"; break;
    case CHANGE_DEFAULT_BOX_APPEARANCE : s = "DEFAULT_BOX_APPEARANCE"; break;
    case ACTION_CHECK_NAME : s = "CHECK_NAME"; break;
    case ACTION_DISPLAY_CHANGE : s = "ACTION_DISPLAY_CHANGE"; break;
    default: s = "UNKNOWN_ACTION(" + action + ")"; break;
    }
    return s + "{\n  circuit=" +  circuit + "\n  data=" + data +"\n}";
  }
}
