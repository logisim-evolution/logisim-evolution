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

package com.cburch.logisim.std.gates;

import java.awt.Color;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.util.Map;

import javax.swing.Icon;

import com.bfh.logisim.designrulecheck.CorrectLabel;
import com.cburch.logisim.LogisimVersion;
import com.cburch.logisim.analyze.model.Expression;
import com.cburch.logisim.analyze.model.Expressions;
import com.cburch.logisim.circuit.ExpressionComputer;
import com.cburch.logisim.comp.TextField;
import com.cburch.logisim.data.Attribute;
import com.cburch.logisim.data.AttributeSet;
import com.cburch.logisim.data.BitWidth;
import com.cburch.logisim.data.Bounds;
import com.cburch.logisim.data.Direction;
import com.cburch.logisim.data.Location;
import com.cburch.logisim.data.Value;
import com.cburch.logisim.file.Options;
import com.cburch.logisim.instance.Instance;
import com.cburch.logisim.instance.InstanceFactory;
import com.cburch.logisim.instance.InstancePainter;
import com.cburch.logisim.instance.InstanceState;
import com.cburch.logisim.instance.Port;
import com.cburch.logisim.instance.StdAttr;
import com.cburch.logisim.prefs.AppPreferences;
import com.cburch.logisim.tools.WireRepair;
import com.cburch.logisim.tools.WireRepairData;
import com.cburch.logisim.tools.key.BitWidthConfigurator;
import com.cburch.logisim.tools.key.IntegerConfigurator;
import com.cburch.logisim.tools.key.JoinedConfigurator;
import com.cburch.logisim.util.GraphicsUtil;
import com.cburch.logisim.util.Icons;
import com.cburch.logisim.util.StringGetter;

abstract class AbstractGate extends InstanceFactory {
	static Value pullOutput(Value value, Object outType) {
		if (outType == GateAttributes.OUTPUT_01) {
			return value;
		} else {
			Value[] v = value.getAll();
			if (outType == GateAttributes.OUTPUT_0Z) {
				for (int i = 0; i < v.length; i++) {
					if (v[i] == Value.TRUE)
						v[i] = Value.UNKNOWN;
				}
			} else if (outType == GateAttributes.OUTPUT_Z1) {
				for (int i = 0; i < v.length; i++) {
					if (v[i] == Value.FALSE)
						v[i] = Value.UNKNOWN;
				}
			}
			return Value.create(v);
		}
	}

	private String[] iconNames = new String[3];
	private Icon[] icons = new Icon[3];
	private int bonusWidth = 0;
	private boolean negateOutput = false;
	private boolean isXor = false;
	private String rectLabel = "";

	private boolean paintInputLines;

	protected AbstractGate(String name, StringGetter desc) {
		this(name, desc, false);
	}

	protected AbstractGate(String name, StringGetter desc, boolean isXor) {
		super(name, desc);
		this.isXor = isXor;
		setFacingAttribute(StdAttr.FACING);
		setKeyConfigurator(JoinedConfigurator.create(new IntegerConfigurator(
				GateAttributes.ATTR_INPUTS, 2, GateAttributes.MAX_INPUTS, 0),
				new BitWidthConfigurator(StdAttr.WIDTH)));
	}

	protected abstract Expression computeExpression(Expression[] inputs,
			int numInputs);

	private void computeLabel(Instance instance) {
		GateAttributes attrs = (GateAttributes) instance.getAttributeSet();
		Direction facing = attrs.facing;
		int baseWidth = ((Integer) attrs.size.getValue()).intValue();

		int axis = baseWidth / 2 + (negateOutput ? 10 : 0);
		int perp = 0;
		if (AppPreferences.GATE_SHAPE.get().equals(
				AppPreferences.SHAPE_RECTANGULAR)) {
			perp += 6;
		}
		Location loc = instance.getLocation();
		int cx;
		int cy;
		if (facing == Direction.NORTH) {
			cx = loc.getX() + perp;
			cy = loc.getY() + axis;
		} else if (facing == Direction.SOUTH) {
			cx = loc.getX() - perp;
			cy = loc.getY() - axis;
		} else if (facing == Direction.WEST) {
			cx = loc.getX() + axis;
			cy = loc.getY() - perp;
		} else {
			cx = loc.getX() - axis;
			cy = loc.getY() + perp;
		}
		instance.setTextField(StdAttr.LABEL, StdAttr.LABEL_FONT, cx, cy,
				TextField.H_CENTER, TextField.V_CENTER);
	}

	protected abstract Value computeOutput(Value[] inputs, int numInputs,
			InstanceState state);

	void computePorts(Instance instance) {
		GateAttributes attrs = (GateAttributes) instance.getAttributeSet();
		int inputs = attrs.inputs;

		Port[] ports = new Port[inputs + 1];
		ports[0] = new Port(0, 0, Port.OUTPUT, StdAttr.WIDTH);
		for (int i = 0; i < inputs; i++) {
			Location offs = getInputOffset(attrs, i);
			ports[i + 1] = new Port(offs.getX(), offs.getY(), Port.INPUT,
					StdAttr.WIDTH);
		}
		instance.setPorts(ports);
	}

	//
	// methods for instances
	//
	@Override
	protected void configureNewInstance(Instance instance) {
		instance.addAttributeListener();
		computePorts(instance);
		computeLabel(instance);
	}

	@Override
	public boolean contains(Location loc, AttributeSet attrsBase) {
		GateAttributes attrs = (GateAttributes) attrsBase;
		if (super.contains(loc, attrs)) {
			if (attrs.negated == 0) {
				return true;
			} else {
				Direction facing = attrs.facing;
				Bounds bds = getOffsetBounds(attrsBase);
				int delt;
				if (facing == Direction.NORTH) {
					delt = loc.getY() - (bds.getY() + bds.getHeight());
				} else if (facing == Direction.SOUTH) {
					delt = loc.getY() - bds.getY();
				} else if (facing == Direction.WEST) {
					delt = loc.getX() - (bds.getX() + bds.getHeight());
				} else {
					delt = loc.getX() - bds.getX();
				}
				if (Math.abs(delt) > 5) {
					return true;
				} else {
					int inputs = attrs.inputs;
					for (int i = 1; i <= inputs; i++) {
						Location offs = getInputOffset(attrs, i);
						if (loc.manhattanDistanceTo(offs) <= 5)
							return true;
					}
					return false;
				}
			}
		} else {
			return false;
		}
	}

	@Override
	public AttributeSet createAttributeSet() {
		return new GateAttributes(isXor);
	}

	@Override
	public Object getDefaultAttributeValue(Attribute<?> attr, LogisimVersion ver) {
		if (attr instanceof NegateAttribute) {
			return Boolean.FALSE;
		} else {
			return super.getDefaultAttributeValue(attr, ver);
		}
	}

	@Override
	public String getHDLName(AttributeSet attrs) {
		GateAttributes myattrs = (GateAttributes) attrs;
		StringBuffer CompleteName = new StringBuffer();
		CompleteName.append(CorrectLabel.getCorrectLabel(this.getName())
				.toUpperCase());
		BitWidth width = myattrs.getValue(StdAttr.WIDTH);
		if (width.getWidth() > 1)
			CompleteName.append("_BUS");
		Integer inputCount = myattrs.getValue(GateAttributes.ATTR_INPUTS);
		if (inputCount > 2) {
			CompleteName.append("_" + inputCount.toString() + "_INPUTS");
		}
		if (myattrs.containsAttribute(GateAttributes.ATTR_XOR)) {
			if (myattrs.getValue(GateAttributes.ATTR_XOR).equals(
					GateAttributes.XOR_ONE)) {
				CompleteName.append("_ONEHOT");
			}
		}
		return CompleteName.toString();
	}

	private Icon getIcon(int type) {
		Icon ret = icons[type];
		if (ret != null) {
			return ret;
		} else {
			String iconName = iconNames[type];
			if (iconName == null) {
				return null;
			} else {
				ret = Icons.getIcon(iconName);
				if (ret == null) {
					iconNames[type] = null;
				} else {
					icons[type] = ret;
				}
				return ret;
			}
		}
	}

	private Icon getIconDin40700() {
		return getIcon(2);
	}

	private Icon getIconRectangular() {
		return getIcon(1);
	}

	private Icon getIconShaped() {
		return getIcon(0);
	}

	//
	// protected methods intended to be overridden
	//
	protected abstract Value getIdentity();

	Location getInputOffset(GateAttributes attrs, int index) {
		int inputs = attrs.inputs;
		Direction facing = attrs.facing;
		int size = ((Integer) attrs.size.getValue()).intValue();
		int axisLength = size + bonusWidth + (negateOutput ? 10 : 0);
		int negated = attrs.negated;

		int skipStart;
		int skipDist;
		int skipLowerEven = 10;
		if (inputs <= 3) {
			if (size < 40) {
				skipStart = -5;
				skipDist = 10;
				skipLowerEven = 10;
			} else if (size < 60 || inputs <= 2) {
				skipStart = -10;
				skipDist = 20;
				skipLowerEven = 20;
			} else {
				skipStart = -15;
				skipDist = 30;
				skipLowerEven = 30;
			}
		} else if (inputs == 4 && size >= 60) {
			skipStart = -5;
			skipDist = 20;
			skipLowerEven = 0;
		} else {
			skipStart = -5;
			skipDist = 10;
			skipLowerEven = 10;
		}

		int dy;
		if ((inputs & 1) == 1) {
			dy = skipStart * (inputs - 1) + skipDist * index;
		} else {
			dy = skipStart * inputs + skipDist * index;
			if (index >= inputs / 2)
				dy += skipLowerEven;
		}

		int dx = axisLength;
		int negatedBit = (negated >> index) & 1;
		if (negatedBit == 1) {
			dx += 10;
		}

		if (facing == Direction.NORTH) {
			return Location.create(dy, dx);
		} else if (facing == Direction.SOUTH) {
			return Location.create(dy, -dx);
		} else if (facing == Direction.WEST) {
			return Location.create(dx, dy);
		} else {
			return Location.create(-dx, dy);
		}
	}

	@Override
	protected Object getInstanceFeature(final Instance instance, Object key) {
		if (key == WireRepair.class) {
			return new WireRepair() {
				public boolean shouldRepairWire(WireRepairData data) {
					return AbstractGate.this.shouldRepairWire(instance, data);
				}
			};
		}
		if (key == ExpressionComputer.class) {
			return new ExpressionComputer() {
				public void computeExpression(
						Map<Location, Expression> expressionMap) {
					GateAttributes attrs = (GateAttributes) instance
							.getAttributeSet();
					int inputCount = attrs.inputs;
					int negated = attrs.negated;

					Expression[] inputs = new Expression[inputCount];
					int numInputs = 0;
					for (int i = 1; i <= inputCount; i++) {
						Expression e = expressionMap.get(instance
								.getPortLocation(i));
						if (e != null) {
							int negatedBit = (negated >> (i - 1)) & 1;
							if (negatedBit == 1) {
								e = Expressions.not(e);
							}
							inputs[numInputs] = e;
							++numInputs;
						}
					}
					if (numInputs > 0) {
						Expression out = AbstractGate.this.computeExpression(
								inputs, numInputs);
						expressionMap.put(instance.getPortLocation(0), out);
					}
				}
			};
		}
		return super.getInstanceFeature(instance, key);
	}

	@Override
	public Bounds getOffsetBounds(AttributeSet attrsBase) {
		GateAttributes attrs = (GateAttributes) attrsBase;
		Direction facing = attrs.facing;
		int size = ((Integer) attrs.size.getValue()).intValue();
		int inputs = attrs.inputs;
		if (inputs % 2 == 0) {
			inputs++;
		}
		int negated = attrs.negated;

		int width = size + bonusWidth + (negateOutput ? 10 : 0);
		if (negated != 0) {
			width += 10;
		}
		int height = Math.max(10 * inputs, size);
		if (facing == Direction.SOUTH) {
			return Bounds.create(-height / 2, -width, height, width);
		} else if (facing == Direction.NORTH) {
			return Bounds.create(-height / 2, 0, height, width);
		} else if (facing == Direction.WEST) {
			return Bounds.create(0, -height / 2, width, height);
		} else {
			return Bounds.create(-width, -height / 2, width, height);
		}
	}

	protected String getRectangularLabel(AttributeSet attrs) {
		return rectLabel;
	}

	@Override
	public boolean HasThreeStateDrivers(AttributeSet attrs) {
		if (attrs.containsAttribute(GateAttributes.ATTR_OUTPUT))
			return !(attrs.getValue(GateAttributes.ATTR_OUTPUT) == GateAttributes.OUTPUT_01);
		else
			return false;
	}

	@Override
	protected void instanceAttributeChanged(Instance instance, Attribute<?> attr) {
		if (attr == GateAttributes.ATTR_SIZE || attr == StdAttr.FACING) {
			instance.recomputeBounds();
			computePorts(instance);
			computeLabel(instance);
		} else if (attr == GateAttributes.ATTR_INPUTS
				|| attr instanceof NegateAttribute) {
			instance.recomputeBounds();
			computePorts(instance);
		} else if (attr == GateAttributes.ATTR_XOR) {
			instance.fireInvalidated();
		}
	}

	private void paintBase(InstancePainter painter) {
		GateAttributes attrs = (GateAttributes) painter.getAttributeSet();
		Direction facing = attrs.facing;
		int inputs = attrs.inputs;
		int negated = attrs.negated;

		Object shape = painter.getGateShape();
		Location loc = painter.getLocation();
		Bounds bds = painter.getOffsetBounds();
		int width = bds.getWidth();
		int height = bds.getHeight();
		if (facing == Direction.NORTH || facing == Direction.SOUTH) {
			int t = width;
			width = height;
			height = t;
		}
		if (negated != 0) {
			width -= 10;
		}

		Graphics g = painter.getGraphics();
		Color baseColor = g.getColor();
		if (shape == AppPreferences.SHAPE_SHAPED && paintInputLines) {
			PainterShaped.paintInputLines(painter, this);
		} else if (negated != 0) {
			for (int i = 0; i < inputs; i++) {
				int negatedBit = (negated >> i) & 1;
				if (negatedBit == 1) {
					Location in = getInputOffset(attrs, i);
					Location cen = in.translate(facing, 5);
					painter.drawDongle(loc.getX() + cen.getX(), loc.getY()
							+ cen.getY());
				}
			}
		}

		g.setColor(baseColor);
		g.translate(loc.getX(), loc.getY());
		double rotate = 0.0;
		if (facing != Direction.EAST && g instanceof Graphics2D) {
			rotate = -facing.toRadians();
			Graphics2D g2 = (Graphics2D) g;
			g2.rotate(rotate);
		}

		if (shape == AppPreferences.SHAPE_RECTANGULAR) {
			paintRectangular(painter, width, height);
//		} else if (shape == AppPreferences.SHAPE_DIN40700) {
//			paintDinShape(painter, width, height, inputs);
		} else { // SHAPE_SHAPED
			if (negateOutput) {
				g.translate(-10, 0);
				paintShape(painter, width - 10, height);
				painter.drawDongle(5, 0);
				g.translate(10, 0);
			} else {
				paintShape(painter, width, height);
			}
		}

		if (rotate != 0.0) {
			((Graphics2D) g).rotate(-rotate);
		}
		g.translate(-loc.getX(), -loc.getY());

		painter.drawLabel();
	}

	protected abstract void paintDinShape(InstancePainter painter, int width,
			int height, int inputs);

	//
	// painting methods
	//
	@Override
	public void paintGhost(InstancePainter painter) {
		paintBase(painter);
	}

	@Override
	public final void paintIcon(InstancePainter painter) {
		Graphics g = painter.getGraphics();
		g.setColor(Color.black);
		if (painter.getGateShape() == AppPreferences.SHAPE_RECTANGULAR) {
			Icon iconRect = getIconRectangular();
			if (iconRect != null) {
				iconRect.paintIcon(painter.getDestination(), g, 2, 2);
			} else {
				paintIconRectangular(painter);
			}
//		} else if (painter.getGateShape() == AppPreferences.SHAPE_DIN40700) {
//			Icon iconDin = getIconDin40700();
//			if (iconDin != null) {
//				iconDin.paintIcon(painter.getDestination(), g, 2, 2);
//			} else {
//				paintIconRectangular(painter);
//			}
		} else {
			Icon iconShaped = getIconShaped();
			if (iconShaped != null) {
				iconShaped.paintIcon(painter.getDestination(), g, 2, 2);
			} else {
				paintIconShaped(painter);
			}
		}
	}

	protected void paintIconRectangular(InstancePainter painter) {
		Graphics g = painter.getGraphics();
		g.drawRect(1, 2, 16, 16);
		if (negateOutput)
			g.drawOval(16, 8, 4, 4);
		String label = getRectangularLabel(painter.getAttributeSet());
		GraphicsUtil.drawCenteredText(g, label, 9, 8);
	}

	protected abstract void paintIconShaped(InstancePainter painter);

	@Override
	public void paintInstance(InstancePainter painter) {
		paintBase(painter);
		if (!painter.isPrintView()
				|| painter.getGateShape() == AppPreferences.SHAPE_RECTANGULAR) {
			painter.drawPorts();
		}
	}

	protected void paintRectangular(InstancePainter painter, int width,
			int height) {
		int don = negateOutput ? 10 : 0;
		AttributeSet attrs = painter.getAttributeSet();
		painter.drawRectangle(-width, -height / 2, width - don, height,
				getRectangularLabel(attrs));
		if (negateOutput) {
			painter.drawDongle(-5, 0);
		}
	}

	protected abstract void paintShape(InstancePainter painter, int width,
			int height);

	@Override
	public void propagate(InstanceState state) {
		GateAttributes attrs = (GateAttributes) state.getAttributeSet();
		int inputCount = attrs.inputs;
		int negated = attrs.negated;
		AttributeSet opts = state.getProject().getOptions().getAttributeSet();
		boolean errorIfUndefined = opts.getValue(Options.ATTR_GATE_UNDEFINED)
				.equals(Options.GATE_UNDEFINED_ERROR);

		Value[] inputs = new Value[inputCount];
		int numInputs = 0;
		boolean error = false;
		for (int i = 1; i <= inputCount; i++) {
			if (state.isPortConnected(i)) {
				int negatedBit = (negated >> (i - 1)) & 1;
				if (negatedBit == 1) {
					inputs[numInputs] = state.getPortValue(i).not();
				} else {
					inputs[numInputs] = state.getPortValue(i);
				}
				numInputs++;
			} else {
				if (errorIfUndefined) {
					error = true;
				}
			}
		}
		Value out = null;
		if (numInputs == 0 || error) {
			out = Value.createError(attrs.width);
		} else {
			out = computeOutput(inputs, numInputs, state);
			out = pullOutput(out, attrs.out);
		}
		state.setPort(0, out, GateAttributes.DELAY);
	}

	protected void setAdditionalWidth(int value) {
		bonusWidth = value;
	}

	protected void setIconNames(String all) {
		setIconNames(all, all, all);
	}

	protected void setIconNames(String shaped, String rect, String din) {
		iconNames[0] = shaped;
		iconNames[1] = rect;
		iconNames[2] = din;
	}

	protected void setNegateOutput(boolean value) {
		negateOutput = value;
	}

	protected void setPaintInputLines(boolean value) {
		paintInputLines = value;
	}

	protected void setRectangularLabel(String value) {
		rectLabel = value;
	}

	protected boolean shouldRepairWire(Instance instance, WireRepairData data) {
		return false;
	}

}
