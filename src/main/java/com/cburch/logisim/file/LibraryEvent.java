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

package com.cburch.logisim.file;

import com.cburch.logisim.tools.Library;

public class LibraryEvent {
  public static final int ADD_TOOL = 0;
  public static final int REMOVE_TOOL = 1;
  public static final int MOVE_TOOL = 2;
  public static final int ADD_LIBRARY = 3;
  public static final int REMOVE_LIBRARY = 4;
  public static final int SET_MAIN = 5;
  public static final int SET_NAME = 6;
  public static final int DIRTY_STATE = 7;

  private Library source;
  private int action;
  private Object data;

  LibraryEvent(Library source, int action, Object data) {
    this.source = source;
    this.action = action;
    this.data = data;
  }

  public int getAction() {
    return action;
  }

  public Object getData() {
    return data;
  }

  public Library getSource() {
    return source;
  }
}
