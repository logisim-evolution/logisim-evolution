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
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.io.File;
import java.io.IOException;
import java.util.WeakHashMap;

import com.cburch.hex.HexModel;
import com.cburch.hex.HexModelListener;
import com.cburch.logisim.circuit.CircuitState;
import com.cburch.logisim.data.Attribute;
import com.cburch.logisim.data.AttributeOption;
import com.cburch.logisim.data.AttributeSet;
import com.cburch.logisim.data.Attributes;
import com.cburch.logisim.data.BitWidth;
import com.cburch.logisim.data.Bounds;
import com.cburch.logisim.gui.hex.HexFile;
import com.cburch.logisim.gui.hex.HexFrame;
import com.cburch.logisim.instance.Instance;
import com.cburch.logisim.instance.InstanceFactory;
import com.cburch.logisim.instance.InstancePainter;
import com.cburch.logisim.instance.InstanceState;
import com.cburch.logisim.proj.Project;
import com.cburch.logisim.tools.MenuExtender;
import com.cburch.logisim.tools.key.BitWidthConfigurator;
import com.cburch.logisim.tools.key.JoinedConfigurator;
import com.cburch.logisim.util.GraphicsUtil;
import com.cburch.logisim.util.StringGetter;

public abstract class Mem extends InstanceFactory {
	// Note: The code is meant to be able to handle up to 32-bit addresses, but
	// it
	// hasn't been debugged thoroughly. There are two definite changes I would
	// make if I were to extend the address bits: First, there would need to be
	// some
	// modification to the memory's graphical representation, because there
	// isn't
	// room in the box to include such long memory addresses with the current
	// font
	// size. And second, I'd alter the MemContents class's PAGE_SIZE_BITS
	// constant
	// to 14 so that its "page table" isn't quite so big.

	static class MemListener implements HexModelListener {

		Instance instance;

		MemListener(Instance instance) {
			this.instance = instance;
		}

		public void bytesChanged(HexModel source, long start, long numBytes,
				int[] values) {
			instance.fireInvalidated();
		}

		public void metainfoChanged(HexModel source) {
		}
	}

	public static final int SymbolWidth = 200;
	public static final Attribute<BitWidth> ADDR_ATTR = Attributes.forBitWidth(
			"addrWidth", Strings.getter("ramAddrWidthAttr"), 2, 24);

	public static final Attribute<BitWidth> DATA_ATTR = Attributes.forBitWidth(
			"dataWidth", Strings.getter("ramDataWidthAttr"));
	public static final AttributeOption SEL_HIGH = new AttributeOption("high", Strings.getter("stdTriggerHigh"));

	public static final AttributeOption SEL_LOW = new AttributeOption("low", Strings.getter("stdTriggerLow"));

	public static final Attribute<AttributeOption> ATTR_SELECTION = Attributes.forOption("Select",
			Strings.getter("ramSelAttr"), new AttributeOption[] { SEL_HIGH, SEL_LOW });
	// port-related constants
	static final int DATA = 0;
	static final int ADDR = 1;
	static final int MEM_INPUTS = 2;
	// other constants
	public static final int DELAY = 10;

	private WeakHashMap<Instance, File> currentInstanceFiles;

	Mem(String name, StringGetter desc, int extraPorts) {
		super(name, desc);
		currentInstanceFiles = new WeakHashMap<Instance, File>();
		setInstancePoker(MemPoker.class);
		setKeyConfigurator(JoinedConfigurator.create(new BitWidthConfigurator(
				ADDR_ATTR, 2, 24, 0), new BitWidthConfigurator(DATA_ATTR)));

		setOffsetBounds(Bounds.create(-140, -40, 140, 80));
	}

	@Override
	protected void configureNewInstance(Instance instance) {
		configurePorts(instance);
	}

	abstract void configurePorts(Instance instance);

	@Override
	public abstract AttributeSet createAttributeSet();

	protected void DrawAddress(InstancePainter painter, int xpos, int ypos,
			int NrAddressBits) {
		Graphics g = painter.getGraphics();
		GraphicsUtil.switchToWidth(g, 2);
		g.drawLine(xpos + 10, ypos + 10, xpos + 19, ypos + 10);
		g.drawLine(xpos + 5, ypos + 5, xpos + 10, ypos + 10);
		g.drawLine(xpos + 10, ypos + 30, xpos + 19, ypos + 30);
		g.drawLine(xpos + 5, ypos + 25, xpos + 10, ypos + 30);
		if (NrAddressBits > 2) {
			for (int i = 0; i < 3; i++) {
				g.drawLine(xpos + 15, ypos + 13 + i * 6, xpos + 15, ypos + 15
						+ i * 6);
			}
		}
		GraphicsUtil.switchToWidth(g, 5);
		g.drawLine(xpos, ypos, xpos + 5, ypos + 5);
		g.drawLine(xpos + 5, ypos + 5, xpos + 5, ypos + 25);
		GraphicsUtil.switchToWidth(g, 1);
		GraphicsUtil.drawText(g, "0", xpos + 22, ypos + 10,
				GraphicsUtil.H_LEFT, GraphicsUtil.V_CENTER);
		GraphicsUtil.drawText(g, Integer.toString(NrAddressBits - 1),
				xpos + 22, ypos + 30, GraphicsUtil.H_LEFT,
				GraphicsUtil.V_CENTER);
		GraphicsUtil.drawText(g, "A", xpos + 50, ypos + 20,
				GraphicsUtil.H_LEFT, GraphicsUtil.V_CENTER);
		g.drawLine(xpos + 40, ypos + 5, xpos + 45, ypos + 10);
		g.drawLine(xpos + 45, ypos + 10, xpos + 45, ypos + 17);
		g.drawLine(xpos + 45, ypos + 17, xpos + 48, ypos + 20);
		g.drawLine(xpos + 48, ypos + 20, xpos + 45, ypos + 23);
		g.drawLine(xpos + 45, ypos + 23, xpos + 45, ypos + 30);
		g.drawLine(xpos + 40, ypos + 35, xpos + 45, ypos + 30);
		String size = Integer.toString((1 << NrAddressBits) - 1);
		Font font = g.getFont();
		FontMetrics fm = g.getFontMetrics(font);
		int StrSize = fm.stringWidth(size);
		g.drawLine(xpos + 60, ypos + 20, xpos + 60 + StrSize, ypos + 20);
		GraphicsUtil.drawText(g, "0", xpos + 60 + (StrSize / 2), ypos + 19,
				GraphicsUtil.H_CENTER, GraphicsUtil.V_BOTTOM);
		GraphicsUtil.drawText(g, size, xpos + 60 + (StrSize / 2), ypos + 21,
				GraphicsUtil.H_CENTER, GraphicsUtil.V_TOP);
		painter.drawPort(ADDR);
	}

	public abstract int getControlHeight(AttributeSet attrs);

	File getCurrentImage(Instance instance) {
		return currentInstanceFiles.get(instance);
	}

	abstract HexFrame getHexFrame(Project proj, Instance instance,
			CircuitState state);

	@Override
	protected Object getInstanceFeature(Instance instance, Object key) {
		if (key == MenuExtender.class) {
			return new MemMenu(this, instance);
		}
		return super.getInstanceFeature(instance, key);
	}

	protected String GetSizeLabel(int NrAddressBits) {
		String[] Labels = { "", "k", "M", "G" };
		int pass = 0;
		int AddrBits = NrAddressBits;
		while (AddrBits > 9) {
			pass++;
			AddrBits -= 10;
		}
		int size = 1 << AddrBits;
		return Integer.toString(size) + Labels[pass];
	}

	abstract MemState getState(Instance instance, CircuitState state);

	abstract MemState getState(InstanceState state);

	public void loadImage(InstanceState instanceState, File imageFile)
			throws IOException {
		MemState s = this.getState(instanceState);
		HexFile.open(s.getContents(), imageFile);
		this.setCurrentImage(instanceState.getInstance(), imageFile);
	}

	@Override
	public abstract void propagate(InstanceState state);

	void setCurrentImage(Instance instance, File value) {
		currentInstanceFiles.put(instance, value);
	}
}
