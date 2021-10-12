/*
 * Logisim-evolution - digital logic design tool and simulator
 * Copyright by the Logisim-evolution developers
 *
 * https://github.com/logisim-evolution/
 *
 * This is free software released under GNU GPLv3 license
 */

package com.cburch.contracts;

import java.awt.Component;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.LayoutManager;

public interface BaseLayoutManagerContract extends LayoutManager {
  @Override
  default void addLayoutComponent(String var1, Component var2) {
    // no-op implementation
  }

  @Override
  default void removeLayoutComponent(Component var1) {
    // no-op implementation
  }

  @Override
  Dimension preferredLayoutSize(Container var1);

  @Override
  Dimension minimumLayoutSize(Container var1);

  @Override
  default void layoutContainer(Container var1) {
    // no-op implementation
  }
}
