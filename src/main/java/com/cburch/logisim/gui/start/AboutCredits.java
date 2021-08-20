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
import com.cburch.logisim.generated.BuildInfo;
import com.cburch.logisim.util.StringUtil;
import java.awt.Color;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.GradientPaint;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.image.ImageObserver;
import java.net.URL;
import java.util.ArrayList;
import javax.swing.ImageIcon;
import javax.swing.JComponent;

class AboutCredits extends JComponent {
  private static final long serialVersionUID = 1L;
  /** Speed of how quickly the scrolling occurs. */
  private static final int MILLIS_PER_RASTER = 20;

  /**
   * Path to Hendrix College's logo - if you want your own logo included, please add it separately
   * rather than replacing this.
   */
  private static final String HENDRIX_LOGO_PATH = "resources/logisim/hendrix.png";

  private final Lines lines;

  public AboutCredits(int width, int height) {
    lines = new Lines();
    lines
        .title(Main.APP_DISPLAY_NAME)
        .h2(StringUtil.format("Copyright \u00A92001-%s %s developers", BuildInfo.year, Main.APP_NAME))
        .url(Main.APP_URL)
        .space()
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
        .space()
        .h1(S.get("creditsRoleOriginal"))
        .text("Carl Burch")
        .text("Hendrix College")
        .url("http://www.cburch.com/logisim/")
        .img(getClass().getClassLoader().getResource(HENDRIX_LOGO_PATH))
        .space()
        .space()
        .h1(S.get("creditsBuildInfo"))
        .text(S.get("creditsCompiled", BuildInfo.dateIso8601))
        .text(BuildInfo.buildId);
  }

  private long startMillis = 0;

  @Override
  protected void paintComponent(Graphics g) {
    if (startMillis == 0) {
      startMillis = System.currentTimeMillis();
      lines.initialize(getGraphics(), getWidth(), getHeight());
    }

    final var height = getHeight();
    final var maxOffsetY = lines.totalScrollLinesHeight + height;
    final var offsetY = ((int) (System.currentTimeMillis() - startMillis) / MILLIS_PER_RASTER) % maxOffsetY;
    final var yPos = offsetY - height;

    for (final var line : lines) {
      final var y = line.startY - yPos;
      // do not attempt to draw line contents if it'd be outside of visible area anyway
      if ((y < -line.displayHeight) && (y > height + line.displayHeight)) continue;

      // Drawing of each line is kept outside its class for performance reasons.
      final var cls = line.getClass();
      if (cls.equals(ImgLine.class)) {
        g.drawImage(line.img.getImage(), line.x, y, this);
      } else if (cls.equals(TextLine.class)) {
        ((Graphics2D) g).setPaint(line.paint);
        g.setFont(line.font);
        g.drawString(line.text, line.x, y);
      }
    }
  }

  private class Lines extends ArrayList<CreditsLine> {
    private static final int SPACE_HEIGHT = 20;
    private boolean initialized = false;

    private int totalScrollLinesHeight = 0;

    public void initialize(Graphics g, int displayWidth, int displayHeight) {
      if (initialized) return;

      // Lets's calculate at what Y value given lines should be drawn
      for (final var line : lines) {
        line.init(g, displayWidth, displayHeight, totalScrollLinesHeight);
        totalScrollLinesHeight += line.displayHeight;
      }

      initialized = true;
    }

    public Lines space() {
      add(new SpaceLine());
      return this;
    }

    public Lines title(String text) {
      add(
          new TextLine(
              new Font("Sans Serif", Font.ITALIC | Font.BOLD, 30),
              new Color(0x00, 0x80, 0x00),
              text));
      return this;
    }

    public Lines h1(String text) {
      add(
          new TextLine(
              new Font("Sans Serif", Font.ITALIC | Font.BOLD, 24),
              new Color(0x8F, 0x00, 0x00),
              text));
      return this;
    }

    public Lines h2(String text) {
      add(new TextLine(new Font("Sans Serif", Font.BOLD, 20), new Color(0x69, 0x00, 0x00), text));
      return this;
    }

    public Lines url(String text) {
      add(new TextLine(new Font("Sans Serif", Font.BOLD, 18), new Color(0xCC, 0x80, 0x00), text));
      return this;
    }

    public Lines text(String text) {
      add(new TextLine(new Font("Sans Serif", Font.BOLD, 20), new Color(0x30, 0x00, 0x60), text));
      return this;
    }

    public Lines img(URL url) {
      add(new ImgLine(url));
      return this;
    }
  }

  private static class TextLine extends CreditsLine {
    public TextLine(Font font, Color color, String text) {
      this.font = font;
      this.color = color;
      this.text = text;
    }

    @Override
    public void init(Graphics g, int displayAreaWidth, int displayAreaHeight, int currentY) {
      super.init(g, displayAreaWidth, displayAreaHeight, currentY);

      displayWidth = fm.stringWidth(text);
      displayHeight = fm.getHeight();
      // texts are drawn up Y axis, so we need to adjust startY accordingly
      startY = currentY + displayHeight;
      center(displayAreaWidth);
    }
  }

  private static class SpaceLine extends CreditsLine {
    public SpaceLine() {
      this.displayHeight = 20;
    }
  }

  private static class ImgLine extends CreditsLine implements ImageObserver {
    public ImgLine(URL url) {
      this.img = new ImageIcon(url);
      displayHeight = img.getIconHeight();
      displayWidth = img.getIconWidth();
    }

    @Override
    public boolean imageUpdate(Image image, int i, int i1, int i2, int i3, int i4) {
      return false;
    }

    @Override
    public void init(Graphics g, int displayAreaWidth, int displayAreaHeight, int currentY) {
      super.init(g, displayAreaWidth, displayAreaHeight, currentY);

      // total padding around image top/bottom
      final var padding = 20;
      displayHeight = img.getIconHeight();
      displayWidth = img.getIconWidth();
      startY = currentY + (padding / 2);
      center(displayAreaWidth);
    }
  }

  private abstract static class CreditsLine {
    protected String text = null;
    protected ImageIcon img = null;

    // these will be calculated in prepare phase
    protected int displayWidth = 0;
    protected int displayHeight = 0;
    protected int startY = 0;
    protected int x = 0;

    protected Graphics g = null;
    protected Color color = null;
    protected GradientPaint paint = null;
    protected Font font = null;
    protected FontMetrics fm = null;

    public void center(int displayAreaWidth) {
      x = (displayAreaWidth - displayWidth) >> 1;
    }

    /**
     * As some internals depend on Graphics context which can not be available at creation time this
     * method must be called before using Line to ensure all internals are initialized.
     *
     * @param g Graphics context to use
     */
    public void init(Graphics g, int displayAreaWidth, int displayAreaHeight, int currentY) {
      this.g = g;

      if (font != null) {
        this.fm = g.getFontMetrics(font);
      }

      if (color != null) {
        final var alpha = displayAreaHeight / 4;
        final var derrived = new Color(color.getRed(), color.getGreen(), color.getBlue(), alpha);
        paint = new GradientPaint(0.0f, 0.0f, derrived, 0.0f, alpha, color);
      }
    }
  } // CreditsLine
} // AboutCredits
