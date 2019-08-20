/**
 * This file is part of logisim-evolution.
 *
 * Logisim-evolution is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by the
 * Free Software Foundation, either version 3 of the License, or (at your
 * option) any later version.
 *
 * Logisim-evolution is distributed in the hope that it will be useful, but
 * WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 * or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
 * for more details.
 *
 * You should have received a copy of the GNU General Public License along 
 * with logisim-evolution. If not, see <http://www.gnu.org/licenses/>.
 *
 * Original code by Carl Burch (http://www.cburch.com), 2011.
 * Subsequent modifications by:
 *   + College of the Holy Cross
 *     http://www.holycross.edu
 *   + Haute École Spécialisée Bernoise/Berner Fachhochschule
 *     http://www.bfh.ch
 *   + Haute École du paysage, d'ingénierie et d'architecture de Genève
 *     http://hepia.hesge.ch/
 *   + Haute École d'Ingénierie et de Gestion du Canton de Vaud
 *     http://www.heig-vd.ch/
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
import javax.swing.JFrame;
import javax.swing.SwingUtilities;

public class AppearanceView {
  private static final double[] ZOOM_OPTIONS = {100, 150, 200, 300, 400, 600, 800};

  private DrawingAttributeSet attrs;
  private AppearanceCanvas canvas;
  private CanvasPane canvasPane;
  private AppearanceToolbarModel toolbarModel;
  private AttrTableDrawManager attrTableManager;
  private ZoomModel zoomModel;
  private AppearanceEditHandler editHandler;

  public AppearanceView() {
    attrs = new DrawingAttributeSet();
    SelectTool selectTool = new SelectTool();
    canvas = new AppearanceCanvas(selectTool);
    canvasPane = new CanvasPane(canvas);
    ShowStateTool ssTool = new ShowStateTool(this, canvas, attrs);
    toolbarModel = new AppearanceToolbarModel(selectTool, ssTool, canvas, attrs);
    zoomModel = new BasicZoomModel(AppPreferences.APPEARANCE_SHOW_GRID, AppPreferences.APPEARANCE_ZOOM,
            ZOOM_OPTIONS, canvasPane);
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
    AttrTableDrawManager ret = attrTableManager;
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
