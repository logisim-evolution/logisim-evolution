/*
 * Logisim-evolution - digital logic design tool and simulator
 * Copyright by the Logisim-evolution developers
 *
 * https://github.com/logisim-evolution/
 *
 * This is free software released under GNU GPLv3 license
 */

package com.cburch.logisim.proj;

import java.util.Arrays;
import java.util.List;

public class JoinedAction extends Action {
  private Action[] todo;

  public JoinedAction(Action... actions) {
    todo = actions;
  }

  @Override
  public Action append(Action other) {
    final var oldLen = todo.length;
    final var newToDo = new Action[oldLen + 1];
    System.arraycopy(todo, 0, newToDo, 0, oldLen);
    newToDo[oldLen] = other;
    todo = newToDo;
    return this;
  }

  @Override
  public void doIt(Project proj) {
    for (final var act : todo) {
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
    for (final var act : todo) {
      if (act.isModification()) return true;
    }
    return false;
  }

  @Override
  public void undo(Project proj) {
    for (var i = todo.length - 1; i >= 0; i--) {
      todo[i].undo(proj);
    }
  }
}
