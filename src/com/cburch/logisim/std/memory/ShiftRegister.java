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

package com.cburch.logisim.std.memory;

import java.awt.Color;
import java.awt.Graphics;

import com.bfh.logisim.designrulecheck.NetlistComponent;
import com.cburch.logisim.data.Attribute;
import com.cburch.logisim.data.AttributeSet;
import com.cburch.logisim.data.Attributes;
import com.cburch.logisim.data.BitWidth;
import com.cburch.logisim.data.Bounds;
import com.cburch.logisim.data.Value;
import com.cburch.logisim.instance.Instance;
import com.cburch.logisim.instance.InstanceFactory;
import com.cburch.logisim.instance.InstancePainter;
import com.cburch.logisim.instance.InstanceState;
import com.cburch.logisim.instance.Port;
import com.cburch.logisim.instance.StdAttr;
import com.cburch.logisim.tools.key.BitWidthConfigurator;
import com.cburch.logisim.tools.key.IntegerConfigurator;
import com.cburch.logisim.tools.key.JoinedConfigurator;
import com.cburch.logisim.util.GraphicsUtil;
import com.cburch.logisim.util.StringUtil;

public class ShiftRegister extends InstanceFactory {
	static final Attribute<Integer> ATTR_LENGTH = Attributes.forIntegerRange(
			"length", Strings.getter("shiftRegLengthAttr"), 1, 32);
	static final Attribute<Boolean> ATTR_LOAD = Attributes.forBoolean(
			"parallel", Strings.getter("shiftRegParallelAttr"));

	static final int IN = 0;
	static final int SH = 1;
	public static final int CK = 2;
	static final int CLR = 3;
	static final int OUT = 4;
	static final int LD = 5;
	static final int SymbolWidth = 100;

	public ShiftRegister() {
		super("Shift Register", Strings.getter("shiftRegisterComponent"));
		setAttributes(new Attribute[] { StdAttr.WIDTH, ATTR_LENGTH, ATTR_LOAD,
				StdAttr.EDGE_TRIGGER, StdAttr.LABEL, StdAttr.LABEL_FONT },
				new Object[] { BitWidth.ONE, Integer.valueOf(8), Boolean.TRUE,
						StdAttr.TRIG_RISING, "", StdAttr.DEFAULT_LABEL_FONT });
		setKeyConfigurator(JoinedConfigurator
				.create(new IntegerConfigurator(ATTR_LENGTH, 1, 32, 0),
						new BitWidthConfigurator(StdAttr.WIDTH)));

		setIconName("shiftreg.gif");
		setInstanceLogger(ShiftRegisterLogger.class);
		setInstancePoker(ShiftRegisterPoker.class);
	}

	@Override
	protected void configureNewInstance(Instance instance) {
		configurePorts(instance);
		instance.addAttributeListener();
	}

	private void configurePorts(Instance instance) {
		BitWidth widthObj = instance.getAttributeValue(StdAttr.WIDTH);
		int width = widthObj.getWidth();
		Boolean parallelObj = instance.getAttributeValue(ATTR_LOAD);
		Bounds bds = instance.getBounds();
		Port[] ps;
		Integer lenObj = instance.getAttributeValue(ATTR_LENGTH);
		int len = lenObj == null ? 8 : lenObj.intValue();
		if (parallelObj == null || parallelObj.booleanValue()) {
			ps = new Port[6 + 2 * len - 1];
			ps[LD] = new Port(0, 30, Port.INPUT, 1);
			ps[LD].setToolTip(Strings.getter("shiftRegLoadTip"));
			for (int i = 0; i < len; i++) {
				ps[6 + 2 * i] = new Port(0, 90 + i * 20, Port.INPUT, width);
				if (i < (len - 1))
					ps[6 + 2 * i + 1] = new Port(SymbolWidth + 20, 90 + i * 20,
							Port.OUTPUT, width);
			}
		} else {
			ps = new Port[5];
		}
		ps[OUT] = new Port(SymbolWidth + 20, 70 + len * 20, Port.OUTPUT, width);
		ps[IN] = new Port(0, 80, Port.INPUT, width);
		ps[SH] = new Port(0, 40, Port.INPUT, 1);
		ps[CK] = new Port(0, 50, Port.INPUT, 1);
		ps[CLR] = new Port(0, 20, Port.INPUT, 1);
		ps[OUT].setToolTip(Strings.getter("shiftRegOutTip"));
		ps[SH].setToolTip(Strings.getter("shiftRegShiftTip"));
		ps[IN].setToolTip(Strings.getter("shiftRegInTip"));
		ps[CK].setToolTip(Strings.getter("shiftRegClockTip"));
		ps[CLR].setToolTip(Strings.getter("shiftRegClearTip"));
		instance.setPorts(ps);
		instance.setTextField(StdAttr.LABEL, StdAttr.LABEL_FONT, bds.getX()
				+ bds.getWidth() / 2, bds.getY() - 3, GraphicsUtil.H_CENTER,
				GraphicsUtil.V_BASELINE);
	}

	private void DrawControl(InstancePainter painter, int xpos, int ypos,
			int nr_of_stages, int nr_of_bits, boolean has_load,
			boolean active_low_clock) {
		Graphics g = painter.getGraphics();
		GraphicsUtil.switchToWidth(g, 2);
		int blockwidth = SymbolWidth;
		g.drawLine(xpos + 10, ypos, xpos + blockwidth + 10, ypos);
		g.drawLine(xpos + 10, ypos, xpos + 10, ypos + 60);
		g.drawLine(xpos + blockwidth + 10, ypos, xpos + blockwidth + 10,
				ypos + 60);
		g.drawLine(xpos + 10, ypos + 60, xpos + 20, ypos + 60);
		g.drawLine(xpos + blockwidth, ypos + 60, xpos + blockwidth + 10,
				ypos + 60);
		g.drawLine(xpos + 20, ypos + 60, xpos + 20, ypos + 70);
		g.drawLine(xpos + blockwidth, ypos + 60, xpos + blockwidth, ypos + 70);
		if (nr_of_bits > 1) {
			g.drawLine(xpos + blockwidth + 10, ypos + 5,
					xpos + blockwidth + 15, ypos + 5);
			g.drawLine(xpos + blockwidth + 15, ypos + 5,
					xpos + blockwidth + 15, ypos + 65);
			g.drawLine(xpos + blockwidth + 5, ypos + 65,
					xpos + blockwidth + 15, ypos + 65);
			g.drawLine(xpos + blockwidth + 5, ypos + 65, xpos + blockwidth + 5,
					ypos + 70);
			if (nr_of_bits > 2) {
				g.drawLine(xpos + blockwidth + 15, ypos + 10, xpos + blockwidth
						+ 20, ypos + 10);
				g.drawLine(xpos + blockwidth + 20, ypos + 10, xpos + blockwidth
						+ 20, ypos + 70);
				g.drawLine(xpos + blockwidth + 10, ypos + 70, xpos + blockwidth
						+ 20, ypos + 70);
			}
		}
		String Identifier = "SRG" + Integer.toString(nr_of_stages);
		GraphicsUtil.drawCenteredText(g, Identifier, xpos + (SymbolWidth / 2)
				+ 10, ypos + 5);
		/* Draw the clock input */
		painter.drawClockSymbol(xpos + 10, ypos + 50);
		GraphicsUtil.switchToWidth(g, 2);
		if (active_low_clock)
			g.drawOval(xpos, ypos + 45, 10, 10);
		else
			g.drawLine(xpos, ypos + 50, xpos + 10, ypos + 50);
		painter.drawPort(CK);
		String Cntrl = "1\u2192/C3";
		GraphicsUtil.drawText(g, Cntrl, xpos + 20, ypos + 50,
				GraphicsUtil.H_LEFT, GraphicsUtil.V_CENTER);
		/* draw shift input */
		g.drawLine(xpos, ypos + 40, xpos + 10, ypos + 40);
		GraphicsUtil.drawText(g, "M1 [shift]", xpos + 20, ypos + 40,
				GraphicsUtil.H_LEFT, GraphicsUtil.V_CENTER);
		painter.drawPort(SH);
		/* draw load input */
		if (has_load) {
			g.drawLine(xpos, ypos + 30, xpos + 10, ypos + 30);
			GraphicsUtil.drawText(g, "M2 [load]", xpos + 20, ypos + 30,
					GraphicsUtil.H_LEFT, GraphicsUtil.V_CENTER);
			painter.drawPort(LD);
		}
		/* draw reset */
		g.drawLine(xpos, ypos + 20, xpos + 10, ypos + 20);
		GraphicsUtil.drawText(g, "R", xpos + 20, ypos + 20,
				GraphicsUtil.H_LEFT, GraphicsUtil.V_CENTER);
		painter.drawPort(CLR);
		GraphicsUtil.switchToWidth(g, 1);
	}

	private void DrawDataBlock(InstancePainter painter, int xpos, int ypos,
			int nr_of_stages, int nr_of_bits, int current_stage,
			Value data_value, boolean has_load) {
		int real_ypos = ypos + 70 + current_stage * 20;
		if (current_stage > 0)
			real_ypos += 10;
		int real_xpos = xpos + 10;
		int DataWidth = (nr_of_bits == 1) ? 3 : 5;
		int height = (current_stage == 0) ? 30 : 20;
		boolean LastBlock = (current_stage == (nr_of_stages - 1));
		int blockwidth = SymbolWidth;
		Graphics g = painter.getGraphics();
		GraphicsUtil.switchToWidth(g, 2);
		g.drawRect(real_xpos, real_ypos, blockwidth, height);
		if (nr_of_bits > 1) {
			g.drawLine(real_xpos + blockwidth, real_ypos + 5, real_xpos
					+ blockwidth + 5, real_ypos + 5);
			g.drawLine(real_xpos + blockwidth + 5, real_ypos + 5, real_xpos
					+ blockwidth + 5, real_ypos + height + 5);
			if (LastBlock) {
				g.drawLine(real_xpos + 5, real_ypos + height + 5, real_xpos
						+ blockwidth + 5, real_ypos + height + 5);
				g.drawLine(real_xpos + 5, real_ypos + height, real_xpos + 5,
						real_ypos + height + 5);
			}
			if (nr_of_bits > 2) {
				g.drawLine(real_xpos + blockwidth + 5, real_ypos + 10,
						real_xpos + blockwidth + 10, real_ypos + 10);
				g.drawLine(real_xpos + blockwidth + 10, real_ypos + 10,
						real_xpos + blockwidth + 10, real_ypos + height + 10);
				if (LastBlock) {
					g.drawLine(real_xpos + 10, real_ypos + height + 10,
							real_xpos + blockwidth + 10, real_ypos + height
									+ 10);
					g.drawLine(real_xpos + 10, real_ypos + height + 5,
							real_xpos + 10, real_ypos + height + 10);
				}
			}
		}
		/* Draw the Inputs */
		if (current_stage == 0 || has_load) {
			GraphicsUtil.switchToWidth(g, DataWidth);
			g.drawLine(real_xpos - 10, real_ypos + 10, real_xpos - 1,
					real_ypos + 10);
			if (has_load)
				painter.drawPort(6 + 2 * current_stage);
			if (current_stage == 0) {
				painter.drawPort(IN);
				GraphicsUtil.drawText(g, "1,3D", real_xpos + 1, real_ypos + 10,
						GraphicsUtil.H_LEFT, GraphicsUtil.V_CENTER);
				if (has_load) {
					g.drawLine(real_xpos - 10, real_ypos + 20, real_xpos - 1,
							real_ypos + 20);
					GraphicsUtil.drawText(g, "2,3D", real_xpos + 1,
							real_ypos + 20, GraphicsUtil.H_LEFT,
							GraphicsUtil.V_CENTER);
				}
			} else {
				GraphicsUtil.drawText(g, "2,3D", real_xpos + 1, real_ypos + 10,
						GraphicsUtil.H_LEFT, GraphicsUtil.V_CENTER);
			}
			GraphicsUtil.switchToWidth(g, 1);
		}
		GraphicsUtil.switchToWidth(g, 1);
		/* Draw the outputs */
		if (LastBlock) {
			GraphicsUtil.switchToWidth(g, DataWidth);
			g.drawLine(real_xpos + blockwidth, real_ypos + 10, real_xpos
					+ blockwidth + 10, real_ypos + 10);
			painter.drawPort(OUT);
			GraphicsUtil.switchToWidth(g, 1);
		}
		if (has_load && !LastBlock) {
			GraphicsUtil.switchToWidth(g, DataWidth);
			if (current_stage == 0)
				g.drawLine(real_xpos + blockwidth, real_ypos + 20, real_xpos
						+ blockwidth + 10, real_ypos + 20);
			else
				g.drawLine(real_xpos + blockwidth, real_ypos + 10, real_xpos
						+ blockwidth + 10, real_ypos + 10);
			painter.drawPort(6 + 2 * current_stage + 1);
			GraphicsUtil.switchToWidth(g, 1);
		}
		/* Draw stage value */
		if (painter.getShowState()&&(data_value!=null)) {
			if (data_value.isFullyDefined())
				g.setColor(Color.LIGHT_GRAY);
			else if (data_value.isErrorValue())
				g.setColor(Color.RED);
			else
				g.setColor(Color.BLUE);
			int yoff = (current_stage == 0) ? 10 : 0;
			int len = (nr_of_bits + 3) / 4;
			int boxXpos = ((blockwidth - 30) / 2 + 30) - (len * 4);
			g.fillRect(real_xpos + boxXpos, real_ypos + yoff + 2, 2 + len * 8,
					16);
			String Value;
			if (data_value.isFullyDefined()) {
				g.setColor(Color.DARK_GRAY);
				Value = StringUtil.toHexString(nr_of_bits, data_value.toIntValue());
			} else {
				g.setColor(Color.YELLOW);
				Value = (data_value.isUnknown()) ? "?" : "!";
			}
			GraphicsUtil.drawText(g, Value, real_xpos + boxXpos + 1, real_ypos
					+ yoff + 10, GraphicsUtil.H_LEFT, GraphicsUtil.V_CENTER);
			g.setColor(Color.BLACK);
		}
	}

	private ShiftRegisterData getData(InstanceState state) {
		BitWidth width = state.getAttributeValue(StdAttr.WIDTH);
		Integer lenObj = state.getAttributeValue(ATTR_LENGTH);
		int length = lenObj == null ? 8 : lenObj.intValue();
		ShiftRegisterData data = (ShiftRegisterData) state.getData();
		if (data == null) {
			data = new ShiftRegisterData(width, length);
			state.setData(data);
		} else {
			data.setDimensions(width, length);
		}
		return data;
	}

	@Override
	public Bounds getOffsetBounds(AttributeSet attrs) {
		int len = attrs.getValue(ATTR_LENGTH).intValue();
		return Bounds.create(0, 0, SymbolWidth + 20, 80 + 20 * len);
	}

	@Override
	public boolean HDLSupportedComponent(String HDLIdentifier,
			AttributeSet attrs) {
		if (MyHDLGenerator == null)
			MyHDLGenerator = new ShiftRegisterHDLGeneratorFactory();
		return MyHDLGenerator.HDLTargetSupported(HDLIdentifier, attrs);
	}

	@Override
	protected void instanceAttributeChanged(Instance instance, Attribute<?> attr) {
		if (attr == ATTR_LOAD || attr == ATTR_LENGTH || attr == StdAttr.WIDTH) {
			instance.recomputeBounds();
			configurePorts(instance);
		}
	}

	@Override
	public void paintInstance(InstancePainter painter) {
		// draw boundary, label
		painter.drawLabel();
		int xpos = painter.getLocation().getX();
		int ypos = painter.getLocation().getY();
		BitWidth widObj = painter.getAttributeValue(StdAttr.WIDTH);
		int wid = widObj.getWidth();
		Integer lenObj = painter.getAttributeValue(ATTR_LENGTH);
		int len = lenObj == null ? 8 : lenObj.intValue();
		Boolean parallelObj = painter.getAttributeValue(ATTR_LOAD);
		Boolean Negedge = painter.getAttributeValue(StdAttr.EDGE_TRIGGER)
				.equals(StdAttr.TRIG_FALLING);
		DrawControl(painter, xpos, ypos, len, wid, parallelObj, Negedge);
		ShiftRegisterData data = (ShiftRegisterData) painter.getData();

		/** In the case data is null we assume that the 
		 * different value are null. This allow the user to 
		 * instantiate the shift register without simulation mode
		 */
		if (data == null) {
			for (int stage = 0; stage < len; stage++) {
			DrawDataBlock(painter, xpos, ypos, len, wid, stage,
					null, parallelObj);
			}
		}
		else {
			for (int stage = 0; stage < len; stage++) {
			DrawDataBlock(painter, xpos, ypos, len, wid, stage,
					data.get(len - stage - 1), parallelObj);
			}
		}
	}

	@Override
	public void propagate(InstanceState state) {
		Object triggerType = state.getAttributeValue(StdAttr.EDGE_TRIGGER);
		boolean parallel = state.getAttributeValue(ATTR_LOAD).booleanValue();
		ShiftRegisterData data = getData(state);
		int len = data.getLength();

		boolean triggered = data.updateClock(state.getPortValue(CK),
				triggerType);
		if (state.getPortValue(CLR) == Value.TRUE) {
			data.clear();
		} else if (triggered) {
			if (parallel && state.getPortValue(LD) == Value.TRUE) {
				data.clear();
				for (int i = len - 1; i >= 0; i--) {
					data.push(state.getPortValue(6 + 2 * i));
				}
			} else if (state.getPortValue(SH) != Value.FALSE) {
				data.push(state.getPortValue(IN));
			}
		}

		state.setPort(OUT, data.get(0), 4);
		if (parallel) {
			for (int i = 0; i < len - 1; i++) {
				state.setPort(6 + 2 * i + 1, data.get(len - 1 - i), 4);
			}
		}
	}

	@Override
	public boolean CheckForGatedClocks(NetlistComponent comp) {
		return true;
	}
	
	@Override
	public int[] ClockPinIndex(NetlistComponent comp) {
		return new int[] {CK};
	}
}
