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

package com.cburch.logisim.std.wiring;

import java.awt.Color;
import java.awt.Graphics;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import com.cburch.logisim.analyze.model.Expression;
import com.cburch.logisim.analyze.model.Expressions;
import com.cburch.logisim.circuit.ExpressionComputer;
import com.cburch.logisim.data.AbstractAttributeSet;
import com.cburch.logisim.data.Attribute;
import com.cburch.logisim.data.AttributeSet;
import com.cburch.logisim.data.Attributes;
import com.cburch.logisim.data.BitWidth;
import com.cburch.logisim.data.Bounds;
import com.cburch.logisim.data.Direction;
import com.cburch.logisim.data.Location;
import com.cburch.logisim.data.Value;
import com.cburch.logisim.instance.Instance;
import com.cburch.logisim.instance.InstanceFactory;
import com.cburch.logisim.instance.InstancePainter;
import com.cburch.logisim.instance.InstanceState;
import com.cburch.logisim.instance.Port;
import com.cburch.logisim.instance.StdAttr;
import com.cburch.logisim.tools.key.BitWidthConfigurator;
import com.cburch.logisim.tools.key.JoinedConfigurator;
import com.cburch.logisim.util.GraphicsUtil;

public class Constant extends InstanceFactory {
	private static class ConstantAttributes extends AbstractAttributeSet {
		private Direction facing = Direction.EAST;;
		private BitWidth width = BitWidth.ONE;
		private Value value = Value.TRUE;

		@Override
		protected void copyInto(AbstractAttributeSet destObj) {
			ConstantAttributes dest = (ConstantAttributes) destObj;
			dest.facing = this.facing;
			dest.width = this.width;
			dest.value = this.value;
		}

		@Override
		public List<Attribute<?>> getAttributes() {
			return ATTRIBUTES;
		}

		@Override
		@SuppressWarnings("unchecked")
		public <V> V getValue(Attribute<V> attr) {
			if (attr == StdAttr.FACING)
				return (V) facing;
			if (attr == StdAttr.WIDTH)
				return (V) width;
			if (attr == ATTR_VALUE)
				return (V) Integer.valueOf(value.toIntValue());
			return null;
		}

		@Override
		public <V> void setValue(Attribute<V> attr, V value) {
			if (attr == StdAttr.FACING) {
				facing = (Direction) value;
			} else if (attr == StdAttr.WIDTH) {
				width = (BitWidth) value;
				this.value = this.value.extendWidth(width.getWidth(),
						this.value.get(this.value.getWidth() - 1));
			} else if (attr == ATTR_VALUE) {
				int val = ((Integer) value).intValue();
				this.value = Value.createKnown(width, val);
			} else {
				throw new IllegalArgumentException("unknown attribute " + attr);
			}
			fireAttributeValueChanged(attr, value,null);
		}
	}

	private static class ConstantExpression implements ExpressionComputer {
		private Instance instance;

		public ConstantExpression(Instance instance) {
			this.instance = instance;
		}

		public void computeExpression(Map<Location, Expression> expressionMap) {
			AttributeSet attrs = instance.getAttributeSet();
			int intValue = attrs.getValue(ATTR_VALUE).intValue();

			expressionMap.put(instance.getLocation(),
					Expressions.constant(intValue));
		}
	}

	private class ConstantHDLGeneratorFactory extends
			AbstractConstantHDLGeneratorFactory {
		@Override
		public int GetConstant(AttributeSet attrs) {
			return attrs.getValue(Constant.ATTR_VALUE);
		}
	}

	public static final Attribute<Integer> ATTR_VALUE = Attributes
			.forHexInteger("value", Strings.getter("constantValueAttr"));

	public static InstanceFactory FACTORY = new Constant();

	private static final Color BACKGROUND_COLOR = new Color(230, 230, 230);

	private static final List<Attribute<?>> ATTRIBUTES = Arrays
			.asList(new Attribute<?>[] { StdAttr.FACING, StdAttr.WIDTH,
					ATTR_VALUE });

	public Constant() {
		super("Constant", Strings.getter("constantComponent"));
		setFacingAttribute(StdAttr.FACING);
		setKeyConfigurator(JoinedConfigurator.create(
				new ConstantConfigurator(), new BitWidthConfigurator(
						StdAttr.WIDTH)));
	}

	@Override
	protected void configureNewInstance(Instance instance) {
		instance.addAttributeListener();
		updatePorts(instance);
	}

	@Override
	public AttributeSet createAttributeSet() {
		return new ConstantAttributes();
	}

	@Override
	protected Object getInstanceFeature(Instance instance, Object key) {
		if (key == ExpressionComputer.class)
			return new ConstantExpression(instance);
		return super.getInstanceFeature(instance, key);
	}

	@Override
	public Bounds getOffsetBounds(AttributeSet attrs) {
		Direction facing = attrs.getValue(StdAttr.FACING);
		BitWidth width = attrs.getValue(StdAttr.WIDTH);
		int chars = (width.getWidth() + 3) / 4;

		Bounds ret = null;
		if (facing == Direction.EAST) {
			switch (chars) {
			case 1:
				ret = Bounds.create(-16, -8, 16, 16);
				break;
			case 2:
				ret = Bounds.create(-16, -8, 16, 16);
				break;
			case 3:
				ret = Bounds.create(-26, -8, 26, 16);
				break;
			case 4:
				ret = Bounds.create(-36, -8, 36, 16);
				break;
			case 5:
				ret = Bounds.create(-46, -8, 46, 16);
				break;
			case 6:
				ret = Bounds.create(-56, -8, 56, 16);
				break;
			case 7:
				ret = Bounds.create(-66, -8, 66, 16);
				break;
			case 8:
				ret = Bounds.create(-76, -8, 76, 16);
				break;
			}
		} else if (facing == Direction.WEST) {
			switch (chars) {
			case 1:
				ret = Bounds.create(0, -8, 16, 16);
				break;
			case 2:
				ret = Bounds.create(0, -8, 16, 16);
				break;
			case 3:
				ret = Bounds.create(0, -8, 26, 16);
				break;
			case 4:
				ret = Bounds.create(0, -8, 36, 16);
				break;
			case 5:
				ret = Bounds.create(0, -8, 46, 16);
				break;
			case 6:
				ret = Bounds.create(0, -8, 56, 16);
				break;
			case 7:
				ret = Bounds.create(0, -8, 66, 16);
				break;
			case 8:
				ret = Bounds.create(0, -8, 76, 16);
				break;
			}
		} else if (facing == Direction.SOUTH) {
			switch (chars) {
			case 1:
				ret = Bounds.create(-8, -16, 16, 16);
				break;
			case 2:
				ret = Bounds.create(-8, -16, 16, 16);
				break;
			case 3:
				ret = Bounds.create(-13, -16, 26, 16);
				break;
			case 4:
				ret = Bounds.create(-18, -16, 36, 16);
				break;
			case 5:
				ret = Bounds.create(-23, -16, 46, 16);
				break;
			case 6:
				ret = Bounds.create(-28, -16, 56, 16);
				break;
			case 7:
				ret = Bounds.create(-33, -16, 66, 16);
				break;
			case 8:
				ret = Bounds.create(-38, -16, 76, 16);
				break;
			}
		} else if (facing == Direction.NORTH) {
			switch (chars) {
			case 1:
				ret = Bounds.create(-8, 0, 16, 16);
				break;
			case 2:
				ret = Bounds.create(-8, 0, 16, 16);
				break;
			case 3:
				ret = Bounds.create(-13, 0, 26, 16);
				break;
			case 4:
				ret = Bounds.create(-18, 0, 36, 16);
				break;
			case 5:
				ret = Bounds.create(-23, 0, 46, 16);
				break;
			case 6:
				ret = Bounds.create(-28, 0, 56, 16);
				break;
			case 7:
				ret = Bounds.create(-33, 0, 66, 16);
				break;
			case 8:
				ret = Bounds.create(-38, 0, 76, 16);
				break;
			}
		}
		if (ret == null) {
			throw new IllegalArgumentException("unrecognized arguments "
					+ facing + " " + width);
		}
		return ret;
	}

	@Override
	public boolean HDLSupportedComponent(String HDLIdentifier,
			AttributeSet attrs) {
		if (MyHDLGenerator == null)
			MyHDLGenerator = new ConstantHDLGeneratorFactory();
		return MyHDLGenerator.HDLTargetSupported(HDLIdentifier, attrs);
	}

	@Override
	protected void instanceAttributeChanged(Instance instance, Attribute<?> attr) {
		if (attr == StdAttr.WIDTH) {
			instance.recomputeBounds();
			updatePorts(instance);
		} else if (attr == StdAttr.FACING) {
			instance.recomputeBounds();
		} else if (attr == ATTR_VALUE) {
			instance.fireInvalidated();
		}
	}

	@Override
	public void paintGhost(InstancePainter painter) {
		int v = painter.getAttributeValue(ATTR_VALUE).intValue();
		String vStr = Integer.toHexString(v);
		Bounds bds = getOffsetBounds(painter.getAttributeSet());

		Graphics g = painter.getGraphics();
		GraphicsUtil.switchToWidth(g, 2);
		g.fillOval(-2, -2, 5, 5);
		GraphicsUtil.drawCenteredText(g, vStr, bds.getX() + bds.getWidth() / 2,
				bds.getY() + bds.getHeight() / 2);
	}

	//
	// painting methods
	//
	@Override
	public void paintIcon(InstancePainter painter) {
		int w = painter.getAttributeValue(StdAttr.WIDTH).getWidth();
		int pinx = 16;
		int piny = 9;
		Direction dir = painter.getAttributeValue(StdAttr.FACING);
		if (dir == Direction.EAST) {
		} // keep defaults
		else if (dir == Direction.WEST) {
			pinx = 4;
		} else if (dir == Direction.NORTH) {
			pinx = 9;
			piny = 4;
		} else if (dir == Direction.SOUTH) {
			pinx = 9;
			piny = 16;
		}

		Graphics g = painter.getGraphics();
		if (w == 1) {
			int v = painter.getAttributeValue(ATTR_VALUE).intValue();
			Value val = v == 1 ? Value.TRUE : Value.FALSE;
			g.setColor(val.getColor());
			GraphicsUtil.drawCenteredText(g, "" + v, 10, 9);
		} else {
			g.setFont(g.getFont().deriveFont(9.0f));
			GraphicsUtil.drawCenteredText(g, "x" + w, 10, 9);
		}
		g.fillOval(pinx, piny, 3, 3);
	}

	@Override
	public void paintInstance(InstancePainter painter) {
		Bounds bds = painter.getOffsetBounds();
		BitWidth width = painter.getAttributeValue(StdAttr.WIDTH);
		int intValue = painter.getAttributeValue(ATTR_VALUE).intValue();
		Value v = Value.createKnown(width, intValue);
		Location loc = painter.getLocation();
		int x = loc.getX();
		int y = loc.getY();

		Graphics g = painter.getGraphics();
		if (painter.shouldDrawColor()) {
			g.setColor(BACKGROUND_COLOR);
			g.fillRect(x + bds.getX(), y + bds.getY(), bds.getWidth(),
					bds.getHeight());
		}
		if (v.getWidth() == 1) {
			if (painter.shouldDrawColor())
				g.setColor(v.getColor());
			GraphicsUtil.drawCenteredText(g, v.toString(),
					x + bds.getX() + bds.getWidth() / 2,
					y + bds.getY() + bds.getHeight() / 2 - 2);
		} else {
			g.setColor(Color.BLACK);
			GraphicsUtil.drawCenteredText(g, v.toHexString(), x + bds.getX()
					+ bds.getWidth() / 2, y + bds.getY() + bds.getHeight() / 2
					- 2);
		}
		painter.drawPorts();
	}

	@Override
	public void propagate(InstanceState state) {
		BitWidth width = state.getAttributeValue(StdAttr.WIDTH);
		int value = state.getAttributeValue(ATTR_VALUE).intValue();
		state.setPort(0, Value.createKnown(width, value), 1);
	}

	private void updatePorts(Instance instance) {
		Port[] ps = { new Port(0, 0, Port.OUTPUT, StdAttr.WIDTH) };
		instance.setPorts(ps);
	}

	// TODO: Allow editing of value via text tool/attribute table
}
