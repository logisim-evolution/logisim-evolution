/*
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
import com.cburch.logisim.tools.AddTool;
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
    ToolbarData data = proj.getOptions().getToolbarData();
    data.addToolbarListener(myListener);
    data.addToolAttributeListener(myListener);
    AppPreferences.GATE_SHAPE.addPropertyChangeListener(myListener);
    proj.addProjectListener(myListener);
  }

  private static ToolbarItem findItem(List<ToolbarItem> items, Tool tool) {
    for (ToolbarItem item : items) {
      if (item instanceof ToolItem) {
        if (tool == ((ToolItem) item).tool) {
          return item;
        }
      }
    }
    return null;
  }

  private void buildContents() {
    List<ToolbarItem> oldItems = items;
    List<ToolbarItem> newItems = new ArrayList<>();
    ToolbarData data = proj.getLogisimFile().getOptions().getToolbarData();
    for (Tool tool : data.getContents()) {
      if (tool == null) {
        newItems.add(new ToolbarSeparator(4));
      } else {
        if (tool instanceof AddTool) tool.registerParrent(frame.getToolbar());
        ToolbarItem i = findItem(oldItems, tool);
        if (i == null) {
          newItems.add(new ToolItem(tool));
        } else {
          newItems.add(i);
        }
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
    if (item instanceof ToolItem) {
      Tool tool = ((ToolItem) item).tool;
      return tool == proj.getTool();
    } else {
      return false;
    }
  }

  @Override
  public void itemSelected(ToolbarItem item) {
    if (item instanceof ToolItem) {
      Tool tool = ((ToolItem) item).tool;
      proj.setTool(tool);
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
    public void attributeListChanged(AttributeEvent e) {}

    public void attributeValueChanged(AttributeEvent e) {
      fireToolbarAppearanceChanged();
    }

    //
    // ProjectListener methods
    //
    public void projectChanged(ProjectEvent e) {
      int act = e.getAction();
      if (act == ProjectEvent.ACTION_SET_TOOL) {
        fireToolbarAppearanceChanged();
      } else if (act == ProjectEvent.ACTION_SET_FILE) {
        LogisimFile old = (LogisimFile) e.getOldData();
        if (old != null) {
          ToolbarData data = old.getOptions().getToolbarData();
          data.removeToolbarListener(this);
          data.removeToolAttributeListener(this);
        }
        LogisimFile file = (LogisimFile) e.getData();
        if (file != null) {
          ToolbarData data = file.getOptions().getToolbarData();
          data.addToolbarListener(this);
          data.addToolAttributeListener(this);
        }
        buildContents();
      }
    }

    //
    // PropertyChangeListener method
    //
    public void propertyChange(PropertyChangeEvent event) {
      if (AppPreferences.GATE_SHAPE.isSource(event)) {
        fireToolbarAppearanceChanged();
      }
    }

    //
    // ToolbarListener methods
    //
    public void toolbarChanged() {
      buildContents();
    }
  }

  private class ToolItem implements ToolbarItem {
    private final Tool tool;

    ToolItem(Tool tool) {
      this.tool = tool;
    }

    public Dimension getDimension(Object orientation) {
      return new Dimension(
          AppPreferences.getIconSize() + 2 * AppPreferences.IconBorder,
          AppPreferences.getIconSize() + 2 * AppPreferences.IconBorder);
    }

    public String getToolTip() {
      String ret = tool.getDescription();
      int index = 1;
      for (ToolbarItem item : items) {
        if (item == this) break;
        if (item instanceof ToolItem) ++index;
      }
      if (index <= 10) {
        if (index == 10) index = 0;
        int mask = frame.getToolkit().getMenuShortcutKeyMaskEx();
        ret += " (" + InputEventUtil.toKeyDisplayString(mask) + "-" + index + ")";
      }
      return ret;
    }

    public boolean isSelectable() {
      return true;
    }

    public void paintIcon(Component destination, Graphics g) {
      // draw halo
      if (tool == haloedTool && AppPreferences.ATTRIBUTE_HALO.getBoolean()) {
        g.setColor(Canvas.HALO_COLOR);
        g.fillRect(
            AppPreferences.IconBorder,
            AppPreferences.IconBorder,
            AppPreferences.getIconSize(),
            AppPreferences.getIconSize());
      }

      // draw tool icon
      g.setColor(Color.BLACK);
      Graphics g_copy = g.create();
      ComponentDrawContext c = new ComponentDrawContext(destination, null, null, g, g_copy);
      tool.paintIcon(c, AppPreferences.IconBorder, AppPreferences.IconBorder);
      g_copy.dispose();
    }
  }
}
