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
import java.awt.event.MouseEvent;
import java.util.ArrayList;

import com.bfh.logisim.fpgaboardeditor.FPGAIOInformationContainer;
import com.bfh.logisim.hdlgenerator.IOComponentInformationContainer;
import com.cburch.draw.shapes.DrawAttr;
import com.cburch.logisim.data.Attribute;
import com.cburch.logisim.data.AttributeSet;
import com.cburch.logisim.data.Attributes;
import com.cburch.logisim.data.Bounds;
import com.cburch.logisim.data.Direction;
import com.cburch.logisim.data.Location;
import com.cburch.logisim.data.Value;
import com.cburch.logisim.instance.Instance;
import com.cburch.logisim.instance.InstanceData;
import com.cburch.logisim.instance.InstanceFactory;
import com.cburch.logisim.instance.InstancePainter;
import com.cburch.logisim.instance.InstancePoker;
import com.cburch.logisim.instance.InstanceState;
import com.cburch.logisim.instance.Port;
import com.cburch.logisim.instance.StdAttr;
import com.cburch.logisim.util.GraphicsUtil;

public class DipSwitch extends InstanceFactory {

	public static class Poker extends InstancePoker {

		@Override
		public void mousePressed(InstanceState state, MouseEvent e) {
			State val = (State) state.getData();
			Location loc = state.getInstance().getLocation();
			int cx = e.getX() - loc.getX() - 5;
			int i = cx / 10;
			val.ToggleBit(i);
			state.getInstance().fireInvalidated();
		}
	}

	private static class State implements InstanceData, Cloneable {

		private int Value;
		private int size;

		public State(int value, int size) {
			Value = value;
			this.size = size;
		}

		public boolean BitSet(int bitindex) {
			if (bitindex >= size) {
				return false;
			}
			int mask = 1 << bitindex;
			return (Value & mask) != 0;
		}

		@Override
		public Object clone() {
			try {
				return super.clone();
			} catch (CloneNotSupportedException e) {
				return null;
			}
		}

		public void ToggleBit(int bitindex) {
			if ((bitindex < 0) || (bitindex >= size)) {
				return;
			}
			int mask = 1 << bitindex;
			Value ^= mask;
		}
	}

	public static final ArrayList<String> GetLabels(int size) {
		ArrayList<String> LabelNames = new ArrayList<String>();
		for (int i = 0; i < size; i++) {
			LabelNames.add("sw_" + Integer.toString(i + 1));
		}
		return LabelNames;
	}

	public static final int MAX_SWITCH = 32;

	public static final int MIN_SWITCH = 2;

	public static final Attribute<Integer> ATTR_SIZE = Attributes
			.forIntegerRange("number", Strings.getter("nrOfSwitch"),
					MIN_SWITCH, MAX_SWITCH);

	public DipSwitch() {
		super("DipSwitch", Strings.getter("DipSwitchComponent"));
		int dipSize = 8;
		setAttributes(new Attribute[] { StdAttr.LABEL, Io.ATTR_LABEL_LOC,
				StdAttr.LABEL_FONT, StdAttr.LABEL_COLOR, StdAttr.LABEL_VISIBILITY, ATTR_SIZE },
				new Object[] { "", Direction.EAST, StdAttr.DEFAULT_LABEL_FONT,
						StdAttr.DEFAULT_LABEL_COLOR, false, dipSize });
		setFacingAttribute(StdAttr.FACING);
		setIconName("dipswitch.gif");
		setInstancePoker(Poker.class);
		MyIOInformation = new IOComponentInformationContainer(dipSize, 0, 0,
				GetLabels(dipSize), null, null,
				FPGAIOInformationContainer.IOComponentTypes.DIPSwitch);
		MyIOInformation
				.AddAlternateMapType(FPGAIOInformationContainer.IOComponentTypes.Button);
		MyIOInformation
				.AddAlternateMapType(FPGAIOInformationContainer.IOComponentTypes.Pin);
	}

	private void computeTextField(Instance instance) {
		Direction facing = Direction.NORTH;
		Object labelLoc = instance.getAttributeValue(Io.ATTR_LABEL_LOC);

		Bounds bds = instance.getBounds();
		int x = bds.getX() + bds.getWidth() / 2;
		int y = bds.getY() + bds.getHeight() / 2;
		int halign = GraphicsUtil.H_CENTER;
		int valign = GraphicsUtil.V_CENTER;
		if (labelLoc == Direction.NORTH) {
			y = bds.getY() - 2;
			valign = GraphicsUtil.V_BOTTOM;
		} else if (labelLoc == Direction.SOUTH) {
			y = bds.getY() + bds.getHeight() + 2;
			valign = GraphicsUtil.V_TOP;
		} else if (labelLoc == Direction.EAST) {
			x = bds.getX() + bds.getWidth() + 2;
			halign = GraphicsUtil.H_LEFT;
		} else if (labelLoc == Direction.WEST) {
			x = bds.getX() - 2;
			halign = GraphicsUtil.H_RIGHT;
		}
		if (labelLoc == facing) {
			if (labelLoc == Direction.NORTH || labelLoc == Direction.SOUTH) {
				x += 2;
				halign = GraphicsUtil.H_LEFT;
			} else {
				y -= 2;
				valign = GraphicsUtil.V_BOTTOM;
			}
		}

		instance.setTextField(StdAttr.LABEL, StdAttr.LABEL_FONT, x, y, halign,
				valign);
	}

	@Override
	protected void configureNewInstance(Instance instance) {
		instance.addAttributeListener();
		configurePorts(instance);
		computeTextField(instance);
		MyIOInformation.setNrOfInports(instance.getAttributeValue(ATTR_SIZE),
				GetLabels(instance.getAttributeValue(ATTR_SIZE)));
	}

	private void configurePorts(Instance instance) {
		Port[] ps = new Port[instance.getAttributeValue(ATTR_SIZE)];
		for (int i = 0; i < instance.getAttributeValue(ATTR_SIZE); i++) {
			ps[i] = new Port((i + 1) * 10, 0, Port.OUTPUT, 1);
		}
		instance.setPorts(ps);
	}

	@Override
	public Bounds getOffsetBounds(AttributeSet attrs) {
		return Bounds.create(0, 0,
				10 + attrs.getValue(ATTR_SIZE).intValue() * 10, 40).rotate(
				Direction.NORTH, Direction.NORTH, 0, 0);
	}

	@Override
	public boolean HDLSupportedComponent(String HDLIdentifier,
			AttributeSet attrs) {
		if (MyHDLGenerator == null) {
			MyHDLGenerator = new ButtonHDLGeneratorFactory();
		}
		return MyHDLGenerator.HDLTargetSupported(HDLIdentifier, attrs);
	}

	@Override
	protected void instanceAttributeChanged(Instance instance, Attribute<?> attr) {
		if (attr == Io.ATTR_LABEL_LOC) {
			computeTextField(instance);
		} else if (attr == ATTR_SIZE) {
			instance.recomputeBounds();
			configurePorts(instance);
			computeTextField(instance);
			MyIOInformation.setNrOfInports(
					instance.getAttributeValue(ATTR_SIZE),
					GetLabels(instance.getAttributeValue(ATTR_SIZE)));
		}
	}

	@Override
	public void paintInstance(InstancePainter painter) {
		State state = (State) painter.getData();
		if (state == null || state.size != painter.getAttributeValue(ATTR_SIZE)) {
			int val = (state == null) ? 0 : state.Value;
			state = new State(val, painter.getAttributeValue(ATTR_SIZE));
			painter.setData(state);
		}
		Bounds bds = painter.getBounds().expand(-1);

		Graphics g = painter.getGraphics();
		GraphicsUtil.switchToWidth(g, 2);
		g.setColor(Color.darkGray);
		g.fillRect(bds.getX(), bds.getY(), bds.getWidth(), bds.getHeight());
		GraphicsUtil.switchToWidth(g, 1);
		g.setColor(Color.white);
		g.setFont(DrawAttr.DEFAULT_FONT);
		int offset = 0;
		for (int i = 0; i < painter.getAttributeValue(ATTR_SIZE); i++) {
			if (i == 9) {
				g.setFont(g.getFont()
						.deriveFont(g.getFont().getSize2D() * 0.6f));
				offset = -2;
			}
			g.fillRect(bds.getX() + 6 + (i * 10), bds.getY() + 15, 6, 20);
			g.drawChars(Integer.toString(i + 1).toCharArray(), 0, Integer
					.toString(i + 1).toCharArray().length, bds.getX() + 5
					+ offset + i * 10, bds.getY() + 12);
		}
		g.setColor(Color.lightGray);
		for (int i = 0; i < painter.getAttributeValue(ATTR_SIZE); i++) {
			int ypos = (state.BitSet(i)) ? bds.getY() + 16 : bds.getY() + 25;
			g.fillRect(bds.getX() + 7 + (i * 10), ypos, 4, 9);
		}
		painter.drawLabel();
		painter.drawPorts();

	}

	@Override
	public void propagate(InstanceState state) {
		State pins = (State) state.getData();
		if (pins == null || pins.size != state.getAttributeValue(ATTR_SIZE)) {
			int val = (pins == null) ? 0 : pins.Value;
			pins = new State(val, state.getAttributeValue(ATTR_SIZE));
			state.setData(pins);
		}
		for (int i = 0; i < pins.size; i++) {
			Value pinstate = (pins.BitSet(i)) ? Value.TRUE : Value.FALSE;
			state.setPort(i, pinstate, 1);
		}
	}

	@Override
	public boolean RequiresNonZeroLabel() {
		return true;
	}
}
