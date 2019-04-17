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

package com.cburch.logisim.proj;

import java.util.Arrays;
import java.util.List;

public class JoinedAction extends Action {
  Action[] todo;

  JoinedAction(Action... actions) {
    todo = actions;
  }

  @Override
  public Action append(Action other) {
    int oldLen = todo.length;
    Action[] newToDo = new Action[oldLen + 1];
    System.arraycopy(todo, 0, newToDo, 0, oldLen);
    newToDo[oldLen] = other;
    todo = newToDo;
    return this;
  }

  @Override
  public void doIt(Project proj) {
    for (Action act : todo) {
      act.doIt(proj);
    }
  }

  public List<Action> getActions() {
    return Arrays.asList(todo);
  }

  public Action getFirstAction() {
    return todo[0];
  }

  public Action getLastAction() {
    return todo[todo.length - 1];
  }

  @Override
  public String getName() {
    return todo[0].getName();
  }

  @Override
  public boolean isModification() {
    for (Action act : todo) {
      if (act.isModification()) return true;
    }
    return false;
  }

  @Override
  public void undo(Project proj) {
    for (int i = todo.length - 1; i >= 0; i--) {
      todo[i].undo(proj);
    }
  }
}
