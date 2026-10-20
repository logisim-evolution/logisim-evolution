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
import javax.swing.JCheckBox;

class PrefBoolean extends JCheckBox implements ActionListener, PropertyChangeListener {
  private static final long serialVersionUID = 1L;
  private final PrefMonitor<Boolean> pref;
  private final StringGetter title;

  PrefBoolean(PrefMonitor<Boolean> pref, StringGetter title) {
    super(title.toString());
    this.pref = pref;
    this.title = title;

    addActionListener(this);
    pref.addPropertyChangeListener(this);
    setSelected(pref.getBoolean());
  }

  @Override
  public void actionPerformed(ActionEvent e) {
    pref.setBoolean(this.isSelected());
  }

  void localeChanged() {
    setText(title.toString());
  }

  @Override
  public void propertyChange(PropertyChangeEvent event) {
    if (pref.isSource(event)) {
      setSelected(pref.getBoolean());
    }
  }
}
