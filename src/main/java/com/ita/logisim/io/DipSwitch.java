package com.ita.logisim.io;

import java.awt.Color;
import java.awt.Graphics;
import java.awt.event.MouseEvent;
import java.util.ArrayList;

import com.bfh.logisim.fpgaboardeditor.FPGAIOInformationContainer;
import com.bfh.logisim.hdlgenerator.IOComponentInformationContainer;
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
import com.cburch.logisim.instance.InstanceLogger;
import com.cburch.logisim.instance.InstancePainter;
import com.cburch.logisim.instance.InstancePoker;
import com.cburch.logisim.instance.InstanceState;
import com.cburch.logisim.instance.Port;
import com.cburch.logisim.instance.StdAttr;
import com.cburch.logisim.std.io.ButtonHDLGeneratorFactory;
import com.cburch.logisim.std.io.Io;
import com.cburch.logisim.util.GraphicsUtil;

public class DipSwitch extends InstanceFactory {
	public static class Logger extends InstanceLogger {
		@Override
		public String getLogName(InstanceState state, Object option) {
			String inName = state.getAttributeValue(StdAttr.LABEL);
			if (inName == null || inName.equals("")) {
				inName = Strings.get("DipSwitchComponent") + state.getInstance().getLocation();
			}
			if (option instanceof Integer) {
				return inName + "[" + option + "]";
			} else {
				return inName;
			}
		}

		@Override
		public Object[] getLogOptions(InstanceState state) {
			byte stages = state.getAttributeValue(ATTR_NSWITCHES).byteValue();
			Object[] ret = new Object[stages];
			for (int i = 0; i < ret.length; i++) {
				ret[i] = Integer.valueOf(i);
			}
			return ret;
		}

		@Override
		public Value getLogValue(InstanceState state, Object option) {
			return state.getPortValue((Integer) option);
		}
	}

	private class pinValues implements InstanceData, Cloneable {
		Value[] vals = null;

		private pinValues(InstanceState state) {
			vals = new Value[state.getAttributeValue(ATTR_NSWITCHES)];
			Valfalse(vals);
			state.setData(this);
		}

		@Override
		public Object clone() {
			try {
				return super.clone();
			} catch (CloneNotSupportedException e) {
				return null;
			}
		}

		private Value getValue(int i) {
			if (vals[i] == null)
				return Value.FALSE;
			else
				return vals[i];
		}

		private Value[] getValues() {
			return vals;
		}

		private void setValue(InstanceState state, Value val, int port) {
			vals[port] = val;
			state.getInstance().fireInvalidated();
		}

		private void updateValues(int switches) {
			if (getValues().length != switches) {
				Value[] oldvals = getValues();
				vals = new Value[switches];
				byte min = (byte) (oldvals.length < switches ? oldvals.length : switches);
				for (byte i = 0; i < min; i++) {
					vals[i] = oldvals[i];
				}
			}
		}

		private void Valfalse(Value[] vals) {
			for (int i = 0; i < vals.length; i++)
				vals[i] = Value.FALSE;
		}

	}

	public static class Poker extends InstancePoker {

		@Override
		public void mouseReleased(InstanceState state, MouseEvent e) {
			pinValues obj = (pinValues) state.getData();
			Location loc = state.getInstance().getLocation();
			int cx = loc.getX();
			int cy = loc.getY();
			Direction dir = state.getAttributeValue(StdAttr.FACING);
			int gx = e.getX();
			int gy = e.getY();
			int x = gx - cx;
			int y = gy - cy;
			if ((y < -5 && y > -25) && (dir == Direction.SOUTH)) {
				x -= 5;
				x = x / 10;
				if (x % 2 == 0) {
					x = x / 2;
					obj.setValue(state, obj.getValue(x).not(), x);// opposite value
				}
			} else if ((x < -5 && x > -25) && (dir == Direction.EAST)) {
				y -= 5;
				y = y / 10;
				if (y % 2 == 0) {
					y = y / 2;
					obj.setValue(state, obj.getValue(y).not(), y);// opposite value
				}
			} else if ((y > 5 && y < 25) && (dir == Direction.NORTH)) {
				x -= 5;
				x = x / 10;
				if (x % 2 == 0) {
					x = x / 2;
					obj.setValue(state, obj.getValue(x).not(), x);// opposite value
				}
			} else if ((x > 5 && x < 25) && (dir == Direction.WEST)) {
				y -= 5;
				y /= 10;
				if (y % 2 == 0) {
					y /= 2;
					obj.setValue(state, obj.getValue(y).not(), y);// opposite value
				}
			}
		}
	}

	private static final Attribute<Integer> ATTR_NSWITCHES = Attributes.forIntegerRange("NSwitches",
			Strings.getter("NumberOfSwitch"), 1, 32);

	private static final int DEPTH = 3;

	public DipSwitch() {
		super("DipSwitch", Strings.getter("DipSwitchComponent"));
		setAttributes(
				new Attribute[] { StdAttr.FACING, ATTR_NSWITCHES, StdAttr.LABEL, Io.ATTR_LABEL_LOC,
						StdAttr.LABEL_FONT,StdAttr.LABEL_VISIBILITY},
				new Object[] { Direction.EAST, 4, "", Direction.NORTH, StdAttr.DEFAULT_LABEL_FONT,false});
		setFacingAttribute(StdAttr.FACING);
		setIconName("dipswitch.gif");
		setInstancePoker(Poker.class);
		setInstanceLogger(Logger.class);
		MyIOInformation = new IOComponentInformationContainer(DEPTH, 0, 0,
				GetLabels(DEPTH), null, null,
				FPGAIOInformationContainer.IOComponentTypes.DIPSwitch);
		MyIOInformation
				.AddAlternateMapType(FPGAIOInformationContainer.IOComponentTypes.Button);
		MyIOInformation
				.AddAlternateMapType(FPGAIOInformationContainer.IOComponentTypes.Pin);
	}

	private void computeTextField(Instance instance) {
		Direction facing = instance.getAttributeValue(StdAttr.FACING);
		Object labelLoc = instance.getAttributeValue(Io.ATTR_LABEL_LOC);

		Bounds bds = instance.getBounds();
		int x = bds.getX() + bds.getWidth() / 2;
		int y = bds.getY() + bds.getHeight() / 2;
		int halign = GraphicsUtil.H_CENTER;
		int valign = GraphicsUtil.V_CENTER_OVERALL;
		if (labelLoc == Io.LABEL_CENTER) {
			x = bds.getX() + (bds.getWidth() - DEPTH) / 2;
			y = bds.getY() + (bds.getHeight() - DEPTH) / 2;
		} else if (labelLoc == Direction.NORTH) {
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

		instance.setTextField(StdAttr.LABEL, StdAttr.LABEL_FONT, x, y, halign, valign);
	}

	@Override
	protected void configureNewInstance(Instance instance) {
		instance.addAttributeListener();
		computeTextField(instance);
		updateports(instance);
		MyIOInformation.setNrOfInports(instance.getAttributeValue(ATTR_NSWITCHES),
				GetLabels(instance.getAttributeValue(ATTR_NSWITCHES)));
	}

	public static final ArrayList<String> GetLabels(int size) {
		ArrayList<String> LabelNames = new ArrayList<String>();
		for (int i = 0; i < size; i++) {
			LabelNames.add("sw_" + Integer.toString(i + 1));
		}
		return LabelNames;
	}

	@Override
	public Bounds getOffsetBounds(AttributeSet attrs) {
		Direction facing = attrs.getValue(StdAttr.FACING);
		short y = (short) (attrs.getValue(ATTR_NSWITCHES).intValue() * 20);
		if (facing == Direction.EAST)
			return Bounds.create(-30, 0, 30, y);
		else if (facing == Direction.WEST)
			return Bounds.create(0, 0, 30, y);
		else if (facing == Direction.NORTH)
			return Bounds.create(0, 0, y, 30);
		else
			return Bounds.create(0, -30, y, 30);
	}

	private pinValues getValueState(InstanceState state) {
		byte switches = state.getAttributeValue(ATTR_NSWITCHES).byteValue();
		pinValues ret = (pinValues) state.getData();
		if (ret == null) {
			ret = new pinValues(state);
			state.setData(ret);
		} else {
			ret.updateValues(switches);
		}
		return ret;
	}

	@Override
	protected void instanceAttributeChanged(Instance instance, Attribute<?> attr) {
		if (attr == StdAttr.FACING || attr == ATTR_NSWITCHES) {
			instance.recomputeBounds();
			computeTextField(instance);
			updateports(instance);
		} else if (attr == Io.ATTR_LABEL_LOC) {
			computeTextField(instance);
		}
	}

	@Override
	public void paintGhost(InstancePainter painter) {
		Bounds bds = painter.getBounds();
		Graphics g = painter.getGraphics();
		GraphicsUtil.switchToWidth(g, 2);
		g.drawRoundRect(bds.getX(), bds.getY(), bds.getWidth(), bds.getHeight(), 10, 10);
	}

	@Override
	public void paintInstance(InstancePainter painter) {
		pinValues obj = getValueState(painter);
		Bounds bds = painter.getBounds();
		Direction dir = painter.getAttributeValue(StdAttr.FACING);
		painter.drawBounds();

		int x = bds.getX();
		int y = bds.getY();
		byte switches = painter.getAttributeValue(ATTR_NSWITCHES).byteValue();
		Graphics g = painter.getGraphics();
		GraphicsUtil.switchToWidth(g, 2);
		if (dir == Direction.EAST) {
			g.setColor(Color.BLACK);
			for (byte i = 1; i < switches; i++) {
				g.drawLine(0 + x, i * 20 + y, 30 + x, i * 20 + y);

			}
			for (byte i = 0; i < switches; i++) {
				if (obj.getValue(i) != Value.FALSE) {
					g.setColor(Value.TRUE_COLOR);
					g.fillRect(5 + x, i * 20 + 5 + y, 20, 10);
					g.setColor(Color.DARK_GRAY);
					g.fillRect(15 + x, i * 20 + 5 + y, 10, 10);
					g.setColor(Color.BLACK);
					g.drawRect(15 + x, i * 20 + 5 + y, 10, 10);
					if (i<9) {
						g.setColor(Color.WHITE);
						GraphicsUtil.drawCenteredText(g, Integer.toString(i+1), 20+x, i*20+y+10);
					}
				} else if (obj.getValue(i) != Value.TRUE) {
					g.setColor(Value.FALSE_COLOR);
					g.fillRect(5 + x, i * 20 + 5 + y, 20, 10);
					g.setColor(Color.DARK_GRAY);
					g.fillRect(5 + x, i * 20 + 5 + y, 10, 10);
					g.setColor(Color.BLACK);
					g.drawRect(5 + x, i * 20 + 5 + y, 10, 10);
					if (i<9) {
						g.setColor(Color.WHITE);
						GraphicsUtil.drawCenteredText(g, Integer.toString(i+1), 10+x, i*20+y+10);
					}
				}
			}
		} else if (dir == Direction.WEST) {
			g.setColor(Color.BLACK);
			for (byte i = 1; i < switches; i++) {
				g.drawLine(0 + x, i * 20 + y, 30 + x, i * 20 + y);

			}
			for (byte i = 0; i < switches; i++) {
				if (obj.getValue(i) != Value.FALSE) {
					g.setColor(Value.TRUE_COLOR);
					g.fillRect(5 + x, i * 20 + 5 + y, 20, 10);
					g.setColor(Color.DARK_GRAY);
					g.fillRect(5 + x, i * 20 + 5 + y, 10, 10);
					g.setColor(Color.BLACK);
					g.drawRect(5 + x, i * 20 + 5 + y, 10, 10);
				} else if (obj.getValue(i) != Value.TRUE) {
					g.setColor(Value.FALSE_COLOR);
					g.fillRect(5 + x, i * 20 + 5 + y, 20, 10);
					g.setColor(Color.DARK_GRAY);
					g.fillRect(15 + x, i * 20 + 5 + y, 10, 10);
					g.setColor(Color.BLACK);
					g.drawRect(15 + x, i * 20 + 5 + y, 10, 10);
				}
			}
		} else if (dir == Direction.NORTH) {
			g.setColor(Color.BLACK);
			for (byte i = 0; i < switches; i++) {
				if (i > 0)
					g.drawLine(i * 20 + x, 0 + y, i * 20 + x, 30 + y);
				if (obj.getValue(i) != Value.FALSE) {
					g.setColor(Value.TRUE_COLOR);
					g.fillRect(i * 20 + 5 + x, 5 + y, 10, 20);
					g.setColor(Color.DARK_GRAY);
					g.fillRect(i * 20 + 5 + x, 5 + y, 10, 10);
					g.setColor(Color.BLACK);
					g.drawRect(i * 20 + 5 + x, 5 + y, 10, 10);
				} else if (obj.getValue(i) != Value.TRUE) {
					g.setColor(Value.FALSE_COLOR);
					g.fillRect(i * 20 + 5 + x, 5 + y, 10, 20);
					g.setColor(Color.DARK_GRAY);
					g.fillRect(i * 20 + 5 + x, 15 + y, 10, 10);
					g.setColor(Color.BLACK);
					g.drawRect(i * 20 + 5 + x, 15 + y, 10, 10);
				}
			}
		} else if (dir == Direction.SOUTH) {
			g.setColor(Color.BLACK);
			for (byte i = 0; i < switches; i++) {
				if (i > 0)
					g.drawLine(i * 20 + x, 30 + y, i * 20 + x, 0 + y);
				if (obj.getValue(i) != Value.FALSE) {
					g.setColor(Value.TRUE_COLOR);
					g.fillRect(i * 20 + 5 + x, 5 + y, 10, 20);
					g.setColor(Color.DARK_GRAY);
					g.fillRect(i * 20 + 5 + x, 15 + y, 10, 10);
					g.setColor(Color.BLACK);
					g.drawRect(i * 20 + 5 + x, 15 + y, 10, 10);
				} else if (obj.getValue(i) != Value.TRUE) {
					g.setColor(Value.FALSE_COLOR);
					g.fillRect(i * 20 + 5 + x, 5 + y, 10, 20);
					g.setColor(Color.DARK_GRAY);
					g.fillRect(i * 20 + 5 + x, 5 + y, 10, 10);
					g.setColor(Color.BLACK);
					g.drawRect(i * 20 + 5 + x, 5 + y, 10, 10);
				}
			}
		}
		painter.drawLabel();
		painter.drawPorts();
	}

	@Override
	public void propagate(InstanceState state) {
		pinValues obj = getValueState(state);
		Value val;
		for (byte i = 0; i < state.getAttributeValue(ATTR_NSWITCHES).byteValue(); i++) {
			val = obj.getValue(i);
			state.setPort(i, val, 1);
		}
	}

	private void updateports(Instance instance) {
		Direction dir = instance.getAttributeValue(StdAttr.FACING);

		byte switches = instance.getAttributeValue(ATTR_NSWITCHES).byteValue();
		Port[] ports = new Port[switches];

		if (dir == Direction.EAST || dir == Direction.WEST) {
			for (byte i = 0; i < ports.length; i++)
				ports[i] = new Port(0, 20 * i + 10, Port.OUTPUT, 1);
		} else {
			for (byte i = 0; i < ports.length; i++)
				ports[i] = new Port(20 * i + 10, 0, Port.OUTPUT, 1);
		}
		instance.setPorts(ports);
	}

	@Override
	public boolean RequiresNonZeroLabel() {
		return true;
	}

	@Override
	public boolean HDLSupportedComponent(String HDLIdentifier,
			AttributeSet attrs) {
		if (MyHDLGenerator == null) {
			MyHDLGenerator = new ButtonHDLGeneratorFactory();
		}
		return MyHDLGenerator.HDLTargetSupported(HDLIdentifier, attrs);
	}

}