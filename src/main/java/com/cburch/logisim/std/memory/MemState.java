/**
 * This file is part of logisim-evolution.
 *
 * Logisim-evolution is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by the
 * Free Software Foundation, either version 3 of the License, or (at your
 * option) any later version.
 *
 * Logisim-evolution is distributed in the hope that it will be useful, but
 * WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 * or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
 * for more details.
 *
 * You should have received a copy of the GNU General Public License along 
 * with logisim-evolution. If not, see <http://www.gnu.org/licenses/>.
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
 */

package com.cburch.logisim.std.memory;

import com.cburch.hex.HexModel;
import com.cburch.hex.HexModelListener;
import com.cburch.logisim.data.Bounds;
import com.cburch.logisim.instance.InstanceData;
import com.cburch.logisim.util.GraphicsUtil;
import com.cburch.logisim.util.StringUtil;
import java.awt.Color;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;

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
  private Bounds displayWindow;

  MemState(MemContents contents) {
    this.contents = contents;
    setBits(contents.getLogLength(), contents.getWidth());
    contents.addHexModelListener(this);
  }

  public void bytesChanged(HexModel source, long start, long numBytes, long[] oldValues) {}

  private void CalculateDisplayParameters(
      Graphics g, int offsetX, int offsetY, int DisplayWidth, int DisplayHeight) {
    RecalculateParameters = false;
    displayWindow = Bounds.create(offsetX, offsetY, DisplayWidth, DisplayHeight);
    int addrBits = getAddrBits();
    int dataBits = contents.getWidth();
    Font font = g.getFont();
    FontMetrics fm = g.getFontMetrics(font);
    AddrBlockSize = ((fm.stringWidth(StringUtil.toHexString(addrBits, 0)) + 9) / 10) * 10;
    DataSize = fm.stringWidth(StringUtil.toHexString(dataBits, 0) + " ");
    SpaceSize = fm.stringWidth(" ");
    NrDataSymbolsEachLine = (DisplayWidth - AddrBlockSize) / DataSize;
    if (NrDataSymbolsEachLine == 0) NrDataSymbolsEachLine++;
    if (NrDataSymbolsEachLine > 3 && NrDataSymbolsEachLine % 2 != 0) NrDataSymbolsEachLine--;
    NrOfLines =
        DisplayHeight
            / (fm.getHeight() + 2); // (dataBits == 1) ? 1 : TotalHeight / (fm.getHeight() + 2);
    if (NrOfLines == 0) NrOfLines = 1;
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
    int TotalWidth = AddrBlockSize + DataBlockSize;
    xOffset = offsetX + (DisplayWidth / 2) - (TotalWidth / 2);
    /* Same calculations for the height */
    CharHeight = fm.getHeight();
    yOffset = offsetY;
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
    int ystart = yOffset;
    int ystop = ystart + NrOfLines * (CharHeight + 2);
    int xstart = xOffset + AddrBlockSize;
    int xstop = xstart + DataBlockSize;
    if ((x < xstart) | (x > xstop) | (y < ystart) | (y > ystop)) return -1;
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
      return Bounds.create(
          bds.getX() + xOffset, bds.getY() + yOffset, AddrBlockSize, CharHeight + 2);
    } else {
      addr -= curScroll;
      return Bounds.create(
          bds.getX() + xOffset, bds.getY() + yOffset, AddrBlockSize, CharHeight + 2);
    }
  }
  
  public Bounds getDataBounds(long addr, Bounds bds) {
    int curAddr = (int) curScroll;
    for (int row = 0 ; row < NrOfLines ; row++) {
      for (int column = 0 ; column < NrDataSymbolsEachLine ; column++) {
        if ((curAddr+column) == addr && isValidAddr(curAddr+column)) 
          return getDataBound(bds.getX(),bds.getY(),row,column);
      }
      curAddr += NrDataSymbolsEachLine;
    }
    return Bounds.EMPTY_BOUNDS;
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

  private boolean windowChanged(int offsetX, int offsetY, int DisplayWidth, int DisplayHeight) {
    return displayWindow.getX() != offsetX || displayWindow.getY() != offsetY ||
           displayWindow.getWidth() != DisplayWidth || displayWindow.getHeight() != DisplayHeight;
  }

  public void paint(
      Graphics g,
      int leftX,
      int topY,
      int offsetX,
      int offsetY,
      int DisplayWidth,
      int DisplayHeight,
      int nrItemsToHighlight) {
    if (RecalculateParameters || windowChanged(offsetX,offsetY,DisplayWidth,DisplayHeight))
      CalculateDisplayParameters(g, offsetX, offsetY, DisplayWidth, DisplayHeight);
    int BlockHeight = NrOfLines * (CharHeight + 2);
    int TotalNrOfEntries = (1 << getAddrBits());
    g.setColor(Color.LIGHT_GRAY);
    g.fillRect(leftX + xOffset, topY + yOffset, DataBlockSize + AddrBlockSize, BlockHeight);
    g.setColor(Color.DARK_GRAY);
    g.drawRect(leftX + xOffset + AddrBlockSize, topY + yOffset, DataBlockSize, BlockHeight);
    g.setColor(Color.BLACK);
    /* draw the addresses */
    int addr = (int) curScroll;
    if ((addr + (NrOfLines * NrDataSymbolsEachLine)) > TotalNrOfEntries) {
      addr = TotalNrOfEntries - (NrOfLines * NrDataSymbolsEachLine);
      if (addr < 0) addr = 0;
      curScroll = addr;
    }
    /* draw the contents */
    int firsty = topY + getFirstYoffset();
    int yinc = getDataBlockHeight();
    int firstx = leftX + getFirstXoffset();
    for (int i = 0; i < NrOfLines; i++) {
      /* Draw address */
      GraphicsUtil.drawText(
          g,
          StringUtil.toHexString(getAddrBits(), addr),
          leftX + xOffset + (AddrBlockSize / 2),
          firsty + i * (yinc),
          GraphicsUtil.H_CENTER,
          GraphicsUtil.V_CENTER);
      /* Draw data */
      for (int j = 0; j < NrDataSymbolsEachLine; j++) {
        long value = contents.get(addr + j);
        if (isValidAddr(addr + j)) {
          
          if (highLight((addr + j), nrItemsToHighlight)) {
        	Bounds dataBounds = getDataBound(leftX,topY,i,j);
            g.setColor(Color.DARK_GRAY);
            g.fillRect(dataBounds.getX(),dataBounds.getY(),dataBounds.getWidth(),dataBounds.getHeight());
            g.setColor(Color.WHITE);
            GraphicsUtil.drawText(
                g,
                StringUtil.toHexString(contents.getWidth(), value),
                firstx + j * DataSize,
                firsty + i * yinc,
                GraphicsUtil.H_CENTER,
                GraphicsUtil.V_CENTER);
            g.setColor(Color.BLACK);
          } else {
            GraphicsUtil.drawText(
                g,
                StringUtil.toHexString(contents.getWidth(), value),
                firstx + j * DataSize,
                firsty + i * yinc,
                GraphicsUtil.H_CENTER,
                GraphicsUtil.V_CENTER);
          }
        }
      }
      addr += NrDataSymbolsEachLine;
    }
  }
  
  private boolean highLight(int addr, int nrItemsToHighlight) {
	return (addr >= curAddr)&&(addr < curAddr+nrItemsToHighlight);
  }
  
  private Bounds getDataBound(int xoff, int yoff, int line, int column) {
    return Bounds.create(
        xoff + getFirstXoffset() + column*DataSize - (DataSize / 2) - 1, 
        yoff + getFirstYoffset() + line*getDataBlockHeight() - (CharHeight / 2) - 1, 
            getDataBlockWidth(), getDataBlockHeight());
  }
  
  private int getFirstXoffset() {
    return xOffset + AddrBlockSize + (SpaceSize / 2) + ((DataSize - SpaceSize) / 2);
  }
  private int getFirstYoffset() {
    return yOffset + (CharHeight / 2) + 1;
  }
  private int getDataBlockWidth() {
    return DataSize + 2;
  }
  private int getDataBlockHeight() {
    return CharHeight + 2;
  }

  void scrollToShow(long addr) {
    if (RecalculateParameters) return;
    if (isValidAddr(addr)) {
      int NrOfDataItemsDisplayed = NrOfLines * NrDataSymbolsEachLine;
      while (addr < curScroll) {
        curScroll -= NrDataSymbolsEachLine;
        if (curScroll < 0) curScroll = 0;
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
      contents = MemContents.create(addrBits, dataBits);
    } else {
      contents.setDimensions(addrBits, dataBits);
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
    if (RecalculateParameters) return;
    long maxAddr = (1 << getAddrBits()) - (NrOfLines * NrDataSymbolsEachLine);
    if (addr > maxAddr) {
      addr = maxAddr; // note: maxAddr could be negative
    }
    if (addr < 0) {
      addr = 0;
    }
    curScroll = addr;
  }
}
