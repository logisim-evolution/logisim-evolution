/*
 * Logisim-evolution - digital logic design tool and simulator
 * Copyright by the Logisim-evolution developers
 *
 * https://github.com/logisim-evolution/
 *
 * This is free software released under GNU GPLv3 license
 */

package com.cburch.logisim.gui.prefs;

import com.bric.colorpicker.ColorPickerDialog;
import com.cburch.logisim.gui.icons.BaseIcon;
import com.cburch.logisim.prefs.PrefMonitor;
import java.awt.Color;
import java.awt.Frame;
import java.awt.Graphics2D;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import javax.swing.JButton;

public class ColorChooserButton extends JButton implements PropertyChangeListener, ActionListener {

  private static final long serialVersionUID = 1L;

  private final PrefMonitor<Integer> myMonitor;
  private final Frame frame;

  public ColorChooserButton(Frame frame, PrefMonitor<Integer> pref) {
    super();
    myMonitor = pref;
    this.frame = frame;
    setIcon(new ColorIcon());
    pref.addPropertyChangeListener(this);
    addActionListener(this);
  }

  @Override
  public void propertyChange(PropertyChangeEvent evt) {
    repaint();
  }

  @Override
  public void actionPerformed(ActionEvent e) {
    final var but = (JButton) e.getSource();
    if (but.getIcon() instanceof ColorIcon i) {
      i.update(frame);
    }
  }

  private class ColorIcon extends BaseIcon {
    @Override
    protected void paintIcon(Graphics2D g2) {
      g2.setColor(new Color(myMonitor.get()));
      g2.fillRect(0, 0, this.getIconWidth(), this.getIconHeight());
    }

    public void update(Frame frame) {
      var col = new Color(myMonitor.get());
      final var newCol = ColorPickerDialog.showDialog(frame, col, false);
      if (newCol == null) return;
      if (!newCol.equals(col)) {
        col = newCol;
        myMonitor.set(col.getRGB());
      }
    }
  }
}
