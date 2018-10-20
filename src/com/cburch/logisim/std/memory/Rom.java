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
import java.util.WeakHashMap;

import javax.swing.JLabel;

import com.bfh.logisim.designrulecheck.CorrectLabel;
import com.cburch.logisim.circuit.CircuitState;
import com.cburch.logisim.data.Attribute;
import com.cburch.logisim.data.AttributeSet;
import com.cburch.logisim.data.BitWidth;
import com.cburch.logisim.data.Bounds;
import com.cburch.logisim.data.Value;
import com.cburch.logisim.gui.hex.HexFile;
import com.cburch.logisim.gui.hex.HexFrame;
import com.cburch.logisim.gui.main.Frame;
import com.cburch.logisim.instance.Instance;
import com.cburch.logisim.instance.InstancePainter;
import com.cburch.logisim.instance.InstanceState;
import com.cburch.logisim.instance.Port;
import com.cburch.logisim.instance.StdAttr;
import com.cburch.logisim.proj.Project;
import com.cburch.logisim.util.GraphicsUtil;

public class Rom extends Mem {
	static class ContentsAttribute extends Attribute<MemContents> {
		public ContentsAttribute() {
			super("contents", Strings.getter("romContentsAttr"));
		}

		@Override
		public java.awt.Component getCellEditor(Window source, MemContents value) {
			if (source instanceof Frame) {
				Project proj = ((Frame) source).getProject();
				RomAttributes.register(value, proj);
			}
			ContentsCell ret = new ContentsCell(source, value);
			ret.mouseClicked(null);
			return ret;
		}

		@Override
		public MemContents parse(String value) {
			int lineBreak = value.indexOf('\n');
			String first = lineBreak < 0 ? value : value
					.substring(0, lineBreak);
			String rest = lineBreak < 0 ? "" : value.substring(lineBreak + 1);
			StringTokenizer toks = new StringTokenizer(first);
			try {
				String header = toks.nextToken();
				if (!header.equals("addr/data:"))
					return null;
				int addr = Integer.parseInt(toks.nextToken());
				int data = Integer.parseInt(toks.nextToken());
				MemContents ret = MemContents.create(addr, data,true);
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
			if (contents == null)
				return;
			Project proj = source instanceof Frame ? ((Frame) source)
					.getProject() : null;
			HexFrame frame = RomAttributes.getHexFrame(contents, proj);
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

	public static Attribute<MemContents> CONTENTS_ATTR = new ContentsAttribute();

	// The following is so that instance's MemListeners aren't freed by the
	// garbage collector until the instance itself is ready to be freed.
	private WeakHashMap<Instance, MemListener> memListeners;

	public Rom() {
		super("ROM", Strings.getter("romComponent"), 0);
		setIconName("rom.gif");
		memListeners = new WeakHashMap<Instance, MemListener>();
	}

	@Override
	protected void configureNewInstance(Instance instance) {
		super.configureNewInstance(instance);
		MemContents contents = getMemContents(instance);
		MemListener listener = new MemListener(instance);
		memListeners.put(instance, listener);
		contents.addHexModelListener(listener);
		instance.addAttributeListener();
	}

	@Override
	void configurePorts(Instance instance) {
		Port[] ps = new Port[MEM_INPUTS];
		ps[ADDR] = new Port(0, 10, Port.INPUT, ADDR_ATTR);
		ps[ADDR].setToolTip(Strings.getter("memAddrTip"));
		int ypos = (instance.getAttributeValue(Mem.DATA_ATTR).getWidth() == 1) ? getControlHeight(instance
				.getAttributeSet()) + 10 : getControlHeight(instance
				.getAttributeSet());
		ps[DATA] = new Port(SymbolWidth + 40, ypos, Port.OUTPUT, DATA_ATTR);
		ps[DATA].setToolTip(Strings.getter("memDataTip"));
		instance.setPorts(ps);
	}

	@Override
	public AttributeSet createAttributeSet() {
		return new RomAttributes();
	}

	private void DrawControlBlock(InstancePainter painter, int xpos, int ypos) {
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
				"ROM "
						+ GetSizeLabel(painter.getAttributeValue(Mem.ADDR_ATTR)
								.getWidth())
						+ " x "
						+ Integer.toString(painter.getAttributeValue(
								Mem.DATA_ATTR).getWidth()), xpos
						+ (SymbolWidth / 2) + 20, ypos + 5);
		GraphicsUtil.switchToWidth(g, 1);
		DrawAddress(painter, xpos, ypos + 10,
				painter.getAttributeValue(Mem.ADDR_ATTR).getWidth());
	}

	private void DrawDataBlock(InstancePainter painter, int xpos, int ypos,
			int bit, int NrOfBits) {
		int realypos = ypos + getControlHeight(painter.getAttributeSet()) + bit
				* 20;
		int realxpos = xpos + 20;
		boolean FirstBlock = bit == 0;
		boolean LastBlock = bit == (NrOfBits - 1);
		Graphics g = painter.getGraphics();
		Font font = g.getFont();
		GraphicsUtil.switchToWidth(g, 2);
		g.drawRect(realxpos, realypos, SymbolWidth, 20);
		GraphicsUtil.drawText(g, "A", realxpos + SymbolWidth - 3,
				realypos + 10, GraphicsUtil.H_RIGHT, GraphicsUtil.V_CENTER);
		if (FirstBlock && LastBlock) {
			GraphicsUtil.switchToWidth(g, 3);
			g.drawLine(realxpos + SymbolWidth + 1, realypos + 10, realxpos
					+ SymbolWidth + 20, realypos + 10);
			painter.drawPort(DATA);
			return;
		}
		g.drawLine(realxpos + SymbolWidth, realypos + 10, realxpos
				+ SymbolWidth + 10, realypos + 10);
		g.drawLine(realxpos + SymbolWidth + 10, realypos + 10, realxpos
				+ SymbolWidth + 15, realypos + 5);
		g.setFont(font.deriveFont(7.0f));
		GraphicsUtil
				.drawText(g, Integer.toString(bit), realxpos + SymbolWidth + 3,
						realypos + 7, GraphicsUtil.H_LEFT,
						GraphicsUtil.V_BASELINE);
		g.setFont(font);
		GraphicsUtil.switchToWidth(g, 5);
		if (FirstBlock) {
			g.drawLine(realxpos + SymbolWidth + 15, realypos + 5, realxpos
					+ SymbolWidth + 15, realypos + 20);
			g.drawLine(realxpos + SymbolWidth + 15, realypos + 5, realxpos
					+ SymbolWidth + 20, realypos);
			painter.drawPort(DATA);
		} else if (LastBlock) {
			g.drawLine(realxpos + SymbolWidth + 15, realypos, realxpos
					+ SymbolWidth + 15, realypos + 10);
		} else
			g.drawLine(realxpos + SymbolWidth + 15, realypos, realxpos
					+ SymbolWidth + 15, realypos + 20);
		GraphicsUtil.switchToWidth(g, 1);
	}

	public int getControlHeight(AttributeSet attrs) {
		return 60;
	}

	@Override
	public String getHDLName(AttributeSet attrs) {
		StringBuffer CompleteName = new StringBuffer();
		String Label = CorrectLabel.getCorrectLabel(attrs.getValue(StdAttr.LABEL));
		if (Label.length()==0) {
			CompleteName.append("ROM");
		} else {
			CompleteName.append("ROM_"+Label);
		}
		return CompleteName.toString();
	}

	@Override
	HexFrame getHexFrame(Project proj, Instance instance, CircuitState state) {
		return RomAttributes.getHexFrame(getMemContents(instance), proj);
	}

	// TODO - maybe delete this method?
	MemContents getMemContents(Instance instance) {
		return instance.getAttributeValue(CONTENTS_ATTR);
	}

	@Override
	public Bounds getOffsetBounds(AttributeSet attrs) {
		int len = attrs.getValue(Mem.DATA_ATTR).getWidth();
		return Bounds.create(0, 0, SymbolWidth + 40, getControlHeight(attrs)
				+ 20 * len);
	}

	@Override
	MemState getState(Instance instance, CircuitState state) {
		MemState ret = (MemState) instance.getData(state);
		if (ret == null) {
			MemContents contents = getMemContents(instance);
			ret = new MemState(contents);
			instance.setData(state, ret);
		}
		return ret;
	}

	@Override
	MemState getState(InstanceState state) {
		MemState ret = (MemState) state.getData();
		if (ret == null) {
			MemContents contents = getMemContents(state.getInstance());
			ret = new MemState(contents);
			state.setData(ret);
		}
		return ret;
	}

	@Override
	public boolean HDLSupportedComponent(String HDLIdentifier,
			AttributeSet attrs) {
		if (MyHDLGenerator == null)
			MyHDLGenerator = new RomHDLGeneratorFactory();
		return MyHDLGenerator.HDLTargetSupported(HDLIdentifier, attrs);
	}

	@Override
	protected void instanceAttributeChanged(Instance instance, Attribute<?> attr) {
		if (attr == Mem.DATA_ATTR) {
			instance.recomputeBounds();
			configurePorts(instance);
		}
	}

	@Override
	public void paintInstance(InstancePainter painter) {
		Graphics g = painter.getGraphics();
		Bounds bds = painter.getBounds();

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
		int NrOfBits = painter.getAttributeValue(Mem.DATA_ATTR).getWidth();
		/* draw control */
		DrawControlBlock(painter, xpos, ypos);
		/* draw body */
		for (int i = 0; i < NrOfBits; i++) {
			DrawDataBlock(painter, xpos, ypos, i, NrOfBits);
		}
		/* Draw contents */
		if (painter.getShowState()) {
			MemState state = getState(painter);
			state.paint(painter.getGraphics(), bds.getX() + 20, bds.getY(),
					false, getControlHeight(painter.getAttributeSet()));
		}
	}

	@Override
	public void propagate(InstanceState state) {
		MemState myState = getState(state);
		BitWidth dataBits = state.getAttributeValue(DATA_ATTR);

		Value addrValue = state.getPortValue(ADDR);

		int addr = addrValue.toIntValue();
		if (!addrValue.isFullyDefined() || addr < 0)
			return;
		if (addr != myState.getCurrent()) {
			myState.setCurrent(addr);
			myState.scrollToShow(addr);
		}

		int val = myState.getContents().get(addr);
		state.setPort(DATA, Value.createKnown(dataBits, val), DELAY);
	}

	@Override
	public boolean RequiresNonZeroLabel() {
		return true;
	}
}
