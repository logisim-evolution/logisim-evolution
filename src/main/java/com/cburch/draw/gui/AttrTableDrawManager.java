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
import com.cburch.draw.canvas.SelectionEvent;
import com.cburch.draw.canvas.SelectionListener;
import com.cburch.draw.tools.AbstractTool;
import com.cburch.draw.tools.DrawingAttributeSet;
import com.cburch.draw.tools.SelectTool;
import com.cburch.logisim.gui.generic.AttrTable;
import com.cburch.logisim.gui.generic.AttrTableModel;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.function.Supplier;

public class AttrTableDrawManager implements PropertyChangeListener, SelectionListener {
  private final Canvas canvas;
  private final AttrTable table;
  private final Supplier<AttrTableModel> emptySelectionModel;
  private final AttrTableSelectionModel selectionModel;
  private final AttrTableToolModel toolModel;

  public AttrTableDrawManager(Canvas canvas, AttrTable table, DrawingAttributeSet attrs) {
    this(canvas, table, attrs, null);
  }

  public AttrTableDrawManager(
      Canvas canvas,
      AttrTable table,
      DrawingAttributeSet attrs,
      Supplier<AttrTableModel> emptySelectionModel) {
    this.canvas = canvas;
    this.table = table;
    this.emptySelectionModel = emptySelectionModel;
    this.selectionModel = new AttrTableSelectionModel(canvas);
    this.toolModel = new AttrTableToolModel(attrs, null);

    canvas.addPropertyChangeListener(Canvas.TOOL_PROPERTY, this);
    if (emptySelectionModel != null) {
      canvas.getSelection().addSelectionListener(this);
    }
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

  @Override
  public void selectionChanged(SelectionEvent e) {
    if (emptySelectionModel != null && canvas.getTool() instanceof SelectTool) {
      updateSelectionAttributes();
    }
  }

  private void updateSelectionAttributes() {
    if (emptySelectionModel != null && canvas.getSelection().isEmpty()) {
      table.setAttrTableModel(emptySelectionModel.get());
    } else {
      table.setAttrTableModel(selectionModel);
    }
  }

  private void updateToolAttributes() {
    final var tool = canvas.getTool();
    if (tool instanceof SelectTool) {
      updateSelectionAttributes();
    } else if (tool instanceof AbstractTool absTool) {
      toolModel.setTool(absTool);
      table.setAttrTableModel(toolModel);
    } else {
      table.setAttrTableModel(null);
    }
  }
}
