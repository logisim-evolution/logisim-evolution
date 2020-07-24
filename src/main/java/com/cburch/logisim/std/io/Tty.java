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
import java.awt.FontMetrics;
import java.awt.Graphics;

public class Tty extends InstanceFactory  implements DynamicElementProvider {
  private static int getColumnCount(Object val) {
    if (val instanceof Integer) return ((Integer) val).intValue();
    else return 16;
  }

  private static int getRowCount(Object val) {
    if (val instanceof Integer) return ((Integer) val).intValue();
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
    super("TTY", S.getter("ttyComponent"));
    setAttributes(
        new Attribute[] {
          ATTR_ROWS, ATTR_COLUMNS, StdAttr.EDGE_TRIGGER, Io.ATTR_COLOR, Io.ATTR_BACKGROUND
        },
        new Object[] {
          Integer.valueOf(8),
          Integer.valueOf(32),
          StdAttr.TRIG_RISING,
          Color.BLACK,
          DEFAULT_BACKGROUND
        });
    setIcon(new TtyIcon());

    Port[] ps = new Port[4];
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
    int rows = getRowCount(attrs.getValue(ATTR_ROWS));
    int cols = getColumnCount(attrs.getValue(ATTR_COLUMNS));
    int width = 2 * BORDER + cols * COL_WIDTH;
    int height = 2 * BORDER + rows * ROW_HEIGHT;
    if (width < 30) width = 30;
    if (height < 30) height = 30;
    return Bounds.create(0, 10 - height, width, height);
  }

  private TtyState getTtyState(InstanceState state) {
    int rows = getRowCount(state.getAttributeValue(ATTR_ROWS));
    int cols = getColumnCount(state.getAttributeValue(ATTR_COLUMNS));
    TtyState ret = (TtyState) state.getData();
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
    Graphics g = painter.getGraphics();
    GraphicsUtil.switchToWidth(g, 2);
    Bounds bds = painter.getBounds();
    g.drawRoundRect(bds.getX(), bds.getY(), bds.getWidth(), bds.getHeight(), 10, 10);
  }

  @Override
  public void paintInstance(InstancePainter painter) {
    boolean showState = painter.getShowState();
    Graphics g = painter.getGraphics();
    Bounds bds = painter.getBounds();
    painter.drawClock(CK, Direction.EAST);
    if (painter.shouldDrawColor()) {
      g.setColor(painter.getAttributeValue(Io.ATTR_BACKGROUND));
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

    int rows = getRowCount(painter.getAttributeValue(ATTR_ROWS));
    int cols = getColumnCount(painter.getAttributeValue(ATTR_COLUMNS));

    if (showState) {
      String[] rowData = new String[rows];
      int curRow;
      int curCol;
      TtyState state = getTtyState(painter);
      synchronized (state) {
        for (int i = 0; i < rows; i++) {
          rowData[i] = state.getRowString(i);
        }
        curRow = state.getCursorRow();
        curCol = state.getCursorColumn();
      }

      g.setFont(DEFAULT_FONT);
      g.setColor(painter.getAttributeValue(Io.ATTR_COLOR));
      FontMetrics fm = g.getFontMetrics();
      int x = bds.getX() + BORDER;
      int y = bds.getY() + BORDER + (ROW_HEIGHT + fm.getAscent()) / 2;
      for (int i = 0; i < rows; i++) {
        g.drawString(rowData[i], x, y);
        if (i == curRow) {
          int x0 = x + fm.stringWidth(rowData[i].substring(0, curCol));
          g.drawLine(x0, y - fm.getAscent(), x0, y);
        }
        y += ROW_HEIGHT;
      }
    } else {
      String str = S.fmt("ttyDesc", "" + rows, "" + cols);
      FontMetrics fm = g.getFontMetrics();
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
    TtyState state = getTtyState(circState);
    Value clear = circState.getPortValue(CLR);
    Value clock = circState.getPortValue(CK);
    Value enable = circState.getPortValue(WE);
    Value in = circState.getPortValue(IN);

    synchronized (state) {
      Value lastClock = state.setLastClock(clock);
      if (clear == Value.TRUE) {
        state.clear();
      } else if (enable != Value.FALSE) {
        boolean go;
        if (trigger == StdAttr.TRIG_FALLING) {
          go = lastClock == Value.TRUE && clock == Value.FALSE;
        } else {
          go = lastClock == Value.FALSE && clock == Value.TRUE;
        }
        if (go) state.add(in.isFullyDefined() ? (char) in.toLongValue() : '?');
      }
    }
  }

  public void sendToStdout(InstanceState state) {
    TtyState tty = getTtyState(state);
    tty.setSendStdout(true);
  }

  @Override
  public DynamicElement createDynamicElement(int x, int y, Path path) {
    return new TtyShape(x, y, path);
  }
}
