/*
 * Logisim-evolution - digital logic design tool and simulator
 * Copyright by the Logisim-evolution developers
 *
 * https://github.com/logisim-evolution/
 *
 * This is free software released under GNU GPLv3 license
 */

package com.cburch.logisim.gui.main;

import com.cburch.draw.toolbar.AbstractToolbarModel;
import com.cburch.draw.toolbar.ToolbarItem;
import com.cburch.draw.toolbar.ToolbarSeparator;
import com.cburch.logisim.comp.ComponentDrawContext;
import com.cburch.logisim.data.AttributeEvent;
import com.cburch.logisim.data.AttributeListener;
import com.cburch.logisim.file.LogisimFile;
import com.cburch.logisim.file.ToolbarData;
import com.cburch.logisim.prefs.AppPreferences;
import com.cburch.logisim.proj.Project;
import com.cburch.logisim.proj.ProjectEvent;
import com.cburch.logisim.proj.ProjectListener;
import com.cburch.logisim.tools.Tool;
import com.cburch.logisim.util.InputEventUtil;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Graphics;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

class LayoutToolbarModel extends AbstractToolbarModel {
  private final Frame frame;
  private final Project proj;
  private final MyListener myListener;
  private List<ToolbarItem> items;
  private Tool haloedTool;

  public LayoutToolbarModel(Frame frame, Project proj) {
    this.frame = frame;
    this.proj = proj;
    myListener = new MyListener();
    items = Collections.emptyList();
    haloedTool = null;
    buildContents();

    // set up listeners
    final var data = proj.getOptions().getToolbarData();
    data.addToolbarListener(myListener);
    data.addToolAttributeListener(myListener);
    AppPreferences.GATE_SHAPE.addPropertyChangeListener(myListener);
    proj.addProjectListener(myListener);
  }

  private static ToolbarItem findItem(List<ToolbarItem> items, Tool tool) {
    for (final var item : items) {
      if (item instanceof ToolItem toolItem) {
        if (tool == toolItem.tool) {
          return item;
        }
      }
    }
    return null;
  }

  private void buildContents() {
    final var oldItems = items;
    final var newItems = new ArrayList<ToolbarItem>();
    final var data = proj.getLogisimFile().getOptions().getToolbarData();
    for (final var tool : data.getContents()) {
      if (tool == null) {
        newItems.add(new ToolbarSeparator(4));
      } else {
        final var i = findItem(oldItems, tool);
        newItems.add(Objects.requireNonNullElseGet(i, () -> new ToolItem(tool)));
      }
    }
    items = Collections.unmodifiableList(newItems);
    fireToolbarContentsChanged();
  }

  @Override
  public List<ToolbarItem> getItems() {
    return items;
  }

  @Override
  public boolean isSelected(ToolbarItem item) {
    return (item instanceof ToolItem toolItem)
           ? toolItem.tool == proj.getTool()
           : false;
  }

  @Override
  public void itemSelected(ToolbarItem item) {
    if (item instanceof ToolItem toolItem) {
      proj.setTool(toolItem.tool);
    }
  }

  public void setHaloedTool(Tool t) {
    if (haloedTool != t) {
      haloedTool = t;
      fireToolbarAppearanceChanged();
    }
  }

  private class MyListener
      implements ProjectListener,
          AttributeListener,
          ToolbarData.ToolbarListener,
          PropertyChangeListener {
    //
    // AttributeListener methods
    //
    @Override
    public void attributeListChanged(AttributeEvent e) {
      // dummy
    }

    @Override
    public void attributeValueChanged(AttributeEvent e) {
      fireToolbarAppearanceChanged();
    }

    //
    // ProjectListener methods
    //
    @Override
    public void projectChanged(ProjectEvent e) {
      final var act = e.getAction();
      if (act == ProjectEvent.ACTION_SET_TOOL) {
        fireToolbarAppearanceChanged();
      } else if (act == ProjectEvent.ACTION_SET_FILE) {
        final var old = (LogisimFile) e.getOldData();
        if (old != null) {
          final var data = old.getOptions().getToolbarData();
          data.removeToolbarListener(this);
          data.removeToolAttributeListener(this);
        }
        final var file = (LogisimFile) e.getData();
        if (file != null) {
          final var data = file.getOptions().getToolbarData();
          data.addToolbarListener(this);
          data.addToolAttributeListener(this);
        }
        buildContents();
      }
    }

    //
    // PropertyChangeListener method
    //
    @Override
    public void propertyChange(PropertyChangeEvent event) {
      if (AppPreferences.GATE_SHAPE.isSource(event)) {
        fireToolbarAppearanceChanged();
      }
    }

    //
    // ToolbarListener methods
    //
    @Override
    public void toolbarChanged() {
      buildContents();
    }
  }

  private class ToolItem implements ToolbarItem {
    private final Tool tool;

    ToolItem(Tool tool) {
      this.tool = tool;
    }

    @Override
    public Dimension getDimension(Object orientation) {
      final var pad = 2 * AppPreferences.ICON_BORDER;
      return new Dimension(AppPreferences.getIconSize() + pad, AppPreferences.getIconSize() + pad);
    }

    @Override
    public String getToolTip() {
      var ret = tool.getDescription();
      var index = 1;
      for (final var item : items) {
        if (item == this) break;
        if (item instanceof ToolItem) ++index;
      }
      if (index <= 10) {
        if (index == 10) index = 0;
        final var mask = frame.getToolkit().getMenuShortcutKeyMaskEx();
        ret += " (" + InputEventUtil.toKeyDisplayString(mask) + "-" + index + ")";
      }
      return ret;
    }

    @Override
    public boolean isSelectable() {
      return true;
    }

    @Override
    public void paintIcon(Component destination, Graphics gfx) {
      // draw halo
      if (tool == haloedTool && AppPreferences.ATTRIBUTE_HALO.getBoolean()) {
        gfx.setColor(Canvas.HALO_COLOR);
        gfx.fillRect(
            AppPreferences.ICON_BORDER,
            AppPreferences.ICON_BORDER,
            AppPreferences.getIconSize(),
            AppPreferences.getIconSize());
      }

      // draw tool icon
      gfx.setColor(new Color(AppPreferences.COMPONENT_ICON_COLOR.get()));
      final var gfxCopy = gfx.create();
      final var c = new ComponentDrawContext(destination, null, null, gfx, gfxCopy);
      tool.paintIcon(c, AppPreferences.ICON_BORDER, AppPreferences.ICON_BORDER);
      gfxCopy.dispose();
    }
  }
}
