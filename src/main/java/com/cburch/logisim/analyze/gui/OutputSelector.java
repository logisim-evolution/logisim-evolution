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

package com.cburch.logisim.analyze.gui;

import static com.cburch.logisim.analyze.Strings.S;

import com.cburch.logisim.analyze.model.AnalyzerModel;
import com.cburch.logisim.analyze.model.VariableList;
import com.cburch.logisim.analyze.model.VariableListEvent;
import com.cburch.logisim.analyze.model.VariableListListener;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Insets;
import java.awt.event.ItemListener;
import java.awt.font.FontRenderContext;
import java.awt.font.TextAttribute;
import java.awt.font.TextLayout;
import java.text.AttributedString;
import javax.swing.AbstractListModel;
import javax.swing.ComboBoxModel;
import javax.swing.DefaultListCellRenderer;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.ListCellRenderer;

class OutputSelector {
  private class Model extends AbstractListModel<String>
      implements ComboBoxModel<String>, VariableListListener {

    private class AttributedJLabel extends DefaultListCellRenderer {
      private static final long serialVersionUID = 1L;

      @Override
      public Dimension getPreferredSize() {
        Dimension dim = super.getPreferredSize();
        Font f = getFont();
        Insets i = getInsets();
        dim.height = i.top + i.bottom + f.getSize() + f.getSize() / 2;
        return dim;
      }

      @Override
      public Component getListCellRendererComponent(
          JList<? extends Object> list,
          Object value,
          int index,
          boolean isSelected,
          boolean cellHasFocus) {
        super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
        this.setText((String) value);
        return this;
      }

      @Override
      public void paint(Graphics g) {
        String txt = getText();
        if (txt == null) {
          super.paint(g);
          return;
        }
        Graphics2D g2 = (Graphics2D) g.create();
        Insets i = getInsets();
        Font font = getFont();
        g2.setPaint(getBackground());
        g2.fillRect(0, 0, getWidth(), getHeight());
        FontRenderContext frc = g2.getFontRenderContext();
        AttributedString as;
        if (txt.contains(":")) {
          int idx = txt.indexOf(':');
          as = new AttributedString(txt.substring(0, idx) + txt.substring(idx + 1, txt.length()));
          as.addAttribute(TextAttribute.SUPERSCRIPT, TextAttribute.SUPERSCRIPT_SUB, idx, txt.length() - 1);
        } else if (txt.contains("[")) {
          int start = txt.indexOf('[');
          int stop = txt.lastIndexOf(']');
          as = new AttributedString(txt.substring(0,start)+txt.substring(start+1,stop));
          as.addAttribute(TextAttribute.SUPERSCRIPT, TextAttribute.SUPERSCRIPT_SUB, start, start+(stop-start-1));
        } else {
          as = new AttributedString(txt);
        }
        as.addAttribute(TextAttribute.FAMILY, font.getFamily());
        as.addAttribute(TextAttribute.SIZE, font.getSize());
        TextLayout tl = new TextLayout(as.getIterator(), frc);
        g2.setColor(getForeground());
        tl.draw(g2, i.left, i.top + tl.getAscent());
      }
    }

    private static final long serialVersionUID = 1L;
    private String selected;
    private AttributedJLabel MyRenderer = new AttributedJLabel();

    public ListCellRenderer<Object> getMyRenderer() {
      return MyRenderer;
    }

    @Override
    public String getElementAt(int index) {
      return source.bits.get(index);
    }

    @Override
    public String getSelectedItem() {
      return selected;
    }

    public int getSize() {
      return source.bits.size();
    }

    public void listChanged(VariableListEvent event) {
      int oldSize = select.getItemCount();
      int newSize = source.bits.size();
      fireContentsChanged(this, 0, oldSize > newSize ? oldSize : newSize);
      if (!source.bits.contains(selected)) {
        selected = (newSize == 0 ? null : source.bits.get(0));
        select.setSelectedItem(selected);
      }
    }

    public void setSelectedItem(Object value) {
      selected = (String) value;
    }
  }

  private VariableList source;
  private JLabel label = new JLabel();
  private JComboBox<String> select = new JComboBox<String>();

  public OutputSelector(AnalyzerModel model) {
    this.source = model.getOutputs();

    Model listModel = new Model();
    select.setModel(listModel);
    select.setRenderer(listModel.getMyRenderer());
    source.addVariableListListener(listModel);
  }

  public void addItemListener(ItemListener l) {
    select.addItemListener(l);
  }

  public JPanel createPanel() {
    JPanel ret = new JPanel();
    ret.add(label);
    ret.add(select);
    return ret;
  }

  public JComboBox<String> getComboBox() {
    return select;
  }

  public JLabel getLabel() {
    return label;
  }

  public String getSelectedOutput() {
    String value = (String) select.getSelectedItem();
    if (value != null && !source.bits.contains(value)) {
      if (source.bits.isEmpty()) {
        value = null;
      } else {
        value = source.bits.get(0);
      }
      select.setSelectedItem(value);
    }
    return value;
  }

  void localeChanged() {
    label.setText(S.get("outputSelectLabel"));
  }

  public void removeItemListener(ItemListener l) {
    select.removeItemListener(l);
  }
}
