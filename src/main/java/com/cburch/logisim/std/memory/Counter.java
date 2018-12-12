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
import java.awt.Font;
import java.awt.Graphics;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.bfh.logisim.designrulecheck.NetlistComponent;
import com.cburch.logisim.data.Attribute;
import com.cburch.logisim.data.AttributeOption;
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
import com.cburch.logisim.util.GraphicsUtil;
import com.cburch.logisim.util.StringUtil;

public class Counter extends InstanceFactory {

	public static int SymbolWidth(int NrOfBits) {
		return 150 + ((NrOfBits - 8) / 5) * 10;
	}

	final static Logger logger = LoggerFactory.getLogger(Counter.class);

	static final AttributeOption ON_GOAL_WRAP = new AttributeOption("wrap",
			"wrap", Strings.getter("counterGoalWrap"));
	static final AttributeOption ON_GOAL_STAY = new AttributeOption("stay",
			"stay", Strings.getter("counterGoalStay"));
	static final AttributeOption ON_GOAL_CONT = new AttributeOption("continue",
			"continue", Strings.getter("counterGoalContinue"));

	static final AttributeOption ON_GOAL_LOAD = new AttributeOption("load",
			"load", Strings.getter("counterGoalLoad"));
	static final Attribute<Integer> ATTR_MAX = Attributes.forHexInteger("max",
			Strings.getter("counterMaxAttr"));

	static final Attribute<AttributeOption> ATTR_ON_GOAL = Attributes
			.forOption("ongoal", Strings.getter("counterGoalAttr"),
					new AttributeOption[] { ON_GOAL_WRAP, ON_GOAL_STAY,
							ON_GOAL_CONT, ON_GOAL_LOAD });
	static final int DELAY = 8;
	static final int OUT = 0;
	static final int IN = 1;
	public static final int CK = 2;
	static final int CLR = 3;
	static final int LD = 4;
	static final int UD = 5;
	static final int EN = 6;

	static final int CARRY = 7;

	public Counter() {
		super("Counter", Strings.getter("counterComponent"));
		setOffsetBounds(Bounds.create(0, 0, 30, 40));
		setIconName("counter.gif");
		setInstancePoker(CounterPoker.class);
		setInstanceLogger(RegisterLogger.class);
		setKeyConfigurator(new BitWidthConfigurator(StdAttr.WIDTH));

	}

	@Override
	protected void configureNewInstance(Instance instance) {
		configurePorts(instance);
		instance.addAttributeListener();
	}

	private void configurePorts(Instance instance) {
		Bounds bds = instance.getBounds();
		BitWidth widthVal = instance.getAttributeValue(StdAttr.WIDTH);
		int width = widthVal == null ? 8 : widthVal.getWidth();
		Port[] ps = new Port[8];
		if (width == 1) {
			ps[OUT] = new Port(SymbolWidth(width) + 40, 120, Port.OUTPUT,
					StdAttr.WIDTH);
			ps[IN] = new Port(0, 120, Port.INPUT, StdAttr.WIDTH);
		} else {
			ps[OUT] = new Port(SymbolWidth(width) + 40, 110, Port.OUTPUT,
					StdAttr.WIDTH);
			ps[IN] = new Port(0, 110, Port.INPUT, StdAttr.WIDTH);
		}
		ps[CK] = new Port(0, 80, Port.INPUT, 1);
		ps[CLR] = new Port(0, 20, Port.INPUT, 1);
		ps[LD] = new Port(0, 30, Port.INPUT, 1);
		ps[UD] = new Port(0, 50, Port.INPUT, 1);
		ps[EN] = new Port(0, 70, Port.INPUT, 1);
		ps[CARRY] = new Port(40 + SymbolWidth(width), 50, Port.OUTPUT, 1);
		ps[OUT].setToolTip(Strings.getter("counterQTip"));
		ps[IN].setToolTip(Strings.getter("counterDataTip"));
		ps[CK].setToolTip(Strings.getter("counterClockTip"));
		ps[CLR].setToolTip(Strings.getter("counterResetTip"));
		ps[LD].setToolTip(Strings.getter("counterLoadTip"));
		ps[UD].setToolTip(Strings.getter("counterUpDownTip"));
		ps[EN].setToolTip(Strings.getter("counterEnableTip"));
		ps[CARRY].setToolTip(Strings.getter("counterCarryTip"));
		instance.setPorts(ps);
		instance.setTextField(StdAttr.LABEL, StdAttr.LABEL_FONT, bds.getX()
				+ bds.getWidth() / 2, bds.getY() - 3, GraphicsUtil.H_CENTER,
				GraphicsUtil.V_BASELINE);
	}

	@Override
	public AttributeSet createAttributeSet() {
		return new CounterAttributes();
	}

	private void DrawControl(InstancePainter painter, int xpos, int ypos) {
		Graphics g = painter.getGraphics();
		GraphicsUtil.switchToWidth(g, 2);
		BitWidth widthVal = painter.getAttributeValue(StdAttr.WIDTH);
		int width = widthVal == null ? 8 : widthVal.getWidth();
		g.drawLine(xpos + 20, ypos, xpos + 20 + SymbolWidth(width), ypos);
		g.drawLine(xpos + 20, ypos, xpos + 20, ypos + 100);
		g.drawLine(xpos + 20 + SymbolWidth(width), ypos, xpos + 20
				+ SymbolWidth(width), ypos + 100);
		g.drawLine(xpos + 20, ypos + 100, xpos + 30, ypos + 100);
		g.drawLine(xpos + 10 + SymbolWidth(width), ypos + 100, xpos + 20
				+ SymbolWidth(width), ypos + 100);
		g.drawLine(xpos + 30, ypos + 100, xpos + 30, ypos + 110);
		g.drawLine(xpos + 10 + SymbolWidth(width), ypos + 100, xpos + 10
				+ SymbolWidth(width), ypos + 110);
		/* Draw clock entry symbols */
		painter.drawClockSymbol(xpos + 20, ypos + 80);
		painter.drawClockSymbol(xpos + 20, ypos + 90);
		/* Draw Label */
		int max = painter.getAttributeValue(ATTR_MAX).intValue();
		boolean IsCTRm = (max == painter.getAttributeValue(StdAttr.WIDTH)
				.getMask());
		Object onGoal = painter.getAttributeValue(ATTR_ON_GOAL);
		IsCTRm |= onGoal == ON_GOAL_CONT;
		String Label = (IsCTRm) ? "CTR"
				+ Integer.toString(painter.getAttributeValue(StdAttr.WIDTH)
						.getWidth()) : "CTR DIV0x" + Integer.toHexString(max);
		GraphicsUtil.drawCenteredText(g, Label, xpos + (SymbolWidth(width) / 2)
				+ 20, ypos + 5);
		GraphicsUtil.switchToWidth(g, 2);
		/* Draw Reset Input */
		g.drawLine(xpos, ypos + 20, xpos + 20, ypos + 20);
		GraphicsUtil.drawText(g, "R", xpos + 30, ypos + 20,
				GraphicsUtil.H_LEFT, GraphicsUtil.V_CENTER);
		painter.drawPort(CLR);
		/* Draw Load Input */
		g.drawLine(xpos, ypos + 30, xpos + 20, ypos + 30);
		g.drawLine(xpos + 5, ypos + 40, xpos + 12, ypos + 40);
		g.drawLine(xpos + 5, ypos + 30, xpos + 5, ypos + 40);
		g.drawOval(xpos + 12, ypos + 36, 8, 8);
		g.fillOval(xpos + 2, ypos + 27, 6, 6);
		painter.drawPort(LD);
		GraphicsUtil.drawText(g, "M2 [count]", xpos + 30, ypos + 40,
				GraphicsUtil.H_LEFT, GraphicsUtil.V_CENTER);
		GraphicsUtil.drawText(g, "M1 [load]", xpos + 30, ypos + 30,
				GraphicsUtil.H_LEFT, GraphicsUtil.V_CENTER);
		/* Draw UpDn input */
		g.drawLine(xpos, ypos + 50, xpos + 20, ypos + 50);
		g.drawLine(xpos + 5, ypos + 60, xpos + 12, ypos + 60);
		g.drawLine(xpos + 5, ypos + 50, xpos + 5, ypos + 60);
		g.drawOval(xpos + 12, ypos + 56, 8, 8);
		g.fillOval(xpos + 2, ypos + 47, 6, 6);
		GraphicsUtil.drawText(g, "M3 [up]", xpos + 30, ypos + 50,
				GraphicsUtil.H_LEFT, GraphicsUtil.V_CENTER);
		GraphicsUtil.drawText(g, "M4 [down]", xpos + 30, ypos + 60,
				GraphicsUtil.H_LEFT, GraphicsUtil.V_CENTER);
		painter.drawPort(UD);
		/* Draw Enable Port */
		g.drawLine(xpos, ypos + 70, xpos + 20, ypos + 70);
		GraphicsUtil.drawText(g, "G5", xpos + 30, ypos + 70,
				GraphicsUtil.H_LEFT, GraphicsUtil.V_CENTER);
		painter.drawPort(EN);
		/* Draw Clock */
		boolean inverted = painter.getAttributeValue(StdAttr.EDGE_TRIGGER)
				.equals(StdAttr.TRIG_FALLING);
		int xend = (inverted) ? xpos + 12 : xpos + 20;
		g.drawLine(xpos, ypos + 80, xend, ypos + 80);
		g.drawLine(xpos + 5, ypos + 90, xend, ypos + 90);
		g.drawLine(xpos + 5, ypos + 80, xpos + 5, ypos + 90);
		g.fillOval(xpos + 2, ypos + 77, 6, 6);
		if (inverted) {
			g.drawOval(xend, ypos + 76, 8, 8);
			g.drawOval(xend, ypos + 86, 8, 8);
		}
		GraphicsUtil.drawText(g, "2,3,5+/C6", xpos + 30, ypos + 80,
				GraphicsUtil.H_LEFT, GraphicsUtil.V_CENTER);
		GraphicsUtil.drawText(g, "2,4,5-", xpos + 30, ypos + 90,
				GraphicsUtil.H_LEFT, GraphicsUtil.V_CENTER);
		painter.drawPort(CK);
		/* Draw Carry */
		g.drawLine(xpos + 20 + SymbolWidth(width), ypos + 50, xpos + 40
				+ SymbolWidth(width), ypos + 50);
		g.drawLine(xpos + 20 + SymbolWidth(width), ypos + 60, xpos + 35
				+ SymbolWidth(width), ypos + 60);
		g.drawLine(xpos + 35 + SymbolWidth(width), ypos + 50, xpos + 35
				+ SymbolWidth(width), ypos + 60);
		g.fillOval(xpos + 32 + SymbolWidth(width), ypos + 47, 6, 6);
		String MaxVal = "3CT=0x"
				+ Integer.toHexString(
						painter.getAttributeValue(ATTR_MAX).intValue())
						.toUpperCase();
		GraphicsUtil.drawText(g, MaxVal, xpos + 17 + SymbolWidth(width),
				ypos + 50, GraphicsUtil.H_RIGHT, GraphicsUtil.V_CENTER);
		GraphicsUtil.drawText(g, "4CT=0", xpos + 17 + SymbolWidth(width),
				ypos + 60, GraphicsUtil.H_RIGHT, GraphicsUtil.V_CENTER);
		painter.drawPort(CARRY);
		GraphicsUtil.switchToWidth(g, 1);
		/* Draw counter Value */
		RegisterData state = (RegisterData) painter.getData();
		if (painter.getShowState()&&(state != null)) {
			int len = (width + 3) / 4;
			int xcenter = SymbolWidth(width) - 25;
			Value val = state.value;
			if (val.isFullyDefined())
				g.setColor(Color.LIGHT_GRAY);
			else if (val.isErrorValue())
				g.setColor(Color.RED);
			else
				g.setColor(Color.BLUE);
			g.fillRect(xpos + xcenter - len * 4, ypos + 22, len * 8, 16);
			String Value = "";
			if (val.isFullyDefined()) {
				g.setColor(Color.DARK_GRAY);
				Value = StringUtil.toHexString(width, val.toIntValue()).toUpperCase(); 
			} else {
				g.setColor(Color.YELLOW);
				for (int i = 0 ; i < StringUtil.toHexString(width, val.toIntValue()).length() ; i++)
					Value = (val.isUnknown()) ? Value.concat("?") : Value.concat("!"); 
			}
			GraphicsUtil.drawText(g, Value, xpos + xcenter - len * 4 + 1,
					ypos + 30, GraphicsUtil.H_LEFT, GraphicsUtil.V_CENTER);
			g.setColor(Color.BLACK);
		}
	}

	private void DrawDataBlock(InstancePainter painter, int xpos, int ypos,
			int BitNr, int NrOfBits) {
		int RealYpos = ypos + BitNr * 20;
		boolean first = BitNr == 0;
		boolean last = BitNr == (NrOfBits - 1);
		Graphics g = painter.getGraphics();
		Font font = g.getFont();
		g.setFont(font.deriveFont(7.0f));
		GraphicsUtil.switchToWidth(g, 2);
		g.drawRect(xpos + 20, RealYpos, SymbolWidth(NrOfBits), 20);
		/* Input Line */
		if (NrOfBits > 1) {
			g.drawLine(xpos + 10, RealYpos + 10, xpos + 20, RealYpos + 10);
			g.drawLine(xpos + 5, RealYpos + 5, xpos + 10, RealYpos + 10);
		} else {
			g.drawLine(xpos, RealYpos + 10, xpos + 20, RealYpos + 10);
		}
		/* Ouput Line */
		if (NrOfBits > 1) {
			g.drawLine(xpos + 20 + SymbolWidth(NrOfBits), RealYpos + 10, xpos
					+ 30 + SymbolWidth(NrOfBits), RealYpos + 10);
			g.drawLine(xpos + 30 + SymbolWidth(NrOfBits), RealYpos + 10, xpos
					+ 35 + SymbolWidth(NrOfBits), RealYpos + 5);
		} else {
			g.drawLine(xpos + 20 + SymbolWidth(NrOfBits), RealYpos + 10, xpos
					+ 40 + SymbolWidth(NrOfBits), RealYpos + 10);
		}
		g.setColor(Color.BLACK);
		if (NrOfBits > 1) {
			GraphicsUtil.drawText(g, Integer.toString(BitNr), xpos + 30
					+ SymbolWidth(NrOfBits), RealYpos + 8,
					GraphicsUtil.H_RIGHT, GraphicsUtil.V_BASELINE);
			GraphicsUtil.drawText(g, Integer.toString(BitNr), xpos + 10,
					RealYpos + 8, GraphicsUtil.H_LEFT, GraphicsUtil.V_BASELINE);
		}
		g.setFont(font);
		GraphicsUtil.drawText(g, "1,6D", xpos + 21, RealYpos + 10,
				GraphicsUtil.H_LEFT, GraphicsUtil.V_CENTER);
		int LineWidth = (NrOfBits == 1) ? 2 : 5;
		GraphicsUtil.switchToWidth(g, LineWidth);
		if (first) {
			painter.drawPort(IN);
			painter.drawPort(OUT);
			if (NrOfBits > 1) {
				g.drawLine(xpos, RealYpos, xpos + 5, RealYpos + 5);
				g.drawLine(xpos + 35 + SymbolWidth(NrOfBits), RealYpos + 5,
						xpos + 40 + SymbolWidth(NrOfBits), RealYpos);
				g.drawLine(xpos + 5, RealYpos + 5, xpos + 5, RealYpos + 20);
				g.drawLine(xpos + 35 + SymbolWidth(NrOfBits), RealYpos + 5,
						xpos + 35 + SymbolWidth(NrOfBits), RealYpos + 20);
			}
		} else if (last) {
			g.drawLine(xpos + 5, RealYpos, xpos + 5, RealYpos + 5);
			g.drawLine(xpos + 35 + SymbolWidth(NrOfBits), RealYpos, xpos + 35
					+ SymbolWidth(NrOfBits), RealYpos + 5);
		} else {
			g.drawLine(xpos + 5, RealYpos, xpos + 5, RealYpos + 20);
			g.drawLine(xpos + 35 + SymbolWidth(NrOfBits), RealYpos, xpos + 35
					+ SymbolWidth(NrOfBits), RealYpos + 20);
		}
		GraphicsUtil.switchToWidth(g, 1);
		RegisterData state = (RegisterData) painter.getData();
		if (painter.getShowState()&&(state != null)) {
			/* Here we draw the bit value */
			Value val = state.value;
			BitWidth widthVal = painter.getAttributeValue(StdAttr.WIDTH);
			int width = widthVal == null ? 8 : widthVal.getWidth();
			int xcenter = (SymbolWidth(width) / 2) + 10;
			String value = "";
			if (val.isFullyDefined()) {
				g.setColor(Color.LIGHT_GRAY);
				value = ((1 << BitNr) & val.toIntValue()) != 0 ? "1" : "0";
			} else if (val.isUnknown()) {
				g.setColor(Color.BLUE);
				value = "?";
			} else {
				g.setColor(Color.RED);
				value = "!";
			}
			g.fillRect(xpos + xcenter + 16, RealYpos + 4, 8, 16);
			if (val.isFullyDefined())
				g.setColor(Color.DARK_GRAY);
			else
				g.setColor(Color.YELLOW);
			GraphicsUtil.drawText(g, value, xpos + xcenter
					+ 20, RealYpos + 10, GraphicsUtil.H_CENTER,
					GraphicsUtil.V_CENTER);
			g.setColor(Color.BLACK);
		}
	}

	@Override
	public String getHDLName(AttributeSet attrs) {
		return "LogisimCounter";
	}

	@Override
	public Bounds getOffsetBounds(AttributeSet attrs) {
		BitWidth widthVal = attrs.getValue(StdAttr.WIDTH);
		int width = widthVal == null ? 8 : widthVal.getWidth();
		return Bounds.create(0, 0, SymbolWidth(width) + 40, 110 + 20 * width);
	}

	@Override
	public boolean HDLSupportedComponent(String HDLIdentifier,
			AttributeSet attrs) {
		if (MyHDLGenerator == null)
			MyHDLGenerator = new CounterHDLGeneratorFactory();
		return MyHDLGenerator.HDLTargetSupported(HDLIdentifier, attrs);
	}

	@Override
	protected void instanceAttributeChanged(Instance instance, Attribute<?> attr) {
		if (attr == StdAttr.WIDTH) {
			instance.recomputeBounds();
			configurePorts(instance);
		}
	}

	@Override
	public void paintInstance(InstancePainter painter) {
		int Xpos = painter.getLocation().getX();
		int Ypos = painter.getLocation().getY();
		painter.drawLabel();

		DrawControl(painter, Xpos, Ypos);
		BitWidth widthVal = painter.getAttributeValue(StdAttr.WIDTH);
		int width = widthVal == null ? 8 : widthVal.getWidth();
		for (int bit = 0; bit < width; bit++) {
			DrawDataBlock(painter, Xpos, Ypos + 110, bit, width);
		}
	}

	@Override
	public void propagate(InstanceState state) {
		RegisterData data = (RegisterData) state.getData();
		if (data == null) {
			data = new RegisterData(state.getAttributeValue(StdAttr.WIDTH));
			state.setData(data);
		}

		BitWidth dataWidth = state.getAttributeValue(StdAttr.WIDTH);
		Object triggerType = state.getAttributeValue(StdAttr.EDGE_TRIGGER);
		int max = state.getAttributeValue(ATTR_MAX).intValue();
		Value clock = state.getPortValue(CK);
		boolean triggered = data.updateClock(clock, triggerType);

		Value newValue;
		boolean carry;
		if (state.getPortValue(CLR) == Value.TRUE) {
			newValue = Value.createKnown(dataWidth, 0);
			carry = false;
		} else {
			boolean ld = state.getPortValue(LD) == Value.TRUE;
			boolean en = state.getPortValue(EN) != Value.FALSE;
			boolean UpCount = state.getPortValue(UD) != Value.FALSE;
			Value oldVal = data.value;
			Value newVal;
			if (!triggered) {
				newVal = oldVal;
			} else if (ld) {
				Value in = state.getPortValue(IN);
				newVal = in;
				if (newVal.toIntValue() > max)
					newVal = Value.createKnown(dataWidth, newVal.toIntValue()&max);
			} else if (!oldVal.isFullyDefined()) {
				newVal = oldVal;
			} else if (en) {
				int goal = (UpCount) ? max : 0;
				if (oldVal.toIntValue() == goal) {
					Object onGoal = state.getAttributeValue(ATTR_ON_GOAL);
					if (onGoal == ON_GOAL_WRAP) {
						newVal = Value.createKnown(dataWidth, (UpCount) ? 0 : max);
					} else if (onGoal == ON_GOAL_STAY) {
						newVal = oldVal;
					} else if (onGoal == ON_GOAL_LOAD) {
						Value in = state.getPortValue(IN);
						newVal = in;
						if (newVal.toIntValue() > max)
							newVal = Value.createKnown(dataWidth, newVal.toIntValue()&max);
					} else if (onGoal == ON_GOAL_CONT) {
						newVal = Value.createKnown(dataWidth, (UpCount) ? oldVal.toIntValue() + 1 : oldVal.toIntValue() - 1);
					} else {
						logger.error("Invalid goal attribute {}", onGoal);
						newVal = Value.createKnown(dataWidth, ld ? max : 0);
					}
				} else {
					newVal = Value.createKnown(dataWidth, (UpCount) ? oldVal.toIntValue() + 1 : oldVal.toIntValue() - 1);
				}
			} else {
				newVal = oldVal;
			}
			newValue = newVal;
			carry = newVal.toIntValue() == (UpCount ? max : 0);
			/*
			 * I would want this if I were worried about the carry signal
			 * outrunning the clock. But the component's delay should be enough
			 * to take care of it. if (carry) { if (triggerType ==
			 * StdAttr.TRIG_FALLING) { carry = clock == Value.TRUE; } else {
			 * carry = clock == Value.FALSE; } }
			 */
		}

		data.value = newValue;
		state.setPort(OUT, newValue, DELAY);
		state.setPort(CARRY, carry ? Value.TRUE : Value.FALSE, DELAY);
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