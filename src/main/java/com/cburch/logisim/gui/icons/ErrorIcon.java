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
import java.awt.Component;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.font.TextLayout;

import javax.swing.Icon;

import com.cburch.logisim.prefs.AppPreferences;

public class ErrorIcon implements Icon {

  private int wh;
  private boolean forwardArrow = false;
  private boolean backwardArrow = false;
  
  public ErrorIcon() {
    wh = AppPreferences.getIconSize();
  }

  public ErrorIcon(double scale) {
    wh = (int)AppPreferences.getScaled(scale*AppPreferences.getIconSize());
  }

  public ErrorIcon(int size) {
    wh = (int)AppPreferences.getScaled(size);
  }
  
  public ErrorIcon(boolean forward,boolean backward) {
    wh = AppPreferences.getIconSize();
    if (forward) {
      forwardArrow = true;
      backwardArrow = false;
    } else {
      forwardArrow = false;
      backwardArrow = backward;
    }
  }

  @Override
  public void paintIcon(Component c, Graphics g, int x, int y) {
    Graphics2D g2 = (Graphics2D) g.create();
    g2.translate(x, y);
    int mywh = !forwardArrow && !backwardArrow ? wh : (3*wh)>>2;
    int xoff = !forwardArrow && !backwardArrow ? 0 : wh>>3;
    double trd = mywh/3;
    int[] xpos = {xoff,xoff+(int)trd,xoff+(int)(2*trd),xoff+mywh-1,xoff+mywh-1,xoff+(int)(2*trd),xoff+(int)trd,xoff};
    int[] ypos = {(int)trd,0,0,(int)trd,(int)(2*trd),mywh-1,mywh-1,(int)(2*trd)};
    g2.setColor(Color.RED.brighter().brighter());
    g2.fillPolygon(xpos, ypos, 8);
    g2.setStroke(new BasicStroke(scale(1)));
    g2.setColor(Color.RED.darker().darker());
    g2.drawPolygon(xpos, ypos, 8);
    g2.setColor(Color.WHITE);
    Font f = g2.getFont().deriveFont((float)mywh/(float)1.3).deriveFont(Font.BOLD);
    TextLayout t = new TextLayout("X",f,g2.getFontRenderContext());
    float xc = (float)mywh/(float)2-(float)t.getBounds().getCenterX()+(float)xoff;
    float yc = (float)mywh/(float)2-(float)t.getBounds().getCenterY();
    t.draw(g2, xc, yc);
    if (forwardArrow) {
      g2.setColor(Color.BLACK);
      int five = (5*wh)>>3;
      int six = (6*wh)>>3;
      int seven = (7*wh)>>3;
      int[] axpos = {xoff,five,five,seven,five,five,xoff};
      int yoff = AppPreferences.getScaled(1);
      int[] aypos = {seven-yoff,seven-yoff,six,seven,wh-1,seven+yoff,seven+yoff};
      g2.fillPolygon(axpos, aypos, 7);
    }
    if (backwardArrow) {
      g2.setColor(Color.BLACK);
      int three = (3*wh)>>3;
      int six = (6*wh)>>3;
      int seven = (7*wh)>>3;
      int[] axpos = {seven,three,three,xoff,three,three,seven};
      int yoff = AppPreferences.getScaled(1);
      int[] aypos = {seven-yoff,seven-yoff,six,seven,wh-1,seven+yoff,seven+yoff};
      g2.fillPolygon(axpos, aypos, 7);
    }
    g2.dispose();
  }
  
  public static int scale(int v) {
    return AppPreferences.getScaled(v);
  }
  
  @Override
  public int getIconWidth() {
    return wh;
  }

  @Override
  public int getIconHeight() {
    return wh;
  }

}
