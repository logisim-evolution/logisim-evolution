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
import com.cburch.logisim.util.GraphicsUtil;
import java.awt.Color;
import java.awt.Font;

public class Tty extends InstanceFactory implements DynamicElementProvider {
  /**
   * Unique identifier of the tool, used as reference in project files.
   * Do NOT change as it will prevent project files from loading.
   *
   * Identifier value must MUST be unique string among all tools.
   */
  public static final String _ID = "TTY";

  private static int getColumnCount(Object val) {
    return (val instanceof Integer)
           ? (Integer) val
           : 16;
  }

  private static int getRowCount(Object val) {
    if (val instanceof Integer) return (Integer) val;
    else return 4;
  }

  private static final int CLR = 0;
  private static final int CK = 1;

  private static final int WE = 2;
  private static final int IN = 3;
  public static final int BORDER = 5;
  public static final int ROW_HEIGHT = 15;

  public static final int COL_WIDTH = 7;

  private static final Color DEFAULT_BACKGROUND = new Color(0, 0, 0, 64);
  public static final Font DEFAULT_FONT = new Font("monospaced", Font.PLAIN, 12);

  private static final Attribute<Integer> ATTR_COLUMNS =
      Attributes.forIntegerRange("cols", S.getter("ttyColsAttr"), 1, 120);

  private static final Attribute<Integer> ATTR_ROWS =
      Attributes.forIntegerRange("rows", S.getter("ttyRowsAttr"), 1, 48);

  public Tty() {
    super(_ID, S.getter("ttyComponent"));
    setAttributes(
        new Attribute[] {
          ATTR_ROWS, ATTR_COLUMNS, StdAttr.EDGE_TRIGGER, IoLibrary.ATTR_COLOR, IoLibrary.ATTR_BACKGROUND
        },
        new Object[] {
            8,
            32,
          StdAttr.TRIG_RISING,
          Color.BLACK,
          DEFAULT_BACKGROUND
        });
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
    var width = 2 * BORDER + cols * COL_WIDTH;
    if (width < 30) width = 30;
    var height = 2 * BORDER + rows * ROW_HEIGHT;
    if (height < 30) height = 30;
    return Bounds.create(0, 10 - height, width, height);
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
    if (attr == ATTR_ROWS || attr == ATTR_COLUMNS) {
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
    painter.drawClock(CK, Direction.EAST);
    if (painter.shouldDrawColor()) {
      g.setColor(painter.getAttributeValue(IoLibrary.ATTR_BACKGROUND));
      g.fillRoundRect(bds.getX(), bds.getY(), bds.getWidth(), bds.getHeight(), 10, 10);
    }
    GraphicsUtil.switchToWidth(g, 2);
    g.setColor(Color.BLACK);
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

      g.setFont(DEFAULT_FONT);
      g.setColor(painter.getAttributeValue(IoLibrary.ATTR_COLOR));
      final var fm = g.getFontMetrics();
      int x = bds.getX() + BORDER;
      int y = bds.getY() + BORDER + (ROW_HEIGHT + fm.getAscent()) / 2;
      for (var i = 0; i < rows; i++) {
        g.drawString(rowData[i], x, y);
        if (i == curRow) {
          final var x0 = x + fm.stringWidth(rowData[i].substring(0, curCol));
          g.drawLine(x0, y - fm.getAscent(), x0, y);
        }
        y += ROW_HEIGHT;
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
        final var go = (trigger == StdAttr.TRIG_FALLING)
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
