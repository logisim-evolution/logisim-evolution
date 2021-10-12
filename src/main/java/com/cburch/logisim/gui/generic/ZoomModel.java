/*
 * Logisim-evolution - digital logic design tool and simulator
 * Copyright by the Logisim-evolution developers
 *
 * https://github.com/logisim-evolution/
 *
 * This is free software released under GNU GPLv3 license
 */

package com.cburch.logisim.gui.generic;

import java.awt.event.MouseEvent;
import java.beans.PropertyChangeListener;
import java.util.List;

public interface ZoomModel {
  String ZOOM = "zoom";
  String SHOW_GRID = "grid";
  String CENTER = "center";

  boolean getShowGrid();

  void setShowGrid(boolean value);

  double getZoomFactor();

  List<Double> getZoomOptions();

  void setZoomFactor(double value);

  void setZoomFactor(double value, MouseEvent e);

  void setZoomFactorCenter(double value);

  void addPropertyChangeListener(String prop, PropertyChangeListener l);

  void removePropertyChangeListener(String prop, PropertyChangeListener l);

}
