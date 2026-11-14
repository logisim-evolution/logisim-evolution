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
 * Geometry, port layout and painting for the multi-port mode of the RAM component.
 *
 * <p>The single-port mode of the RAM component keeps using {@link RamAppearance}; this class is
 * only used when {@link RamAttributes#getNrPorts(AttributeSet)} returns a value greater than one.
 *
 * <p>Every port has its own address input, data input, data output, write enable, clock and
 * (depending on the configured enable mode) output enable, byte enables or line enables. The clear
 * pin and the memory contents are shared by all ports.
 */
public final class RamMultiPortAppearance {

  private RamMultiPortAppearance() {}

  /* ------------------------------------------------------------------ */
  /* port capabilities                                                   */
  /* ------------------------------------------------------------------ */

  public static int getNrPorts(AttributeSet attrs) {
    return RamAttributes.getNrPorts(attrs);
  }

  public static boolean isMultiPort(AttributeSet attrs) {
    return getNrPorts(attrs) > 1;
  }

  public static boolean isLineMode(AttributeSet attrs) {
    final Object enables = attrs.getValue(Mem.ENABLES_ATTR);
    return enables != null && Mem.USELINEENABLES.equals(enables);
  }

  private static boolean isByteMode(AttributeSet attrs) {
    return !isLineMode(attrs);
  }

  private static boolean synchronous(AttributeSet attrs) {
    final Object trigger = attrs.getValue(StdAttr.TRIGGER);
    return StdAttr.TRIG_RISING.equals(trigger) || StdAttr.TRIG_FALLING.equals(trigger);
  }

  /** Number of data lines (memory words) transferred by a single port access. */
  public static int getDataLines(AttributeSet attrs) {
    if (!isLineMode(attrs)) return 1;
    final Object line = attrs.getValue(Mem.LINE_ATTR);
    if (Mem.DUAL.equals(line)) return 2;
    if (Mem.QUAD.equals(line)) return 4;
    if (Mem.OCTO.equals(line)) return 8;
    return 1;
  }

  /** Number of byte enables of a single port (0 when byte enables are disabled). */
  public static int getBytesPerWord(AttributeSet attrs) {
    if (!isByteMode(attrs)) return 0;
    final Object enableOption = attrs.getValue(RamAttributes.ATTR_ByteEnables);
    if (enableOption == null
        || !RamAttributes.BUS_WITH_BYTEENABLES.equals(enableOption)
        || !synchronous(attrs)) {
      return 0;
    }
    final BitWidth data = attrs.getValue(Mem.DATA_ATTR);
    return data.getWidth() < 9 ? 0 : (data.getWidth() + 7) >> 3;
  }

  public static int getNrAddrPorts(AttributeSet attrs) {
    return getNrPorts(attrs);
  }

  public static int getNrDataOutPorts(AttributeSet attrs) {
    return getNrPorts(attrs) * getDataLines(attrs);
  }

  public static int getNrDataInPorts(AttributeSet attrs) {
    return getNrDataOutPorts(attrs);
  }

  public static int getNrOEPorts(AttributeSet attrs) {
    return isByteMode(attrs) ? getNrPorts(attrs) : 0;
  }

  public static int getNrWEPorts(AttributeSet attrs) {
    return getNrPorts(attrs);
  }

  public static int getNrClkPorts(AttributeSet attrs) {
    if (synchronous(attrs)) return getNrPorts(attrs);
    return isLineMode(attrs) ? getNrPorts(attrs) : 0;
  }

  public static int getNrLEPorts(AttributeSet attrs) {
    return isLineMode(attrs) ? getNrPorts(attrs) * getDataLines(attrs) : 0;
  }

  public static int getNrBEPorts(AttributeSet attrs) {
    return getNrPorts(attrs) * getBytesPerWord(attrs);
  }

  public static int getNrClrPorts(AttributeSet attrs) {
    final Object clear = attrs.getValue(RamAttributes.CLEAR_PIN);
    return Boolean.TRUE.equals(clear) ? 1 : 0;
  }

  public static int getNrOfPorts(AttributeSet attrs) {
    return getNrAddrPorts(attrs)
        + getNrDataInPorts(attrs)
        + getNrDataOutPorts(attrs)
        + getNrOEPorts(attrs)
        + getNrWEPorts(attrs)
        + getNrClkPorts(attrs)
        + getNrLEPorts(attrs)
        + getNrBEPorts(attrs)
        + getNrClrPorts(attrs);
  }

  /* ------------------------------------------------------------------ */
  /* port index layout: addr, dataOut, dataIn, OE, WE, CLK, LE, BE, CLR  */
  /* ------------------------------------------------------------------ */

  private static int dataOutBase(AttributeSet attrs) {
    return getNrAddrPorts(attrs);
  }

  private static int dataInBase(AttributeSet attrs) {
    return dataOutBase(attrs) + getNrDataOutPorts(attrs);
  }

  private static int oeBase(AttributeSet attrs) {
    return dataInBase(attrs) + getNrDataInPorts(attrs);
  }

  private static int weBase(AttributeSet attrs) {
    return oeBase(attrs) + getNrOEPorts(attrs);
  }

  private static int clkBase(AttributeSet attrs) {
    return weBase(attrs) + getNrWEPorts(attrs);
  }

  private static int leBase(AttributeSet attrs) {
    return clkBase(attrs) + getNrClkPorts(attrs);
  }

  private static int beBase(AttributeSet attrs) {
    return leBase(attrs) + getNrLEPorts(attrs);
  }

  private static int clrBase(AttributeSet attrs) {
    return beBase(attrs) + getNrBEPorts(attrs);
  }

  public static int getAddrIndex(int portIndex, AttributeSet attrs) {
    if (portIndex < 0 || portIndex >= getNrPorts(attrs)) return -1;
    return portIndex;
  }

  public static int getDataOutIndex(int portIndex, int line, AttributeSet attrs) {
    if (portIndex < 0 || portIndex >= getNrPorts(attrs)) return -1;
    if (line < 0 || line >= getDataLines(attrs)) return -1;
    return dataOutBase(attrs) + portIndex * getDataLines(attrs) + line;
  }

  public static int getDataInIndex(int portIndex, int line, AttributeSet attrs) {
    if (portIndex < 0 || portIndex >= getNrPorts(attrs)) return -1;
    if (line < 0 || line >= getDataLines(attrs)) return -1;
    return dataInBase(attrs) + portIndex * getDataLines(attrs) + line;
  }

  public static int getOEIndex(int portIndex, AttributeSet attrs) {
    if (portIndex < 0 || portIndex >= getNrOEPorts(attrs)) return -1;
    return oeBase(attrs) + portIndex;
  }

  public static int getWEIndex(int portIndex, AttributeSet attrs) {
    if (portIndex < 0 || portIndex >= getNrWEPorts(attrs)) return -1;
    return weBase(attrs) + portIndex;
  }

  public static int getClkIndex(int portIndex, AttributeSet attrs) {
    if (portIndex < 0 || portIndex >= getNrClkPorts(attrs)) return -1;
    return clkBase(attrs) + portIndex;
  }

  public static int getLEIndex(int portIndex, int line, AttributeSet attrs) {
    if (portIndex < 0 || portIndex >= getNrPorts(attrs)) return -1;
    if (line < 0 || line >= getDataLines(attrs)) return -1;
    return leBase(attrs) + portIndex * getDataLines(attrs) + line;
  }

  public static int getBEIndex(int portIndex, int byteIndex, AttributeSet attrs) {
    final int bytes = getBytesPerWord(attrs);
    if (bytes == 0 || portIndex < 0 || portIndex >= getNrPorts(attrs)) return -1;
    if (byteIndex < 0 || byteIndex >= bytes) return -1;
    return beBase(attrs) + portIndex * bytes + byteIndex;
  }

  public static int getClrIndex(int portIndex, AttributeSet attrs) {
    if (getNrClrPorts(attrs) == 0 || portIndex != 0) return -1;
    return clrBase(attrs);
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

  private static int leftPinCount(AttributeSet attrs) {
    return 1 // address
        + getNrWEPorts(attrs) / Math.max(1, getNrPorts(attrs))
        + (getNrClkPorts(attrs) > 0 ? 1 : 0)
        + (getNrOEPorts(attrs) > 0 ? 1 : 0)
        + (isLineMode(attrs) ? getDataLines(attrs) : 0)
        + getBytesPerWord(attrs);
  }

  private static int rightPinCount(AttributeSet attrs) {
    return 2 * getDataLines(attrs);
  }

  private static int bandHeight(AttributeSet attrs) {
    final int rows = Math.max(leftPinCount(attrs), rightPinCount(attrs));
    return Math.max(30, rows * PIN_STEP) + BAND_PADDING;
  }

  private static int bandTop(int portIndex, AttributeSet attrs) {
    return HEADER_HEIGHT + portIndex * bandHeight(attrs);
  }

  private static int leftY(int portIndex, int pin, AttributeSet attrs) {
    return bandTop(portIndex, attrs) + (BAND_PADDING / 2) + pin * PIN_STEP;
  }

  private static int rightY(int portIndex, int pin, AttributeSet attrs) {
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
    final int bytes = getBytesPerWord(attrs);

    for (var p = 0; p < ports; p++) {
      var pin = 0;
      ps[getAddrIndex(p, attrs)] = new Port(0, leftY(p, pin++, attrs), Port.INPUT, addrBits);
      ps[getWEIndex(p, attrs)] = new Port(0, leftY(p, pin++, attrs), Port.INPUT, 1);
      if (getNrClkPorts(attrs) > 0) {
        ps[getClkIndex(p, attrs)] = new Port(0, leftY(p, pin++, attrs), Port.INPUT, 1);
      }
      if (getNrOEPorts(attrs) > 0) {
        ps[getOEIndex(p, attrs)] = new Port(0, leftY(p, pin++, attrs), Port.INPUT, 1);
      }
      if (isLineMode(attrs)) {
        for (var l = 0; l < lines; l++) {
          ps[getLEIndex(p, l, attrs)] = new Port(0, leftY(p, pin++, attrs), Port.INPUT, 1);
        }
      } else {
        for (var b = 0; b < bytes; b++) {
          ps[getBEIndex(p, b, attrs)] = new Port(0, leftY(p, pin++, attrs), Port.INPUT, 1);
        }
      }
    }

    for (var p = 0; p < ports; p++) {
      for (var l = 0; l < lines; l++) {
        ps[getDataOutIndex(p, l, attrs)] =
            new Port(SYMBOL_WIDTH, rightY(p, l, attrs), Port.OUTPUT, dataBits);
        ps[getDataInIndex(p, l, attrs)] =
            new Port(SYMBOL_WIDTH, rightY(p, lines + l, attrs), Port.INPUT, dataBits);
      }
    }

    if (getNrClrPorts(attrs) > 0) {
      ps[getClrIndex(0, attrs)] = new Port(0, 20, Port.INPUT, 1);
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

    /* label above the symbol */
    final var label = painter.getAttributeValue(StdAttr.LABEL);
    if (label != null && !label.isEmpty() && painter.getAttributeValue(StdAttr.LABEL_VISIBILITY)) {
      final var oldFont = g.getFont();
      g.setFont(painter.getAttributeValue(StdAttr.LABEL_FONT));
      GraphicsUtil.drawCenteredText(g, label, x + SYMBOL_WIDTH / 2, y - g.getFont().getSize());
      g.setFont(oldFont);
    }

    /* body */
    painter.drawBounds();

    /* title line */
    final var type = inst.getFactory() instanceof Ram ? "RAM " : "ROM ";
    final var size =
        Mem.getSizeLabel(attrs.getValue(Mem.ADDR_ATTR).getWidth())
            + " x "
            + attrs.getValue(Mem.DATA_ATTR).getWidth();
    g.setColor(color);
    GraphicsUtil.drawCenteredText(g, type + size, x + SYMBOL_WIDTH / 2, y + 14);
    GraphicsUtil.drawCenteredText(g, ports + " ports", x + SYMBOL_WIDTH / 2, y + 28);

    /* clear pin */
    if (getNrClrPorts(attrs) > 0) {
      painter.drawPort(getClrIndex(0, attrs), "CLR", Direction.EAST);
    }

    /* one band per port */
    final int bandHeight = bandHeight(attrs);
    final int lines2 = lines;
    for (var p = 0; p < ports; p++) {
      final int top = y + bandTop(p, attrs);

      /* separator between port bands */
      g.setColor(new Color(0x80, 0x80, 0x80));
      g.drawLine(x, top, x + SYMBOL_WIDTH, top);
      g.setColor(color);

      /* band caption, centered so it does not overlap the left hand side pin labels */
      final var oldFont = g.getFont();
      g.setFont(oldFont.deriveFont(9.0f));
      GraphicsUtil.drawCenteredText(g, "PORT " + p, x + SYMBOL_WIDTH / 2, top + 12);
      g.setFont(oldFont);

      /* left hand side pins: address, WE, CLK, OE, LE or BE */
      var pin = 0;
      painter.drawPort(getAddrIndex(p, attrs), "A" + p, Direction.EAST);
      painter.drawPort(getWEIndex(p, attrs), "WE" + p, Direction.EAST);
      pin += 2;
      if (getNrClkPorts(attrs) > 0) {
        painter.drawPort(getClkIndex(p, attrs), "CLK" + p, Direction.EAST);
        pin++;
      }
      if (getNrOEPorts(attrs) > 0) {
        painter.drawPort(getOEIndex(p, attrs), "OE" + p, Direction.EAST);
        pin++;
      }
      if (isLineMode(attrs)) {
        for (var l = 0; l < lines2; l++) {
          painter.drawPort(
              getLEIndex(p, l, attrs), lines2 == 1 ? "LE" + p : "LE" + p + "_" + l, Direction.EAST);
        }
      } else {
        final int bytes = getBytesPerWord(attrs);
        for (var b = 0; b < bytes; b++) {
          painter.drawPort(getBEIndex(p, b, attrs), "BE" + p + "_" + b, Direction.EAST);
        }
      }

      /* right hand side pins: data out then data in */
      for (var l = 0; l < lines2; l++) {
        painter.drawPort(
            getDataOutIndex(p, l, attrs),
            lines2 == 1 ? "D" + p : "D" + p + "_" + l,
            Direction.WEST);
      }
      for (var l = 0; l < lines2; l++) {
        painter.drawPort(
            getDataInIndex(p, l, attrs),
            lines2 == 1 ? "Din" + p : "Din" + p + "_" + l,
            Direction.WEST);
      }
    }

    /* memory contents block */
    final int contentTop = HEADER_HEIGHT + ports * bandHeight + CONTENT_GAP;
    final var contentBounds =
        Bounds.create(x + 20, y + contentTop, SYMBOL_WIDTH - 40, CONTENT_HEIGHT);
    g.setColor(color);
    g.drawRect(contentBounds.getX(), contentBounds.getY(), contentBounds.getWidth(), contentBounds.getHeight());
    final var oldStroke = ((Graphics2D) g).getStroke();
    ((Graphics2D) g).setStroke(new BasicStroke(1));
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
            isLineMode(attrs) ? lines2 : 1);
      } else {
        GraphicsUtil.drawCenteredText(
            g,
            type + size + " contents",
            contentBounds.getX() + contentBounds.getWidth() / 2,
            contentBounds.getY() + contentBounds.getHeight() / 2);
      }
    } else {
      GraphicsUtil.drawCenteredText(
          g,
          type + size + " contents",
          contentBounds.getX() + contentBounds.getWidth() / 2,
          contentBounds.getY() + contentBounds.getHeight() / 2);
    }
    ((Graphics2D) g).setStroke(oldStroke);
  }
}
