package com.cburch.logisim.gui.icons.symbolic;

import com.cburch.logisim.prefs.AppPreferences;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Component;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import javax.swing.Icon;
import javax.swing.UIManager;

/**
 * Base class for modern vector icons.
 * Automatically adapts to light/dark themes and HiDPI display scaling.
 */
public abstract class VectorBaseIcon implements Icon {

  private final int baseSize;
  private Color fixedColor = null;

  public VectorBaseIcon() {
    this(AppPreferences.IconSize);
  }

  public VectorBaseIcon(int baseSize) {
    this.baseSize = baseSize;
  }

  public void setColor(Color color) {
    this.fixedColor = color;
  }

  @Override
  public int getIconWidth() {
    return AppPreferences.getScaled(baseSize);
  }

  @Override
  public int getIconHeight() {
    return AppPreferences.getScaled(baseSize);
  }

  public Color getSemanticColor() {
    return null;
  }

  public static Color toDisabled(Color c) {
    if (c == null) {
      return new Color(128, 128, 128, Math.round(255 * 0.40f));
    }
    int gray = (int) Math.round(0.299 * c.getRed() + 0.587 * c.getGreen() + 0.114 * c.getBlue());
    return new Color(gray, gray, gray, Math.round(255 * 0.40f));
  }

  public static Color getRoleColor(String key, Color lightFallback, Color darkFallback, boolean isEnabled) {
    Color c = UIManager.getColor(key);
    if (c == null) {
      c = AppPreferences.isDarkTheme(AppPreferences.LookAndFeel.get()) ? darkFallback : lightFallback;
    }
    return isEnabled ? c : toDisabled(c);
  }

  @Override
  public void paintIcon(Component c, Graphics g, int x, int y) {
    final var g2 = (Graphics2D) g.create();
    g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
    g2.setRenderingHint(RenderingHints.KEY_STROKE_CONTROL, RenderingHints.VALUE_STROKE_PURE);

    final var isEnabled = (c == null || c.isEnabled());
    Color fg;
    if (fixedColor != null) {
      fg = isEnabled ? fixedColor : toDisabled(fixedColor);
    } else {
      final var semantic = getSemanticColor();
      if (!isEnabled) {
        if (semantic != null) {
          fg = toDisabled(semantic);
        } else {
          Color base = (c != null && c.getForeground() != null) ? c.getForeground() : UIManager.getColor("Button.foreground");
          if (base == null) {
            base = AppPreferences.isDarkTheme(AppPreferences.LookAndFeel.get()) ? new Color(235, 235, 235) : new Color(38, 40, 44);
          }
          fg = toDisabled(base);
        }
      } else if (semantic != null) {
        fg = semantic;
      } else if (c != null && c.getForeground() != null) {
        fg = c.getForeground();
      } else {
        Color defaultFg = UIManager.getColor("Button.foreground");
        if (defaultFg == null) defaultFg = UIManager.getColor("Label.foreground");
        fg = (defaultFg != null) ? defaultFg : (AppPreferences.isDarkTheme(AppPreferences.LookAndFeel.get()) ? new Color(235, 235, 235) : new Color(38, 40, 44));
      }
    }

    g2.setColor(fg);
    g2.translate(x, y);

    float scale = (float) getIconWidth() / (float) baseSize;
    if (scale != 1.0f) {
      g2.scale(scale, scale);
    }

    paintSymbolic(g2, baseSize, fg, c);
    g2.dispose();
  }

  protected void paintSymbolic(Graphics2D g2, int size, Color color, Component c) {
    paintSymbolic(g2, size, color);
  }

  protected void paintSymbolic(Graphics2D g2, int size, Color color) {
  }

  public static BasicStroke createStroke(float width) {
    return new BasicStroke(width, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND);
  }

  protected float scale(float val) {
    return val;
  }

  protected void drawKnockoutBadge(Graphics2D g2, float cx, float cy, float badgeRadius, float knockoutGap, Color fg, Component c) {
    Color bg = null;
    if (c != null) {
      bg = c.getBackground();
    }
    if (bg == null) {
      bg = AppPreferences.isDarkTheme(AppPreferences.LookAndFeel.get()) ? new Color(0x2E, 0x2E, 0x2E) : Color.WHITE;
    }

    // 1. Fill knockout background circle
    float kR = badgeRadius + knockoutGap;
    g2.setColor(bg);
    g2.fill(new java.awt.geom.Ellipse2D.Float(cx - kR, cy - kR, kR * 2, kR * 2));

    // 2. Draw plus cross in badge
    g2.setColor(fg);
    g2.setStroke(createStroke(1.75f));
    float arm = badgeRadius * 0.70f;
    g2.draw(new java.awt.geom.Line2D.Float(cx - arm, cy, cx + arm, cy));
    g2.draw(new java.awt.geom.Line2D.Float(cx, cy - arm, cx, cy + arm));
  }
}
