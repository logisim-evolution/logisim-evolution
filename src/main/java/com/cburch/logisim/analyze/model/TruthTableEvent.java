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
public record TruthTableEvent(TruthTable getSource, int getColumn, VariableListEvent getData) {

  public TruthTableEvent(TruthTable source, int column) {
    this(source, column, null);
  }

  public TruthTableEvent(TruthTable source, VariableListEvent data) {
    this(source, 0, data);
  }

}
