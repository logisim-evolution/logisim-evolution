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
 *   * Marcin Orlowski (http://MarcinOrlowski.com/), 2021
 */

package com.cburch.logisim.std.io;

import static com.cburch.logisim.std.Strings.S;

import com.cburch.logisim.circuit.appear.DynamicElement;
import com.cburch.logisim.circuit.appear.DynamicElementProvider;
import com.cburch.logisim.data.Attribute;
import com.cburch.logisim.data.AttributeOption;
import com.cburch.logisim.data.AttributeSet;
import com.cburch.logisim.data.Attributes;
import com.cburch.logisim.data.BitWidth;
import com.cburch.logisim.data.Bounds;
import com.cburch.logisim.data.Direction;
import com.cburch.logisim.data.Value;
import com.cburch.logisim.fpga.data.ComponentMapInformationContainer;
import com.cburch.logisim.fpga.hdlgenerator.HdlGeneratorFactory;
import com.cburch.logisim.gui.icons.LedMatrixIcon;
import com.cburch.logisim.instance.Instance;
import com.cburch.logisim.instance.InstanceData;
import com.cburch.logisim.instance.InstanceFactory;
import com.cburch.logisim.instance.InstancePainter;
import com.cburch.logisim.instance.InstanceState;
import com.cburch.logisim.instance.Port;
import com.cburch.logisim.instance.StdAttr;
import com.cburch.logisim.std.wiring.DurationAttribute;
import com.cburch.logisim.util.GraphicsUtil;
import com.cburch.logisim.util.StringGetter;
import java.awt.Color;
import java.awt.Graphics;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

// TODO repropagate when rows/cols change

public abstract class DotMatrixBase extends InstanceFactory implements DynamicElementProvider {
  protected static class State implements InstanceData, Cloneable {
    protected int rows;
    protected int cols;
    protected Value[] grid;
    protected long[] persistTo;

    public State(int rows, int cols, long curClock) {
      this.rows = -1;
      this.cols = -1;
      updateSize(rows, cols, curClock);
    }

    @Override
    public Object clone() {
      try {
        final var ret = (State) super.clone();
        ret.grid = this.grid.clone();
        ret.persistTo = this.persistTo.clone();
        return ret;
      } catch (CloneNotSupportedException e) {
        return null;
      }
    }

    protected Value get(int row, int col, long curTick) {
      final var index = row * cols + col;
      var ret = grid[index];
      if (ret == Value.FALSE && persistTo[index] - curTick >= 0) {
        ret = Value.TRUE;
      }
      return ret;
    }

    protected void setColumn(int index, Value colVector, long persist) {
      var gridloc = (rows - 1) * cols + index;
      final var stride = -cols;
      final var vals = colVector.getAll();
      for (var i = 0; i < vals.length; i++, gridloc += stride) {
        final var val = vals[i];
        if (grid[gridloc] == Value.TRUE) {
          persistTo[gridloc] = persist - 1;
        }
        grid[gridloc] = val;
        if (val == Value.TRUE) {
          persistTo[gridloc] = persist;
        }
      }
    }

    protected void setRow(int index, Value rowVector, long persist) {
      var gridloc = (index + 1) * cols - 1;
      final var stride = -1;
      final var vals = rowVector.getAll();
      for (var i = 0; i < vals.length; i++, gridloc += stride) {
        final var val = vals[i];
        if (grid[gridloc] == Value.TRUE) {
          persistTo[gridloc] = persist - 1;
        }
        grid[gridloc] = vals[i];
        if (val == Value.TRUE) {
          persistTo[gridloc] = persist;
        }
      }
    }

    protected void setSelect(Value rowVector, Value colVector, long persist) {
      final var rowVals = rowVector.getAll();
      final var colVals = colVector.getAll();
      var gridloc = 0;
      for (var i = rowVals.length - 1; i >= 0; i--) {
        var wholeRow = rowVals[i];
        if (wholeRow == Value.TRUE) {
          for (var j = colVals.length - 1; j >= 0; j--, gridloc++) {
            final var val = colVals[colVals.length - 1 - j];
            if (grid[gridloc] == Value.TRUE) {
              persistTo[gridloc] = persist - 1;
            }
            grid[gridloc] = val;
            if (val == Value.TRUE) {
              persistTo[gridloc] = persist;
            }
          }
        } else {
          if (wholeRow != Value.FALSE) wholeRow = Value.ERROR;
          for (int j = colVals.length - 1; j >= 0; j--, gridloc++) {
            if (grid[gridloc] == Value.TRUE) {
              persistTo[gridloc] = persist - 1;
            }
            grid[gridloc] = wholeRow;
          }
        }
      }
    }

    protected void updateSize(int rows, int cols, long curClock) {
      if (this.rows != rows || this.cols != cols) {
        this.rows = rows;
        this.cols = cols;
        int length = rows * cols;
        grid = new Value[length];
        persistTo = new long[length];
        Arrays.fill(grid, Value.UNKNOWN);
        Arrays.fill(persistTo, curClock - 1);
      }
    }
  }

  protected static final AttributeOption INPUT_SELECT =
      new AttributeOption("select", S.getter("ioInputSelect"));
  protected static final AttributeOption INPUT_COLUMN =
      new AttributeOption("column", S.getter("ioInputColumn"));
  protected static final AttributeOption INPUT_ROW =
      new AttributeOption("row", S.getter("ioInputRow"));

  protected static final AttributeOption SHAPE_CIRCLE =
      new AttributeOption("circle", S.getter("ioShapeCircle"));
  protected static final AttributeOption SHAPE_SQUARE =
      new AttributeOption("square", S.getter("ioShapeSquare"));
  protected static final AttributeOption SHAPE_PADDED_SQUARE =
      new AttributeOption("clusterSegment", S.getter("ioShapePaddedSquare"));

  protected static final Attribute<AttributeOption> ATTR_INPUT_TYPE =
      Attributes.forOption(
          "inputtype",
          S.getter("ioMatrixInput"),
          new AttributeOption[] {INPUT_COLUMN, INPUT_ROW, INPUT_SELECT});

  protected static final Attribute<AttributeOption> ATTR_DOT_SHAPE =
      Attributes.forOption(
          "dotshape",
          S.getter("ioMatrixShape"),
          new AttributeOption[] {
            SHAPE_CIRCLE, SHAPE_SQUARE, SHAPE_PADDED_SQUARE,
          });

  protected static final Attribute<Integer> ATTR_PERSIST =
      new DurationAttribute(
          "persist", S.getter("ioMatrixPersistenceAttr"), 0, Integer.MAX_VALUE, true);

  protected static List<String> getLabels(int rows, int cols) {
    final var result = new ArrayList<String>();
    for (var r = 0; r < rows; r++) for (int c = 0; c < cols; c++) result.add("Row" + r + "Col" + c);
    return result;
  }

  protected boolean drawBorder = true;

  public void setDrawBorder(boolean val) {
    drawBorder = val;
  }

  protected int scaleX = 1;
  protected int scaleY = 1;

  public void setScaleX(int val) {
    scaleX = val;
  }

  public void setScaleY(int val) {
    scaleY = val;
  }

  public DotMatrixBase(
      String name, StringGetter displayName, int cols, int rows, HdlGeneratorFactory generator) {
    super(name, displayName, generator, true);
    setAttributes(
        new Attribute<?>[] {
          getAttributeInputType(),
          getAttributeColumns(),
          getAttributeRows(),
          StdAttr.SELECT_LOC,
          IoLibrary.ATTR_ON_COLOR,
          IoLibrary.ATTR_OFF_COLOR,
          ATTR_PERSIST,
          getAttributeShape(),
          StdAttr.LABEL,
          StdAttr.LABEL_LOC,
          StdAttr.LABEL_FONT,
          StdAttr.LABEL_COLOR,
          StdAttr.LABEL_VISIBILITY,
          StdAttr.MAPINFO
        },
        new Object[] {
          getAttributeItemColumn(),
          BitWidth.create(cols),
          BitWidth.create(rows),
          StdAttr.SELECT_BOTTOM_LEFT,
          Color.GREEN,
          Color.gray,
          0,
          getDefaultShape(),
          "",
          Direction.NORTH,
          StdAttr.DEFAULT_LABEL_FONT,
          StdAttr.DEFAULT_LABEL_COLOR,
          true,
          new ComponentMapInformationContainer(0, cols * rows, 0, null, getLabels(rows, cols), null)
        });
    setIcon(new LedMatrixIcon());
  }

  public abstract Attribute<BitWidth> getAttributeRows();

  public abstract Attribute<BitWidth> getAttributeColumns();

  public abstract Attribute<AttributeOption> getAttributeShape();

  public abstract AttributeOption getDefaultShape();

  public abstract Attribute<AttributeOption> getAttributeInputType();

  public abstract AttributeOption getAttributeItemColumn();

  public abstract AttributeOption getAttributeItemRow();

  public abstract AttributeOption getAttributeItemSelect();

  @Override
  protected void configureNewInstance(Instance instance) {
    instance.computeLabelTextField(Instance.AVOID_LEFT);
    instance.addAttributeListener();
    updatePorts(instance);
    int rows = instance.getAttributeValue(getAttributeRows()).getWidth();
    int cols = instance.getAttributeValue(getAttributeColumns()).getWidth();
    instance
        .getAttributeSet()
        .setValue(
            StdAttr.MAPINFO,
            new ComponentMapInformationContainer(
                0, rows * cols, 0, null, getLabels(rows, cols), null));
  }

  @Override
  public Bounds getOffsetBounds(AttributeSet attrs) {
    Object input = attrs.getValue(getAttributeInputType());
    final var cols = attrs.getValue(getAttributeColumns()).getWidth();
    final var rows = attrs.getValue(getAttributeRows()).getWidth();
    if (input.equals(getAttributeItemColumn())) {
      return Bounds.create(
          -5 * scaleX, -10 * scaleY * rows, 10 * scaleX * cols, 10 * scaleY * rows);
    } else if (input.equals(getAttributeItemRow())) {
      return Bounds.create(0, -5 * scaleY, 10 * scaleX * cols, 10 * scaleY * rows);
    } else { // input == INPUT_SELECT
      if (rows == 1) {
        return Bounds.create(0, -5 * scaleY, 10 * scaleX * cols, 10 * scaleY * rows);
      } else {
        return Bounds.create(0, -5 * scaleY * rows + 5, 10 * scaleX * cols, 10 * scaleY * rows);
      }
    }
  }

  protected State getState(InstanceState state) {
    final var rows = state.getAttributeValue(getAttributeRows()).getWidth();
    final var cols = state.getAttributeValue(getAttributeColumns()).getWidth();
    final var clock = state.getTickCount();

    var data = (State) state.getData();
    if (data == null) {
      data = new State(rows, cols, clock);
      state.setData(data);
    } else {
      data.updateSize(rows, cols, clock);
    }
    return data;
  }

  @Override
  protected void instanceAttributeChanged(Instance instance, Attribute<?> attr) {
    if (attr == StdAttr.LABEL_LOC) {
      instance.computeLabelTextField(Instance.AVOID_LEFT);
    } else if (attr == StdAttr.SELECT_LOC) {
      updatePorts(instance);
    } else if (attr == getAttributeRows()
        || attr == getAttributeColumns()
        || attr == getAttributeInputType()) {
      instance.recomputeBounds();
      instance.computeLabelTextField(Instance.AVOID_LEFT);
      updatePorts(instance);
      if (attr == getAttributeRows() || attr == getAttributeColumns()) {
        final var rows = instance.getAttributeValue(getAttributeRows()).getWidth();
        final var cols = instance.getAttributeValue(getAttributeColumns()).getWidth();
        ComponentMapInformationContainer cm = instance.getAttributeValue(StdAttr.MAPINFO);
        cm.setNrOfOutports(rows * cols, getLabels(rows, cols));
      }
    }
  }

  protected void drawCircle(Graphics g, int x, int y) {
    g.fillOval((x + 1) * scaleX, (y + 1) * scaleY, 8 * scaleX, 8 * scaleY);
  }

  protected void drawSquare(Graphics g, int x, int y) {
    g.fillRect(x, y, 10 * scaleX, 10 * scaleY);
  }

  protected void drawPaddedSquare(Graphics g, int x, int y) {
    final var paddingY = 2;
    final var paddingX = 2;
    g.fillRect(
        x + (paddingX * scaleX),
        y + (paddingY * scaleY),
        (10 - (2 * paddingX)) * scaleX,
        (10 - (2 * paddingY)) * scaleY);
  }

  @Override
  public void paintInstance(InstancePainter painter) {
    final var onColor = painter.getAttributeValue(IoLibrary.ATTR_ON_COLOR);
    final var offColor = painter.getAttributeValue(IoLibrary.ATTR_OFF_COLOR);
    final var shape = painter.getAttributeValue(getAttributeShape());

    final var data = getState(painter);
    final var ticks = painter.getTickCount();
    final var bounds = painter.getBounds();
    final var showState = painter.getShowState();
    final var g = painter.getGraphics();

    final var rows = data.rows;
    final var cols = data.cols;

    // If user wants port dots to be hug it would normally cover the component
    // so we draw ports first, then happily paint over it.
    painter.drawPorts();

    g.setColor(Color.DARK_GRAY);
    g.fillRect(bounds.getX(), bounds.getY(), cols * 10 * scaleX, rows * 10 * scaleY);

    for (var j = 0; j < rows; j++) {
      for (var i = 0; i < cols; i++) {
        int x = bounds.getX() + 10 * i * scaleX;
        int y = bounds.getY() + 10 * j * scaleY;

        if (!showState) {
          g.setColor(Color.GRAY);
          drawCircle(g, x, y);
          continue;
        }

        final var val = data.get(j, i, ticks);
        Color c;
        if (val == Value.TRUE) {
          c = onColor;
        } else if (val == Value.FALSE) {
          c = offColor;
        } else {
          c = Value.errorColor;
        }
        g.setColor(c);
        if (SHAPE_SQUARE.equals(shape)) {
          drawSquare(g, x, y);
        } else if (SHAPE_PADDED_SQUARE.equals(shape)) {
          drawPaddedSquare(g, x, y);
        } else {
          // SHAPE_CIRCLE is default shape
          drawCircle(g, x, y);
        }
      }
    }

    if (drawBorder) {
      g.setColor(Color.DARK_GRAY);
      GraphicsUtil.switchToWidth(g, 2);
      g.drawRect(bounds.getX(), bounds.getY(), bounds.getWidth(), bounds.getHeight());
      GraphicsUtil.switchToWidth(g, 1);
    }
    painter.drawLabel();
  }

  @Override
  public void propagate(InstanceState state) {
    Object type = state.getAttributeValue(getAttributeInputType());
    final var rows = state.getAttributeValue(getAttributeRows()).getWidth();
    final var cols = state.getAttributeValue(getAttributeColumns()).getWidth();
    final long clock = state.getTickCount();
    long persist = clock + state.getAttributeValue(ATTR_PERSIST);

    final var data = getState(state);
    if (getAttributeItemRow().equals(type)) {
      for (var i = 0; i < rows; i++) {
        data.setRow(i, state.getPortValue(i), persist);
      }
    } else if (getAttributeItemColumn().equals(type)) {
      for (var i = 0; i < cols; i++) {
        data.setColumn(i, state.getPortValue(i), persist);
      }
    } else if (getAttributeItemSelect().equals(type)) {
      data.setSelect(state.getPortValue(1), state.getPortValue(0), persist);
    } else {
      throw new RuntimeException("Unexpected matrix type: " + type);
    }
  }

  protected void updatePorts(Instance instance) {
    Object input = instance.getAttributeValue(getAttributeInputType());
    final var rows = instance.getAttributeValue(getAttributeRows()).getWidth();
    final var cols = instance.getAttributeValue(getAttributeColumns()).getWidth();
    final var selectLoc = instance.getAttributeValue(StdAttr.SELECT_LOC);
    Port[] ps;
    if (input == getAttributeItemColumn()) {
      ps = new Port[cols];
      for (var i = 0; i < cols; i++) {
        ps[i] =
            new Port(
                10 * i,
                selectLoc == StdAttr.SELECT_BOTTOM_LEFT ? 0 : rows * -10 * scaleY,
                Port.INPUT,
                rows);
      }
    } else if (input == getAttributeItemRow()) {
      ps = new Port[rows];
      for (var i = 0; i < rows; i++) {
        ps[i] =
            new Port(
                selectLoc == StdAttr.SELECT_BOTTOM_LEFT ? 0 : cols * 10, 10 * i, Port.INPUT, cols);
      }
    } else {
      if (rows <= 1) {
        ps =
            new Port[] {new Port(0, 0, Port.INPUT, cols), new Port(10 * cols, 0, Port.INPUT, rows)};
      } else {
        final var dx = selectLoc == StdAttr.SELECT_BOTTOM_LEFT ? 0 : cols * 10;
        ps = new Port[] {new Port(dx, 0, Port.INPUT, cols), new Port(dx, 10, Port.INPUT, rows)};
      }
    }
    instance.setPorts(ps);
  }

  @Override
  public abstract DynamicElement createDynamicElement(int x, int y, DynamicElement.Path path);
}
