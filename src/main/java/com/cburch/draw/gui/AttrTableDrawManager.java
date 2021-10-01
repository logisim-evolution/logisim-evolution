/*
 * Logisim-evolution - digital logic design tool and simulator
 * Copyright by the Logisim-evolution developers
 *
 * https://github.com/logisim-evolution/
 *
 * This is free software released under GNU GPLv3 license
 */

package com.cburch.draw.gui;

import com.cburch.draw.canvas.Canvas;
import com.cburch.draw.tools.AbstractTool;
import com.cburch.draw.tools.DrawingAttributeSet;
import com.cburch.draw.tools.SelectTool;
import com.cburch.logisim.gui.generic.AttrTable;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;

public class AttrTableDrawManager implements PropertyChangeListener {
  private final Canvas canvas;
  private final AttrTable table;
  private final AttrTableSelectionModel selectionModel;
  private final AttrTableToolModel toolModel;

  public AttrTableDrawManager(Canvas canvas, AttrTable table, DrawingAttributeSet attrs) {
    this.canvas = canvas;
    this.table = table;
    this.selectionModel = new AttrTableSelectionModel(canvas);
    this.toolModel = new AttrTableToolModel(attrs, null);

    canvas.addPropertyChangeListener(Canvas.TOOL_PROPERTY, this);
    updateToolAttributes();
  }

  public void attributesSelected() {
    updateToolAttributes();
  }

  //
  // PropertyChangeListener method
  //
  @Override
  public void propertyChange(PropertyChangeEvent evt) {
    final var prop = evt.getPropertyName();
    if (prop.equals(Canvas.TOOL_PROPERTY)) {
      updateToolAttributes();
    }
  }

  private void updateToolAttributes() {
    final var tool = canvas.getTool();
    if (tool instanceof SelectTool) {
      table.setAttrTableModel(selectionModel);
    } else if (tool instanceof AbstractTool absTool) {
      toolModel.setTool(absTool);
      table.setAttrTableModel(toolModel);
    } else {
      table.setAttrTableModel(null);
    }
  }
}
