/*
 * Logisim-evolution - digital logic design tool and simulator
 * Copyright by the Logisim-evolution developers
 *
 * https://github.com/logisim-evolution/
 *
 * This is free software released under GNU GPLv3 license
 */

package com.cburch.logisim.gui.appear;

import com.cburch.draw.canvas.Canvas;
import com.cburch.draw.gui.AttrTableDrawManager;
import com.cburch.draw.toolbar.ToolbarModel;
import com.cburch.draw.tools.DrawingAttributeSet;
import com.cburch.draw.tools.SelectTool;
import com.cburch.logisim.circuit.CircuitState;
import com.cburch.logisim.data.AttributeSet;
import com.cburch.logisim.gui.generic.AttrTable;
import com.cburch.logisim.gui.generic.BasicZoomModel;
import com.cburch.logisim.gui.generic.CanvasPane;
import com.cburch.logisim.gui.generic.ZoomModel;
import com.cburch.logisim.gui.menu.EditHandler;
import com.cburch.logisim.prefs.AppPreferences;
import com.cburch.logisim.proj.Project;
import java.util.ArrayList;
import javax.swing.JFrame;
import javax.swing.SwingUtilities;

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

  private final DrawingAttributeSet attrs;
  private final AppearanceCanvas canvas;
  private final CanvasPane canvasPane;
  private final AppearanceToolbarModel toolbarModel;
  private final ZoomModel zoomModel;
  private final AppearanceEditHandler editHandler;
  private AttrTableDrawManager attrTableManager;

  public AppearanceView() {
    attrs = new DrawingAttributeSet();
    final var selectTool = new SelectTool();
    canvas = new AppearanceCanvas(selectTool);
    canvasPane = new CanvasPane(canvas);
    final var ssTool = new ShowStateTool(this, canvas, attrs);
    toolbarModel = new AppearanceToolbarModel(selectTool, ssTool, canvas, attrs);
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

  public AttributeSet getAttributeSet() {
    return attrs;
  }

  public AttrTableDrawManager getAttrTableDrawManager(AttrTable table) {
    var ret = attrTableManager;
    if (ret == null) {
      ret = new AttrTableDrawManager(canvas, table, attrs);
      attrTableManager = ret;
    }
    return ret;
  }

  public Canvas getCanvas() {
    return canvas;
  }

  public CanvasPane getCanvasPane() {
    return canvasPane;
  }

  public EditHandler getEditHandler() {
    return editHandler;
  }

  public ToolbarModel getToolbarModel() {
    return toolbarModel;
  }

  public ZoomModel getZoomModel() {
    return zoomModel;
  }

  public void setCircuit(Project proj, CircuitState circuitState) {
    canvas.setCircuit(proj, circuitState);
  }
}
