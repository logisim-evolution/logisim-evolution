/*
 * Logisim-evolution - digital logic design tool and simulator
 * Copyright by the Logisim-evolution developers
 *
 * https://github.com/logisim-evolution/
 *
 * This is free software released under GNU GPLv3 license
 */

package com.cburch.logisim.std.memory;

import com.cburch.logisim.data.AttributeSet;
import com.cburch.logisim.data.BitWidth;
import com.cburch.logisim.data.Bounds;
import com.cburch.logisim.data.Direction;
import com.cburch.logisim.instance.Instance;
import com.cburch.logisim.instance.InstancePainter;
import com.cburch.logisim.instance.Port;
import com.cburch.logisim.instance.StdAttr;
import com.cburch.logisim.prefs.AppPreferences;
import com.cburch.logisim.util.GraphicsUtil;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics;
import java.awt.Graphics2D;

/**
 * Geometry, port layout and painting for the multi-port, read-only mode of the ROM component.
 *
 * <p>Every port has an independent address input and an independent data output. There is no write
 * enable, no clock and no data input: a ROM never changes its contents while the circuit runs.
 */
public final class RomMultiPortAppearance {

  private RomMultiPortAppearance() {}

  public static int getNrPorts(AttributeSet attrs) {
    return RamAttributes.getNrPorts(attrs);
  }

  public static boolean isMultiPort(AttributeSet attrs) {
    return getNrPorts(attrs) > 1;
  }

  /** Number of data words returned by a single read access (line size). */
  public static int getDataLines(AttributeSet attrs) {
    final Object line = attrs.getValue(Mem.LINE_ATTR);
    if (Mem.DUAL.equals(line)) return 2;
    if (Mem.QUAD.equals(line)) return 4;
    if (Mem.OCTO.equals(line)) return 8;
    return 1;
  }

  public static int getNrAddrPorts(AttributeSet attrs) {
    return getNrPorts(attrs);
  }

  public static int getNrDataOutPorts(AttributeSet attrs) {
    return getNrPorts(attrs) * getDataLines(attrs);
  }

  public static int getNrOfPorts(AttributeSet attrs) {
    return getNrAddrPorts(attrs) + getNrDataOutPorts(attrs);
  }

  public static int getAddrIndex(int portIndex, AttributeSet attrs) {
    if (portIndex < 0 || portIndex >= getNrPorts(attrs)) return -1;
    return portIndex;
  }

  public static int getDataOutIndex(int portIndex, int line, AttributeSet attrs) {
    if (portIndex < 0 || portIndex >= getNrPorts(attrs)) return -1;
    final int lines = getDataLines(attrs);
    if (line < 0 || line >= lines) return -1;
    return getNrAddrPorts(attrs) + portIndex * lines + line;
  }

  /* ------------------------------------------------------------------ */
  /* geometry                                                            */
  /* ------------------------------------------------------------------ */

  private static final int HEADER_HEIGHT = 40;
  private static final int PIN_STEP = 10;
  private static final int BAND_PADDING = 20;
  private static final int CONTENT_GAP = 10;
  private static final int CONTENT_HEIGHT = 120;
  private static final int SYMBOL_WIDTH = Mem.SymbolWidth + 80;

  private static int bandHeight(AttributeSet attrs) {
    final int rows = Math.max(1, getDataLines(attrs));
    return Math.max(30, rows * PIN_STEP) + BAND_PADDING;
  }

  private static int bandTop(int portIndex, AttributeSet attrs) {
    return HEADER_HEIGHT + portIndex * bandHeight(attrs);
  }

  private static int pinY(int portIndex, int pin, AttributeSet attrs) {
    return bandTop(portIndex, attrs) + (BAND_PADDING / 2) + pin * PIN_STEP;
  }

  public static Bounds getBounds(AttributeSet attrs) {
    final int ports = getNrPorts(attrs);
    final int contentTop = HEADER_HEIGHT + ports * bandHeight(attrs) + CONTENT_GAP;
    return Bounds.create(0, 0, SYMBOL_WIDTH, contentTop + CONTENT_HEIGHT + 10);
  }

  /* ------------------------------------------------------------------ */
  /* port construction                                                   */
  /* ------------------------------------------------------------------ */

  public static void configurePorts(Instance instance) {
    final var attrs = instance.getAttributeSet();
    final var ps = new Port[getNrOfPorts(attrs)];
    final BitWidth addrBits = attrs.getValue(Mem.ADDR_ATTR);
    final BitWidth dataBits = attrs.getValue(Mem.DATA_ATTR);
    final int ports = getNrPorts(attrs);
    final int lines = getDataLines(attrs);

    for (var p = 0; p < ports; p++) {
      ps[getAddrIndex(p, attrs)] = new Port(0, pinY(p, 0, attrs), Port.INPUT, addrBits);
      for (var l = 0; l < lines; l++) {
        ps[getDataOutIndex(p, l, attrs)] =
            new Port(SYMBOL_WIDTH, pinY(p, l, attrs), Port.OUTPUT, dataBits);
      }
    }

    instance.setPorts(ps);
  }

  /* ------------------------------------------------------------------ */
  /* painting                                                            */
  /* ------------------------------------------------------------------ */

  public static void paint(InstancePainter painter) {
    final var attrs = painter.getAttributeSet();
    final var g = painter.getGraphics();
    final var bds = painter.getBounds();
    final var inst = painter.getInstance();
    final var color = new Color(AppPreferences.COMPONENT_COLOR.get());
    final int x = bds.getX();
    final int y = bds.getY();
    final int ports = getNrPorts(attrs);
    final int lines = getDataLines(attrs);

    g.setColor(color);

    final var label = painter.getAttributeValue(StdAttr.LABEL);
    if (label != null && !label.isEmpty() && painter.getAttributeValue(StdAttr.LABEL_VISIBILITY)) {
      final var oldFont = g.getFont();
      g.setFont(painter.getAttributeValue(StdAttr.LABEL_FONT));
      GraphicsUtil.drawCenteredText(g, label, x + SYMBOL_WIDTH / 2, y - g.getFont().getSize());
      g.setFont(oldFont);
    }

    painter.drawBounds();

    final var size =
        Mem.getSizeLabel(attrs.getValue(Mem.ADDR_ATTR).getWidth())
            + " x "
            + attrs.getValue(Mem.DATA_ATTR).getWidth();
    g.setColor(color);
    GraphicsUtil.drawCenteredText(g, "ROM " + size, x + SYMBOL_WIDTH / 2, y + 14);
    GraphicsUtil.drawCenteredText(g, ports + " ports (read only)", x + SYMBOL_WIDTH / 2, y + 28);

    final int bandHeight = bandHeight(attrs);
    for (var p = 0; p < ports; p++) {
      final int top = y + bandTop(p, attrs);

      g.setColor(new Color(0x80, 0x80, 0x80));
      g.drawLine(x, top, x + SYMBOL_WIDTH, top);
      g.setColor(color);

      final var oldFont = g.getFont();
      g.setFont(oldFont.deriveFont(9.0f));
      GraphicsUtil.drawCenteredText(g, "PORT " + p, x + SYMBOL_WIDTH / 2, top + 12);
      g.setFont(oldFont);

      painter.drawPort(getAddrIndex(p, attrs), "A" + p, Direction.EAST);
      for (var l = 0; l < lines; l++) {
        painter.drawPort(
            getDataOutIndex(p, l, attrs),
            lines == 1 ? "D" + p : "D" + p + "_" + l,
            Direction.WEST);
      }
    }

    final int contentTop = HEADER_HEIGHT + ports * bandHeight + CONTENT_GAP;
    final var contentBounds =
        Bounds.create(x + 20, y + contentTop, SYMBOL_WIDTH - 40, CONTENT_HEIGHT);
    g.setColor(color);
    g.drawRect(
        contentBounds.getX(),
        contentBounds.getY(),
        contentBounds.getWidth(),
        contentBounds.getHeight());
    final var g2 = (Graphics2D) g;
    final var oldStroke = g2.getStroke();
    g2.setStroke(new BasicStroke(1));
    boolean paintedContents = false;
    if (painter.getShowState() && painter.getCircuitState() != null) {
      final var state = inst.getData(painter.getCircuitState());
      if (state instanceof MemState memState) {
        memState.paint(
            g,
            x,
            y,
            24,
            contentTop + 6,
            SYMBOL_WIDTH - 48,
            CONTENT_HEIGHT - 12,
            lines);
        paintedContents = true;
      }
    }
    if (!paintedContents) {
      GraphicsUtil.drawCenteredText(
          g,
          "ROM " + size + " contents",
          contentBounds.getX() + contentBounds.getWidth() / 2,
          contentBounds.getY() + contentBounds.getHeight() / 2);
    }
    g2.setStroke(oldStroke);
  }
}
