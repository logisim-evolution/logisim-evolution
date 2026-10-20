/*
 * Logisim-evolution - digital logic design tool and simulator
 * Copyright by the Logisim-evolution developers
 *
 * https://github.com/logisim-evolution/
 *
 * This is free software released under GNU GPLv3 license
 */

package com.cburch.logisim.util;

// NOTE: silly members' names are mostly to avoid refactoring of the whole codebase due to record's
// getters not using Bean naming convention (so i.e. `foo()` instead of `getFoo()`). We may change
// that in future, but for now it looks stupid in this file only.
public record TableConstraints(int getRow, int getCol) {
  public static TableConstraints at(int row, int col) {
    return new TableConstraints(row, col);
  }
}
