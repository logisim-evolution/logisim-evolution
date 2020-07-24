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

package com.cburch.logisim.fpga.gui;

import com.cburch.logisim.prefs.AppPreferences;
import java.awt.Dimension;
import java.util.Hashtable;
import javax.swing.JLabel;
import javax.swing.JSlider;

@SuppressWarnings("serial")
public class ZoomSlider extends JSlider {
  
  private int minzoom;
  private int maxzoom;
  
  public int getMaxZoom() { return maxzoom; }
  public int getMinZoom() { return minzoom; }
  
  public ZoomSlider(int orientation, int min, int max, int value) {
    setup(orientation,min,max,value);
  }

  public ZoomSlider() {
    setup(JSlider.HORIZONTAL,100,200,100);
  }
  
  private void setup(int orientation, int min, int max, int value) {
    minzoom = min;
    maxzoom = max;
    int midvalue = min+((max-min)>>1);
    JLabel label;
    super.setOrientation(orientation);
    super.setMinimum(min);
    super.setMaximum(max);
    super.setValue(value);
    Dimension orig = super.getSize();
    orig.height = AppPreferences.getScaled(orig.height);
    orig.width = AppPreferences.getScaled(orig.width);
    super.setSize(orig);
    setMajorTickSpacing(50);
    setMinorTickSpacing(10);
    setPaintTicks(true);
    Hashtable<Integer, JLabel> labelTable = new Hashtable<Integer, JLabel>();
    label = new JLabel(getId(min));
    label.setFont(AppPreferences.getScaledFont(label.getFont()));
    labelTable.put(min, label);
    label = new JLabel(getId(midvalue));
    label.setFont(AppPreferences.getScaledFont(label.getFont()));
    labelTable.put(midvalue, label);
    label = new JLabel(getId(max));
    label.setFont(AppPreferences.getScaledFont(label.getFont()));
    labelTable.put(max, label);
    setLabelTable(labelTable);
    setPaintLabels(true);
  }
  
  private String getId(int value) {
    int hun = value/100;
    int tens = (value%100)/10;
    return String.format("%d.%dx", hun,tens);
  }
}
