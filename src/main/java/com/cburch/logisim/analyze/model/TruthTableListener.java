/*
 * Logisim-evolution - digital logic design tool and simulator
 * © 2001 Logisim-evolution contributors
 *
 * https://github.com/logisim-evolution/
 *
 * This is free software released under GNU GPLv3 license
 */

package com.cburch.logisim.analyze.model;

public interface TruthTableListener {
  void rowsChanged(TruthTableEvent event);

  void cellsChanged(TruthTableEvent event);

  void structureChanged(TruthTableEvent event);
}
