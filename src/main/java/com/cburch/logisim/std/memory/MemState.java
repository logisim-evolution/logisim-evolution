/*
 * Logisim-evolution - digital logic design tool and simulator
 * Copyright by the Logisim-evolution developers
 *
 * https://github.com/logisim-evolution/
 *
 * This is free software released under GNU GPLv3 license
 */

package com.cburch.logisim.std.memory;

import com.cburch.hex.HexModel;
import com.cburch.hex.HexModelListener;
import com.cburch.logisim.data.Bounds;
import com.cburch.logisim.instance.InstanceData;
import com.cburch.logisim.util.GraphicsUtil;
import com.cburch.logisim.util.StringUtil;
import java.awt.Color;
import java.awt.Graphics;

class MemState implements InstanceData, Cloneable, HexModelListener {

  private MemContents contents;
  private long curScroll = 0;
  private long cursorLoc = -1;
  private long curAddr = -1;
  private boolean recalculateParameters = true;
  private int nrOfLines = 1;
  private int nrDataSymbolsEachLine = 1;
  private int addrBlockSize = 0;
  private int dataBlockSize = 0;
  private int dataSize = 0;
  private int spaceSize = 0;
  private int xOffset = 0;
  private int yOffset = 0;
  private int charHeight = 0;
  private Bounds displayWindow;

  MemState(MemContents contents) {
    this.contents = contents;
    setBits(contents.getLogLength(), contents.getWidth());
    contents.addHexModelListener(this);
  }

  @Override
  public void bytesChanged(HexModel source, long start, long numBytes, long[] oldValues) {}

  private void calculateDisplayParameters(
      Graphics g, int offsetX, int offsetY, int DisplayWidth, int DisplayHeight) {
    recalculateParameters = false;
    displayWindow = Bounds.create(offsetX, offsetY, DisplayWidth, DisplayHeight);
    final var addrBits = getAddrBits();
    final var dataBits = contents.getWidth();
    final var font = g.getFont();
    final var fm = g.getFontMetrics(font);
    addrBlockSize = ((fm.stringWidth(StringUtil.toHexString(addrBits, 0)) + 9) / 10) * 10;
    dataSize = fm.stringWidth(StringUtil.toHexString(dataBits, 0) + " ");
    spaceSize = fm.stringWidth(" ");
    nrDataSymbolsEachLine = (DisplayWidth - addrBlockSize) / dataSize;
    if (nrDataSymbolsEachLine == 0) nrDataSymbolsEachLine++;
    if (nrDataSymbolsEachLine > 3 && nrDataSymbolsEachLine % 2 != 0) nrDataSymbolsEachLine--;
    nrOfLines =
        DisplayHeight
            / (fm.getHeight() + 2); // (dataBits == 1) ? 1 : TotalHeight / (fm.getHeight() + 2);
    if (nrOfLines == 0) nrOfLines = 1;
    var totalShowableEntries = nrDataSymbolsEachLine * nrOfLines;
    final var totalNrOfEntries = (1 << addrBits);
    while (totalShowableEntries > (totalNrOfEntries + nrDataSymbolsEachLine - 1)) {
      nrOfLines--;
      totalShowableEntries -= nrDataSymbolsEachLine;
    }
    if (nrOfLines == 0) {
      nrOfLines = 1;
      nrDataSymbolsEachLine = totalNrOfEntries;
    }
    /* here we calculate to total x-sizes */
    dataBlockSize = nrDataSymbolsEachLine * (dataSize);
    final var totalWidth = addrBlockSize + dataBlockSize;
    xOffset = offsetX + (DisplayWidth / 2) - (totalWidth / 2);
    /* Same calculations for the height */
    charHeight = fm.getHeight();
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
    int ystop = ystart + nrOfLines * (charHeight + 2);
    int xstart = xOffset + addrBlockSize;
    int xstop = xstart + dataBlockSize;
    if ((x < xstart) | (x > xstop) | (y < ystart) | (y > ystop)) return -1;
    x = x - xstart;
    y = y - ystart;
    int line = y / (charHeight + 2);
    int symbol = x / (dataSize);
    long pointedAddr = curScroll + (line * nrDataSymbolsEachLine) + symbol;
    return isValidAddr(pointedAddr) ? pointedAddr : getLastAddress();
  }

  public Bounds getBounds(long addr, Bounds bds) {
    /* This function returns the rectangle shape around an item */
    if (addr >= 0) {
      addr -= curScroll;
    }
    return Bounds.create(bds.getX() + xOffset, bds.getY() + yOffset, addrBlockSize, charHeight + 2);
  }

  public Bounds getDataBounds(long addr, Bounds bds) {
    var curAddr = (int) curScroll;
    for (var row = 0; row < nrOfLines; row++) {
      for (var column = 0; column < nrDataSymbolsEachLine; column++) {
        if ((curAddr + column) == addr && isValidAddr(curAddr + column))
          return getDataBound(bds.getX(), bds.getY(), row, column);
      }
      curAddr += nrDataSymbolsEachLine;
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

  int getNrOfLineItems() {
    return nrDataSymbolsEachLine;
  }

  int getNrOfLines() {
    return nrOfLines;
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

  @Override
  public void metainfoChanged(HexModel source) {
    setBits(contents.getLogLength(), contents.getWidth());
  }

  private boolean windowChanged(int offsetX, int offsetY, int displayWidth, int displayHeight) {
    return displayWindow.getX() != offsetX
        || displayWindow.getY() != offsetY
        || displayWindow.getWidth() != displayWidth
        || displayWindow.getHeight() != displayHeight;
  }

  public void paint(
      Graphics g,
      int leftX,
      int topY,
      int offsetX,
      int offsetY,
      int displayWidth,
      int displayHeight,
      int nrItemsToHighlight) {
    if (recalculateParameters || windowChanged(offsetX, offsetY, displayWidth, displayHeight))
      calculateDisplayParameters(g, offsetX, offsetY, displayWidth, displayHeight);
    final var blockHeight = nrOfLines * (charHeight + 2);
    final var totalNrOfEntries = (1 << getAddrBits());
    g.setColor(Color.LIGHT_GRAY);
    g.fillRect(leftX + xOffset, topY + yOffset, dataBlockSize + addrBlockSize, blockHeight);
    g.setColor(Color.DARK_GRAY);
    g.drawRect(leftX + xOffset + addrBlockSize, topY + yOffset, dataBlockSize, blockHeight);
    g.setColor(Color.BLACK);
    /* draw the addresses */
    int addr = (int) curScroll;
    if ((addr + (nrOfLines * nrDataSymbolsEachLine)) > totalNrOfEntries) {
      addr = totalNrOfEntries - (nrOfLines * nrDataSymbolsEachLine);
      if (addr < 0) addr = 0;
      curScroll = addr;
    }
    /* draw the contents */
    int firsty = topY + getFirstYoffset();
    int yinc = getDataBlockHeight();
    int firstx = leftX + getFirstXoffset();
    for (var i = 0; i < nrOfLines; i++) {
      /* Draw address */
      GraphicsUtil.drawText(
          g,
          StringUtil.toHexString(getAddrBits(), addr),
          leftX + xOffset + (addrBlockSize / 2),
          firsty + i * (yinc),
          GraphicsUtil.H_CENTER,
          GraphicsUtil.V_CENTER);
      /* Draw data */
      for (var j = 0; j < nrDataSymbolsEachLine; j++) {
        long value = contents.get(addr + j);
        if (isValidAddr(addr + j)) {

          if (highLight((addr + j), nrItemsToHighlight)) {
            final var dataBounds = getDataBound(leftX, topY, i, j);
            g.setColor(Color.DARK_GRAY);
            g.fillRect(
                dataBounds.getX(),
                dataBounds.getY(),
                dataBounds.getWidth(),
                dataBounds.getHeight());
            g.setColor(Color.WHITE);
            GraphicsUtil.drawText(
                g,
                StringUtil.toHexString(contents.getWidth(), value),
                firstx + j * dataSize,
                firsty + i * yinc,
                GraphicsUtil.H_CENTER,
                GraphicsUtil.V_CENTER);
            g.setColor(Color.BLACK);
          } else {
            GraphicsUtil.drawText(
                g,
                StringUtil.toHexString(contents.getWidth(), value),
                firstx + j * dataSize,
                firsty + i * yinc,
                GraphicsUtil.H_CENTER,
                GraphicsUtil.V_CENTER);
          }
        }
      }
      addr += nrDataSymbolsEachLine;
    }
  }

  private boolean highLight(int addr, int nrItemsToHighlight) {
    return (addr >= curAddr) && (addr < curAddr + nrItemsToHighlight);
  }

  private Bounds getDataBound(int xoff, int yoff, int line, int column) {
    return Bounds.create(
        xoff + getFirstXoffset() + column * dataSize - (dataSize / 2) - 1,
        yoff + getFirstYoffset() + line * getDataBlockHeight() - (charHeight / 2) - 1,
        getDataBlockWidth(),
        getDataBlockHeight());
  }

  private int getFirstXoffset() {
    return xOffset + addrBlockSize + (spaceSize / 2) + ((dataSize - spaceSize) / 2);
  }

  private int getFirstYoffset() {
    return yOffset + (charHeight / 2) + 1;
  }

  private int getDataBlockWidth() {
    return dataSize + 2;
  }

  private int getDataBlockHeight() {
    return charHeight + 2;
  }

  void scrollToShow(long addr) {
    if (recalculateParameters) return;
    int addrBits = contents.getLogLength();
    if ((addr >>> addrBits) != 0) return;
    if (addr < curScroll) {
      long linesToScroll = (curScroll - addr + nrDataSymbolsEachLine - 1) / nrDataSymbolsEachLine;
      curScroll -= linesToScroll * nrDataSymbolsEachLine;
    } else if (addr >= (curScroll + nrOfLines * nrDataSymbolsEachLine)) {
      long curScrollEnd = curScroll + nrOfLines * nrDataSymbolsEachLine - 1;
      long linesToScroll = (addr - curScrollEnd + nrDataSymbolsEachLine - 1) / nrDataSymbolsEachLine;
      curScroll += linesToScroll * nrDataSymbolsEachLine;
      long totalNrOfEntries = (1 << addrBits);
      if ((curScroll + (nrOfLines * nrDataSymbolsEachLine)) > totalNrOfEntries) {
        curScroll = totalNrOfEntries - (nrOfLines * nrDataSymbolsEachLine);
      }
    }
    if (curScroll < 0) curScroll = 0;
  }

  //
  // methods for accessing the address bits
  //
  private void setBits(int addrBits, int dataBits) {
    recalculateParameters = true;
    if (contents == null) {
      contents = MemContents.create(addrBits, dataBits, false);
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
    if (recalculateParameters) return;
    long maxAddr = (1 << getAddrBits()) - (nrOfLines * nrDataSymbolsEachLine);
    if (addr > maxAddr) {
      addr = maxAddr; // note: maxAddr could be negative
    }
    if (addr < 0) {
      addr = 0;
    }
    curScroll = addr;
  }
}
