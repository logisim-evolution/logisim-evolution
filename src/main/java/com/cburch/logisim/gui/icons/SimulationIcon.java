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

package com.cburch.logisim.gui.icons;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics2D;

public class SimulationIcon extends AbstractIcon {

  public static final int SIM_PLAY = 0;
  public static final int SIM_PAUSE = 1;
  public static final int SIM_STEP = 2;
  public static final int SIM_ENABLE = 3;
  public static final int SIM_DISABLE = 4;
  public static final int SIM_HALF_TICK = 5;
  public static final int SIM_FULL_TICK = 6;
  
  private int currentType;
  
  public SimulationIcon(int type) {
    currentType = type;  
  }
  
  public void setType(int type) {
    currentType = type;  
  }

  @Override
  protected void paintIcon(Graphics2D g2) {
    int wh = getIconWidth()-scale(1);
    g2.setStroke(new BasicStroke(scale(1)));
    g2.setColor(currentType > SIM_STEP ? Color.LIGHT_GRAY : Color.WHITE);
    g2.fillOval(0, 0, wh, wh);
    if (currentType > SIM_STEP) {
      g2.setColor(Color.WHITE);
      int wh1 = wh-scale(4);
      g2.fillOval(scale(2), scale(2), wh1, wh1);
      g2.setStroke(new BasicStroke(scale(2)));
      g2.setColor(Color.BLACK);
      g2.drawLine(getIconWidth()/2, getIconWidth()/2, (3*wh)/4, wh/4);
      g2.setColor(Color.RED);
      g2.drawLine(getIconWidth()/2, getIconWidth()/2, wh/4, wh/4);
      g2.setStroke(new BasicStroke(scale(1)));
    }
    g2.setColor(Color.DARK_GRAY);
    g2.drawOval(0, 0, wh, wh);
    switch (currentType) {
      case SIM_STEP:
        g2.setColor(Color.GREEN.brighter());
        g2.fillRect(scale(10), scale(3), scale(1),scale(10));
        g2.setColor(Color.GREEN.darker());
        g2.drawRect(scale(10), scale(3), scale(1),scale(10));
      case SIM_PLAY :
        int[] xpos = {scale(6),scale(10),scale(6)};
        int[] ypos = {scale(3),scale(8),scale(13)};
        g2.setColor(Color.GREEN.brighter());
        g2.fillPolygon(xpos, ypos, 3);
        g2.setColor(Color.GREEN.darker());
        g2.drawPolygon(xpos, ypos, 3);
        break;
      case SIM_PAUSE:
        g2.setColor(Color.RED.brighter());
        g2.fillRect(scale(5), scale(3), scale(2), scale(10));
        g2.fillRect(scale(9), scale(3), scale(2), scale(10));
        g2.setColor(Color.RED.darker());
        g2.drawRect(scale(5), scale(3), scale(2), scale(10));
        g2.drawRect(scale(9), scale(3), scale(2), scale(10));
        break;
      case SIM_FULL_TICK:
        g2.setStroke(new BasicStroke(scale(2)));
        g2.setColor(Color.MAGENTA.darker().darker());
        g2.drawArc(0, 0, wh, wh, -135, -180);
        int[] x1 = {scale(10),scale(14),scale(14)};
        int[] y1 = {(wh)/4,(wh)/4,(wh)/4-scale(4)};
        g2.fillPolygon(x1, y1, 3);
        g2.drawPolygon(x1, y1, 3);
      case SIM_HALF_TICK:
        g2.setStroke(new BasicStroke(scale(2)));
        g2.setColor(Color.MAGENTA);
        g2.drawArc(0, 0, wh, wh, 45, -180);
        int[] x = {scale(3),scale(7),scale(3)};
        int[] y = {(3*wh)/4,(3*wh)/4,(3*wh)/4+scale(4)};
        g2.fillPolygon(x, y, 3);
        g2.drawPolygon(x, y, 3);
        break;
      case SIM_ENABLE:
    	int[] x0 = {scale(6),scale(10),scale(6)};
    	int[] y0 = {scale(8),scale(12),scale(15)};
    	g2.setColor(Color.GREEN.darker());
    	g2.fillPolygon(x0, y0, 3);
    	g2.setColor(Color.GREEN.darker().darker().darker());
    	g2.drawPolygon(x0, y0, 3);
    	break;
      case SIM_DISABLE:
    	g2.setStroke(new BasicStroke(scale(3)));
    	g2.setColor(Color.RED.darker().darker());
    	g2.drawLine(scale(6), scale(9), scale(6), scale(14));
    	g2.drawLine(scale(10), scale(9), scale(10), scale(14));
    }
  }

}
