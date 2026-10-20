/*
 * Logisim-evolution - digital logic design tool and simulator
 * Copyright by the Logisim-evolution developers
 *
 * https://github.com/logisim-evolution/
 *
 * This is free software released under GNU GPLv3 license
 */

package com.cburch.logisim.gui.prefs;

import com.cburch.logisim.prefs.PrefMonitor;
import com.cburch.logisim.util.StringGetter;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;

public class PrefOptionList implements ActionListener, PropertyChangeListener {
  private final PrefMonitor<String> pref;
  private final StringGetter labelStr;

  private final JLabel label;
  private final JComboBox<PrefOption> combo;

  public PrefOptionList(PrefMonitor<String> pref, StringGetter labelStr, PrefOption[] options) {
    this.pref = pref;
    this.labelStr = labelStr;

    label = new JLabel(labelStr.toString() + " ");
    combo = new JComboBox<>();
    for (final var opt : options) {
      combo.addItem(opt);
    }

    combo.addActionListener(this);
    pref.addPropertyChangeListener(this);
    selectOption(pref.get());
  }

  @Override
  public void actionPerformed(ActionEvent e) {
    final var x = (PrefOption) combo.getSelectedItem();
    pref.set((String) x.getValue());
  }

  JPanel createJPanel() {
    final var ret = new JPanel();
    ret.add(label);
    ret.add(combo);
    return ret;
  }

  public JComboBox<PrefOption> getJComboBox() {
    return combo;
  }

  public JLabel getJLabel() {
    return label;
  }

  void localeChanged() {
    label.setText(labelStr.toString() + " ");
  }

  @Override
  public void propertyChange(PropertyChangeEvent event) {
    if (pref.isSource(event)) {
      selectOption(pref.get());
    }
  }

  private void selectOption(Object value) {
    for (var i = combo.getItemCount() - 1; i >= 0; i--) {
      final var opt = combo.getItemAt(i);
      if (opt.getValue().equals(value)) {
        combo.setSelectedItem(opt);
        return;
      }
    }
    combo.setSelectedItem(combo.getItemAt(0));
  }
}
