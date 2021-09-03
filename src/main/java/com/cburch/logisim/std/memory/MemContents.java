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
import com.cburch.logisim.prefs.AppPreferences;
import com.cburch.logisim.util.EventSourceWeakSupport;
import java.util.Arrays;

public class MemContents implements Cloneable, HexModel {
  public static MemContents create(int addrBits, int width) {
    return new MemContents(addrBits, width);
  }

  private static final int PAGE_SIZE_BITS = 12;
  private static final int PAGE_SIZE = 1 << PAGE_SIZE_BITS;

  private static final int PAGE_MASK = PAGE_SIZE - 1;

  private EventSourceWeakSupport<HexModelListener> listeners = null;
  private int width;
  private int addrBits;
  private long mask;
  private Page[] pages;

  private MemContents(int addrBits, int width) {
    listeners = null;
    setDimensions(addrBits, width);
  }

  //
  // HexModel methods
  //
  @Override
  public void addHexModelListener(HexModelListener l) {
    if (listeners == null) listeners = new EventSourceWeakSupport<>();
    listeners.add(l);
  }

  public void clear() {
    for (var i = 0; i < pages.length; i++) {
      if (pages[i] != null) clearPage(i);
    }
  }

  public void condClear() {
    if (!AppPreferences.Memory_Startup_Unknown.getBoolean())
      clear();
    else {
      for (var i = 0; i < pages.length; i++) {
        long[] oldValues = pages[i] != null ? pages[i].get(0, pages[i].getLength()) : null;
        pages[i] = MemContentsSub.createPage(PAGE_SIZE, width);
        if (oldValues != null)
          fireBytesChanged(i << PAGE_SIZE_BITS, oldValues.length, oldValues);
        else
          fireBytesChanged(i << PAGE_SIZE_BITS, pages[i].getLength(), pages[i].get(0, pages[i].getLength()));
      }
    }
  }

  private void clearPage(int index) {
    final var page = pages[index];
    final var oldValues = new long[page.getLength()];
    var changed = false;
    for (var j = 0; j < oldValues.length; j++) {
      long val = page.get(j) & mask;
      oldValues[j] = val;
      if (val != 0) changed = true;
    }
    if (changed) {
      pages[index] = null;
      fireBytesChanged(index << PAGE_SIZE_BITS, oldValues.length, oldValues);
    }
  }

  //
  // other methods
  //
  @Override
  public MemContents clone() {
    try {
      final var ret = (MemContents) super.clone();
      ret.listeners = null;
      ret.pages = new Page[this.pages.length];
      for (var i = 0; i < ret.pages.length; i++) {
        if (this.pages[i] != null) {
          ret.pages[i] = this.pages[i].clone();
        }
      }
      return ret;
    } catch (CloneNotSupportedException ex) {
      return this;
    }
  }

  private void ensurePage(int index) {
    if (pages[index] == null) {
      pages[index] = MemContentsSub.createPage(PAGE_SIZE, width);
    }
  }

  @Override
  public void fill(long start, long len, long value) {
    if (len == 0) return;

    var pageStart = (int) (start >>> PAGE_SIZE_BITS);
    final var startOffs = (int) (start & PAGE_MASK);
    final var pageEnd = (int) ((start + len - 1) >>> PAGE_SIZE_BITS);
    final var endOffs = (int) ((start + len - 1) & PAGE_MASK);
    value &= mask;

    if (pageStart == pageEnd) {
      ensurePage(pageStart);
      final var vals = new long[(int) len];
      Arrays.fill(vals, value);
      final var page = pages[pageStart];
      if (!page.matches(vals, startOffs, mask)) {
        final var oldValues = page.get(startOffs, (int) len);
        page.load(startOffs, vals, mask);
        if (value == 0 && page.isClear()) pages[pageStart] = null;
        fireBytesChanged(start, len, oldValues);
      }
    } else {
      if (startOffs == 0) {
        pageStart--;
      } else {
        if (value == 0 && pages[pageStart] == null) {
          // nothing to do
        } else {
          ensurePage(pageStart);
          final var vals = new long[PAGE_SIZE - startOffs];
          Arrays.fill(vals, value);
          final var page = pages[pageStart];
          if (!page.matches(vals, startOffs, mask)) {
            final var oldValues = page.get(startOffs, vals.length);
            page.load(startOffs, vals, mask);
            if (value == 0 && page.isClear()) pages[pageStart] = null;
            fireBytesChanged(start, PAGE_SIZE - pageStart, oldValues);
          }
        }
      }
      if (value == 0) {
        for (var i = pageStart + 1; i < pageEnd; i++) {
          if (pages[i] != null) clearPage(i);
        }
      } else {
        final var vals = new long[PAGE_SIZE];
        Arrays.fill(vals, value);
        for (var i = pageStart + 1; i < pageEnd; i++) {
          ensurePage(i);
          final var page = pages[i];
          if (!page.matches(vals, 0, mask)) {
            final var oldValues = page.get(0, PAGE_SIZE);
            page.load(0, vals, mask);
            fireBytesChanged(i << PAGE_SIZE_BITS, PAGE_SIZE, oldValues);
          }
        }
      }
      if (endOffs >= 0) {
        final var page = pages[pageEnd];
        if (value == 0 && page == null) {
          // nothing to do
        } else {
          ensurePage(pageEnd);
          final var vals = new long[endOffs + 1];
          Arrays.fill(vals, value);
          if (!page.matches(vals, 0, mask)) {
            final var oldValues = page.get(0, endOffs + 1);
            page.load(0, vals, mask);
            if (value == 0 && page.isClear()) pages[pageEnd] = null;
            fireBytesChanged(pageEnd << PAGE_SIZE_BITS, endOffs + 1, oldValues);
          }
        }
      }
    }
  }

  private void fireBytesChanged(long start, long numBytes, long[] oldValues) {
    if (listeners == null) return;
    var found = false;
    for (final var l : listeners) {
      found = true;
      l.bytesChanged(this, start, numBytes, oldValues);
    }
    if (!found) listeners = null;
  }

  private void fireMetainfoChanged() {
    if (listeners == null) return;
    var found = false;
    for (final var l : listeners) {
      found = true;
      l.metainfoChanged(this);
    }
    if (!found) listeners = null;
  }

  @Override
  public long get(long addr) {
    int page = (int) (addr >>> PAGE_SIZE_BITS);
    long offs = (addr & PAGE_MASK);
    if (page < 0 || page >= pages.length || pages[page] == null) return 0;
    return pages[page].get(offs) & mask;
  }

  @Override
  public long getFirstOffset() {
    return 0;
  }

  @Override
  public long getLastOffset() {
    return (1L << addrBits) - 1;
  }

  public int getLogLength() {
    return addrBits;
  }

  @Override
  public int getValueWidth() {
    return width;
  }

  public int getWidth() {
    return width;
  }

  public boolean isClear() {
    for (final var page : pages) {
      if (page != null) {
        for (var j = page.getLength() - 1; j >= 0; j--) {
          if (page.get(j) != 0)
            return false;
        }
      }
    }
    return true;
  }

  @Override
  public void removeHexModelListener(HexModelListener l) {
    if (listeners == null) return;
    listeners.add(l);
    if (listeners.isEmpty()) listeners = null;
  }

  @Override
  public void set(long addr, long value) {
    final var page = (int) (addr >>> PAGE_SIZE_BITS);
    long offs = (addr & PAGE_MASK);
    if (page < 0 || page >= pages.length) return;
    long old = pages[page] == null ? 0 : pages[page].get(offs) & mask;
    long val = value & mask;
    if (old != val) {
      if (pages[page] == null) {
        pages[page] = MemContentsSub.createPage(PAGE_SIZE, width);
      }
      pages[page].set(offs, val);
      fireBytesChanged(addr, 1, new long[] {old});
    }
  }

  @Override
  public void set(long start, long[] values) {
    if (values.length == 0) return;

    var pageStart = (int) (start >>> PAGE_SIZE_BITS);
    final var startOffs = (int) (start & PAGE_MASK);
    final var pageEnd = (int) ((start + values.length - 1) >>> PAGE_SIZE_BITS);
    final var endOffs = (int) ((start + values.length - 1) & PAGE_MASK);

    if (pageStart == pageEnd) {
      ensurePage(pageStart);
      final var page = pages[pageStart];
      if (!page.matches(values, startOffs, mask)) {
        final var oldValues = page.get(startOffs, values.length);
        page.load(startOffs, values, mask);
        if (page.isClear()) pages[pageStart] = null;
        fireBytesChanged(start, values.length, oldValues);
      }
    } else {
      int nextOffs;
      if (startOffs == 0) {
        pageStart--;
        nextOffs = 0;
      } else {
        ensurePage(pageStart);
        final var vals = new long[PAGE_SIZE - startOffs];
        System.arraycopy(values, 0, vals, 0, vals.length);
        final var page = pages[pageStart];
        if (!page.matches(vals, startOffs, mask)) {
          final var oldValues = page.get(startOffs, vals.length);
          page.load(startOffs, vals, mask);
          if (page.isClear()) pages[pageStart] = null;
          fireBytesChanged(start, PAGE_SIZE - pageStart, oldValues);
        }
        nextOffs = vals.length;
      }
      var vals = new long[PAGE_SIZE];
      int offs = nextOffs;
      for (var i = pageStart + 1; i < pageEnd; i++, offs += PAGE_SIZE) {
        var page = pages[i];
        if (page == null) {
          var allZeroes = true;
          for (var j = 0; j < PAGE_SIZE; j++) {
            if ((values[offs + j] & mask) != 0) {
              allZeroes = false;
              break;
            }
          }
          if (!allZeroes) {
            page = MemContentsSub.createPage(PAGE_SIZE, width);
            pages[i] = page;
          }
        }
        if (page != null) {
          System.arraycopy(values, offs, vals, 0, PAGE_SIZE);
          if (!page.matches(vals, startOffs, mask)) {
            final var oldValues = page.get(0, PAGE_SIZE);
            page.load(0, vals, mask);
            if (page.isClear()) pages[i] = null;
            fireBytesChanged(i << PAGE_SIZE_BITS, PAGE_SIZE, oldValues);
          }
        }
      }
      if (endOffs >= 0) {
        ensurePage(pageEnd);
        vals = new long[endOffs + 1];
        System.arraycopy(values, offs, vals, 0, endOffs + 1);
        final var page = pages[pageEnd];
        if (!page.matches(vals, startOffs, mask)) {
          final var oldValues = page.get(0, endOffs + 1);
          page.load(0, vals, mask);
          if (page.isClear()) pages[pageEnd] = null;
          fireBytesChanged(pageEnd << PAGE_SIZE_BITS, endOffs + 1, oldValues);
        }
      }
    }
  }

  public void copyFrom(long start, MemContents src, long offs, int count) {
    count = (int) Math.min(count, getLastOffset() - start + 1);
    if (count <= 0)
      return;
    if (src.addrBits != addrBits)
      throw new IllegalArgumentException(String.format(
              "memory width mismatch: src is %d bits wide, dest is %d bits wide",
              src.addrBits, addrBits));
    if (offs + count - 1 > src.getLastOffset())
      throw new IllegalArgumentException(
          String.format(
              "memory offset out of range: offset 0x%x count 0x%x exceeds last valid offset 0x%x",
              offs, count, src.getLastOffset()));

    var dp = (int) (start >>> PAGE_SIZE_BITS);
    var di = (int) (start & PAGE_MASK);

    var sp = (int) (offs >>> PAGE_SIZE_BITS);
    var si = (int) (offs & PAGE_MASK);

    do {
      var dstPage = pages[dp];
      final var srcPage = src.pages[sp];
      final var n = Math.min(count, Math.min(PAGE_SIZE - si, PAGE_SIZE - di));
      if (dstPage == null && srcPage == null) {
        // both already all zeros, so do nothing
      } else if (srcPage == null) {
        // clearing locations di..di+n on this page
        fill(dp * PAGE_SIZE + di, n, 0);
      } else {
        if (dstPage == null) dstPage = pages[dp] = MemContentsSub.createPage(PAGE_SIZE, width);
        // copy locations di..di+n on this page
        final var vals = srcPage.get(si, n);
        dstPage.set(di, vals);
      }
      count -= n;
      di += n;
      si += n;
      if (di >= PAGE_SIZE) {
        di = 0;
        dp++;
      }
      if (si >= PAGE_SIZE) {
        si = 0;
        sp++;
      }
    } while (count > 0); // (dp <= dstPageEnd || di <= dstEndOffs)
    fireBytesChanged(0, 1 << addrBits, null); /* update my listeners */
  }

  public void setDimensions(int addrBits, int width) {
    if (addrBits == this.addrBits && width == this.width) return;
    this.addrBits = addrBits;
    this.width = width;
    this.mask = width == 64 ? -1L : ((1L << width) - 1);

    final var oldPages = pages;
    int pageCount;
    int pageLength;
    if (addrBits < PAGE_SIZE_BITS) {
      pageCount = 1;
      pageLength = 1 << addrBits;
    } else {
      pageCount = 1 << (addrBits - PAGE_SIZE_BITS);
      pageLength = PAGE_SIZE;
    }
    pages = new Page[pageCount];
    if (oldPages != null) {
      final var n = Math.min(oldPages.length, pages.length);
      for (var i = 0; i < n; i++) {
        if (oldPages[i] != null) {
          pages[i] = MemContentsSub.createPage(pageLength, width);
          final var m = Math.min(oldPages[i].getLength(), pageLength);
          for (var j = 0; j < m; j++) {
            pages[i].set(j, oldPages[i].get(j));
          }
        }
      }
    }
    if (pageCount == 0 && pages[0] == null) {
      pages[0] = MemContentsSub.createPage(pageLength, width);
    }

    fireMetainfoChanged();
  }

  public void condFillRandom() {
    if (AppPreferences.Memory_Startup_Unknown.get()) {
      final var pageLength = (addrBits < PAGE_SIZE_BITS) ? 1 << addrBits : PAGE_SIZE;
      for (var i = 0; i < pages.length; i++)
        if (pages[i] == null)
          pages[i] = MemContentsSub.createPage(pageLength, width);
    }
  }

  abstract static class Page implements Cloneable {
    @Override
    public Page clone() {
      try {
        return (Page) super.clone();
      } catch (CloneNotSupportedException e) {
        return this;
      }
    }

    abstract long get(long addr);

    long[] get(long start, int len) {
      final var ret = new long[len];
      for (var i = 0; i < ret.length; i++)
        ret[i] = get(start + i);
      return ret;
    }

    void set(long start, long[] val) {
      for (var i = 0; i < val.length; i++)
        set(start + i, val[i]);
    }

    abstract int getLength();

    boolean isClear() {
      for (int i = 0, n = getLength(); i < n; i++) {
        if (get(i) != 0)
          return false;
      }
      return true;
    }

    abstract void load(long start, long[] values, long mask);

    boolean matches(long[] values, long start, long mask) {
      for (var i = 0; i < values.length; i++) {
        if (get(start + i) != (values[i] & mask))
          return false;
      }
      return true;
    }

    abstract void set(long addr, long value);
  }

}
