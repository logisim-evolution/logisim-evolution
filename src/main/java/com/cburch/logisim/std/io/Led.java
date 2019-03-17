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
 *******************************************************************************/

package com.cburch.logisim.std.io;

import static com.cburch.logisim.std.Strings.S;

import java.awt.Color;
import java.awt.Graphics;

import com.cburch.logisim.fpga.fpgaboardeditor.FPGAIOInformationContainer;
import com.cburch.logisim.fpga.hdlgenerator.IOComponentInformationContainer;
import com.cburch.logisim.circuit.appear.DynamicElement;
import com.cburch.logisim.circuit.appear.DynamicElementProvider;
import com.cburch.logisim.data.Attribute;
import com.cburch.logisim.data.AttributeSet;
import com.cburch.logisim.data.Bounds;
import com.cburch.logisim.data.Direction;
import com.cburch.logisim.data.Value;
import com.cburch.logisim.instance.Instance;
import com.cburch.logisim.instance.InstanceDataSingleton;
import com.cburch.logisim.instance.InstanceFactory;
import com.cburch.logisim.instance.InstanceLogger;
import com.cburch.logisim.instance.InstancePainter;
import com.cburch.logisim.instance.InstanceState;
import com.cburch.logisim.instance.Port;
import com.cburch.logisim.instance.StdAttr;
import com.cburch.logisim.util.GraphicsUtil;

public class Led extends InstanceFactory implements DynamicElementProvider {

	public static class Logger extends InstanceLogger {
		@Override
		public String getLogName(InstanceState state, Object option) {
			return state.getAttributeValue(StdAttr.LABEL);
		}

		@Override
		public Value getLogValue(InstanceState state, Object option) {
			InstanceDataSingleton data = (InstanceDataSingleton) state
					.getData();
			if (data == null)
				return Value.FALSE;
			return data.getValue() == Value.TRUE ? Value.TRUE : Value.FALSE;
		}
	}

	public Led() {
		super("LED", S.getter("ledComponent"));
		setAttributes(new Attribute[] { StdAttr.FACING, Io.ATTR_ON_COLOR,
				Io.ATTR_OFF_COLOR, Io.ATTR_ACTIVE, StdAttr.LABEL,
				Io.ATTR_LABEL_LOC, StdAttr.LABEL_FONT, StdAttr.LABEL_COLOR, StdAttr.LABEL_VISIBILITY },
				new Object[] { Direction.WEST, new Color(240, 0, 0),
						Color.DARK_GRAY, Boolean.TRUE, "", Direction.EAST,
						StdAttr.DEFAULT_LABEL_FONT, StdAttr.DEFAULT_LABEL_COLOR, true });
		setFacingAttribute(StdAttr.FACING);
		setIconName("led.gif");
		setPorts(new Port[] { new Port(0, 0, Port.INPUT, 1) });
		setInstanceLogger(Logger.class);
		MyIOInformation = new IOComponentInformationContainer(0, 1, 0,
				FPGAIOInformationContainer.IOComponentTypes.LED);
		MyIOInformation
				.AddAlternateMapType(FPGAIOInformationContainer.IOComponentTypes.Pin);
	}

	@Override
	public boolean ActiveOnHigh(AttributeSet attrs) {
		return attrs.getValue(Io.ATTR_ACTIVE);
	}

	@Override
	protected void configureNewInstance(Instance instance) {
		instance.addAttributeListener();
		Io.computeLabelTextField(instance);
	}

	@Override
	public Bounds getOffsetBounds(AttributeSet attrs) {
		Direction facing = attrs.getValue(StdAttr.FACING);
		return Bounds.create(0, -10, 20, 20).rotate(Direction.WEST, facing, 0,0);
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
			Io.computeLabelTextField(instance);
		} else if (attr == Io.ATTR_LABEL_LOC) {
			Io.computeLabelTextField(instance);
		}
	}

	@Override
	public void paintGhost(InstancePainter painter) {
		Graphics g = painter.getGraphics();
		Bounds bds = painter.getBounds();
		GraphicsUtil.switchToWidth(g, 2);
		g.drawOval(bds.getX() + 1, bds.getY() + 1, bds.getWidth() - 2,
				bds.getHeight() - 2);
	}

	@Override
	public void paintInstance(InstancePainter painter) {
		InstanceDataSingleton data = (InstanceDataSingleton) painter.getData();
		Value val = data == null ? Value.FALSE : (Value) data.getValue();
		Bounds bds = painter.getBounds().expand(-1);

		Graphics g = painter.getGraphics();
		if (painter.getShowState()) {
			Color onColor = painter.getAttributeValue(Io.ATTR_ON_COLOR);
			Color offColor = painter.getAttributeValue(Io.ATTR_OFF_COLOR);
			Boolean activ = painter.getAttributeValue(Io.ATTR_ACTIVE);
			Object desired = activ.booleanValue() ? Value.TRUE : Value.FALSE;
			g.setColor(val == desired ? onColor : offColor);
			g.fillOval(bds.getX(), bds.getY(), bds.getWidth(), bds.getHeight());
		}
		g.setColor(Color.BLACK);
		GraphicsUtil.switchToWidth(g, 2);
		g.drawOval(bds.getX(), bds.getY(), bds.getWidth(), bds.getHeight());
		GraphicsUtil.switchToWidth(g, 1);
		painter.drawLabel();
		painter.drawPorts();
	}

	@Override
	public void propagate(InstanceState state) {
		Value val = state.getPortValue(0);
		InstanceDataSingleton data = (InstanceDataSingleton) state.getData();
		if (data == null) {
			state.setData(new InstanceDataSingleton(val));
		} else {
			data.setValue(val);
		}
	}

	@Override
	public boolean RequiresNonZeroLabel() {
		return true;
	}
	
	public DynamicElement createDynamicElement(int x, int y, DynamicElement.Path path) {
		return new LedShape(x, y, path);
	}
}
