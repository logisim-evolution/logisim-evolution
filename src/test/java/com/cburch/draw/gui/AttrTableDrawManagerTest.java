/*
 * Logisim-evolution - digital logic design tool and simulator
 * Copyright by the Logisim-evolution developers
 *
 * https://github.com/logisim-evolution/
 *
 * This is free software released under GNU GPLv3 license
 */

package com.cburch.draw.gui;

import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.cburch.draw.canvas.Canvas;
import com.cburch.draw.shapes.Rectangle;
import com.cburch.draw.tools.DrawingAttributeSet;
import com.cburch.draw.tools.SelectTool;
import com.cburch.logisim.gui.generic.AttrTable;
import com.cburch.logisim.gui.generic.AttrTableModel;
import com.cburch.logisim.gui.generic.AttrTableModelListener;
import com.cburch.logisim.gui.generic.AttrTableModelRow;
import org.junit.jupiter.api.Test;

class AttrTableDrawManagerTest {

  @Test
  void selectToolUsesFallbackModelOnlyForEmptySelections() {
    final var canvas = new Canvas();
    final var table = new AttrTable(null);
    final var emptySelectionModel = new TestAttrTableModel();

    new AttrTableDrawManager(canvas, table, new DrawingAttributeSet(), () -> emptySelectionModel);
    canvas.setTool(new SelectTool());
    assertSame(emptySelectionModel, table.getAttrTableModel());

    canvas.getSelection().setSelected(new Rectangle(0, 0, 10, 10), true);
    final var selectionModel = assertInstanceOf(AttrTableSelectionModel.class, table.getAttrTableModel());
    assertTrue(selectionModel.getRowCount() > 1);

    canvas.getSelection().clearSelected();
    assertSame(emptySelectionModel, table.getAttrTableModel());
  }

  private static final class TestAttrTableModel implements AttrTableModel {
    @Override
    public void addAttrTableModelListener(AttrTableModelListener listener) {}

    @Override
    public AttrTableModelRow getRow(int rowIndex) {
      return null;
    }

    @Override
    public int getRowCount() {
      return 0;
    }

    @Override
    public String getTitle() {
      return null;
    }

    @Override
    public void removeAttrTableModelListener(AttrTableModelListener listener) {}
  }
}
