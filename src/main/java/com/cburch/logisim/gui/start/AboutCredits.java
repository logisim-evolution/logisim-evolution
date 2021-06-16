/*
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

package com.cburch.logisim.gui.start;

import static com.cburch.logisim.gui.Strings.S;

import com.cburch.logisim.Main;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.GradientPaint;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.Paint;
import java.net.URL;
import java.util.ArrayList;
import javax.swing.JComponent;

class AboutCredits extends JComponent {
  private static final long serialVersionUID = 1L;

  /** Speed of how quickly the scrolling occurs */
  private static final int MILLIS_PER_PIXEL = 20;
  /**
   * Path to Hendrix College's logo - if you want your own logo included, please add it separately
   * rather than replacing this.
   */
  private static final String HENDRIX_PATH = "resources/logisim/hendrix.png";
  private static final int HENDRIX_WIDTH = 50;
  private final Color[] colorBase;
  private final Paint[] paintSteady;
  private final Font[] font;
  private final float fadeStop;
  private final ArrayList<CreditsLine> lines;
  private int scroll;
  private int initialLines; // number of lines to show in initial freeze
  private int initialHeight; // computed in code based on above
  private int linesHeight; // computed in code based on above
  public AboutCredits() {
    scroll = 0;
    setOpaque(false);

    int prefWidth = About.IMAGE_WIDTH + 2 * About.IMAGE_BORDER;
    int prefHeight = About.IMAGE_HEIGHT / 2 + About.IMAGE_BORDER;
    setPreferredSize(new Dimension(prefWidth, prefHeight));

    fadeStop = (float) (About.IMAGE_HEIGHT / 4.0);

    colorBase =
        new Color[] {
          new Color(143, 0, 0), new Color(48, 0, 96), new Color(48, 0, 96),
        };
    font =
        new Font[] {
          new Font("Sans Serif", Font.ITALIC, 20),
          new Font("Sans Serif", Font.BOLD, 24),
          new Font("Sans Serif", Font.BOLD, 18),
        };
    paintSteady = new Paint[colorBase.length];
    for (int i = 0; i < colorBase.length; i++) {
      Color hue = colorBase[i];
      paintSteady[i] = new GradientPaint(0.0f, 0.0f, derive(hue, 0), 0.0f, fadeStop, hue);
    }

    URL url = AboutCredits.class.getClassLoader().getResource(HENDRIX_PATH);
    Image hendrixLogo = null;
    if (url != null) {
      hendrixLogo = getToolkit().createImage(url);
    }

    lines = new ArrayList<>();
    linesHeight = 0; // computed in paintComponent
    lines.add(new CreditsLine(CreditsLine.HEAD, ""));
    lines.add(new CreditsLine(CreditsLine.HEAD, Main.APP_DISPLAY_NAME));
    lines.add(new CreditsLine(CreditsLine.WWW, Main.APP_URL));

    lines.add(new CreditsLine(CreditsLine.HEAD, S.get("creditsRoleFork")));
    lines.add(new CreditsLine(CreditsLine.B, "College of the Holy Cross"));
    lines.add(new CreditsLine(CreditsLine.WWW, "https://www.holycross.edu"));
    lines.add(new CreditsLine(CreditsLine.B, "Haute \u00C9cole Sp\u00E9cialis\u00E9e Bernoise/"));
    lines.add(new CreditsLine(CreditsLine.B, "Berner Fachhochschule"));
    lines.add(new CreditsLine(CreditsLine.WWW, "https://www.bfh.ch"));
    lines.add(new CreditsLine(CreditsLine.B, "Haute \u00C9cole du paysage, d'ing\u00E9nierie"));
    lines.add(new CreditsLine(CreditsLine.B, "et d'architecture de Gen\u00E8ve"));
    lines.add(new CreditsLine(CreditsLine.WWW, "https://hepia.hesge.ch"));
    lines.add(new CreditsLine(CreditsLine.B, "Haute \u00C9cole d'Ing\u00E9nierie"));
    lines.add(new CreditsLine(CreditsLine.B, "et de Gestion du Canton de Vaud"));
    lines.add(new CreditsLine(CreditsLine.WWW, "https://www.heig-vd.ch"));

    /*
     * If you fork Logisim, feel free to change the above lines, but please
     * do not change these last four lines!
     */
    lines.add(
        new CreditsLine(
            CreditsLine.HEAD, S.get("creditsRoleOriginal"), hendrixLogo, HENDRIX_WIDTH));
    lines.add(new CreditsLine(CreditsLine.B, "Carl Burch"));
    lines.add(new CreditsLine(CreditsLine.WWW, "Hendrix College"));
    lines.add(new CreditsLine(CreditsLine.B, "www.cburch.com/logisim/"));
  }

  private Color derive(Color base, int alpha) {
    return new Color(base.getRed(), base.getGreen(), base.getBlue(), alpha);
  }

  @Override
  protected void paintComponent(Graphics g) {
    FontMetrics[] fms = new FontMetrics[font.length];
    for (int i = 0; i < fms.length; i++) {
      fms[i] = g.getFontMetrics(font[i]);
    }
    if (linesHeight == 0) {
      int y = 0;
      int index = -1;
      for (CreditsLine line : lines) {
        index++;
        if (index == initialLines) initialHeight = y;
        if (line.type == 0) y += 10;
        FontMetrics fm = fms[line.type];
        line.y = y + fm.getAscent();
        y += fm.getHeight();
      }
      linesHeight = y;
    }

    Paint[] paint = paintSteady;
    int yPos = 0;
    int height = getHeight();
    int initY = Math.min(0, initialHeight - height + About.IMAGE_BORDER);
    int maxY = linesHeight - height - initY;
    int totalMillis = (linesHeight + height) * MILLIS_PER_PIXEL;
    int offs = scroll % totalMillis;
    if (offs < maxY * MILLIS_PER_PIXEL) {
      // scrolling through credits
      yPos = initY + offs / MILLIS_PER_PIXEL;
    } else if (offs < maxY * MILLIS_PER_PIXEL) {
      // freezing at bottom of scroll
      yPos = initY + maxY;
    } else if (offs < (linesHeight - initY) * MILLIS_PER_PIXEL) {
      // scrolling bottom off screen
      yPos = initY + offs / MILLIS_PER_PIXEL;
    } else {
      // scrolling next credits onto screen
      int millis = offs - (linesHeight - initY) * MILLIS_PER_PIXEL;
      paint = null;
      yPos = -height + millis / MILLIS_PER_PIXEL;
    }

    int width = getWidth();
    int centerX = width / 2;
    maxY = getHeight();
    for (CreditsLine line : lines) {
      int y = line.y - yPos;
      if (y < -100 || y > maxY + 50) continue;

      int type = line.type;
      if (paint == null) {
        g.setColor(colorBase[type]);
      } else {
        ((Graphics2D) g).setPaint(paint[type]);
      }
      g.setFont(font[type]);
      int textWidth = fms[type].stringWidth(line.text);
      g.drawString(line.text, centerX - textWidth / 2, line.y - yPos);

      Image img = line.img;
      if (img != null) {
        int x = width - line.imgWidth - About.IMAGE_BORDER;
        int top = y - fms[type].getAscent();
        g.drawImage(img, x, top, this);
      }
    }
  }

  public void setScroll(int value) {
    scroll = value;
    repaint();
  }

  private static class CreditsLine {
    public final static int HEAD = 0;
    public final static int B = 1;
    public final static int WWW = 2;

    private final int type;
    private final String text;
    private final Image img;
    private final int imgWidth;
    private int y;

    public CreditsLine(int type, String text) {
      this(type, text, null, 0);
    }

    public CreditsLine(int type, String text, Image img, int imgWidth) {
      this.y = 0;
      this.type = type;
      this.text = text;
      this.img = img;
      this.imgWidth = imgWidth;
    }
  }
}
