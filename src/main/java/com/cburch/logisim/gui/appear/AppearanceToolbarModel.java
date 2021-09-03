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
import com.cburch.draw.toolbar.AbstractToolbarModel;
import com.cburch.draw.toolbar.ToolbarItem;
import com.cburch.draw.tools.AbstractTool;
import com.cburch.draw.tools.CurveTool;
import com.cburch.draw.tools.DrawingAttributeSet;
import com.cburch.draw.tools.LineTool;
import com.cburch.draw.tools.OvalTool;
import com.cburch.draw.tools.PolyTool;
import com.cburch.draw.tools.RectangleTool;
import com.cburch.draw.tools.RoundRectangleTool;
import com.cburch.draw.tools.TextTool;
import com.cburch.draw.tools.ToolbarToolItem;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

class AppearanceToolbarModel extends AbstractToolbarModel implements PropertyChangeListener {
  private final Canvas canvas;
  private final List<ToolbarItem> items;

  public AppearanceToolbarModel(AbstractTool selectTool, ShowStateTool showStateTool, Canvas canvas, DrawingAttributeSet attrs) {
    this.canvas = canvas;

    AbstractTool[] tools = {
      selectTool,
      new TextTool(attrs),
      new LineTool(attrs),
      new CurveTool(attrs),
      new PolyTool(false, attrs),
      new RectangleTool(attrs),
      new RoundRectangleTool(attrs),
      new OvalTool(attrs),
      new PolyTool(true, attrs),
    };

    final var rawItems = new ArrayList<ToolbarItem>();
    for (final var tool : tools) {
      rawItems.add(new ToolbarToolItem(tool));
    }
    rawItems.add(showStateTool);
    items = Collections.unmodifiableList(rawItems);
    canvas.addPropertyChangeListener(Canvas.TOOL_PROPERTY, this);
  }

  AbstractTool getFirstTool() {
    final var item = (ToolbarToolItem) items.get(0);
    return item.getTool();
  }

  @Override
  public List<ToolbarItem> getItems() {
    return items;
  }

  @Override
  public boolean isSelected(ToolbarItem item) {
    if (item instanceof ToolbarToolItem) {
      final var tool = ((ToolbarToolItem) item).getTool();
      return canvas != null && tool == canvas.getTool();
    } else {
      return false;
    }
  }

  @Override
  public void itemSelected(ToolbarItem item) {
    if (item instanceof ToolbarToolItem) {
      final var tool = ((ToolbarToolItem) item).getTool();
      canvas.setTool(tool);
      fireToolbarAppearanceChanged();
    }
  }

  @Override
  public void propertyChange(PropertyChangeEvent e) {
    final var prop = e.getPropertyName();
    if (Canvas.TOOL_PROPERTY.equals(prop)) {
      fireToolbarAppearanceChanged();
    }
  }
}
