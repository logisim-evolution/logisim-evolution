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
import java.awt.event.MouseEvent;

import com.bfh.logisim.designrulecheck.Netlist;
import com.bfh.logisim.designrulecheck.NetlistComponent;
import com.cburch.logisim.data.Attribute;
import com.cburch.logisim.data.AttributeOption;
import com.cburch.logisim.data.AttributeSet;
import com.cburch.logisim.data.Bounds;
import com.cburch.logisim.data.Direction;
import com.cburch.logisim.data.Location;
import com.cburch.logisim.data.Value;
import com.cburch.logisim.instance.Instance;
import com.cburch.logisim.instance.InstanceData;
import com.cburch.logisim.instance.InstanceFactory;
import com.cburch.logisim.instance.InstanceLogger;
import com.cburch.logisim.instance.InstancePainter;
import com.cburch.logisim.instance.InstancePoker;
import com.cburch.logisim.instance.InstanceState;
import com.cburch.logisim.instance.Port;
import com.cburch.logisim.instance.StdAttr;
import com.cburch.logisim.prefs.AppPreferences;
import com.cburch.logisim.util.GraphicsUtil;
import com.cburch.logisim.util.StringGetter;

abstract class AbstractFlipFlop extends InstanceFactory {
	public static class Logger extends InstanceLogger {
		@Override
		public String getLogName(InstanceState state, Object option) {
			String ret = state.getAttributeValue(StdAttr.LABEL);
			return ret != null && !ret.equals("") ? ret : null;
		}

		@Override
		public Value getLogValue(InstanceState state, Object option) {
			StateData s = (StateData) state.getData();
			return s == null ? Value.FALSE : s.curValue;
		}
	}

	public static class Poker extends InstancePoker {
		boolean isPressed = true;

		private boolean isInside(InstanceState state, MouseEvent e) {
			Location loc = state.getInstance().getLocation();
			int dx = e.getX() - (loc.getX() + 20);
			int dy = e.getY() - (loc.getY() + 30);
			int d2 = dx * dx + dy * dy;
			return d2 < 8 * 8;
		}

		@Override
		public void mousePressed(InstanceState state, MouseEvent e) {
			isPressed = isInside(state, e);
		}

		@Override
		public void mouseReleased(InstanceState state, MouseEvent e) {
			if (isPressed && isInside(state, e)) {
				StateData myState = (StateData) state.getData();
				if (myState == null)
					return;

				myState.curValue = myState.curValue.not();
				state.fireInvalidated();
			}
			isPressed = false;
		}
	}

	private static class StateData extends ClockState implements InstanceData {
		Value curValue = (AppPreferences.Memory_Startup_Unknown.get()) ? Value.UNKNOWN : Value.FALSE;
	}

	private static final int STD_PORTS = 5;

	private Attribute<AttributeOption> triggerAttribute;

	protected AbstractFlipFlop(String name, String iconName, StringGetter desc,
			int numInputs, boolean allowLevelTriggers) {
		super(name, desc);
		setIconName(iconName);
		triggerAttribute = allowLevelTriggers ? StdAttr.TRIGGER
				: StdAttr.EDGE_TRIGGER;
		setAttributes(new Attribute[] { triggerAttribute, StdAttr.LABEL,
				StdAttr.LABEL_FONT }, new Object[] { StdAttr.TRIG_RISING, "",
				StdAttr.DEFAULT_LABEL_FONT });
		setOffsetBounds(Bounds.create(-10, 0, 60, 60));
		setInstancePoker(Poker.class);
		setInstanceLogger(Logger.class);

		Port[] ps = new Port[numInputs + STD_PORTS];
		if (numInputs == 1) {
			ps[0] = new Port(-10, 10, Port.INPUT, 1);
			ps[1] = new Port(-10, 50, Port.INPUT, 1);
		} else if (numInputs == 2) {
			ps[0] = new Port(-10, 10, Port.INPUT, 1);
			ps[1] = new Port(-10, 30, Port.INPUT, 1);
			ps[2] = new Port(-10, 50, Port.INPUT, 1);
		} else {
			throw new RuntimeException("flip-flop input > 2");
		}
		ps[numInputs + 1] = new Port(50, 10, Port.OUTPUT, 1);
		ps[numInputs + 2] = new Port(50, 50, Port.OUTPUT, 1);
		ps[numInputs + 3] = new Port(20, 60, Port.INPUT, 1);
		ps[numInputs + 4] = new Port(20, 0, Port.INPUT, 1);
		ps[numInputs].setToolTip(Strings.getter("flipFlopClockTip"));
		ps[numInputs + 1].setToolTip(Strings.getter("flipFlopQTip"));
		ps[numInputs + 2].setToolTip(Strings.getter("flipFlopNotQTip"));
		ps[numInputs + 3].setToolTip(Strings.getter("flipFlopResetTip"));
		ps[numInputs + 4].setToolTip(Strings.getter("flipFlopPresetTip"));
		setPorts(ps);
	}

	protected abstract Value computeValue(Value[] inputs, Value curValue);

	//
	// concrete methods not intended to be overridden
	//
	@Override
	protected void configureNewInstance(Instance instance) {
		Bounds bds = instance.getBounds();
		instance.setTextField(StdAttr.LABEL, StdAttr.LABEL_FONT, bds.getX()
				+ bds.getWidth() / 2, bds.getY() - 3, GraphicsUtil.H_CENTER,
				GraphicsUtil.V_BASELINE);
	}

	@Override
	public String getHDLName(AttributeSet attrs) {
		StringBuffer CompleteName = new StringBuffer();
		String[] Parts = this.getName().split(" ");
		CompleteName.append(Parts[0].replace("-", "_").toUpperCase());
		CompleteName.append("_");
		if (attrs.containsAttribute(StdAttr.EDGE_TRIGGER)) {
			CompleteName.append("FlipFlop".toUpperCase());
		} else {
			if (attrs.containsAttribute(StdAttr.TRIGGER)) {
				if ((attrs.getValue(StdAttr.TRIGGER) == StdAttr.TRIG_FALLING)
						|| (attrs.getValue(StdAttr.TRIGGER) == StdAttr.TRIG_RISING)) {
					CompleteName.append("FlipFlop".toUpperCase());
				} else {
					CompleteName.append("Latch".toUpperCase());
				}
			} else {
				CompleteName.append("FlipFlop".toUpperCase());
			}
		}
		return CompleteName.toString();
	}

	//
	// abstract methods intended to be implemented in subclasses
	//
	protected abstract String getInputName(int index);

	@Override
	public void paintInstance(InstancePainter painter) {
		Graphics g = painter.getGraphics();
		painter.drawLabel();
		Location loc = painter.getLocation();
		int x = loc.getX();
		int y = loc.getY();
		GraphicsUtil.switchToWidth(g, 2);
		g.drawRect(x, y, 40, 60);
		if (painter.getShowState()) {
			StateData myState = (StateData) painter.getData();
			if (myState != null) {
				g.setColor(myState.curValue.getColor());
				g.fillOval(x + 13, y + 23, 14, 14);
				g.setColor(Color.WHITE);
				GraphicsUtil.drawCenteredText(g,
						myState.curValue.toDisplayString(), x + 21, y + 29);
				g.setColor(Color.BLACK);
			}
		}

		int n = getPorts().size() - STD_PORTS;
		g.setColor(Color.GRAY);
		painter.drawPort(n + 3, "R", Direction.SOUTH);
		painter.drawPort(n + 4, "S", Direction.NORTH);
		g.setColor(Color.BLACK);
		for (int i = 0; i < n; i++) {
			g.fillRect(x - 10, y + 9 + i * 20, 10, 3);
			painter.drawPort(i);
			GraphicsUtil.drawCenteredText(g, getInputName(i), x + 8, y + 10 + i
					* 20);
		}
		Object Trigger = painter.getAttributeValue(triggerAttribute);
		if (Trigger.equals(StdAttr.TRIG_RISING)
				|| Trigger.equals(StdAttr.TRIG_FALLING)) {
			painter.drawClockSymbol(x, y + 50);
		} else {
			GraphicsUtil.drawCenteredText(g, "E", x + 8, y + 50);
		}
		if (Trigger.equals(StdAttr.TRIG_RISING)
				|| Trigger.equals(StdAttr.TRIG_HIGH)) {
			g.fillRect(x - 10, y + 49, 10, 3);
		} else {
			GraphicsUtil.switchToWidth(g, 2);
			g.drawOval(x - 10, y + 45, 10, 10);
			GraphicsUtil.switchToWidth(g, 1);
		}
		painter.drawPort(n);

		g.fillRect(x + 40, y + 9, 10, 3);
		GraphicsUtil.drawCenteredText(g, "Q", x + 31, y + 10);
		painter.drawPort(n + 1);
		GraphicsUtil.switchToWidth(g, 2);
		g.drawOval(x + 40, y + 45, 10, 10);
		GraphicsUtil.switchToWidth(g, 1);
		painter.drawPort(n + 2);
	}

	@Override
	public void propagate(InstanceState state) {
		// boolean changed = false;
		StateData data = (StateData) state.getData();
		if (data == null) {
			// changed = true;
			data = new StateData();
			state.setData(data);
		}

		int n = getPorts().size() - STD_PORTS;
		Object triggerType = state.getAttributeValue(triggerAttribute);
		boolean triggered = data
				.updateClock(state.getPortValue(n), triggerType);

		if (state.getPortValue(n + 3) == Value.TRUE) { // clear requested
			// changed |= data.curValue != Value.FALSE;
			data.curValue = Value.FALSE;
		} else if (state.getPortValue(n + 4) == Value.TRUE) { // preset
																// requested
			// changed |= data.curValue != Value.TRUE;
			data.curValue = Value.TRUE;
		} else if (triggered/* && state.getPortValue(n + 5) != Value.FALSE */) {
			// Clock has triggered and flip-flop is enabled: Update the state
			Value[] inputs = new Value[n];
			for (int i = 0; i < n; i++) {
				inputs[i] = state.getPortValue(i);
			}

			Value newVal = computeValue(inputs, data.curValue);
			if (newVal == Value.TRUE || newVal == Value.FALSE) {
				// changed |= data.curValue != newVal;
				data.curValue = newVal;
			}
		}

		state.setPort(n + 1, data.curValue, Memory.DELAY);
		state.setPort(n + 2, data.curValue.not(), Memory.DELAY);
	}
	
	@Override
	public boolean CheckForGatedClocks(NetlistComponent comp) {
		return Netlist.IsFlipFlop(comp.GetComponent().getAttributeSet());
	}
	
	@Override
	public int[] ClockPinIndex(NetlistComponent comp) {
		return new int[] {getPorts().size() - STD_PORTS};
	}
}
