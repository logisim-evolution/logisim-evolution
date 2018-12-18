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

import java.awt.FontMetrics;
import java.awt.Graphics;

import com.cburch.logisim.data.Attribute;
import com.cburch.logisim.data.AttributeOption;
import com.cburch.logisim.data.AttributeSet;
import com.cburch.logisim.data.Attributes;
import com.cburch.logisim.data.BitWidth;
import com.cburch.logisim.data.Bounds;
import com.cburch.logisim.data.Direction;
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

public class BitExtender extends InstanceFactory {
	private static final Attribute<BitWidth> ATTR_IN_WIDTH = Attributes
			.forBitWidth("in_width", Strings.getter("extenderInAttr"));
	private static final Attribute<BitWidth> ATTR_OUT_WIDTH = Attributes
			.forBitWidth("out_width", Strings.getter("extenderOutAttr"));
	static final Attribute<AttributeOption> ATTR_TYPE = Attributes.forOption(
			"type",
			Strings.getter("extenderTypeAttr"),
			new AttributeOption[] {
					new AttributeOption("zero", "zero", Strings
							.getter("extenderZeroType")),
					new AttributeOption("one", "one", Strings
							.getter("extenderOneType")),
					new AttributeOption("sign", "sign", Strings
							.getter("extenderSignType")),
					new AttributeOption("input", "input", Strings
							.getter("extenderInputType")), });

	public static final BitExtender FACTORY = new BitExtender();

	public BitExtender() {
		super("Bit Extender", Strings.getter("extenderComponent"));
		setIconName("extender.gif");
		setAttributes(new Attribute[] { ATTR_IN_WIDTH, ATTR_OUT_WIDTH,
				ATTR_TYPE },
				new Object[] { BitWidth.create(8), BitWidth.create(16),
						ATTR_TYPE.parse("sign") });
		setFacingAttribute(StdAttr.FACING);
		setKeyConfigurator(JoinedConfigurator.create(new BitWidthConfigurator(
				ATTR_OUT_WIDTH), new BitWidthConfigurator(ATTR_IN_WIDTH, 1,
				Value.MAX_WIDTH, 0)));
		setOffsetBounds(Bounds.create(-40, -20, 40, 40));
	}

	//
	// methods for instances
	//
	@Override
	protected void configureNewInstance(Instance instance) {
		configurePorts(instance);
		instance.addAttributeListener();
	}

	private void configurePorts(Instance instance) {
		Port p0 = new Port(0, 0, Port.OUTPUT, ATTR_OUT_WIDTH);
		Port p1 = new Port(-40, 0, Port.INPUT, ATTR_IN_WIDTH);
		String type = getType(instance.getAttributeSet());
		if (type.equals("input")) {
			instance.setPorts(new Port[] { p0, p1,
					new Port(-20, -20, Port.INPUT, 1) });
		} else {
			instance.setPorts(new Port[] { p0, p1 });
		}
	}

	private String getType(AttributeSet attrs) {
		AttributeOption topt = attrs.getValue(ATTR_TYPE);
		return (String) topt.getValue();
	}

	@Override
	public boolean HDLSupportedComponent(String HDLIdentifier,
			AttributeSet attrs) {
		if (MyHDLGenerator == null)
			MyHDLGenerator = new BitExtenderHDLGeneratorFactory();
		return MyHDLGenerator.HDLTargetSupported(HDLIdentifier, attrs);
	}

	@Override
	protected void instanceAttributeChanged(Instance instance, Attribute<?> attr) {
		if (attr == ATTR_TYPE) {
			configurePorts(instance);
			instance.fireInvalidated();
		} else {
			instance.fireInvalidated();
		}
	}

	//
	// graphics methods
	//
	@Override
	public void paintInstance(InstancePainter painter) {
		Graphics g = painter.getGraphics();
		FontMetrics fm = g.getFontMetrics();
		int asc = fm.getAscent();

		painter.drawBounds();

		String s0;
		String type = getType(painter.getAttributeSet());
		if (type.equals("zero"))
			s0 = Strings.get("extenderZeroLabel");
		else if (type.equals("one"))
			s0 = Strings.get("extenderOneLabel");
		else if (type.equals("sign"))
			s0 = Strings.get("extenderSignLabel");
		else if (type.equals("input"))
			s0 = Strings.get("extenderInputLabel");
		else
			s0 = "???"; // should never happen
		String s1 = Strings.get("extenderMainLabel");
		Bounds bds = painter.getBounds();
		int x = bds.getX() + bds.getWidth() / 2;
		int y0 = bds.getY() + (bds.getHeight() / 2 + asc) / 2;
		int y1 = bds.getY() + (3 * bds.getHeight() / 2 + asc) / 2;
		GraphicsUtil.drawText(g, s0, x, y0, GraphicsUtil.H_CENTER,
				GraphicsUtil.V_BASELINE);
		GraphicsUtil.drawText(g, s1, x, y1, GraphicsUtil.H_CENTER,
				GraphicsUtil.V_BASELINE);

		BitWidth w0 = painter.getAttributeValue(ATTR_OUT_WIDTH);
		BitWidth w1 = painter.getAttributeValue(ATTR_IN_WIDTH);
		painter.drawPort(0, "" + w0.getWidth(), Direction.WEST);
		painter.drawPort(1, "" + w1.getWidth(), Direction.EAST);
		if (type.equals("input"))
			painter.drawPort(2);
	}

	@Override
	public void propagate(InstanceState state) {
		Value in = state.getPortValue(1);
		BitWidth wout = state.getAttributeValue(ATTR_OUT_WIDTH);
		String type = getType(state.getAttributeSet());
		Value extend;
		if (type.equals("one")) {
			extend = Value.TRUE;
		} else if (type.equals("sign")) {
			int win = in.getWidth();
			extend = win > 0 ? in.get(win - 1) : Value.ERROR;
		} else if (type.equals("input")) {
			extend = state.getPortValue(2);
			if (extend.getWidth() != 1)
				extend = Value.ERROR;
		} else {
			extend = Value.FALSE;
		}

		Value out = in.extendWidth(wout.getWidth(), extend);
		state.setPort(0, out, 1);
	}

}
