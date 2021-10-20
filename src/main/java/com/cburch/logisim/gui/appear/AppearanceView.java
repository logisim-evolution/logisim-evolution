/*
 * Logisim-evolution - digital logic design tool and simulator
 * Copyright by the Logisim-evolution developers
 *
 * https://github.com/logisim-evolution/
 *
 * This is free software released under GNU GPLv3 license
 */

package com.cburch.logisim.gui.appear;

import com.cburch.draw.gui.AttrTableDrawManager;
import com.cburch.draw.tools.DrawingAttributeSet;
import com.cburch.draw.tools.SelectTool;
import com.cburch.logisim.circuit.CircuitState;
import com.cburch.logisim.gui.generic.AttrTable;
import com.cburch.logisim.gui.generic.BasicZoomModel;
import com.cburch.logisim.gui.generic.CanvasPane;
import com.cburch.logisim.gui.generic.ZoomModel;
import com.cburch.logisim.prefs.AppPreferences;
import com.cburch.logisim.proj.Project;
import java.util.ArrayList;
import javax.swing.JFrame;
import javax.swing.SwingUtilities;
import lombok.Getter;

public class AppearanceView {
  private static final ArrayList<Double> ZOOM_OPTIONS =
      new ArrayList<>() {
        {
          add(100.0);
          add(150.0);
          add(200.0);
          add(300.0);
          add(400.0);
          add(600.0);
          add(800.0);
        }
      };

  @Getter private final DrawingAttributeSet attributeSet;
  @Getter private final AppearanceCanvas canvas;
  @Getter private final CanvasPane canvasPane;
  @Getter private final AppearanceToolbarModel toolbarModel;
  @Getter private final ZoomModel zoomModel;
  @Getter private final AppearanceEditHandler editHandler;
  private AttrTableDrawManager attrTableManager;

  public AppearanceView() {
    attributeSet = new DrawingAttributeSet();
    final var selectTool = new SelectTool();
    canvas = new AppearanceCanvas(selectTool);
    canvasPane = new CanvasPane(canvas);
    final var ssTool = new ShowStateTool(this, canvas, attributeSet);
    toolbarModel = new AppearanceToolbarModel(selectTool, ssTool, canvas, attributeSet);
    zoomModel =
        new BasicZoomModel(
            AppPreferences.APPEARANCE_SHOW_GRID,
            AppPreferences.APPEARANCE_ZOOM,
            ZOOM_OPTIONS,
            canvasPane);
    canvas.getGridPainter().setZoomModel(zoomModel);
    attrTableManager = null;
    canvasPane.setZoomModel(zoomModel);
    editHandler = new AppearanceEditHandler(canvas);
  }

  public JFrame getFrame() {
    return (JFrame) SwingUtilities.getAncestorOfClass(JFrame.class, canvasPane);
  }

  public AttrTableDrawManager getAttrTableDrawManager(AttrTable table) {
    var ret = attrTableManager;
    if (ret == null) {
      ret = new AttrTableDrawManager(canvas, table, attributeSet);
      attrTableManager = ret;
    }
    return ret;
  }

  public void setCircuit(Project proj, CircuitState circuitState) {
    canvas.setCircuit(proj, circuitState);
  }
}
