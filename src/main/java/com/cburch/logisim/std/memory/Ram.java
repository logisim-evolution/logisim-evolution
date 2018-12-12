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

import java.awt.Font;
import java.awt.Graphics;
import java.awt.Window;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.io.IOException;
import java.io.StringReader;
import java.io.StringWriter;
import java.util.NoSuchElementException;
import java.util.StringTokenizer;

import javax.swing.JLabel;

import com.bfh.logisim.designrulecheck.CorrectLabel;
import com.bfh.logisim.designrulecheck.NetlistComponent;
import com.cburch.logisim.circuit.CircuitState;
import com.cburch.logisim.data.Attribute;
import com.cburch.logisim.data.AttributeSet;
import com.cburch.logisim.data.BitWidth;
import com.cburch.logisim.data.Bounds;
import com.cburch.logisim.data.Location;
import com.cburch.logisim.data.Value;
import com.cburch.logisim.gui.hex.HexFile;
import com.cburch.logisim.gui.hex.HexFrame;
import com.cburch.logisim.gui.main.Frame;
import com.cburch.logisim.instance.Instance;
import com.cburch.logisim.instance.InstanceLogger;
import com.cburch.logisim.instance.InstancePainter;
import com.cburch.logisim.instance.InstanceState;
import com.cburch.logisim.instance.Port;
import com.cburch.logisim.instance.StdAttr;
import com.cburch.logisim.proj.Project;
import com.cburch.logisim.util.GraphicsUtil;

public class Ram extends Mem {

	static class ContentsAttribute extends Attribute<MemContents> {

		public ContentsAttribute() {
			super("contents", Strings.getter("ramContentsAttr"));
		}

		@Override
		public java.awt.Component getCellEditor(Window source, MemContents value) {
			ContentsCell ret = new ContentsCell(source, value);
			ret.mouseClicked(null);
			return ret;
		}

		public MemContents parse(String value) {
			int lineBreak = value.indexOf('\n');
			String first = lineBreak < 0 ? value : value
					.substring(0, lineBreak);
			String rest = lineBreak < 0 ? "" : value.substring(lineBreak + 1);
			StringTokenizer toks = new StringTokenizer(first);
			try {
				String header = toks.nextToken();
				if (!header.equals("addr/data:")) {
					return null;
				}
				int addr = Integer.parseInt(toks.nextToken());
				int data = Integer.parseInt(toks.nextToken());
				MemContents ret = MemContents.create(addr, data,false);
				HexFile.open(ret, new StringReader(rest));
				return ret;
			} catch (IOException e) {
				return null;
			} catch (NumberFormatException e) {
				return null;
			} catch (NoSuchElementException e) {
				return null;
			}
		}

		@Override
		public String toDisplayString(MemContents value) {
			return Strings.get("romContentsValue");
		}

		@Override
		public String toStandardString(MemContents state) {
			int addr = state.getLogLength();
			int data = state.getWidth();
			StringWriter ret = new StringWriter();
			ret.write("addr/data: " + addr + " " + data + "\n");
			try {
				HexFile.save(ret, state);
			} catch (IOException e) {
			}
			return ret.toString();
		}
	}

	@SuppressWarnings("serial")
	private static class ContentsCell extends JLabel implements MouseListener {

		Window source;
		MemContents contents;

		ContentsCell(Window source, MemContents contents) {
			super(Strings.get("romContentsValue"));
			this.source = source;
			this.contents = contents;
			addMouseListener(this);
		}

		public void mouseClicked(MouseEvent e) {
			if (contents == null) {
				return;
			}
			Project proj = source instanceof Frame ? ((Frame) source)
					.getProject() : null;
			HexFrame frame = RamAttributes.getHexFrame(contents, proj);
			frame.setVisible(true);
			frame.toFront();
		}

		public void mouseEntered(MouseEvent e) {
		}

		public void mouseExited(MouseEvent e) {
		}

		public void mousePressed(MouseEvent e) {
		}

		public void mouseReleased(MouseEvent e) {
		}
	}

	public static class Logger extends InstanceLogger {

		@Override
		public String getLogName(InstanceState state, Object option) {
			String Label = state.getAttributeValue(StdAttr.LABEL);
			if (Label.equals("")) {
				Label = null;
			}
			if (option instanceof Integer) {
				String disp = Strings.get("ramComponent");
				Location loc = state.getInstance().getLocation();
				return (Label == null) ? disp + loc + "[" + option + "]"
						: Label + "[" + option + "]";
			} else {
				return Label;
			}
		}

		@Override
		public Object[] getLogOptions(InstanceState state) {
			int addrBits = state.getAttributeValue(ADDR_ATTR).getWidth();
			if (addrBits >= logOptions.length) {
				addrBits = logOptions.length - 1;
			}
			synchronized (logOptions) {
				Object[] ret = logOptions[addrBits];
				if (ret == null) {
					ret = new Object[1 << addrBits];
					logOptions[addrBits] = ret;
					for (int i = 0; i < ret.length; i++) {
						ret[i] = Integer.valueOf(i);
					}
				}
				return ret;
			}
		}

		@Override
		public Value getLogValue(InstanceState state, Object option) {
			if (option instanceof Integer) {
				MemState s = (MemState) state.getData();
				int addr = ((Integer) option).intValue();
				return Value.createKnown(BitWidth.create(s.getDataBits()), s
						.getContents().get(addr));
			} else {
				return Value.NIL;
			}
		}
	}

	public static int ByteEnableIndex(AttributeSet Attrs) {
		Object trigger = Attrs.getValue(StdAttr.TRIGGER);
		boolean asynch = trigger.equals(StdAttr.TRIG_HIGH)
				|| trigger.equals(StdAttr.TRIG_LOW);
		Object bus = Attrs.getValue(RamAttributes.ATTR_DBUS);
		boolean separate = bus == null ? false : bus
				.equals(RamAttributes.BUS_SEP);
		Object be = Attrs.getValue(RamAttributes.ATTR_ByteEnables);
		boolean byteEnables = be == null ? false : be
				.equals(RamAttributes.BUS_WITH_BYTEENABLES);
		if (byteEnables) {
			int ByteEnableIndex = (asynch) ? (separate) ? AByEnSep : AByEnBiDir
					: (separate) ? SByEnSep : SByEnBiDir;
			return ByteEnableIndex;
		}
		return -1;
	}

	public static int GetNrOfByteEnables(AttributeSet Attrs) {
		int NrOfBits = Attrs.getValue(Mem.DATA_ATTR).getWidth();
		return (NrOfBits + 7) / 8;
	}

	public static Attribute<MemContents> CONTENTS_ATTR = new ContentsAttribute();
	static final int OE = MEM_INPUTS + 0;
	static final int WE = MEM_INPUTS + 1;
	public static final int CLK = MEM_INPUTS + 2;
	static final int SDIN = MEM_INPUTS + 3;
	static final int ADIN = MEM_INPUTS + 2;

	static final int AByEnBiDir = MEM_INPUTS + 2;

	static final int AByEnSep = MEM_INPUTS + 3;

	static final int SByEnBiDir = MEM_INPUTS + 3;

	static final int SByEnSep = MEM_INPUTS + 4;

	private static Object[][] logOptions = new Object[9][];

	public Ram() {
		super("RAM", Strings.getter("ramComponent"), 3);
		setIconName("ram.gif");
		setInstanceLogger(Logger.class);
	}

	@Override
	protected void configureNewInstance(Instance instance) {
		super.configureNewInstance(instance);
		instance.addAttributeListener();
	}

	@Override
	void configurePorts(Instance instance) {
		Object trigger = instance.getAttributeValue(StdAttr.TRIGGER);
		boolean asynch = trigger.equals(StdAttr.TRIG_HIGH)
				|| trigger.equals(StdAttr.TRIG_LOW);
		Object bus = instance.getAttributeValue(RamAttributes.ATTR_DBUS);
		boolean separate = bus == null ? false : bus
				.equals(RamAttributes.BUS_SEP);
		Object be = instance.getAttributeValue(RamAttributes.ATTR_ByteEnables);
		boolean byteEnables = be == null ? false : be
				.equals(RamAttributes.BUS_WITH_BYTEENABLES);
		int NrOfByteEnables = GetNrOfByteEnables(instance.getAttributeSet());
		int portCount = MEM_INPUTS;
		if (asynch) {
			portCount += 2;
		} else {
			portCount += 3;
		}
		if (separate) {
			portCount++;
		}
		if (byteEnables) {
			portCount += NrOfByteEnables;
		}
		Port[] ps = new Port[portCount];
		ps[ADDR] = new Port(0, 10, Port.INPUT, ADDR_ATTR);
		ps[ADDR].setToolTip(Strings.getter("memAddrTip"));
		ps[OE] = new Port(0, 60, Port.INPUT, 1);
		ps[OE].setToolTip(Strings.getter("ramOETip"));
		ps[WE] = new Port(0, 50, Port.INPUT, 1);
		ps[WE].setToolTip(Strings.getter("ramWETip"));
		if (!asynch) {
			int ClockOffset = 70;
			if (byteEnables) {
				ClockOffset += NrOfByteEnables * 10;
			}
			ps[CLK] = new Port(0, ClockOffset, Port.INPUT, 1);
			ps[CLK].setToolTip(Strings.getter("ramClkTip"));
		}
		int ypos = (instance.getAttributeValue(Mem.DATA_ATTR).getWidth() == 1) ? getControlHeight(instance
				.getAttributeSet()) + 10 : getControlHeight(instance
				.getAttributeSet());

		if (separate) {
			if (asynch) {
				ps[ADIN] = new Port(0, ypos, Port.INPUT, DATA_ATTR);
				ps[ADIN].setToolTip(Strings.getter("ramInTip"));
			} else {
				ps[SDIN] = new Port(0, ypos, Port.INPUT, DATA_ATTR);
				ps[SDIN].setToolTip(Strings.getter("ramInTip"));
			}
			ps[DATA] = new Port(SymbolWidth + 40, ypos, Port.OUTPUT, DATA_ATTR);
			ps[DATA].setToolTip(Strings.getter("memDataTip"));
		} else {
			ps[DATA] = new Port(SymbolWidth + 50, ypos, Port.INOUT, DATA_ATTR);
			ps[DATA].setToolTip(Strings.getter("ramBusTip"));
		}
		if (byteEnables) {
			int ByteEnableIndex = ByteEnableIndex(instance.getAttributeSet());
			for (int i = 0; i < NrOfByteEnables; i++) {
				ps[ByteEnableIndex + i] = new Port(0, 70 + i * 10, Port.INPUT,
						1);
				String Label = "ramByteEnableTip"
						+ Integer.toString(NrOfByteEnables - i - 1);
				ps[ByteEnableIndex + i].setToolTip(Strings.getter(Label));
			}
		}
		instance.setPorts(ps);
	}

	@Override
	public AttributeSet createAttributeSet() {
		return new RamAttributes();
	}

	private void DrawConnections(Graphics g, int xpos, int ypos,
			boolean singleBit, boolean separate, boolean sync,
			boolean ByteEnabled, int bit) {
		Font font = g.getFont();
		GraphicsUtil.switchToWidth(g, 2);
		if (separate) {
			if (singleBit) {
				g.drawLine(xpos, ypos + 10, xpos + 20, ypos + 10);
				g.drawLine(xpos + 20 + SymbolWidth, ypos + 10, xpos + 40
						+ SymbolWidth, ypos + 10);
			} else {
				g.drawLine(xpos + 5, ypos + 5, xpos + 10, ypos + 10);
				g.drawLine(xpos + 10, ypos + 10, xpos + 20, ypos + 10);
				g.drawLine(xpos + 20 + SymbolWidth, ypos + 10, xpos + 30
						+ SymbolWidth, ypos + 10);
				g.drawLine(xpos + 30 + SymbolWidth, ypos + 10, xpos + 35
						+ SymbolWidth, ypos + 5);
				g.setFont(font.deriveFont(7.0f));
				GraphicsUtil
						.drawText(g, Integer.toString(bit), xpos + 17,
								ypos + 7, GraphicsUtil.H_RIGHT,
								GraphicsUtil.V_BASELINE);
				GraphicsUtil.drawText(g, Integer.toString(bit), xpos + 23
						+ SymbolWidth, ypos + 7, GraphicsUtil.H_LEFT,
						GraphicsUtil.V_BASELINE);
				g.setFont(font);
			}
			String ByteIndex = "";
			if (ByteEnabled) {
				int Index = bit / 8;
				ByteIndex = "," + Integer.toString(Index + 4);
			}
			String DLabel = (sync) ? "A,1,3" + ByteIndex + "D" : "A,1"
					+ ByteIndex + "D";
			String QLabel = (sync) ? "A,2,3" + ByteIndex : "A,2" + ByteIndex;
			g.setFont(font.deriveFont(9.0f));
			GraphicsUtil.drawText(g, DLabel, xpos + 23, ypos + 10,
					GraphicsUtil.H_LEFT, GraphicsUtil.V_CENTER);
			GraphicsUtil.drawText(g, QLabel, xpos + 17 + SymbolWidth,
					ypos + 10, GraphicsUtil.H_RIGHT, GraphicsUtil.V_CENTER);
			g.setFont(font);
		} else {
			g.drawLine(xpos + 24 + SymbolWidth, ypos + 2, xpos + 28
					+ SymbolWidth, ypos + 5);
			g.drawLine(xpos + 24 + SymbolWidth, ypos + 8, xpos + 28
					+ SymbolWidth, ypos + 5);
			g.drawLine(xpos + 20 + SymbolWidth, ypos + 5, xpos + 30
					+ SymbolWidth, ypos + 5);
			g.drawLine(xpos + 22 + SymbolWidth, ypos + 15, xpos + 26
					+ SymbolWidth, ypos + 12);
			g.drawLine(xpos + 22 + SymbolWidth, ypos + 15, xpos + 26
					+ SymbolWidth, ypos + 18);
			g.drawLine(xpos + 20 + SymbolWidth, ypos + 15, xpos + 30
					+ SymbolWidth, ypos + 15);
			g.drawLine(xpos + 30 + SymbolWidth, ypos + 5, xpos + 30
					+ SymbolWidth, ypos + 15);
			g.drawLine(xpos + 30 + SymbolWidth, ypos + 10, xpos + 40
					+ SymbolWidth, ypos + 10);
			if (singleBit) {
				g.drawLine(xpos + 40 + SymbolWidth, ypos + 10, xpos + 50
						+ SymbolWidth, ypos + 10);
			} else {
				g.drawLine(xpos + 40 + SymbolWidth, ypos + 10, xpos + 45
						+ SymbolWidth, ypos + 5);
			}
			g.setFont(font.deriveFont(7.0f));
			GraphicsUtil.drawText(g, Integer.toString(bit), xpos + 33
					+ SymbolWidth, ypos + 7, GraphicsUtil.H_LEFT,
					GraphicsUtil.V_BASELINE);
			String ByteIndex = "";
			if (ByteEnabled) {
				int Index = bit / 8;
				ByteIndex = "," + Integer.toString(Index + 4);
			}
			String DLabel = (sync) ? "A,1,3" + ByteIndex + "D" : "A,1"
					+ ByteIndex + "D";
			String QLabel = "A,2" + ByteIndex + "  ";
			g.setFont(font.deriveFont(9.0f));
			GraphicsUtil.drawText(g, DLabel, xpos + 17 + SymbolWidth,
					ypos + 13, GraphicsUtil.H_RIGHT, GraphicsUtil.V_CENTER);
			GraphicsUtil.drawText(g, QLabel, xpos + 17 + SymbolWidth, ypos + 5,
					GraphicsUtil.H_RIGHT, GraphicsUtil.V_CENTER);
			g.setFont(font);
			GraphicsUtil.switchToWidth(g, 1);
			g.drawLine(xpos + 11 + SymbolWidth, ypos + 4, xpos + 19
					+ SymbolWidth, ypos + 4);
			g.drawLine(xpos + 11 + SymbolWidth, ypos + 4, xpos + 15
					+ SymbolWidth, ypos + 8);
			g.drawLine(xpos + 15 + SymbolWidth, ypos + 8, xpos + 19
					+ SymbolWidth, ypos + 4);
		}
		GraphicsUtil.switchToWidth(g, 1);
	}

	private void DrawControlBlock(InstancePainter painter, int xpos, int ypos) {
		Object trigger = painter.getAttributeValue(StdAttr.TRIGGER);
		boolean asynch = trigger.equals(StdAttr.TRIG_HIGH)
				|| trigger.equals(StdAttr.TRIG_LOW);
		boolean inverted = trigger.equals(StdAttr.TRIG_FALLING)
				|| trigger.equals(StdAttr.TRIG_LOW);
		Object be = painter.getAttributeValue(RamAttributes.ATTR_ByteEnables);
		boolean byteEnables = be == null ? false : be
				.equals(RamAttributes.BUS_WITH_BYTEENABLES);
		int NrOfByteEnables = GetNrOfByteEnables(painter.getAttributeSet());
		Graphics g = painter.getGraphics();
		GraphicsUtil.switchToWidth(g, 2);
		AttributeSet attrs = painter.getAttributeSet();
		g.drawLine(xpos + 20, ypos, xpos + 20 + SymbolWidth, ypos);
		g.drawLine(xpos + 20, ypos, xpos + 20, ypos + getControlHeight(attrs)
				- 10);
		g.drawLine(xpos + 20 + SymbolWidth, ypos, xpos + 20 + SymbolWidth, ypos
				+ getControlHeight(attrs) - 10);
		g.drawLine(xpos + 20, ypos + getControlHeight(attrs) - 10, xpos + 30,
				ypos + getControlHeight(attrs) - 10);
		g.drawLine(xpos + 20 + SymbolWidth - 10, ypos + getControlHeight(attrs)
				- 10, xpos + 20 + SymbolWidth, ypos + getControlHeight(attrs)
				- 10);
		g.drawLine(xpos + 30, ypos + getControlHeight(attrs) - 10, xpos + 30,
				ypos + getControlHeight(attrs));
		g.drawLine(xpos + 20 + SymbolWidth - 10, ypos + getControlHeight(attrs)
				- 10, xpos + 20 + SymbolWidth - 10, ypos
				+ getControlHeight(attrs));
		GraphicsUtil.drawCenteredText(
				g,
				"RAM "
						+ GetSizeLabel(painter.getAttributeValue(Mem.ADDR_ATTR)
								.getWidth())
						+ " x "
						+ Integer.toString(painter.getAttributeValue(
								Mem.DATA_ATTR).getWidth()), xpos
						+ (SymbolWidth / 2) + 20, ypos + 5);
		if (asynch && inverted) {
			g.drawLine(xpos, ypos + 50, xpos + 12, ypos + 50);
			g.drawOval(xpos + 12, ypos + 46, 8, 8);
		} else {
			g.drawLine(xpos, ypos + 50, xpos + 20, ypos + 50);
		}
		GraphicsUtil.drawText(g, "M1 [Write Enable]", xpos + 33, ypos + 50,
				GraphicsUtil.H_LEFT, GraphicsUtil.V_CENTER);
		painter.drawPort(WE);
		if (asynch && inverted) {
			g.drawLine(xpos, ypos + 60, xpos + 12, ypos + 60);
			g.drawOval(xpos + 12, ypos + 56, 8, 8);
		} else {
			g.drawLine(xpos, ypos + 60, xpos + 20, ypos + 60);
		}
		GraphicsUtil.drawText(g, "M2 [Output Enable]", xpos + 33, ypos + 60,
				GraphicsUtil.H_LEFT, GraphicsUtil.V_CENTER);
		painter.drawPort(OE);
		if (!asynch) {
			int yoffset = 70;
			if (byteEnables) {
				yoffset += NrOfByteEnables * 10;
			}
			if (inverted) {
				g.drawLine(xpos, ypos + yoffset, xpos + 12, ypos + yoffset);
				g.drawOval(xpos + 12, ypos + yoffset - 4, 8, 8);
			} else {
				g.drawLine(xpos, ypos + yoffset, xpos + 20, ypos + yoffset);
			}
			GraphicsUtil.drawText(g, "C3", xpos + 33, ypos + yoffset,
					GraphicsUtil.H_LEFT, GraphicsUtil.V_CENTER);
			painter.drawClockSymbol(xpos + 20, ypos + yoffset);
			painter.drawPort(CLK);
		}
		if (byteEnables) {
			int ByteEnableIndex = ByteEnableIndex(painter.getAttributeSet());
			GraphicsUtil.switchToWidth(g, 2);
			for (int i = 0; i < NrOfByteEnables; i++) {
				g.drawLine(xpos, ypos + 70 + i * 10, xpos + 20, ypos + 70 + i
						* 10);
				painter.drawPort(ByteEnableIndex + i);
				String Label = "M"
						+ Integer.toString((NrOfByteEnables - i) + 3)
						+ " [ByteEnable "
						+ Integer.toString((NrOfByteEnables - i) - 1) + "]";
				GraphicsUtil.drawText(g, Label, xpos + 33, ypos + 70 + i * 10,
						GraphicsUtil.H_LEFT, GraphicsUtil.V_CENTER);
			}
		}
		GraphicsUtil.switchToWidth(g, 1);
		DrawAddress(painter, xpos, ypos + 10,
				painter.getAttributeValue(Mem.ADDR_ATTR).getWidth());
	}

	private void DrawDataBlock(InstancePainter painter, int xpos, int ypos,
			int bit, int NrOfBits) {
		Object busVal = painter.getAttributeValue(RamAttributes.ATTR_DBUS);
		int realypos = ypos + getControlHeight(painter.getAttributeSet()) + bit
				* 20;
		int realxpos = xpos + 20;
		boolean FirstBlock = bit == 0;
		boolean LastBlock = bit == (NrOfBits - 1);
		Graphics g = painter.getGraphics();
		boolean separate = busVal == null ? false : busVal
				.equals(RamAttributes.BUS_SEP);
		Object trigger = painter.getAttributeValue(StdAttr.TRIGGER);
		boolean asynch = trigger.equals(StdAttr.TRIG_HIGH)
				|| trigger.equals(StdAttr.TRIG_LOW);
		Object be = painter.getAttributeValue(RamAttributes.ATTR_ByteEnables);
		boolean byteEnables = be == null ? false : be
				.equals(RamAttributes.BUS_WITH_BYTEENABLES);
		GraphicsUtil.switchToWidth(g, 2);
		g.drawRect(realxpos, realypos, SymbolWidth, 20);
		DrawConnections(g, xpos, realypos, FirstBlock & LastBlock, separate,
				!asynch, byteEnables, bit);
		if (FirstBlock) {
			painter.drawPort(DATA);
			if (separate) {
				if (asynch) {
					painter.drawPort(ADIN);
				} else {
					painter.drawPort(SDIN);
				}
			}
			if (!LastBlock) {
				GraphicsUtil.switchToWidth(g, 5);
				if (separate) {
					g.drawLine(xpos, realypos, xpos + 5, realypos + 5);
					g.drawLine(xpos + 5, realypos + 5, xpos + 5, realypos + 20);
					g.drawLine(xpos + 40 + SymbolWidth, realypos, xpos + 35
							+ SymbolWidth, realypos + 5);
					g.drawLine(xpos + 35 + SymbolWidth, realypos + 5, xpos + 35
							+ SymbolWidth, realypos + 20);
				} else {
					g.drawLine(xpos + 50 + SymbolWidth, realypos, xpos + 45
							+ SymbolWidth, realypos + 5);
					g.drawLine(xpos + 45 + SymbolWidth, realypos + 5, xpos + 45
							+ SymbolWidth, realypos + 20);
				}
			}
		} else {
			GraphicsUtil.switchToWidth(g, 5);
			if (LastBlock) {
				if (separate) {
					g.drawLine(xpos + 5, realypos, xpos + 5, realypos + 5);
					g.drawLine(xpos + 35 + SymbolWidth, realypos, xpos + 35
							+ SymbolWidth, realypos + 5);
				} else {
					g.drawLine(xpos + 45 + SymbolWidth, realypos, xpos + 45
							+ SymbolWidth, realypos + 5);
				}
			} else {
				if (separate) {
					g.drawLine(xpos + 5, realypos, xpos + 5, realypos + 20);
					g.drawLine(xpos + 35 + SymbolWidth, realypos, xpos + 35
							+ SymbolWidth, realypos + 20);
				} else {
					g.drawLine(xpos + 45 + SymbolWidth, realypos, xpos + 45
							+ SymbolWidth, realypos + 20);
				}
			}
		}
		GraphicsUtil.switchToWidth(g, 1);
	}

	public int getControlHeight(AttributeSet attrs) {
		Object trigger = attrs.getValue(StdAttr.TRIGGER);
		boolean asynch = trigger.equals(StdAttr.TRIG_HIGH)
				|| trigger.equals(StdAttr.TRIG_LOW);
		Object be = attrs.getValue(RamAttributes.ATTR_ByteEnables);
		boolean byteEnables = be == null ? false : be
				.equals(RamAttributes.BUS_WITH_BYTEENABLES);
		int result = 80;
		if (!asynch) {
			result += 10;
		}
		if (byteEnables) {
			int NrByteEnables = GetNrOfByteEnables(attrs);
			result += NrByteEnables * 10;
		}
		return result;
	}

	@Override
	public String getHDLName(AttributeSet attrs) {
		StringBuffer CompleteName = new StringBuffer();
		String Label = CorrectLabel.getCorrectLabel(attrs.getValue(StdAttr.LABEL));
		if (Label.length()==0) {
			CompleteName.append("RAM");
		} else {
			CompleteName.append("RAM_"+Label);
		}
		return CompleteName.toString();
	}

	@Override
	HexFrame getHexFrame(Project proj, Instance instance, CircuitState circState) {
		RamState ret = (RamState) instance.getData(circState);
		return RamAttributes.getHexFrame(
				(ret == null) ? instance.getAttributeValue(CONTENTS_ATTR) :
						ret.getContents(), proj);
	}

	@Override
	public Bounds getOffsetBounds(AttributeSet attrs) {
		int len = attrs.getValue(Mem.DATA_ATTR).getWidth();
		Object bus = attrs.getValue(RamAttributes.ATTR_DBUS);
		boolean separate = bus == null ? false : bus
				.equals(RamAttributes.BUS_SEP);
		int xoffset = (separate) ? 40 : 50;
		return Bounds.create(0, 0, SymbolWidth + xoffset,
				getControlHeight(attrs) + 20 * len);
	}

	@Override
	MemState getState(Instance instance, CircuitState state) {
		RamState ret = (RamState) instance.getData(state);
		if (ret == null) {
			MemContents contents = instance
					.getAttributeValue(Ram.CONTENTS_ATTR);
			ret = new RamState(instance, contents.clone(), new MemListener(instance));
			instance.setData(state, ret);
		} else {
			ret.setRam(instance);
		}
		return ret;
	}

	@Override
	MemState getState(InstanceState state) {
		RamState ret = (RamState) state.getData();
		if (ret == null) {
			MemContents contents = state.getInstance().getAttributeValue(
					Ram.CONTENTS_ATTR);
			Instance instance = state.getInstance();
			ret = new RamState(instance, contents.clone(), new MemListener(instance));
			state.setData(ret);
		} else {
			ret.setRam(state.getInstance());
		}
		return ret;
	}

	@Override
	public boolean HDLSupportedComponent(String HDLIdentifier,
			AttributeSet attrs) {
		if (MyHDLGenerator == null) {
			MyHDLGenerator = new RamHDLGeneratorFactory();
		}
		return MyHDLGenerator.HDLTargetSupported(HDLIdentifier, attrs);
	}

	@Override
	protected void instanceAttributeChanged(Instance instance, Attribute<?> attr) {
		super.instanceAttributeChanged(instance, attr);
		if ((attr == Mem.DATA_ATTR) || (attr == RamAttributes.ATTR_DBUS)
				|| (attr == StdAttr.TRIGGER)
				|| (attr == RamAttributes.ATTR_ByteEnables)) {
			if ((attr == Mem.DATA_ATTR) || (attr == StdAttr.TRIGGER)) {
				boolean disable_due_to_bits = instance.getAttributeValue(
						Mem.DATA_ATTR).getWidth() < 9;
				boolean disable_due_to_async = instance.getAttributeValue(
						StdAttr.TRIGGER).equals(StdAttr.TRIG_HIGH)
						|| instance.getAttributeValue(StdAttr.TRIGGER).equals(
								StdAttr.TRIG_LOW);
				if (disable_due_to_bits || disable_due_to_async) {
					if (!instance.getAttributeValue(
							RamAttributes.ATTR_ByteEnables).equals(
							RamAttributes.BUS_WITHOUT_BYTEENABLES)) {
						instance.getAttributeSet().setValue(
								RamAttributes.ATTR_ByteEnables,
								RamAttributes.BUS_WITHOUT_BYTEENABLES);
						super.instanceAttributeChanged(instance,
								RamAttributes.ATTR_ByteEnables);
					}
					instance.setAttributeReadOnly(
							RamAttributes.ATTR_ByteEnables, true);
					super.instanceAttributeChanged(instance,
							RamAttributes.ATTR_ByteEnables);
					instance.setAttributeReadOnly(
							RamAttributes.ATTR_ByteEnables, true);
					super.instanceAttributeChanged(instance,
							RamAttributes.ATTR_ByteEnables);
				} else {
					if (instance.getAttributeSet().isReadOnly(
							RamAttributes.ATTR_ByteEnables)) {
						instance.setAttributeReadOnly(
								RamAttributes.ATTR_ByteEnables, false);
						super.instanceAttributeChanged(instance,
								RamAttributes.ATTR_ByteEnables);
					}
				}
			}
			instance.recomputeBounds();
			configurePorts(instance);
		}
	}

	@Override
	public void paintInstance(InstancePainter painter) {
		Graphics g = painter.getGraphics();
		Bounds bds = painter.getBounds();
		int NrOfBits = painter.getAttributeValue(Mem.DATA_ATTR).getWidth();

		// int addrb = painter.getAttributeValue(Mem.ADDR_ATTR).getWidth();

		String Label = painter.getAttributeValue(StdAttr.LABEL);
		if (Label != null) {
			Font font = g.getFont();
			g.setFont(painter.getAttributeValue(StdAttr.LABEL_FONT));
			GraphicsUtil.drawCenteredText(g, Label, bds.getX() + bds.getWidth()
					/ 2, bds.getY() - g.getFont().getSize());
			g.setFont(font);
		}
		int xpos = bds.getX();
		int ypos = bds.getY();

		DrawControlBlock(painter, xpos, ypos);
		for (int i = 0; i < NrOfBits; i++) {
			DrawDataBlock(painter, xpos, ypos, i, NrOfBits);
		}
		/* Draw contents */
		if (painter.getShowState()) {
			RamState state = (RamState) getState(painter);
			state.paint(painter.getGraphics(), bds.getX() + 20, bds.getY(),
					true, getControlHeight(painter.getAttributeSet()));
		}
	}

	@Override
	public void propagate(InstanceState state) {
		RamState myState = (RamState) getState(state);
		Object trigger = state.getAttributeValue(StdAttr.TRIGGER);
		Object bus = state.getAttributeValue(RamAttributes.ATTR_DBUS);
		boolean asynch = trigger.equals(StdAttr.TRIG_HIGH)
				|| trigger.equals(StdAttr.TRIG_LOW);
		boolean edge = false;
		if (!asynch) {
			edge = myState.setClock(state.getPortValue(CLK), trigger);
		}
		boolean triggered = asynch || edge;
		boolean separate = bus == null ? false : bus
				.equals(RamAttributes.BUS_SEP);
		boolean outputEnabled = (!asynch || trigger.equals(StdAttr.TRIG_HIGH)) ? state
				.getPortValue(OE) != Value.FALSE
				: state.getPortValue(OE) == Value.FALSE;
		BitWidth dataBits = state.getAttributeValue(DATA_ATTR);
		/* Set the outputs in tri-state in case of combined bus */
		if ((!separate && !outputEnabled)
				|| (separate && asynch && !outputEnabled)) {
			state.setPort(DATA, Value.createUnknown(dataBits), DELAY);
		}
		if (!triggered && !asynch && outputEnabled) {
			state.setPort(DATA,
					Value.createKnown(dataBits, myState.GetCurrentData()),
					DELAY);
		}
		if (triggered) {
			Object be = state.getAttributeValue(RamAttributes.ATTR_ByteEnables);
			boolean byteEnables = be == null ? false : be
					.equals(RamAttributes.BUS_WITH_BYTEENABLES);
			int NrOfByteEnables = GetNrOfByteEnables(state.getAttributeSet());
			int ByteEnableIndex = ByteEnableIndex(state.getAttributeSet());
			boolean shouldStore = (!asynch || trigger.equals(StdAttr.TRIG_HIGH)) ? state
					.getPortValue(WE) != Value.FALSE
					: state.getPortValue(WE) == Value.FALSE;
			Value addrValue = state.getPortValue(ADDR);
			int addr = addrValue.toIntValue();
			if (!addrValue.isFullyDefined() || addr < 0) {
				return;
			}
			if (addr != myState.getCurrent()) {
				myState.setCurrent(addr);
				myState.scrollToShow(addr);
			}

			if (shouldStore) {
				int dataValue = state.getPortValue(
						!separate ? DATA : (asynch) ? ADIN : SDIN).toIntValue();
				int memValue = myState.getContents().get(addr);
				if (byteEnables) {
					int mask = 0xFF << (NrOfByteEnables - 1) * 8;
					for (int i = 0; i < NrOfByteEnables; i++) {
						Value bitvalue = state
								.getPortValue(ByteEnableIndex + i);
						boolean disabled = bitvalue == null ? false : bitvalue
								.equals(Value.FALSE);
						if (disabled) {
							dataValue &= ~mask;
							dataValue |= (memValue & mask);
						}
						mask >>= 8;
					}
				}
				myState.getContents().set(addr, dataValue);
			}
			int val = myState.getContents().get(addr);
			int currentValue = myState.GetCurrentData();
			if (byteEnables) {
				int mask = 0xFF << (NrOfByteEnables - 1) * 8;
				for (int i = 0; i < NrOfByteEnables; i++) {
					Value bitvalue = state.getPortValue(ByteEnableIndex + i);
					boolean disabled = bitvalue == null ? false : bitvalue
							.equals(Value.FALSE);
					if (disabled) {
						val &= ~mask;
						val |= (currentValue & mask);
					}
					mask >>= 8;
				}
			}
			myState.SetCurrentData(val);
			if (outputEnabled) {
				state.setPort(DATA, Value.createKnown(dataBits, val), DELAY);
			}
		}
	}

	@Override
	public boolean RequiresNonZeroLabel() {
		return true;
	}

	@Override
	public boolean CheckForGatedClocks(NetlistComponent comp) {
		return true;
	}
	
	@Override
	public int[] ClockPinIndex(NetlistComponent comp) {
		return new int[] {CLK};
	}
}
