/*
 * Logisim-evolution - digital logic design tool and simulator
 * Copyright by the Logisim-evolution developers
 *
 * https://github.com/logisim-evolution/
 *
 * This is free software released under GNU GPLv3 license
 */

package com.cburch.contracts;

import javax.swing.event.TreeModelEvent;
import javax.swing.event.TreeModelListener;

public interface BaseTreeModelListenerContract extends TreeModelListener {
  @Override
  default void treeNodesChanged(TreeModelEvent event) {
      // intentional no-op
  }

  @Override
  default void treeNodesInserted(TreeModelEvent event) {
      // intentional no-op
  }

  @Override
  default void treeNodesRemoved(TreeModelEvent event) {
      // intentional no-op
  }

  @Override
  default void treeStructureChanged(TreeModelEvent event) {
      // intentional no-op
  }
}
