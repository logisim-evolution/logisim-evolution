/*
 * Logisim-evolution - digital logic design tool and simulator
 * Copyright by the Logisim-evolution developers
 *
 * https://github.com/logisim-evolution/
 *
 * This is free software released under GNU GPLv3 license
 */

package com.cburch.logisim.gui.main;

import com.cburch.logisim.circuit.Circuit;
import com.cburch.logisim.circuit.SubcircuitFactory;
import com.cburch.logisim.comp.ComponentDrawContext;
import com.cburch.logisim.prefs.AppPreferences;
import com.cburch.logisim.tools.AddTool;
import com.cburch.logisim.tools.Tool;
import com.cburch.logisim.vhdl.base.VhdlEntity;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Pattern;
import javax.swing.Icon;
import javax.swing.ImageIcon;
import javax.swing.UIManager;

/**
 * High-performance glyph painter and multi-resolution cache for component schematic symbols,
 * DIP chips, abbreviations, and subcircuits.
 */
public class ComponentGlyphCache {

  private static final Pattern CHIP_PATTERN = Pattern.compile(".*(74[A-Za-z0-9]+|[0-9]{4,5}).*");

  private static final Map<String, Image> cache = new HashMap<>();

  static {
    final PropertyChangeListener themeListener = (PropertyChangeEvent evt) -> clearCache();
    AppPreferences.GATE_SHAPE.addPropertyChangeListener(themeListener);
    AppPreferences.LookAndFeel.addPropertyChangeListener(themeListener);
  }

  public static synchronized void clearCache() {
    cache.clear();
  }

  public static Icon getIcon(Tool tool, int targetSize) {
    final var img = getImage(tool, targetSize);
    return (img != null) ? new ImageIcon(img) : null;
  }

  public static synchronized Image getImage(Tool tool, int targetSize) {
    if (tool == null) return null;

    final var name = tool.getDisplayName();
    final var isDark = AppPreferences.isDarkTheme(AppPreferences.LookAndFeel.get());
    // adapt component color
    final var adapt = false;
    final var paletteKind = "default";
    final var scale = AppPreferences.getScaled(1.0f);
    final var cacheKey = name + "@" + targetSize + "@" + (int) (scale * 100) + "@" + isDark + "@" + adapt + "@" + paletteKind;

    var cached = cache.get(cacheKey);
    if (cached != null) {
      return cached;
    }

    final var pixelSize = (int) Math.ceil(targetSize * scale);
    final var img = new BufferedImage(pixelSize, pixelSize, BufferedImage.TYPE_INT_ARGB);
    final var g2 = img.createGraphics();

    g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
    g2.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
    g2.setRenderingHint(RenderingHints.KEY_STROKE_CONTROL, RenderingHints.VALUE_STROKE_PURE);
    g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);

    final var fgColor = isDark ? new Color(220, 220, 220) : new Color(40, 40, 40);
    g2.setColor(fgColor);

    // 1. Check if this is a subcircuit / project circuit
    if (tool instanceof AddTool addTool) {
      final var factory = addTool.getFactory(false);
      if (factory instanceof SubcircuitFactory sub) {
        paintCircuitThumbnail(g2, sub.getSubcircuit(), pixelSize, fgColor);
        g2.dispose();
        cache.put(cacheKey, img);
        return img;
      } else if (factory instanceof VhdlEntity vhdl) {
        paintVhdlThumbnail(g2, vhdl.getContent() != null ? vhdl.getContent().getName() : "VHDL", pixelSize, fgColor);
        g2.dispose();
        cache.put(cacheKey, img);
        return img;
      }
    }

    // 2. Check wiring components that had legacy white-background bitmap icons
    final var lowerName = name.toLowerCase();
    if (lowerName.equals("clock")) {
      paintClockGlyph(g2, pixelSize, fgColor);
      g2.dispose();
      cache.put(cacheKey, img);
      return img;
    } else if (lowerName.equals("por") || lowerName.contains("power-on reset")) {
      paintPorGlyph(g2, pixelSize, fgColor);
      g2.dispose();
      cache.put(cacheKey, img);
      return img;
    } else if (lowerName.equals("noconnect") || lowerName.contains("do not connect") || lowerName.contains("no connection")) {
      paintNoConnectGlyph(g2, pixelSize, fgColor);
      g2.dispose();
      cache.put(cacheKey, img);
      return img;
    }

    // 3. Check if this is a TTL or part-number chip (e.g. 7400, 7404, 74138)
    final var chipNum = extractChipNumber(name);
    if (chipNum != null) {
      paintChipGlyph(g2, chipNum, pixelSize, fgColor);
      g2.dispose();
      cache.put(cacheKey, img);
      return img;
    }

    // 3. Check if this is a known box component that needs an abbreviation badge
    final var abbrev = getAbbreviation(name);
    if (abbrev != null) {
      paintBoxAbbrevGlyph(g2, abbrev, pixelSize, fgColor);
      g2.dispose();
      cache.put(cacheKey, img);
      return img;
    }

    // 4. Default: invoke factory paintIcon vector rendering scaled cleanly to bounds
    try {
      final var baseSize = AppPreferences.BOX_SIZE;
      final var sx = (double) pixelSize / baseSize;
      final var sy = (double) pixelSize / baseSize;
      g2.scale(sx, sy);

      final var ctx = new ComponentDrawContext(null, null, null, g2, g2);
      tool.paintIcon(ctx, 0, 0);
    } catch (Exception ex) {
      // Fallback: draw generic component box
      paintBoxAbbrevGlyph(g2, name.length() > 3 ? name.substring(0, 3).toUpperCase() : name.toUpperCase(), pixelSize, fgColor);
    }

    g2.dispose();
    cache.put(cacheKey, img);
    return img;
  }

  private static String extractChipNumber(String name) {
    if (name == null) return null;
    final var matcher = CHIP_PATTERN.matcher(name);
    if (matcher.matches()) {
      return matcher.group(1);
    }
    return null;
  }

  private static String getAbbreviation(String name) {
    if (name == null) return null;
    final var lower = name.toLowerCase();
    if (lower.equals("register")) return "REG";
    if (lower.equals("counter")) return "CTR";
    if (lower.equals("shift register")) return "SHR";
    if (lower.equals("ram")) return "RAM";
    if (lower.equals("rom")) return "ROM";
    if (lower.equals("random")) return "RND";
    if (lower.equals("d flip-flop")) return "D";
    if (lower.equals("t flip-flop")) return "T";
    if (lower.equals("j-k flip-flop")) return "JK";
    if (lower.equals("s-r flip-flop")) return "SR";
    if (lower.equals("decoder")) return "DEC";
    if (lower.equals("priority encoder")) return "ENC";
    if (lower.equals("bit selector")) return "SEL";
    if (lower.equals("comparator")) return "CMP";
    if (lower.equals("shifter")) return "SFT";
    return null;
  }

  private static void paintChipGlyph(Graphics2D g2, String partNum, int size, Color color) {
    int padX = Math.max(2, size / 10);
    int padY = Math.max(3, size / 5);
    int w = size - 2 * padX;
    int h = size - 2 * padY;
    int arc = Math.max(2, size / 12);

    // DIP body
    g2.setColor(color);
    g2.drawRoundRect(padX, padY, w, h, arc, arc);

    // Pin stubs
    int pinW = Math.max(1, size / 16);
    int pinLen = Math.max(2, size / 8);
    int numPins = 4;
    int step = w / (numPins + 1);

    for (int i = 1; i <= numPins; i++) {
      int px = padX + i * step;
      g2.fillRect(px - pinW / 2, padY - pinLen, pinW, pinLen);
      g2.fillRect(px - pinW / 2, padY + h, pinW, pinLen);
    }

    // Monospace label
    int fontSize = Math.max(8, (int) (size * (partNum.length() > 4 ? 0.24 : 0.28)));
    g2.setFont(new Font(Font.MONOSPACED, Font.BOLD, fontSize));
    final var fm = g2.getFontMetrics();
    int tx = (size - fm.stringWidth(partNum)) / 2;
    int ty = (size - fm.getHeight()) / 2 + fm.getAscent();
    g2.drawString(partNum, tx, ty);
  }

  private static void paintBoxAbbrevGlyph(Graphics2D g2, String abbrev, int size, Color color) {
    int pad = Math.max(3, size / 7);
    int w = size - 2 * pad;
    int h = size - 2 * pad;
    int arc = Math.max(3, size / 8);

    g2.setColor(color);
    g2.drawRoundRect(pad, pad, w, h, arc, arc);

    // Left & right input/output pin stubs
    int stubLen = Math.max(2, pad - 1);
    g2.drawLine(0, pad + h / 3, pad, pad + h / 3);
    g2.drawLine(0, pad + 2 * h / 3, pad, pad + 2 * h / 3);
    g2.drawLine(pad + w, pad + h / 2, size, pad + h / 2);

    int fontSize = Math.max(8, (int) (size * (abbrev.length() > 3 ? 0.24 : 0.30)));
    g2.setFont(new Font(Font.MONOSPACED, Font.BOLD, fontSize));
    final var fm = g2.getFontMetrics();
    int tx = (size - fm.stringWidth(abbrev)) / 2;
    int ty = (size - fm.getHeight()) / 2 + fm.getAscent();
    g2.drawString(abbrev, tx, ty);
  }

  private static void paintCircuitThumbnail(Graphics2D g2, Circuit circuit, int size, Color color) {
    int pad = Math.max(3, size / 7);
    int w = size - 2 * pad;
    int h = size - 2 * pad;
    int arc = Math.max(3, size / 8);

    Color accent = UIManager.getColor("Component.accentColor");
    if (accent == null) accent = new Color(53, 132, 228);

    g2.setColor(accent);
    g2.drawRoundRect(pad, pad, w, h, arc, arc);

    // Subcircuit pin notches
    g2.drawLine(0, pad + h / 4, pad, pad + h / 4);
    g2.drawLine(0, pad + 3 * h / 4, pad, pad + 3 * h / 4);
    g2.drawLine(pad + w, pad + h / 2, size, pad + h / 2);

    String name = (circuit != null) ? circuit.getName() : "CIRC";
    String tag = name.length() > 3 ? name.substring(0, 3).toUpperCase() : name.toUpperCase();

    int fontSize = Math.max(8, (int) (size * 0.26));
    g2.setFont(new Font(Font.SANS_SERIF, Font.BOLD, fontSize));
    final var fm = g2.getFontMetrics();
    int tx = (size - fm.stringWidth(tag)) / 2;
    int ty = (size - fm.getHeight()) / 2 + fm.getAscent();
    g2.drawString(tag, tx, ty);
  }

  private static void paintVhdlThumbnail(Graphics2D g2, String name, int size, Color color) {
    paintBoxAbbrevGlyph(g2, "HDL", size, color);
  }

  private static void paintClockGlyph(Graphics2D g2, int size, Color color) {
    int pad = Math.max(2, size / 8);
    int arc = Math.max(2, size / 6);
    g2.setColor(color);
    g2.setStroke(new BasicStroke(Math.max(1.2f, size / 14.0f), BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
    g2.drawRoundRect(pad, pad, size - 2 * pad, size - 2 * pad, arc, arc);

    int yMid = size / 2;
    int yTop = pad + (size - 2 * pad) * 3 / 10;
    int yBot = size - pad - (size - 2 * pad) * 3 / 10;
    int x1 = pad + (size - 2 * pad) * 2 / 10;
    int x2 = pad + (size - 2 * pad) * 5 / 10;
    int x3 = pad + (size - 2 * pad) * 8 / 10;

    int[] xs = {x1, x1, x2, x2, x3, x3};
    int[] ys = {yMid, yTop, yTop, yBot, yBot, yMid};
    g2.drawPolyline(xs, ys, 6);
  }

  private static void paintPorGlyph(Graphics2D g2, int size, Color color) {
    int pad = Math.max(2, size / 8);
    int arc = Math.max(2, size / 6);
    g2.setColor(color);
    g2.setStroke(new BasicStroke(Math.max(1.2f, size / 14.0f), BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
    g2.drawRoundRect(pad, pad, size - 2 * pad, size - 2 * pad, arc, arc);

    int yTop = pad + (size - 2 * pad) * 3 / 10;
    int yBot = size - pad - (size - 2 * pad) * 3 / 10;
    int x1 = pad + (size - 2 * pad) * 2 / 10;
    int xMid = size / 2;
    int x2 = size - pad - (size - 2 * pad) * 2 / 10;

    int[] xs = {x1, xMid, xMid, x2};
    int[] ys = {yTop, yTop, yBot, yBot};
    g2.drawPolyline(xs, ys, 4);
  }

  private static void paintNoConnectGlyph(Graphics2D g2, int size, Color color) {
    int pad = Math.max(3, size / 4);
    g2.setColor(new Color(225, 50, 50));
    g2.setStroke(new BasicStroke(Math.max(2.0f, size / 7.0f), BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
    g2.drawLine(pad, pad, size - pad, size - pad);
    g2.drawLine(pad, size - pad, size - pad, pad);
  }
}
