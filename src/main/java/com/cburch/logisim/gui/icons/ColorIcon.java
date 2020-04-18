package com.cburch.logisim.gui.icons;

import java.awt.Color;
import java.awt.Frame;
import java.awt.Graphics2D;

import com.bric.colorpicker.ColorPickerDialog;
import com.cburch.logisim.prefs.PrefMonitor;

public class ColorIcon extends AbstractIcon {

  private PrefMonitor<Integer> myPref;

  public ColorIcon(PrefMonitor<Integer> myPref) {
    super();
    this.myPref = myPref;
  }

  @Override
  protected void paintIcon(Graphics2D g2) {
    g2.setColor(new Color(myPref.get()));
    g2.fillRect(0, 0, this.getIconWidth(), this.getIconHeight());
  }
    
  public void update(Frame frame) {
    Color col = new Color(myPref.get());
    Color newCol = ColorPickerDialog.showDialog(frame, col, false);
    if (newCol == null) return;
    if (!newCol.equals(col)) {
      col = newCol;
      myPref.set(col.getRGB());
    }
  }
}
