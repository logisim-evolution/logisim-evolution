/*******************************************************************************
 * This file is part of logisim-evolution.
 *
 *   logisim-evolution is free software: you can redistribute it and/or modify
 *   it under the terms of the GNU General Public License as published by
 *   the Free Software Foundation, either version 3 of the License, or
 *   (at your option) any later version.
 *
 *   logisim-evolution is distributed in the hope that it will be useful,
 *   but WITHOUT ANY WARRANTY; without even the implied warranty of
 *   MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *   GNU General Public License for more details.
 *
 *   You should have received a copy of the GNU General Public License
 *   along with logisim-evolution.  If not, see <http://www.gnu.org/licenses/>.
 *
 *   Original code by Carl Burch (http://www.cburch.com), 2011.
 *   Subsequent modifications by :
 *     + Haute École Spécialisée Bernoise
 *       http://www.bfh.ch
 *     + Haute École du paysage, d'ingénierie et d'architecture de Genève
 *       http://hepia.hesge.ch/
 *     + Haute École d'Ingénierie et de Gestion du Canton de Vaud
 *       http://www.heig-vd.ch/
 *   The project is currently maintained by :
 *     + REDS Institute - HEIG-VD
 *       Yverdon-les-Bains, Switzerland
 *       http://reds.heig-vd.ch
 *******************************************************************************/

package com.cburch.logisim.std.io;

import java.awt.Color;
import java.awt.Graphics;
import java.util.Arrays;

import com.cburch.logisim.data.Attribute;
import com.cburch.logisim.data.AttributeOption;
import com.cburch.logisim.data.AttributeSet;
import com.cburch.logisim.data.Attributes;
import com.cburch.logisim.data.Bounds;
import com.cburch.logisim.data.Value;
import com.cburch.logisim.instance.Instance;
import com.cburch.logisim.instance.InstanceData;
import com.cburch.logisim.instance.InstanceFactory;
import com.cburch.logisim.instance.InstancePainter;
import com.cburch.logisim.instance.InstanceState;
import com.cburch.logisim.instance.Port;
import com.cburch.logisim.std.wiring.DurationAttribute;
import com.cburch.logisim.util.GraphicsUtil;

// TODO repropagate when rows/cols change

public class DotMatrix extends InstanceFactory {
	private static class State implements InstanceData, Cloneable {
		private int rows;
		private int cols;
		private Value[] grid;
		private long[] persistTo;

		public State(int rows, int cols, long curClock) {
			this.rows = -1;
			this.cols = -1;
			updateSize(rows, cols, curClock);
		}

		@Override
		public Object clone() {
			try {
				State ret = (State) super.clone();
				ret.grid = this.grid.clone();
				ret.persistTo = this.persistTo.clone();
				return ret;
			} catch (CloneNotSupportedException e) {
				return null;
			}
		}

		private Value get(int row, int col, long curTick) {
			int index = row * cols + col;
			Value ret = grid[index];
			if (ret == Value.FALSE && persistTo[index] - curTick >= 0) {
				ret = Value.TRUE;
			}
			return ret;
		}

		private void setColumn(int index, Value colVector, long persist) {
			int gridloc = (rows - 1) * cols + index;
			int stride = -cols;
			Value[] vals = colVector.getAll();
			for (int i = 0; i < vals.length; i++, gridloc += stride) {
				Value val = vals[i];
				if (grid[gridloc] == Value.TRUE) {
					persistTo[gridloc] = persist - 1;
				}
				grid[gridloc] = val;
				if (val == Value.TRUE) {
					persistTo[gridloc] = persist;
				}
			}
		}

		private void setRow(int index, Value rowVector, long persist) {
			int gridloc = (index + 1) * cols - 1;
			int stride = -1;
			Value[] vals = rowVector.getAll();
			for (int i = 0; i < vals.length; i++, gridloc += stride) {
				Value val = vals[i];
				if (grid[gridloc] == Value.TRUE) {
					persistTo[gridloc] = persist - 1;
				}
				grid[gridloc] = vals[i];
				if (val == Value.TRUE) {
					persistTo[gridloc] = persist;
				}
			}
		}

		private void setSelect(Value rowVector, Value colVector, long persist) {
			Value[] rowVals = rowVector.getAll();
			Value[] colVals = colVector.getAll();
			int gridloc = 0;
			for (int i = rowVals.length - 1; i >= 0; i--) {
				Value wholeRow = rowVals[i];
				if (wholeRow == Value.TRUE) {
					for (int j = colVals.length - 1; j >= 0; j--, gridloc++) {
						Value val = colVals[colVals.length - 1 - j];
						if (grid[gridloc] == Value.TRUE) {
							persistTo[gridloc] = persist - 1;
						}
						grid[gridloc] = val;
						if (val == Value.TRUE) {
							persistTo[gridloc] = persist;
						}
					}
				} else {
					if (wholeRow != Value.FALSE)
						wholeRow = Value.ERROR;
					for (int j = colVals.length - 1; j >= 0; j--, gridloc++) {
						if (grid[gridloc] == Value.TRUE) {
							persistTo[gridloc] = persist - 1;
						}
						grid[gridloc] = wholeRow;
					}
				}
			}
		}

		private void updateSize(int rows, int cols, long curClock) {
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

	static final AttributeOption INPUT_SELECT = new AttributeOption("select",
			Strings.getter("ioInputSelect"));
	static final AttributeOption INPUT_COLUMN = new AttributeOption("column",
			Strings.getter("ioInputColumn"));

	static final AttributeOption INPUT_ROW = new AttributeOption("row",
			Strings.getter("ioInputRow"));
	static final AttributeOption SHAPE_CIRCLE = new AttributeOption("circle",
			Strings.getter("ioShapeCircle"));

	static final AttributeOption SHAPE_SQUARE = new AttributeOption("square",
			Strings.getter("ioShapeSquare"));
	static final Attribute<AttributeOption> ATTR_INPUT_TYPE = Attributes
			.forOption("inputtype", Strings.getter("ioMatrixInput"),
					new AttributeOption[] { INPUT_COLUMN, INPUT_ROW,
							INPUT_SELECT });
	static final Attribute<Integer> ATTR_MATRIX_COLS = Attributes
			.forIntegerRange("matrixcols", Strings.getter("ioMatrixCols"), 1,
					Value.MAX_WIDTH);
	static final Attribute<Integer> ATTR_MATRIX_ROWS = Attributes
			.forIntegerRange("matrixrows", Strings.getter("ioMatrixRows"), 1,
					Value.MAX_WIDTH);
	static final Attribute<AttributeOption> ATTR_DOT_SHAPE = Attributes
			.forOption("dotshape", Strings.getter("ioMatrixShape"),
					new AttributeOption[] { SHAPE_CIRCLE, SHAPE_SQUARE });

	static final Attribute<Integer> ATTR_PERSIST = new DurationAttribute(
			"persist", Strings.getter("ioMatrixPersistenceAttr"), 0,
			Integer.MAX_VALUE,true);

	public DotMatrix() {
		super("DotMatrix", Strings.getter("dotMatrixComponent"));
		setAttributes(new Attribute<?>[] { ATTR_INPUT_TYPE, ATTR_MATRIX_COLS,
				ATTR_MATRIX_ROWS, Io.ATTR_ON_COLOR, Io.ATTR_OFF_COLOR,
				ATTR_PERSIST, ATTR_DOT_SHAPE }, new Object[] { INPUT_COLUMN,
				Integer.valueOf(5), Integer.valueOf(7), Color.GREEN,
				Color.DARK_GRAY, Integer.valueOf(0), SHAPE_SQUARE });
		setIconName("dotmat.gif");
	}

	@Override
	protected void configureNewInstance(Instance instance) {
		instance.addAttributeListener();
		updatePorts(instance);
	}

	@Override
	public Bounds getOffsetBounds(AttributeSet attrs) {
		Object input = attrs.getValue(ATTR_INPUT_TYPE);
		int cols = attrs.getValue(ATTR_MATRIX_COLS).intValue();
		int rows = attrs.getValue(ATTR_MATRIX_ROWS).intValue();
		if (input == INPUT_COLUMN) {
			return Bounds.create(-5, -10 * rows, 10 * cols, 10 * rows);
		} else if (input == INPUT_ROW) {
			return Bounds.create(0, -5, 10 * cols, 10 * rows);
		} else { // input == INPUT_SELECT
			if (rows == 1) {
				return Bounds.create(0, -5, 10 * cols, 10 * rows);
			} else {
				return Bounds.create(0, -5 * rows + 5, 10 * cols, 10 * rows);
			}
		}
	}

	private State getState(InstanceState state) {
		int rows = state.getAttributeValue(ATTR_MATRIX_ROWS).intValue();
		int cols = state.getAttributeValue(ATTR_MATRIX_COLS).intValue();
		long clock = state.getTickCount();

		State data = (State) state.getData();
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
		if (attr == ATTR_MATRIX_ROWS || attr == ATTR_MATRIX_COLS
				|| attr == ATTR_INPUT_TYPE) {
			instance.recomputeBounds();
			updatePorts(instance);
		}
	}

	@Override
	public void paintInstance(InstancePainter painter) {
		Color onColor = painter.getAttributeValue(Io.ATTR_ON_COLOR);
		Color offColor = painter.getAttributeValue(Io.ATTR_OFF_COLOR);
		boolean drawSquare = painter.getAttributeValue(ATTR_DOT_SHAPE) == SHAPE_SQUARE;

		State data = getState(painter);
		long ticks = painter.getTickCount();
		Bounds bds = painter.getBounds();
		boolean showState = painter.getShowState();
		Graphics g = painter.getGraphics();
		int rows = data.rows;
		int cols = data.cols;
		for (int j = 0; j < rows; j++) {
			for (int i = 0; i < cols; i++) {
				int x = bds.getX() + 10 * i;
				int y = bds.getY() + 10 * j;
				if (showState) {
					Value val = data.get(j, i, ticks);
					Color c;
					if (val == Value.TRUE)
						c = onColor;
					else if (val == Value.FALSE)
						c = offColor;
					else
						c = Value.ERROR_COLOR;
					g.setColor(c);

					if (drawSquare)
						g.fillRect(x, y, 10, 10);
					else
						g.fillOval(x + 1, y + 1, 8, 8);
				} else {
					g.setColor(Color.GRAY);
					g.fillOval(x + 1, y + 1, 8, 8);
				}
			}
		}
		g.setColor(Color.BLACK);
		GraphicsUtil.switchToWidth(g, 2);
		g.drawRect(bds.getX(), bds.getY(), bds.getWidth(), bds.getHeight());
		GraphicsUtil.switchToWidth(g, 1);
		painter.drawPorts();
	}

	@Override
	public void propagate(InstanceState state) {
		Object type = state.getAttributeValue(ATTR_INPUT_TYPE);
		int rows = state.getAttributeValue(ATTR_MATRIX_ROWS).intValue();
		int cols = state.getAttributeValue(ATTR_MATRIX_COLS).intValue();
		long clock = state.getTickCount();
		long persist = clock + state.getAttributeValue(ATTR_PERSIST).intValue();

		State data = getState(state);
		if (type == INPUT_ROW) {
			for (int i = 0; i < rows; i++) {
				data.setRow(i, state.getPortValue(i), persist);
			}
		} else if (type == INPUT_COLUMN) {
			for (int i = 0; i < cols; i++) {
				data.setColumn(i, state.getPortValue(i), persist);
			}
		} else if (type == INPUT_SELECT) {
			data.setSelect(state.getPortValue(1), state.getPortValue(0),
					persist);
		} else {
			throw new RuntimeException("unexpected matrix type");
		}
	}

	private void updatePorts(Instance instance) {
		Object input = instance.getAttributeValue(ATTR_INPUT_TYPE);
		int rows = instance.getAttributeValue(ATTR_MATRIX_ROWS).intValue();
		int cols = instance.getAttributeValue(ATTR_MATRIX_COLS).intValue();
		Port[] ps;
		if (input == INPUT_COLUMN) {
			ps = new Port[cols];
			for (int i = 0; i < cols; i++) {
				ps[i] = new Port(10 * i, 0, Port.INPUT, rows);
			}
		} else if (input == INPUT_ROW) {
			ps = new Port[rows];
			for (int i = 0; i < rows; i++) {
				ps[i] = new Port(0, 10 * i, Port.INPUT, cols);
			}
		} else {
			if (rows <= 1) {
				ps = new Port[] { new Port(0, 0, Port.INPUT, cols) };
			} else if (cols <= 1) {
				ps = new Port[] { new Port(0, 0, Port.INPUT, rows) };
			} else {
				ps = new Port[] { new Port(0, 0, Port.INPUT, cols),
						new Port(0, 10, Port.INPUT, rows) };
			}
		}
		instance.setPorts(ps);
	}
}
