/*
 * Logisim-evolution - digital logic design tool and simulator
 * Copyright by the Logisim-evolution developers
 *
 * https://github.com/logisim-evolution/
 *
 * This is free software released under GNU GPLv3 license
 */

package com.cburch.logisim.gui.generic;

import static com.cburch.logisim.gui.Strings.S;

import com.cburch.logisim.instance.StdAttr;
import com.cburch.logisim.prefs.AppPreferences;
import com.cburch.logisim.util.JInputComponent;
import com.cburch.logisim.util.LocaleListener;
import com.cburch.logisim.util.LocaleManager;

import java.awt.GraphicsEnvironment;
import java.awt.Font;
import java.awt.BorderLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.BoxLayout;
import javax.swing.DefaultListCellRenderer;
import javax.swing.JCheckBox;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.SwingConstants;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;

import java.util.Set;
import java.util.TreeSet;

public class FontSelector extends JPanel implements JInputComponent, ActionListener, ListSelectionListener, LocaleListener {

  private final Set<String> fontNames;
  private Font currentFont = StdAttr.DEFAULT_LABEL_FONT;
  private final JTextArea preview = new JTextArea(3, 20);
  private final JList<String> selectableFontFamilies;
  private final JList<Integer> selectableFontSize;
  private JCheckBox boldAttribute;
  private JCheckBox italicAttribute;
  private int fontSize;
  private int fontStyle;

  public static final FontSelector FONT_SELECTOR = new FontSelector();

  @SuppressWarnings("unchecked")
  public FontSelector() {
    fontNames = new TreeSet<>();
    for (final var font : GraphicsEnvironment.getLocalGraphicsEnvironment().getAllFonts()) {
      fontNames.add(font.getFamily());
    }
    setLayout(new BorderLayout());
    preview.setEditable(false);
    add(new JScrollPane(preview), BorderLayout.SOUTH);
    selectableFontFamilies = new JList(fontNames.toArray());
    selectableFontFamilies.addListSelectionListener(this);
    final var selections = new TreeSet<Integer>();
    for (var size = 2; size < 65; size++) {
      selections.add(size);
    }
    selectableFontSize = new JList(selections.toArray());
    ((DefaultListCellRenderer) selectableFontSize.getCellRenderer()).setHorizontalAlignment(SwingConstants.RIGHT);
    selectableFontSize.addListSelectionListener(this);
    add(new JScrollPane(selectableFontFamilies), BorderLayout.WEST);
    add(new JScrollPane(selectableFontSize), BorderLayout.CENTER);
    add(new JScrollPane(getStyle()), BorderLayout.EAST);
    LocaleManager.addLocaleListener(this);
    localeChanged();
  }

  @Override
  public Object getValue() {
    return currentFont;
  }

  private JPanel getStyle() {
    final var panel = new JPanel();
    panel.setLayout(new BoxLayout(panel, BoxLayout.PAGE_AXIS));
    boldAttribute = new JCheckBox();
    boldAttribute.addActionListener(this);
    panel.add(boldAttribute);
    italicAttribute = new JCheckBox();
    italicAttribute.addActionListener(this);
    panel.add(italicAttribute);
    return panel;
  }

  private void fontChanged() {
    preview.setCaretPosition(0);
    preview.setFont(new Font(currentFont.getFamily(), fontStyle, AppPreferences.getScaled(fontSize)));
    preview.repaint(preview.getVisibleRect());
    selectableFontFamilies.setSelectedValue(currentFont.getFamily(), true);
    selectableFontSize.setSelectedValue(fontSize, true);
  }

  @Override
  public void setValue(Object value) {
    if (value instanceof Font font) {
      if (fontNames.contains(font.getFamily())) {
        currentFont = font;
        fontSize = font.getSize();
        fontStyle = font.getStyle();
        boldAttribute.setSelected((fontStyle & Font.BOLD) != 0);
        italicAttribute.setSelected((fontStyle & Font.ITALIC) != 0);
        fontChanged();
        return;
      }
    }
    throw new IllegalArgumentException("Object is neither a font nor a supported font type!");
  }

  @Override
  public void actionPerformed(ActionEvent e) {
    if (boldAttribute.equals(e.getSource())) {
      final var isChecked = boldAttribute.isSelected();
      final var newStyle = isChecked ? fontStyle | Font.BOLD : fontStyle & (Font.BOLD ^ 0xFFFFFFFF);
      if (newStyle != fontStyle) {
        fontStyle = newStyle;
        currentFont = new Font(currentFont.getFamily(), fontStyle, fontSize);
        fontChanged();
      }
    } else if (italicAttribute.equals(e.getSource())) {
      final var isChecked = italicAttribute.isSelected();
      final var newStyle = isChecked ? fontStyle | Font.ITALIC : fontStyle & (Font.ITALIC ^ 0xFFFFFFFF);
      if (newStyle != fontStyle) {
        fontStyle = newStyle;
        currentFont = new Font(currentFont.getFamily(), fontStyle, fontSize);
        fontChanged();
      }
    }
  }

  @Override
  public void valueChanged(ListSelectionEvent e) {
    final var selectedFont = selectableFontFamilies.getSelectedValue();
    final var selectedFontSize = selectableFontSize.getSelectedValue();
    var change = (selectedFontSize != null) && (selectedFontSize != fontSize);
    if ((selectedFont != null) && fontNames.contains(selectedFont)) {
      change |= !selectedFont.equals(currentFont.getFamily());
    }
    if (change) {
      if (selectedFontSize != null) fontSize = selectedFontSize;
      currentFont = new Font(selectedFont, fontStyle, fontSize);
      fontChanged();
    }
  }

  @Override
  public void localeChanged() {
    boldAttribute.setText(S.get("fontBoldFont"));
    italicAttribute.setText(S.get("fontItalicFont"));
    preview.setText(S.get("fontExampleLineText"));
    repaint();
  }

}
