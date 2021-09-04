/*
 * Logisim-evolution - digital logic design tool and simulator
 * Copyright by the Logisim-evolution developers
 *
 * https://github.com/logisim-evolution/
 *
 * This is free software released under GNU GPLv3 license
 */

package com.cburch.logisim.util;

public class TableConstraints {
  public static TableConstraints at(int row, int col) {
    return new TableConstraints(row, col);
  }

  private final int col;
  private final int row;

  private TableConstraints(int row, int col) {
    this.col = col;
    this.row = row;
  }

  int getCol() {
    return col;
  }

  int getRow() {
    return row;
  }
}
