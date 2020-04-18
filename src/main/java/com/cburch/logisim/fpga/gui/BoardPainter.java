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

import static com.cburch.logisim.fpga.Strings.S;

import java.awt.Color;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;

import com.cburch.logisim.prefs.AppPreferences;
import com.cburch.logisim.util.GraphicsUtil;

public class BoardPainter {

  public static void errorBoardPainter(BoardManipulator bm, Graphics2D g) {
    g.setColor(Color.gray);
    g.fillRect(0, 0, bm.getWidth(), bm.getHeight());
    Graphics g1 = g.create();
    Font curfont = AppPreferences.getScaledFont(new Font(g1.getFont().getFontName(), Font.BOLD, 20));
    g1.setFont(curfont);
    g1.setColor(Color.red);
    GraphicsUtil.drawCenteredText(g1, S.get("BoardPainterError"), bm.getWidth()/2, bm.getHeight()/2);
  }
  
  public static void newBoardpainter(BoardManipulator bm, Graphics2D g) {
    g.setColor(Color.gray);
    g.fillRect(0, 0, bm.getWidth(), bm.getHeight());
    String message;
    int xpos;
    Font curfont =
        AppPreferences.getScaledFont(new Font(g.getFont().getFontName(), Font.BOLD, 20));
    g.setColor(Color.black);
    g.setFont(curfont);
    FontMetrics fm = g.getFontMetrics();
    message = S.get("BoardPainterMsg1");
    xpos = (bm.getWidth() - fm.stringWidth(message)) / 2;
    g.drawString(message, xpos, 100);
    message = S.get("BoardPainterMsg2");
    xpos = (bm.getWidth() - fm.stringWidth(message)) / 2;
    g.drawString(message, xpos, 200);
    message = S.fmt("BoardPainterMsg3", BoardManipulator.IMAGE_WIDTH, BoardManipulator.IMAGE_HEIGHT);
    xpos = (bm.getWidth() - fm.stringWidth(message)) / 2;
    g.drawString(message, xpos, (200 + (int) (1.5 * fm.getAscent())));
    message = S.get("BoardPainterMsg4");
    xpos = (bm.getWidth() - fm.stringWidth(message)) / 2;
    g.drawString(message, xpos, (200 + (int) (3 * fm.getAscent())));
    message = S.get("BoardPainterMsg5");
    xpos = (bm.getWidth() - fm.stringWidth(message)) / 2;
    g.drawString(message, xpos, (200 + (int) (4.5 * fm.getAscent())));
    message = S.fmt("BoardPainterMsg6", bm.getWidth(), bm.getHeight());
    xpos = (bm.getWidth() - fm.stringWidth(message)) / 2;
    g.drawString(message, xpos, (200 + (int) (6 * fm.getAscent())));
  }
  
}
