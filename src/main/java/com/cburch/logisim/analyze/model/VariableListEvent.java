/*
 * Logisim-evolution - digital logic design tool and simulator
 * Copyright by the Logisim-evolution developers
 *
 * https://github.com/logisim-evolution/
 *
 * This is free software released under GNU GPLv3 license
 */

package com.cburch.logisim.analyze.model;

// NOTE: silly members' names are mostly to avoid refactoring of the whole codebase due to record's
// getters not using Bean naming convention (so i.e. `foo()` instead of `getFoo()`. We may change
// that in future, but for now it looks stupid in this file only.
public record VariableListEvent(VariableList getSource, int getType, Var getVariable, Integer getIndex, Integer getBitIndex) {

  public static final int ALL_REPLACED = 0;
  public static final int ADD = 1;
  public static final int REMOVE = 2;
  public static final int MOVE = 3;
  public static final int REPLACE = 4;

}
