/*
 * Logisim-evolution - digital logic design tool and simulator
 * Copyright by the Logisim-evolution developers
 *
 * https://github.com/logisim-evolution/
 *
 * This is free software released under GNU GPLv3 license
 */

package com.cburch.logisim.std.io;

import static com.cburch.logisim.std.Strings.S;

import com.cburch.logisim.circuit.appear.DynamicElement;
import com.cburch.logisim.circuit.appear.DynamicElement.Path;
import com.cburch.logisim.circuit.appear.DynamicElementProvider;
import com.cburch.logisim.data.Attribute;
import com.cburch.logisim.data.AttributeSet;
import com.cburch.logisim.data.Attributes;
import com.cburch.logisim.data.Bounds;
import com.cburch.logisim.data.Direction;
import com.cburch.logisim.data.Value;
import com.cburch.logisim.gui.icons.TtyIcon;
import com.cburch.logisim.instance.Instance;
import com.cburch.logisim.instance.InstanceFactory;
import com.cburch.logisim.instance.InstancePainter;
import com.cburch.logisim.instance.InstanceState;
import com.cburch.logisim.instance.Port;
import com.cburch.logisim.instance.StdAttr;
import com.cburch.logisim.prefs.AppPreferences;
import com.cburch.logisim.util.GraphicsUtil;
import java.awt.Color;
import java.awt.Font;
import java.awt.FontMetrics;

public class Tty extends InstanceFactory implements DynamicElementProvider {
  /**
   * Unique identifier of the tool, used as reference in project files. Do NOT change as it will
   * prevent project files from loading.
   *
   * <p>Identifier value must MUST be unique string among all tools.
   */
  public static final String _ID = "TTY";

  private static int getColumnCount(Object val) {
    return (val instanceof Integer) ? (Integer) val : 16;
  }

  private static int getRowCount(Object val) {
    if (val instanceof Integer) return (Integer) val;
    else return 4;
  }

  private static final int CLR = 0;
  private static final int CK = 1;

  private static final int WE = 2;
  private static final int IN = 3;
  public static final int BORDER = 6;
  public static final int ROW_HEIGHT = 15;

  public static final int COL_WIDTH = 7;

  private static final Color DEFAULT_BACKGROUND = new Color(0, 0, 0, 64);
  public static final Font DEFAULT_FONT = new Font("monospaced", Font.PLAIN, 11);

  public static final Attribute<Font> ATTR_FONT =
      Attributes.forFont("font", S.getter("ttyFontAttr"));

  static final Attribute<Integer> ATTR_COLUMNS =
      Attributes.forIntegerRange("cols", S.getter("ttyColsAttr"), 1, 120);

  static final Attribute<Integer> ATTR_ROWS =
      Attributes.forIntegerRange("rows", S.getter("ttyRowsAttr"), 1, 48);

  /** Resolves the clamped row count from an attribute set, applying the legacy fallback. */
  static int resolveRowCount(AttributeSet attrs) {
    return getRowCount(attrs.getValue(ATTR_ROWS));
  }

  /** Resolves the clamped column count from an attribute set, applying the legacy fallback. */
  static int resolveColumnCount(AttributeSet attrs) {
    return getColumnCount(attrs.getValue(ATTR_COLUMNS));
  }

  /**
   * Width of a single terminal column for the given font.
   *
   * <p>The TTY input is 7-bit and {@link TtyState} ignores ISO control characters, so a column is
   * sized conservatively from the widest advance among the printable ASCII characters (32 through
   * 126). This avoids assuming {@code 'W'} is widest or that the selected font is monospaced, which
   * keeps the contents from overflowing the component shape.
   */
  public static int getColWidth(FontMetrics fm) {
    var colWidth = 0;
    for (var c = 32; c <= 126; c++) {
      final var advance = fm.charWidth(c);
      if (advance > colWidth) colWidth = advance;
    }
    if (colWidth <= 0) colWidth = COL_WIDTH;
    return colWidth;
  }

  /**
   * Height of a single terminal row for the given font.
   *
   * <p>The legacy layout used a fixed {@link #ROW_HEIGHT} for the default font. To preserve that
   * footprint for existing circuits while still preventing overflow with larger fonts, the row
   * height is scaled by the ratio of the font's height to the default font's height and floored at
   * {@link #ROW_HEIGHT}. The default font therefore reproduces exactly the historical row height.
   */
  public static int getRowHeight(FontMetrics fm) {
    final var defaultHeight = getFontMetrics(DEFAULT_FONT).getHeight();
    if (defaultHeight <= 0) return Math.max(ROW_HEIGHT, fm.getHeight());
    final var scaled = (int) Math.round((double) ROW_HEIGHT * fm.getHeight() / defaultHeight);
    return Math.max(ROW_HEIGHT, scaled);
  }

  /** Shared, font-aware component/shape bounds so ordinary and dynamic layouts agree. */
  public static Bounds computeBounds(int x, int y, int rows, int cols, Font font) {
    final var fm = getFontMetrics(font);
    var width = 2 * BORDER + cols * getColWidth(fm);
    if (width < 30) width = 30;
    var height = 2 * BORDER + rows * getRowHeight(fm);
    if (height < 30) height = 30;
    return Bounds.create(x, y, width, height);
  }

  private static FontMetrics getFontMetrics(Font font) {
    final var tempImage =
        new java.awt.image.BufferedImage(1, 1, java.awt.image.BufferedImage.TYPE_INT_ARGB);
    final var tempGraphics = tempImage.getGraphics();
    tempGraphics.setFont(font);
    final var fm = tempGraphics.getFontMetrics();
    tempGraphics.dispose();
    return fm;
  }

  public Tty() {
    super(_ID, S.getter("ttyComponent"));
    setAttributes(
        new Attribute[] {
          ATTR_ROWS,
          ATTR_COLUMNS,
          ATTR_FONT,
          StdAttr.EDGE_TRIGGER,
          IoLibrary.ATTR_COLOR,
          IoLibrary.ATTR_BACKGROUND
        },
        new Object[] {8, 32, DEFAULT_FONT, StdAttr.TRIG_RISING, Color.BLACK, DEFAULT_BACKGROUND});
    setIcon(new TtyIcon());

    final var ps = new Port[4];
    ps[CLR] = new Port(20, 10, Port.INPUT, 1);
    ps[CK] = new Port(0, 0, Port.INPUT, 1);
    ps[WE] = new Port(10, 10, Port.INPUT, 1);
    ps[IN] = new Port(0, -10, Port.INPUT, 7);
    ps[CLR].setToolTip(S.getter("ttyClearTip"));
    ps[CK].setToolTip(S.getter("ttyClockTip"));
    ps[WE].setToolTip(S.getter("ttyEnableTip"));
    ps[IN].setToolTip(S.getter("ttyInputTip"));
    setPorts(ps);
  }

  @Override
  protected void configureNewInstance(Instance instance) {
    instance.addAttributeListener();
  }

  @Override
  public Bounds getOffsetBounds(AttributeSet attrs) {
    final var rows = getRowCount(attrs.getValue(ATTR_ROWS));
    final var cols = getColumnCount(attrs.getValue(ATTR_COLUMNS));
    final var font = attrs.getValue(ATTR_FONT);
    final var bds = computeBounds(0, 0, rows, cols, font);
    return Bounds.create(0, 10 - bds.getHeight(), bds.getWidth(), bds.getHeight());
  }

  private TtyState getTtyState(InstanceState state) {
    final var rows = getRowCount(state.getAttributeValue(ATTR_ROWS));
    final var cols = getColumnCount(state.getAttributeValue(ATTR_COLUMNS));
    var ret = (TtyState) state.getData();
    if (ret == null) {
      ret = new TtyState(rows, cols);
      state.setData(ret);
    } else {
      ret.updateSize(rows, cols);
    }
    return ret;
  }

  @Override
  protected void instanceAttributeChanged(Instance instance, Attribute<?> attr) {
    if (attr == ATTR_ROWS || attr == ATTR_COLUMNS || attr == ATTR_FONT) {
      instance.recomputeBounds();
    }
  }

  @Override
  public void paintGhost(InstancePainter painter) {
    final var g = painter.getGraphics();
    GraphicsUtil.switchToWidth(g, 2);
    final var bds = painter.getBounds();
    g.drawRoundRect(bds.getX(), bds.getY(), bds.getWidth(), bds.getHeight(), 10, 10);
  }

  @Override
  public void paintInstance(InstancePainter painter) {
    final var showState = painter.getShowState();
    final var g = painter.getGraphics();
    final var bds = painter.getBounds();
    if (painter.shouldDrawColor()) {
      g.setColor(painter.getAttributeValue(IoLibrary.ATTR_BACKGROUND));
      g.fillRoundRect(bds.getX(), bds.getY(), bds.getWidth(), bds.getHeight(), 10, 10);
    }
    GraphicsUtil.switchToWidth(g, 2);
    g.setColor(new Color(AppPreferences.COMPONENT_COLOR.get()));
    painter.drawClock(CK, Direction.EAST);
    g.drawRoundRect(
        bds.getX(), bds.getY(), bds.getWidth(), bds.getHeight(), 2 * BORDER, 2 * BORDER);
    GraphicsUtil.switchToWidth(g, 1);
    painter.drawPort(CLR);
    painter.drawPort(WE);
    painter.drawPort(IN);

    final var rows = getRowCount(painter.getAttributeValue(ATTR_ROWS));
    final var cols = getColumnCount(painter.getAttributeValue(ATTR_COLUMNS));

    if (showState) {
      final var rowData = new String[rows];
      int curRow;
      int curCol;
      final var state = getTtyState(painter);
      synchronized (state) {
        for (var i = 0; i < rows; i++) {
          rowData[i] = state.getRowString(i);
        }
        curRow = state.getCursorRow();
        curCol = state.getCursorColumn();
      }

      g.setFont(painter.getAttributeValue(ATTR_FONT));
      g.setColor(painter.getAttributeValue(IoLibrary.ATTR_COLOR));
      final var fm = g.getFontMetrics();
      final var rowHeight = getRowHeight(fm);
      int x = bds.getX() + BORDER;
      int y = bds.getY() + BORDER + (rowHeight + fm.getAscent()) / 2;
      for (var i = 0; i < rows; i++) {
        g.drawString(rowData[i], x, y);
        if (i == curRow) {
          final var x0 = x + fm.stringWidth(rowData[i].substring(0, curCol));
          g.drawLine(x0, y - fm.getAscent(), x0, y);
        }
        y += rowHeight;
      }
    } else {
      var str = S.get("ttyDesc", "" + rows, "" + cols);
      var fm = g.getFontMetrics();
      int strWidth = fm.stringWidth(str);
      if (strWidth + BORDER > bds.getWidth()) {
        str = S.get("ttyDescShort");
        strWidth = fm.stringWidth(str);
      }
      int x = bds.getX() + (bds.getWidth() - strWidth) / 2;
      int y = bds.getY() + (bds.getHeight() + fm.getAscent()) / 2;
      g.drawString(str, x, y);
    }
  }

  @Override
  public void propagate(InstanceState circState) {
    Object trigger = circState.getAttributeValue(StdAttr.EDGE_TRIGGER);
    final var state = getTtyState(circState);
    final var clear = circState.getPortValue(CLR);
    final var clock = circState.getPortValue(CK);
    final var enable = circState.getPortValue(WE);
    final var in = circState.getPortValue(IN);

    synchronized (state) {
      final var lastClock = state.setLastClock(clock);
      if (clear == Value.TRUE) {
        state.clear();
      } else if (enable != Value.FALSE) {
        final var go =
            (trigger == StdAttr.TRIG_FALLING)
                ? lastClock == Value.TRUE && clock == Value.FALSE
                : lastClock == Value.FALSE && clock == Value.TRUE;
        if (go) state.add(in.isFullyDefined() ? (char) in.toLongValue() : '?');
      }
    }
  }

  public void sendToStdout(InstanceState state) {
    final var tty = getTtyState(state);
    tty.setSendStdout(true);
  }

  @Override
  public DynamicElement createDynamicElement(int x, int y, Path path) {
    return new TtyShape(x, y, path);
  }
}
