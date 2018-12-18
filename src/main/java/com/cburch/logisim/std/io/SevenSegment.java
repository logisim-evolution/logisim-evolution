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
import java.util.ArrayList;

import com.bfh.logisim.fpgaboardeditor.FPGAIOInformationContainer;
import com.bfh.logisim.hdlgenerator.IOComponentInformationContainer;
import com.cburch.logisim.data.Attribute;
import com.cburch.logisim.data.AttributeSet;
import com.cburch.logisim.data.Bounds;
import com.cburch.logisim.data.Direction;
import com.cburch.logisim.data.Value;
import com.cburch.logisim.instance.Instance;
import com.cburch.logisim.instance.InstanceDataSingleton;
import com.cburch.logisim.instance.InstanceFactory;
import com.cburch.logisim.instance.InstancePainter;
import com.cburch.logisim.instance.InstanceState;
import com.cburch.logisim.instance.Port;
import com.cburch.logisim.instance.StdAttr;
import com.cburch.logisim.util.GraphicsUtil;

public class SevenSegment extends InstanceFactory {
	static void drawBase(InstancePainter painter, boolean DrawPoint) {
		ensureSegments();
		InstanceDataSingleton data = (InstanceDataSingleton) painter.getData();
		int summ = (data == null ? 0 : ((Integer) data.getValue()).intValue());
		Boolean active = painter.getAttributeValue(Io.ATTR_ACTIVE);
		int desired = active == null || active.booleanValue() ? 1 : 0;

		Bounds bds = painter.getBounds();
		int x = bds.getX() + 5;
		int y = bds.getY();

		Graphics g = painter.getGraphics();
		Color onColor = painter.getAttributeValue(Io.ATTR_ON_COLOR);
		Color offColor = painter.getAttributeValue(Io.ATTR_OFF_COLOR);
		Color bgColor = painter.getAttributeValue(Io.ATTR_BACKGROUND);
		if (painter.shouldDrawColor() && bgColor.getAlpha() != 0) {
			g.setColor(bgColor);
			g.fillRect(bds.getX(), bds.getY(), bds.getWidth(), bds.getHeight());
			g.setColor(Color.BLACK);
		}
		painter.drawBounds();
		g.setColor(Color.DARK_GRAY);
		for (int i = 0; i <= 7; i++) {
			if (painter.getShowState()) {
				g.setColor(((summ >> i) & 1) == desired ? onColor : offColor);
			}
			if (i < 7) {
				Bounds seg = SEGMENTS[i];
				g.fillRect(x + seg.getX(), y + seg.getY(), seg.getWidth(),
						seg.getHeight());
			} else {
				if (DrawPoint)
					g.fillOval(x + 28, y + 48, 5, 5); // draw decimal point
			}
		}
		g.setColor(Color.BLACK);
		painter.drawLabel();
		painter.drawPorts();
	}

	static void ensureSegments() {
		if (SEGMENTS == null) {
			SEGMENTS = new Bounds[] { Bounds.create(3, 8, 19, 4),
					Bounds.create(23, 10, 4, 19), Bounds.create(23, 30, 4, 19),
					Bounds.create(3, 47, 19, 4), Bounds.create(-2, 30, 4, 19),
					Bounds.create(-2, 10, 4, 19), Bounds.create(3, 28, 19, 4) };
		}
	}

	public static final ArrayList<String> GetLabels() {
		ArrayList<String> LabelNames = new ArrayList<String>();
		for (int i = 0; i < 8; i++)
			LabelNames.add("");
		LabelNames.set(Segment_A, "Segment_A");
		LabelNames.set(Segment_B, "Segment_B");
		LabelNames.set(Segment_C, "Segment_C");
		LabelNames.set(Segment_D, "Segment_D");
		LabelNames.set(Segment_E, "Segment_E");
		LabelNames.set(Segment_F, "Segment_F");
		LabelNames.set(Segment_G, "Segment_G");
		LabelNames.set(DP, "DecimalPoint");
		return LabelNames;
	}

	public static final int Segment_A = 0;
	public static final int Segment_B = 1;
	public static final int Segment_C = 2;
	public static final int Segment_D = 3;
	public static final int Segment_E = 4;
	public static final int Segment_F = 5;
	public static final int Segment_G = 6;

	public static final int DP = 7;

	static Bounds[] SEGMENTS = null;

	static Color DEFAULT_OFF = new Color(220, 220, 220);

	public SevenSegment() {
		super("7-Segment Display", Strings.getter("sevenSegmentComponent"));
		setAttributes(new Attribute[] { Io.ATTR_ON_COLOR, Io.ATTR_OFF_COLOR,
				Io.ATTR_BACKGROUND, Io.ATTR_ACTIVE, StdAttr.LABEL,
				Io.ATTR_LABEL_LOC, StdAttr.LABEL_FONT, StdAttr.LABEL_VISIBILITY }, new Object[] {
				new Color(240, 0, 0), DEFAULT_OFF, Io.DEFAULT_BACKGROUND,
				Boolean.TRUE, "", Direction.EAST, StdAttr.DEFAULT_LABEL_FONT, false });
		setOffsetBounds(Bounds.create(-5, 0, 40, 60));
		setIconName("7seg.gif");
		Port[] ps = new Port[8];
		ps[Segment_A] = new Port(20, 0, Port.INPUT, 1);
		ps[Segment_B] = new Port(30, 0, Port.INPUT, 1);
		ps[Segment_C] = new Port(20, 60, Port.INPUT, 1);
		ps[Segment_D] = new Port(10, 60, Port.INPUT, 1);
		ps[Segment_E] = new Port(0, 60, Port.INPUT, 1);
		ps[Segment_F] = new Port(10, 0, Port.INPUT, 1);
		ps[Segment_G] = new Port(0, 0, Port.INPUT, 1);
		ps[DP] = new Port(30, 60, Port.INPUT, 1);
		ps[Segment_A].setToolTip(Strings.getter("Segment_A"));
		ps[Segment_B].setToolTip(Strings.getter("Segment_B"));
		ps[Segment_C].setToolTip(Strings.getter("Segment_C"));
		ps[Segment_D].setToolTip(Strings.getter("Segment_D"));
		ps[Segment_E].setToolTip(Strings.getter("Segment_E"));
		ps[Segment_F].setToolTip(Strings.getter("Segment_F"));
		ps[Segment_G].setToolTip(Strings.getter("Segment_G"));
		ps[DP].setToolTip(Strings.getter("DecimalPoint"));
		MyIOInformation = new IOComponentInformationContainer(0, 8, 0, null,
				GetLabels(), null,
				FPGAIOInformationContainer.IOComponentTypes.SevenSegment);
		MyIOInformation
				.AddAlternateMapType(FPGAIOInformationContainer.IOComponentTypes.LED);
		MyIOInformation
				.AddAlternateMapType(FPGAIOInformationContainer.IOComponentTypes.Pin);
		setPorts(ps);
	}

	@Override
	public boolean ActiveOnHigh(AttributeSet attrs) {
		return attrs.getValue(Io.ATTR_ACTIVE);
	}

	private void computeTextField(Instance instance) {
		Direction facing = instance.getAttributeValue(StdAttr.FACING);
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
		computeTextField(instance);
	}

	@Override
	public boolean HDLSupportedComponent(String HDLIdentifier,
			AttributeSet attrs) {
		if (MyHDLGenerator == null)
			MyHDLGenerator = new AbstractLedHDLGeneratorFactory();
		return MyHDLGenerator.HDLTargetSupported(HDLIdentifier, attrs);
	}

	@Override
	protected void instanceAttributeChanged(Instance instance, Attribute<?> attr) {
		if (attr == StdAttr.FACING) {
			instance.recomputeBounds();
			computeTextField(instance);
		} else if (attr == Io.ATTR_LABEL_LOC) {
			computeTextField(instance);
		}
	}

	@Override
	public void paintInstance(InstancePainter painter) {
		drawBase(painter, true);
	}

	@Override
	public void propagate(InstanceState state) {
		int summary = 0;
		for (int i = 0; i < 8; i++) {
			Value val = state.getPortValue(i);
			if (val == Value.TRUE)
				summary |= 1 << i;
		}
		Object value = Integer.valueOf(summary);
		InstanceDataSingleton data = (InstanceDataSingleton) state.getData();
		if (data == null) {
			state.setData(new InstanceDataSingleton(value));
		} else {
			data.setValue(value);
		}
	}

	@Override
	public boolean RequiresNonZeroLabel() {
		return true;
	}
}
