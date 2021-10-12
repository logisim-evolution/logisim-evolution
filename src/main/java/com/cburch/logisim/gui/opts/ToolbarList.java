/*
 * Logisim-evolution - digital logic design tool and simulator
 * Copyright by the Logisim-evolution developers
 *
 * https://github.com/logisim-evolution/
 *
 * This is free software released under GNU GPLv3 license
 */

package com.cburch.logisim.gui.opts;

import com.cburch.logisim.comp.ComponentDrawContext;
import com.cburch.logisim.data.AttributeEvent;
import com.cburch.logisim.data.AttributeListener;
import com.cburch.logisim.file.ToolbarData;
import com.cburch.logisim.file.ToolbarData.ToolbarListener;
import com.cburch.logisim.prefs.AppPreferences;
import com.cburch.logisim.tools.Tool;
import java.awt.Component;
import java.awt.Graphics;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import javax.swing.AbstractListModel;
import javax.swing.DefaultListCellRenderer;
import javax.swing.Icon;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.ListSelectionModel;

@SuppressWarnings({"serial", "rawtypes"})
class ToolbarList extends JList {
  private final ToolbarData base;
  private final Model model;

  @SuppressWarnings("unchecked")
  public ToolbarList(ToolbarData base) {
    this.base = base;
    this.model = new Model();

    setModel(model);
    setCellRenderer(new ListRenderer());
    setSelectionMode(ListSelectionModel.SINGLE_SELECTION);

    AppPreferences.GATE_SHAPE.addPropertyChangeListener(model);
    base.addToolbarListener(model);
    base.addToolAttributeListener(model);
  }

  public void localeChanged() {
    model.toolbarChanged();
  }

  private static class ListRenderer extends DefaultListCellRenderer {
    @Override
    public Component getListCellRendererComponent(
        JList<?> list, Object value, int index, boolean isSelected, boolean cellHasFocus) {
      Component ret;
      Icon icon;
      if (value instanceof Tool t) {
        ret = super.getListCellRendererComponent(list, t.getDisplayName(), index, isSelected, cellHasFocus);
        icon = new ToolIcon(t);
      } else if (value == null) {
        ret = super.getListCellRendererComponent(list, "---", index, isSelected, cellHasFocus);
        icon = null;
      } else {
        ret = super.getListCellRendererComponent(list, value.toString(), index, isSelected, cellHasFocus);
        icon = null;
      }
      if (ret instanceof JLabel label) {
        label.setIcon(icon);
      }
      return ret;
    }
  }

  private static class ToolIcon implements Icon {
    private final Tool tool;

    ToolIcon(Tool tool) {
      this.tool = tool;
    }

    @Override
    public int getIconHeight() {
      return 20;
    }

    @Override
    public int getIconWidth() {
      return 20;
    }

    @Override
    public void paintIcon(Component comp, Graphics g, int x, int y) {
      final var gfxNew = g.create();
      tool.paintIcon(new ComponentDrawContext(comp, null, null, g, gfxNew), x + 2, y + 2);
      gfxNew.dispose();
    }
  }

  private class Model extends AbstractListModel implements ToolbarListener, AttributeListener, PropertyChangeListener {
    @Override
    public void attributeValueChanged(AttributeEvent e) {
      repaint();
    }

    @Override
    public Object getElementAt(int index) {
      return base.get(index);
    }

    @Override
    public int getSize() {
      return base.size();
    }

    @Override
    public void propertyChange(PropertyChangeEvent event) {
      if (AppPreferences.GATE_SHAPE.isSource(event)) {
        repaint();
      }
    }

    @Override
    public void toolbarChanged() {
      fireContentsChanged(this, 0, getSize());
    }
  }
}
