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

import static com.cburch.logisim.gui.Strings.S;

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
  private final Font[] font;
  private final Paint[] paintSteady;
  private final float fadeStop;
  private final Lines lines;
  private int scroll;
  private int initialHeight; // computed in code based on above
  private int linesHeight; // computed in code based on above

  private class Lines extends ArrayList<CreditsLine> {
    public Lines separator() {
      add(new CreditsLine(CreditsLine.SEPARATOR, ""));
      return this;
    }

    public Lines title(String line) {
      add(new CreditsLine(CreditsLine.TITLE, line));
      return this;
    }

    public Lines h1(String line) {
      add(new CreditsLine(CreditsLine.H1, line));
      return this;
    }

    public Lines h2(String line) {
      add(new CreditsLine(CreditsLine.H2, line));
      return this;
    }

    public Lines url(String line) {
      add(new CreditsLine(CreditsLine.URL, line));
      return this;
    }

    public Lines text(String line) {
      add(new CreditsLine(CreditsLine.TEXT, line));
      return this;
    }

    public Lines img(Image img, int displayWidth) {
      add(new CreditsLine(CreditsLine.IMG, "", img, displayWidth));
      return this;
    }
  }

  public AboutCredits() {
    scroll = 0;
    setOpaque(false);

    int prefWidth = About.IMAGE_WIDTH + 2 * About.IMAGE_BORDER;
    int prefHeight = About.IMAGE_HEIGHT / 2 + About.IMAGE_BORDER;
    setPreferredSize(new Dimension(prefWidth, prefHeight));

    fadeStop = (float) (About.IMAGE_HEIGHT / 4.0);

    colorBase =
            new Color[] {
                    new Color(0x00, 0x80, 0x00), // TITLE
                    new Color(143, 0, 0), // H1
                    new Color(105, 0, 0), // H2
                    new Color(0xCC, 0x80, 0x00), // URL
                    new Color(48, 0, 96), // TEXT
                    new Color(0, 0, 0), // SEPARATOR
                    new Color(0, 0, 0), // IMG
            };
    font =
            new Font[] {
                    new Font("Sans Serif", Font.ITALIC | Font.BOLD, 30), // TITLE
                    new Font("Sans Serif", Font.ITALIC | Font.BOLD, 24), // H1
                    new Font("Sans Serif", Font.BOLD, 20), // H2
                    new Font("Sans Serif", Font.BOLD, 18), // URL
                    new Font("Sans Serif", Font.BOLD, 20), // TEXT
                    new Font("Sans Serif", Font.PLAIN, 10), // SEP
                    new Font("Sans Serif", Font.PLAIN, 10), // IMG
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

    linesHeight = 0; // computed in paintComponent()

    lines = new Lines();
    lines
            .separator()
            .title(Main.APP_DISPLAY_NAME)
            .h2("Copyright \u00A9" + Main.COPYRIGHT_YEAR + " " + Main.APP_NAME + " developers")
            .url(Main.APP_URL)
            .separator()
            .h1(S.get("creditsRoleFork"))
            .text("College of the Holy Cross")
            .url("https://www.holycross.edu")
            .text("Haute \u00C9cole Sp\u00E9cialis\u00E9e Bernoise/")
            .text("Berner Fachhochschule")
            .url("https://www.bfh.ch/")
            .text("Haute \u00C9cole du paysage, d'ing\u00E9nierie")
            .text("et d'architecture de Gen\u00E8ve")
            .url("https://hepia.hesge.ch")
            .text("Haute \u00C9cole d'Ing\u00E9nierie")
            .text("et de Gestion du Canton de Vaud")
            .url("https://www.heig-vd.ch/")
            .separator()
            .h1(S.get("creditsRoleOriginal"))
            .text("Carl Burch")
            .text("Hendrix College")
            .url("http://www.cburch.com/logisim/")
            .img(hendrixLogo, HENDRIX_WIDTH)
            .separator()
            .separator()
            .separator()
            .separator()
            .separator();
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
        if (index == 0) initialHeight = y;
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

      if (line.img == null) {
        int type = line.type;
        if (paint == null) {
          g.setColor(colorBase[type]);
        } else {
          ((Graphics2D) g).setPaint(paint[type]);
        }
        g.setFont(font[type]);
        int textWidth = fms[type].stringWidth(line.text);
        g.drawString(line.text, centerX - textWidth / 2, line.y - yPos);

      } else {
        int x = (width - line.imgWidth) / 2;
        g.drawImage(line.img, x, y, this);
      }
    }
  }

  public void setScroll(int value) {
    scroll = value;
    repaint();
  }

  private static class CreditsLine {
    private final int type;
    private final String text;
    private final Image img;
    private final int imgWidth;
    private int y;

    public static final int TITLE = 0;
    public static final int H1 = 1;
    public static final int H2 = 2;
    public static final int URL = 3;
    public static final int TEXT = 4;
    public static final int SEPARATOR = 5;
    public static final int IMG = 6;

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
