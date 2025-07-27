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

class Measures {
  private final HexEditor hex;
  private int headerChars;
  private int cellChars;
  private int headerWidth;
  private int spacerWidth;
  private int cellWidth;
  private int cellHeight;
  private int cols;
  private int baseX;
  private boolean guessed;
  private static int bankRows;       // <= 107 374 182 with cellHeight = 20

  public Measures(HexEditor hex) {
    this.hex = hex;
    this.guessed = true;
    this.cols = 1;
    this.cellWidth = -1;
    this.cellHeight = -1;
    this.cellChars = 2;
    this.headerChars = 4;

    computeCellSize(null);
  }

  private void computeCellSize(Graphics g) {
    HexModel model = hex.getModel();

    // compute number of characters in headers and cells
    if (model == null) {
      headerChars = 4;
      cellChars = 2;
    } else {
      int logSize = 0;
      long addrEnd = model.getLastOffset();
      while (addrEnd > (1L << logSize)) {
        logSize++;
      }
      headerChars = Math.min((logSize + 3) / 4, 8);  // máx 8 dígitos hex para 32 bits
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
    headerWidth = headerChars * charWidth + spaceWidth;
    spacerWidth = spaceWidth;
    cellWidth = cellChars * charWidth + spaceWidth;
    cellHeight = lineHeight;

    // compute preferred size
    final var width = headerWidth + cols * cellWidth + (cols / 4) * spacerWidth + 2 * spacerWidth;
    long height;
    if (model == null) {
      height = 16 * cellHeight;
    } else {
      final var addr0 = getBaseAddress(model);
      final var addr1 = model.getLastOffset();
      final var rows = (int) (((addr1 - addr0 + 1) + cols - 1) / cols);
      bankRows = Integer.MAX_VALUE / cellHeight;
      height = Math.min(rows, bankRows) * cellHeight;
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
      return addr0 - addr0 % cols;
    }
  }

  public int getBaseX() {
    return baseX;
  }

  public int getCellChars() {
    return cellChars;
  }

  public int getCellHeight() {
    return cellHeight;
  }

  public int getCellWidth() {
    return cellWidth;
  }

  public int getColumnCount() {
    return cols;
  }

  public int getLabelChars() {
    return headerChars;
  }

  public int getLabelWidth() {
    return headerWidth;
  }

  public int getValuesWidth() {
    return ((cols - 1) / 4) * spacerWidth + cols * cellWidth;
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

    long base = getBaseAddress(model) + ((long) y / cellHeight) * cols;
    int offs = (x - baseX) / (cellWidth + (spacerWidth + 2) / 4);
    if (offs < 0) offs = 0;
    if (offs >= cols) offs = cols - 1;

    long ret = base + offs;
    if (ret > addr1) ret = addr1;
    if (ret < addr0) ret = addr0;
    return ret;
  }

  public int toX(long addr) {
    int col = (int) (addr % cols);
    return baseX + (1 + (col / 4)) * spacerWidth + col * cellWidth;
  }

  public int toY(long addr) {
    long row = (addr - getBaseAddress(hex.getModel())) / cols;
    long rowInBank = row % bankRows; // 0 … bankRows‑1
    return (int) (rowInBank * cellHeight); // always < Integer.MAX_VALUE
  }

  void widthChanged() {
    int oldCols = cols;
    int width;
    if (guessed || cellWidth < 0) {
      cols = 16;
      width = hex.getPreferredSize().width;
    } else {
      width = hex.getWidth();
      int groupWidth = cellWidth * 4 + spacerWidth;
      int ret = ((width - headerWidth + spacerWidth) / groupWidth) * 4;
      if (ret >= 64) cols = 64;
      else if (ret >= 48) cols = 48;      
      else if (ret >= 32) cols = 32;
      else if (ret >= 16) cols = 16;
      else if (ret >= 8) cols = 8;
      else cols = 4;
    }
    int lineWidth = headerWidth + cols * cellWidth + ((cols / 4) - 1) * spacerWidth;
    int newBase = headerWidth + Math.max(0, (width - lineWidth) / 2);
    if (baseX != newBase) {
      baseX = newBase;
      hex.repaint();
    }
    if (cols != oldCols) recompute();
  }
}
