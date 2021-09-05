/*
 * Logisim-evolution - digital logic design tool and simulator
 * Copyright by the Logisim-evolution developers
 *
 * https://github.com/logisim-evolution/
 *
 * This is free software released under GNU GPLv3 license
 */

package com.cburch.logisim.analyze.model;

import lombok.Getter;

@Getter
public class TruthTableEvent {
  private final TruthTable source;
  private int column;
  private Object data;

  public TruthTableEvent(TruthTable source, int column) {
    this.source = source;
    this.column = column;
  }

  public TruthTableEvent(TruthTable source, VariableListEvent event) {
    this.source = source;
    this.data = event;
  }
}
