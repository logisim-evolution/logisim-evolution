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

import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.font.TextLayout;
import java.awt.geom.GeneralPath;

import com.cburch.logisim.instance.StdAttr;
import com.cburch.logisim.prefs.AppPreferences;

public class FlipFlopIcon extends AbstractIcon {

  public static final int D_FLIPFLOP = 0;
  public static final int T_FLIPFLOP = 1;
  public static final int JK_FLIPFLOP = 2;
  public static final int SR_FLIPFLOP = 3;
  public static final int REGISTER = 4;
  
  private int type;
  
  public FlipFlopIcon( int type ) {
    this.type = type;
  }

  @Override
  protected void paintIcon(Graphics2D g2) {
    if (AppPreferences.getDefaultAppearance().equals(StdAttr.APPEAR_CLASSIC))
      paintClassicIcon(g2);
    else
      paintEvolutionIcon(g2);
  }
  
  private void paintClassicIcon(Graphics2D g2) {
    g2.drawRect(scale(2), scale(2), scale(12), scale(12));
    String str = "";
    switch (type) {
      case D_FLIPFLOP : str = "D";
                        break;
      case T_FLIPFLOP : str = "T";
                        break;
      case JK_FLIPFLOP: str = "JK";
                        break;
      case SR_FLIPFLOP: str = "SR";
                        break;
      case REGISTER   : str = "00";
                        break;
    }
    Font f = g2.getFont().deriveFont((float)((double)AppPreferences.getIconSize()/2.1));
    TextLayout l = new TextLayout(str,f,g2.getFontRenderContext());
    l.draw(g2, (float)(getIconWidth()/2-l.getBounds().getCenterX()), (float)(getIconHeight()/2-l.getBounds().getCenterY()));
  }

  private void paintEvolutionIcon(Graphics2D g2) {
    if (type == REGISTER) {
      paintRegisterIcon(g2);
      return;
    }
    g2.drawRect(scale(4), 0, scale(8), scale(16));
    g2.drawLine(scale(0), scale(3), scale(4), scale(3));
    g2.drawLine(scale(12), scale(3), scale(15), scale(3));
    if (type == JK_FLIPFLOP || type == SR_FLIPFLOP)
      g2.drawLine(scale(0), scale(7), scale(4), scale(7));
    g2.drawLine(scale(0), scale(12), scale(4), scale(12));
    int[] xp = {scale(4),scale(7),scale(4)};
    int[] yp = {scale(11),scale(12),scale(13)};
    g2.drawPolygon(xp, yp, 3);
    g2.drawOval(scale(12), scale(10), scale(4), scale(4));
    GeneralPath p = new GeneralPath();
    switch (type) {
      case D_FLIPFLOP :
        p.moveTo(scale(7), scale(5));
        p.lineTo(scale(6), scale(5));
        p.lineTo(scale(6), scale(2));
        p.lineTo(scale(7), scale(2));
        p.quadTo(scale(10), scale(4), scale(7), scale(5));
        break;
      case T_FLIPFLOP :
        p.moveTo(scale(6), scale(2));
        p.lineTo(scale(8), scale(2));
        p.moveTo(scale(7), scale(2));
        p.lineTo(scale(7), scale(4));
        break;
      case JK_FLIPFLOP :
        p.moveTo(scale(6), scale(2));
        p.lineTo(scale(8), scale(2));
        p.lineTo(scale(8), scale(4));
        p.quadTo(scale(7), scale(6), scale(6), scale(4));
        p.moveTo(scale(6), scale(6));
        p.lineTo(scale(6), scale(8));
        p.moveTo(scale(8), scale(6));
        p.lineTo(scale(7), scale(7));
        p.lineTo(scale(8), scale(8));
        break;
      case SR_FLIPFLOP :
        p.moveTo(scale(7), scale(1));
        p.curveTo(scale(4), scale(2), scale(9), scale(4), scale(6), scale(5));
        p.moveTo(scale(6), scale(9));
        p.lineTo(scale(6), scale(6));
        p.quadTo(scale(9), scale(7), scale(7), scale(8));
        p.lineTo(scale(8), scale(9));
        break;
    }
    g2.draw(p);
  }
  
  private void paintRegisterIcon(Graphics2D g2) {
    for (int i = 2 ; i >= 0 ; i--) {
      g2.setColor(Color.WHITE);
      g2.fillRect(scale((i+1)*2), scale(4-i*2), scale(8), scale(12));
      g2.setColor(Color.BLACK);
      g2.drawRect(scale((i+1)*2), scale(4-i*2), scale(8), scale(12));
    }
    int[] xp = {scale(2),scale(5),scale(2)};
    int[] yp = {scale(11),scale(12),scale(13)};
    g2.drawPolygon(xp, yp, 3);
    g2.drawLine(scale(0), scale(12), scale(2), scale(12));
    g2.fillRect(0, scale(5), scale(2), scale(2));
    g2.fillRect(scale(10), scale(5), scale(6), scale(2));
  }
}
