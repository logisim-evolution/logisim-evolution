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
      if (value instanceof Tool) {
        final var t = (Tool) value;
        ret = super.getListCellRendererComponent(list, t.getDisplayName(), index, isSelected, cellHasFocus);
        icon = new ToolIcon(t);
      } else if (value == null) {
        ret = super.getListCellRendererComponent(list, "---", index, isSelected, cellHasFocus);
        icon = null;
      } else {
        ret = super.getListCellRendererComponent(list, value.toString(), index, isSelected, cellHasFocus);
        icon = null;
      }
      if (ret instanceof JLabel) {
        ((JLabel) ret).setIcon(icon);
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
