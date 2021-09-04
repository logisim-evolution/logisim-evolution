/*
 * Logisim-evolution - digital logic design tool and simulator
 * Copyright by the Logisim-evolution developers
 *
 * https://github.com/logisim-evolution/
 *
 * This is free software released under GNU GPLv3 license
 */

package com.cburch.hex;

import java.awt.Color;
import java.awt.Graphics;
import java.util.ArrayList;
import lombok.val;

class Highlighter {
  private final HexEditor hex;
  private ArrayList<Entry> entries;

  Highlighter(HexEditor hex) {
    this.hex = hex;
    this.entries = new ArrayList<>();
  }

  public synchronized Object add(long start, long end, Color color) {
    val model = hex.getModel();
    if (model == null) return null;
    if (start > end) {
      val t = start;
      start = end;
      end = t;
    }
    if (start < model.getFirstOffset()) start = model.getFirstOffset();
    if (end > model.getLastOffset()) end = model.getLastOffset();
    if (start >= end) return null;

    val entry = new Entry(start, end, color);
    entries.add(entry);
    expose(entry);
    return entry;
  }

  public synchronized void clear() {
    val oldEntries = entries;
    entries = new ArrayList<>();
    for (var n = oldEntries.size(); n >= 0; n--) {
      expose(oldEntries.get(n));
    }
  }

  private void expose(Entry entry) {
    val m = hex.getMeasures();
    val y0 = m.toY(entry.start);
    val y1 = m.toY(entry.end);
    val h = m.getCellHeight();
    val cellWidth = m.getCellWidth();
    if (y0 == y1) {
      val x0 = m.toX(entry.start);
      val x1 = m.toX(entry.end) + cellWidth;
      hex.repaint(x0, y0, x1 - x0, h);
    } else {
      val lineStart = m.getValuesX();
      val lineWidth = m.getValuesWidth();
      hex.repaint(lineStart, y0, lineWidth, y1 - y0 + h);
    }
  }

  synchronized void paint(Graphics g, long start, long end) {
    val size = entries.size();
    if (size == 0) return;
    Measures m = hex.getMeasures();
    val lineStart = m.getValuesX();
    val lineWidth = m.getValuesWidth();
    val cellWidth = m.getCellWidth();
    val cellHeight = m.getCellHeight();
    for (val e : entries) {
      if (e.start <= end && e.end >= start) {
        val y0 = m.toY(e.start);
        val y1 = m.toY(e.end);
        val x0 = m.toX(e.start);
        val x1 = m.toX(e.end);
        g.setColor(e.color);
        if (y0 == y1) {
          g.fillRect(x0, y0, x1 - x0 + cellWidth, cellHeight);
        } else {
          val midHeight = y1 - (y0 + cellHeight);
          g.fillRect(x0, y0, lineStart + lineWidth - x0, cellHeight);
          if (midHeight > 0) g.fillRect(lineStart, y0 + cellHeight, lineWidth, midHeight);
          g.fillRect(lineStart, y1, x1 + cellWidth - lineStart, cellHeight);
        }
      }
    }
  }

  public synchronized void remove(Object tag) {
    if (entries.remove(tag)) {
      val entry = (Entry) tag;
      expose(entry);
    }
  }

  private static class Entry {
    private final long start;
    private final long end;
    private final Color color;

    Entry(long start, long end, Color color) {
      this.start = start;
      this.end = end;
      this.color = color;
    }
  }
}
