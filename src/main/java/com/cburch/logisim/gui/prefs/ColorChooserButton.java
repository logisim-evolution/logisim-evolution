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

package com.cburch.logisim.gui.prefs;

import java.awt.Color;
import java.awt.Frame;
import java.awt.Graphics2D;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;

import javax.swing.JButton;

import com.bric.colorpicker.ColorPickerDialog;
import com.cburch.logisim.gui.icons.AbstractIcon;
import com.cburch.logisim.prefs.PrefMonitor;

public class ColorChooserButton extends JButton implements PropertyChangeListener, ActionListener {

  private static final long serialVersionUID = 1L;
  
  private PrefMonitor<Integer> myMonitor;
  private Frame frame;

  private class ColorIcon extends AbstractIcon {
    @Override
    protected void paintIcon(Graphics2D g2) {
      g2.setColor(new Color(myMonitor.get()));
      g2.fillRect(0, 0, this.getIconWidth(), this.getIconHeight());
    }
    
    public void update(Frame frame) {
      Color col = new Color(myMonitor.get());
      Color newCol = ColorPickerDialog.showDialog(frame, col, false);
      if (newCol == null) return;
      if (!newCol.equals(col)) {
        col = newCol;
        myMonitor.set(col.getRGB());
      }
    }
  }
    
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
      JButton but = (JButton) e.getSource();
      if (but.getIcon() instanceof ColorIcon) {
        ColorIcon i = (ColorIcon) but.getIcon();
        i.update(frame);
      }
  }
}
