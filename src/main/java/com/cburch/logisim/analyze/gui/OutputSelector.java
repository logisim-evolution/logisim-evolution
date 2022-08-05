/*
 * Logisim-evolution - digital logic design tool and simulator
 * Copyright by the Logisim-evolution developers
 *
 * https://github.com/logisim-evolution/
 *
 * This is free software released under GNU GPLv3 license
 */

package com.cburch.logisim.analyze.gui;

import static com.cburch.logisim.analyze.Strings.S;

import com.cburch.logisim.analyze.model.AnalyzerModel;
import com.cburch.logisim.analyze.model.VariableList;
import com.cburch.logisim.analyze.model.VariableListEvent;
import com.cburch.logisim.analyze.model.VariableListListener;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.event.ItemListener;
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
        final var dim = super.getPreferredSize();
        final var f = getFont();
        final var i = getInsets();
        dim.height = i.top + i.bottom + f.getSize() + f.getSize() / 2;
        return dim;
      }

      @Override
      public Component getListCellRendererComponent(
          JList<?> list, Object value, int index, boolean isSelected, boolean cellHasFocus) {
        super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
        this.setText((String) value);
        return this;
      }

      @Override
      public void paint(Graphics g) {
        final var txt = getText();
        if (txt == null) {
          super.paint(g);
          return;
        }
        final var g2 = (Graphics2D) g.create();
        final var i = getInsets();
        final var font = getFont();
        g2.setPaint(getBackground());
        g2.fillRect(0, 0, getWidth(), getHeight());
        final var frc = g2.getFontRenderContext();
        AttributedString as;
        if (txt.contains(":")) {
          final var idx = txt.indexOf(':');
          as = new AttributedString(txt.substring(0, idx) + txt.substring(idx + 1));
          as.addAttribute(
              TextAttribute.SUPERSCRIPT, TextAttribute.SUPERSCRIPT_SUB, idx, txt.length() - 1);
        } else if (txt.contains("[")) {
          final var start = txt.indexOf('[');
          final var stop = txt.lastIndexOf(']');
          as = new AttributedString(txt.substring(0, start) + txt.substring(start + 1, stop));
          as.addAttribute(
              TextAttribute.SUPERSCRIPT,
              TextAttribute.SUPERSCRIPT_SUB,
              start,
              start + (stop - start - 1));
        } else {
          as = new AttributedString(txt);
        }
        as.addAttribute(TextAttribute.FAMILY, font.getFamily());
        as.addAttribute(TextAttribute.SIZE, font.getSize());

        final var tl = new TextLayout(as.getIterator(), frc);
        g2.setColor(getForeground());
        tl.draw(g2, i.left, i.top + tl.getAscent());
      }
    }

    private static final long serialVersionUID = 1L;
    private String selected;
    private final AttributedJLabel myRenderer = new AttributedJLabel();

    public ListCellRenderer<Object> getMyRenderer() {
      return myRenderer;
    }

    @Override
    public String getElementAt(int index) {
      return source.bits.get(index);
    }

    @Override
    public String getSelectedItem() {
      return selected;
    }

    @Override
    public int getSize() {
      return source.bits.size();
    }

    @Override
    public void listChanged(VariableListEvent event) {
      final var oldSize = select.getItemCount();
      final var newSize = source.bits.size();
      fireContentsChanged(this, 0, Math.max(oldSize, newSize));
      if (!source.bits.contains(selected)) {
        selected = (newSize == 0 ? null : source.bits.get(0));
        select.setSelectedItem(selected);
      }
    }

    @Override
    public void setSelectedItem(Object value) {
      selected = (String) value;
    }
  }

  private final VariableList source;
  private final JLabel label = new JLabel();
  private final JComboBox<String> select = new JComboBox<>();

  public OutputSelector(AnalyzerModel model) {
    this.source = model.getOutputs();

    final var listModel = new Model();
    select.setModel(listModel);
    select.setRenderer(listModel.getMyRenderer());
    source.addVariableListListener(listModel);
  }

  public void addItemListener(ItemListener l) {
    select.addItemListener(l);
  }

  public JPanel createPanel() {
    final var ret = new JPanel();
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
    var value = (String) select.getSelectedItem();
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
