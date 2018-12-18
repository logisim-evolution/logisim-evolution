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
import java.awt.FontMetrics;
import java.awt.Graphics;

import com.cburch.hex.HexModel;
import com.cburch.hex.HexModelListener;
import com.cburch.logisim.data.Bounds;
import com.cburch.logisim.instance.InstanceData;
import com.cburch.logisim.util.GraphicsUtil;
import com.cburch.logisim.util.StringUtil;

class MemState implements InstanceData, Cloneable, HexModelListener {

	private MemContents contents;
	private long curScroll = 0;
	private long cursorLoc = -1;
	private long curAddr = -1;
	private boolean RecalculateParameters = true;
	private int NrOfLines = 1;
	private int NrDataSymbolsEachLine = 1;
	private int AddrBlockSize = 0;
	private int DataBlockSize = 0;
	private int DataSize = 0;
	private int SpaceSize = 0;
	private int xOffset = 0;
	private int yOffset = 0;
	private int CharHeight = 0;
	private int ControlBlockHeight = 0;

	MemState(MemContents contents) {
		this.contents = contents;
		setBits(contents.getLogLength(), contents.getWidth());
		contents.addHexModelListener(this);
	}

	public void bytesChanged(HexModel source, long start, long numBytes,
			int[] oldValues) {
	}

	private void CalculateDisplayParameters(Graphics g, boolean HasDataIn) {
		RecalculateParameters = false;
		int addrBits = getAddrBits();
		int dataBits = contents.getWidth();
		int TotalWidth = (HasDataIn) ? Mem.SymbolWidth - 80
				: Mem.SymbolWidth - 40;
		int TotalHeight = (dataBits == 1) ? 16 : (dataBits * 20) - 6;
		Font font = g.getFont();
		FontMetrics fm = g.getFontMetrics(font);
		AddrBlockSize = ((fm.stringWidth(StringUtil.toHexString(addrBits, 0)) + 9) / 10) * 10;
		DataSize = fm.stringWidth(StringUtil.toHexString(dataBits, 0) + " ");
		SpaceSize = fm.stringWidth(" ");
		NrDataSymbolsEachLine = (TotalWidth - AddrBlockSize) / DataSize;
		NrOfLines = (dataBits == 1) ? 1 : TotalHeight / (fm.getHeight() + 2);
		int TotalShowableEntries = NrDataSymbolsEachLine * NrOfLines;
		int TotalNrOfEntries = (1 << addrBits);
		while (TotalShowableEntries > (TotalNrOfEntries + NrDataSymbolsEachLine - 1)) {
			NrOfLines--;
			TotalShowableEntries -= NrDataSymbolsEachLine;
		}
		if (NrOfLines == 0) {
			NrOfLines = 1;
			NrDataSymbolsEachLine = TotalNrOfEntries;
		}
		/* here we calculate to total x-sizes */
		DataBlockSize = NrDataSymbolsEachLine * (DataSize);
		int TotalSize = AddrBlockSize + DataBlockSize;
		xOffset = (Mem.SymbolWidth / 2) - (TotalSize / 2);
		/* Same calculations for the height */
		CharHeight = fm.getHeight();
		int BlockHeigt = NrOfLines * (CharHeight + 2);
		yOffset = (dataBits * 10) - (BlockHeigt / 2);
		if (yOffset > 5)
			yOffset = 5;
	}

	@Override
	public MemState clone() {
		try {
			MemState ret = (MemState) super.clone();
			ret.contents = contents.clone();
			ret.contents.addHexModelListener(ret);
			return ret;
		} catch (CloneNotSupportedException e) {
			return null;
		}
	}

	//
	// methods for accessing data within memory
	//
	int getAddrBits() {
		return contents.getLogLength();
	}

	//
	// graphical methods
	//
	public long getAddressAt(int x, int y) {
		/*
		 * This function returns the address of a data symbol inside the data
		 * block
		 */
		int ystart = ControlBlockHeight + yOffset;
		int ystop = ystart + NrOfLines * (CharHeight + 2);
		int xstart = 20 + xOffset + AddrBlockSize;
		int xstop = xstart + DataBlockSize;
		if ((x < xstart) | (x > xstop) | (y < ystart) | (y > ystop))
			return -1;
		x = x - xstart;
		y = y - ystart;
		int line = y / (CharHeight + 2);
		int symbol = x / (DataSize);
		long pointedAddr = curScroll + (line * NrDataSymbolsEachLine) + symbol;
		return isValidAddr(pointedAddr) ? pointedAddr : getLastAddress();
	}

	public Bounds getBounds(long addr, Bounds bds) {
		/* This function returns the rectangle shape around an item */
		if (addr < 0) {
			return Bounds.create(bds.getX() + 20 + xOffset, bds.getY()
					+ ControlBlockHeight + yOffset, AddrBlockSize,
					CharHeight + 2);
		} else {
			addr -= curScroll;
			int line = ((int) addr) / NrDataSymbolsEachLine;
			int item = ((int) addr) % NrDataSymbolsEachLine;
			return Bounds.create(bds.getX() + 20 + xOffset + AddrBlockSize
					+ item * (DataSize), bds.getY() + ControlBlockHeight
					+ yOffset + line * (CharHeight + 2), DataSize,
					CharHeight + 2);
		}
	}

	public MemContents getContents() {
		return contents;
	}

	long getCurrent() {
		return curAddr;
	}

	//
	// methods for manipulating cursor and scroll location
	//
	long getCursor() {
		return cursorLoc;
	}

	int getDataBits() {
		return contents.getWidth();
	}

	long getLastAddress() {
		return (1L << contents.getLogLength()) - 1;
	}

	int GetNrOfLineItems() {
		return NrDataSymbolsEachLine;
	}

	int GetNrOfLines() {
		return NrOfLines;
	}

	long getScroll() {
		return curScroll;
	}

	public boolean isSplitted() {
		return false;
	}

	boolean isValidAddr(long addr) {
		int addrBits = contents.getLogLength();
		return addr >>> addrBits == 0;
	}

	public void metainfoChanged(HexModel source) {
		setBits(contents.getLogLength(), contents.getWidth());
	}

	public void paint(Graphics g, int leftX, int topY, boolean HasDataIn,
			int ControlBlockHeight) {
		this.ControlBlockHeight = ControlBlockHeight;
		topY += ControlBlockHeight;
		if (RecalculateParameters)
			CalculateDisplayParameters(g, HasDataIn);
		int BlockHeigt = NrOfLines * (CharHeight + 2);
		int TotalNrOfEntries = (1 << getAddrBits());
		g.setColor(Color.LIGHT_GRAY);
		g.fillRect(leftX + xOffset, topY + yOffset, DataBlockSize
				+ AddrBlockSize, BlockHeigt);
		g.setColor(Color.DARK_GRAY);
		g.drawRect(leftX + xOffset + AddrBlockSize, topY + yOffset,
				DataBlockSize, BlockHeigt);
		g.setColor(Color.BLACK);
		/* draw the addresses */
		int addr = (int) curScroll;
		if ((addr + (NrOfLines * NrDataSymbolsEachLine)) > TotalNrOfEntries) {
			addr = TotalNrOfEntries - (NrOfLines * NrDataSymbolsEachLine);
			if (addr < 0)
				addr = 0;
			curScroll = addr;
		}
		/* draw the contents */
		int firsty = topY + yOffset + (CharHeight / 2) + 1;
		int yinc = CharHeight + 2;
		int firstx = leftX + xOffset + AddrBlockSize + (SpaceSize / 2)
				+ ((DataSize - SpaceSize) / 2);
		for (int i = 0; i < NrOfLines; i++) {
			/* Draw address */
			GraphicsUtil.drawText(g,
					StringUtil.toHexString(getAddrBits(), addr), leftX
							+ xOffset + (AddrBlockSize / 2), firsty + i
							* (yinc), GraphicsUtil.H_CENTER,
					GraphicsUtil.V_CENTER);
			/* Draw data */
			for (int j = 0; j < NrDataSymbolsEachLine; j++) {
				int value = contents.get(addr + j);
				if (isValidAddr(addr + j)) {
					if ((addr + j) == curAddr) {
						g.setColor(Color.DARK_GRAY);
						g.fillRect(firstx + j * DataSize - (DataSize / 2) - 1,
								firsty + i * yinc - (CharHeight / 2) - 1,
								DataSize + 2, CharHeight + 2);
						g.setColor(Color.WHITE);
						GraphicsUtil.drawText(g, StringUtil.toHexString(
								contents.getWidth(), value), firstx + j
								* DataSize, firsty + i * yinc,
								GraphicsUtil.H_CENTER, GraphicsUtil.V_CENTER);
						g.setColor(Color.BLACK);
					} else {
						GraphicsUtil.drawText(g, StringUtil.toHexString(
								contents.getWidth(), value), firstx + j
								* DataSize, firsty + i * yinc,
								GraphicsUtil.H_CENTER, GraphicsUtil.V_CENTER);
					}
				}
			}
			addr += NrDataSymbolsEachLine;
		}
	}

	void scrollToShow(long addr) {
		if (RecalculateParameters)
			return;
		if (isValidAddr(addr)) {
			int NrOfDataItemsDisplayed = NrOfLines * NrDataSymbolsEachLine;
			while (addr < curScroll) {
				curScroll -= NrDataSymbolsEachLine;
				if (curScroll < 0)
					curScroll = 0;
			}
			while (addr >= (curScroll + NrOfDataItemsDisplayed)) {
				curScroll += NrDataSymbolsEachLine;
			}
		}
	}

	//
	// methods for accessing the address bits
	//
	private void setBits(int addrBits, int dataBits) {
		RecalculateParameters = true;
		if (contents == null) {
			contents = MemContents.create(addrBits, dataBits,false);
		} else {
			contents.setDimensions(addrBits, dataBits,false);
		}
		cursorLoc = -1;
		curAddr = -1;
		curScroll = 0;
	}

	void setCurrent(long value) {
		curAddr = isValidAddr(value) ? value : -1L;
	}

	void setCursor(long value) {
		cursorLoc = isValidAddr(value) ? value : -1L;
	}

	void setScroll(long addr) {
		if (RecalculateParameters)
			return;
		long maxAddr = (1 << getAddrBits())
				- (NrOfLines * NrDataSymbolsEachLine);
		if (addr > maxAddr) {
			addr = maxAddr; // note: maxAddr could be negative
		}
		if (addr < 0) {
			addr = 0;
		}
		curScroll = addr;
	}
}
