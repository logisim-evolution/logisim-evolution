/*
 * Logisim-evolution - digital logic design tool and simulator
 * Copyright by the Logisim-evolution developers
 *
 * https://github.com/logisim-evolution/
 *
 * This is free software released under GNU GPLv3 license
 */

package com.cburch.logisim.gui.generic;

import java.awt.Dimension;
import java.awt.Rectangle;
import java.awt.event.MouseWheelListener;
import javax.swing.Scrollable;

public interface CanvasPaneContents extends Scrollable {
  Dimension getPreferredScrollableViewportSize();

  int getScrollableBlockIncrement(Rectangle visibleRect, int orientation, int direction);

  boolean getScrollableTracksViewportHeight();

  boolean getScrollableTracksViewportWidth();

  @Override
  int getScrollableUnitIncrement(Rectangle visibleRect, int orientation, int direction);

  void recomputeSize();

  void setCanvasPane(CanvasPane pane);

  void addMouseWheelListener(MouseWheelListener listener);

  void center();
}
