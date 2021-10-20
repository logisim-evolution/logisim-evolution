/*
 * Logisim-evolution - digital logic design tool and simulator
 * Copyright by the Logisim-evolution developers
 *
 * https://github.com/logisim-evolution/
 *
 * This is free software released under GNU GPLv3 license
 */

package com.cburch.hex;

import java.awt.FontMetrics;
import java.awt.Graphics;
import lombok.Getter;

class Measures {
  private final HexEditor hex;
  /**
   * Header chars
   */
  @Getter private int labelChars;
  @Getter private int cellChars;
  @Getter private int headerWidth;
  private int spacerWidth;
  @Getter private int cellWidth;
  @Getter private int cellHeight;
  @Getter private int columnCount;
  @Getter private int baseX;
  private boolean guessed;

  public Measures(HexEditor hex) {
    this.hex = hex;
    this.guessed = true;
    this.columnCount = 1;
    this.cellWidth = -1;
    this.cellHeight = -1;
    this.cellChars = 2;
    this.labelChars = 4;

    computeCellSize(null);
  }

  private void computeCellSize(Graphics g) {
    HexModel model = hex.getModel();

    // compute number of characters in headers and cells
    if (model == null) {
      labelChars = 4;
      cellChars = 2;
    } else {
      int logSize = 0;
      long addrEnd = model.getLastOffset();
      while (addrEnd > (1L << logSize)) {
        logSize++;
      }
      labelChars = (logSize + 3) / 4;
      cellChars = (model.getValueWidth() + 3) / 4;
    }

    // compute character sizes
    FontMetrics fm = g == null ? null : g.getFontMetrics(hex.getFont());
    int charWidth;
    int spaceWidth;
    int lineHeight;
    if (fm == null) {
      charWidth = 8;
      spaceWidth = 6;
      final var font = hex.getFont();
      lineHeight = (font == null) ? 16 : font.getSize();
    } else {
      guessed = false;
      charWidth = 0;
      for (int i = 0; i < 16; i++) {
        int width = fm.stringWidth(Integer.toHexString(i));
        if (width > charWidth) charWidth = width;
      }
      spaceWidth = fm.stringWidth(" ");
      lineHeight = fm.getHeight();
    }

    // update header and cell dimensions
    headerWidth = labelChars * charWidth + spaceWidth;
    spacerWidth = spaceWidth;
    cellWidth = cellChars * charWidth + spaceWidth;
    cellHeight = lineHeight;

    // compute preferred size
    final var width = headerWidth + columnCount * cellWidth + (columnCount / 4) * spacerWidth;
    long height;
    if (model == null) {
      height = 16 * cellHeight;
    } else {
      final var addr0 = getBaseAddress(model);
      final var addr1 = model.getLastOffset();
      final var rows = (int) (((addr1 - addr0 + 1) + columnCount - 1) / columnCount);
      height = rows * cellHeight;
      if (height > Integer.MAX_VALUE) height = Integer.MAX_VALUE;
    }

    // update preferred size
    final var pref = hex.getPreferredSize();
    if (pref.width != width || pref.height != height) {
      pref.width = width;
      pref.height = (int) height;
      hex.setPreferredSize(pref);
      hex.revalidate();
    }

    widthChanged();
  }

  void ensureComputed(Graphics g) {
    if (guessed || cellWidth < 0) computeCellSize(g);
  }

  public long getBaseAddress(HexModel model) {
    if (model == null) {
      return 0;
    } else {
      long addr0 = model.getFirstOffset();
      return addr0 - addr0 % columnCount;
    }
  }

  public int getValuesWidth() {
    return ((columnCount - 1) / 4) * spacerWidth + columnCount * cellWidth;
  }

  public int getValuesX() {
    return baseX + spacerWidth;
  }

  void recompute() {
    computeCellSize(hex.getGraphics());
  }

  public long toAddress(int x, int y) {
    HexModel model = hex.getModel();
    if (model == null) return Integer.MIN_VALUE;
    final long addr0 = model.getFirstOffset();
    final long addr1 = model.getLastOffset();

    long base = getBaseAddress(model) + ((long) y / cellHeight) * columnCount;
    int offs = (x - baseX) / (cellWidth + (spacerWidth + 2) / 4);
    if (offs < 0) offs = 0;
    if (offs >= columnCount) offs = columnCount - 1;

    long ret = base + offs;
    if (ret > addr1) ret = addr1;
    if (ret < addr0) ret = addr0;
    return ret;
  }

  public int toX(long addr) {
    int col = (int) (addr % columnCount);
    return baseX + (1 + (col / 4)) * spacerWidth + col * cellWidth;
  }

  public int toY(long addr) {
    long row = (addr - getBaseAddress(hex.getModel())) / columnCount;
    long ret = row * cellHeight;
    return ret < Integer.MAX_VALUE ? (int) ret : Integer.MAX_VALUE;
  }

  void widthChanged() {
    int oldCols = columnCount;
    int width;
    if (guessed || cellWidth < 0) {
      columnCount = 16;
      width = hex.getPreferredSize().width;
    } else {
      width = hex.getWidth();
      int ret = (width - headerWidth) / (cellWidth + (spacerWidth + 3) / 4);
      if (ret >= 16) columnCount = 16;
      else if (ret >= 8) columnCount = 8;
      else columnCount = 4;
    }
    int lineWidth = headerWidth + columnCount * cellWidth + ((columnCount / 4) - 1) * spacerWidth;
    int newBase = headerWidth + Math.max(0, (width - lineWidth) / 2);
    if (baseX != newBase) {
      baseX = newBase;
      hex.repaint();
    }
    if (columnCount != oldCols) recompute();
  }
}
