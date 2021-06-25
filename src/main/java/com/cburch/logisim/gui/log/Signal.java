/*
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

package com.cburch.logisim.gui.log;

import com.cburch.logisim.data.BitWidth;
import com.cburch.logisim.data.Value;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.Transferable;
import java.awt.datatransfer.UnsupportedFlavorException;
import java.util.ArrayList;

public class Signal {

  private static final int CHUNK = 512;

  // Signal position in list, name, etc.
  public int idx;
  public final SignalInfo info;

  // Signal data
  private long timeStart;
  private Value[][] val; // values, in blocks no larger than CHUNK
  private Value last;
  private long[][] dur; // duration of each value
  private int curSize;
  private int maxSize; // limit, or zero for unlimited
  private short firstIndex; // for wrapping, only when limited

  public Signal(
      int idx, SignalInfo info, Value initialValue, long duration, long timeStart, int maxSize) {
    this.idx = idx;
    this.info = info;
    this.timeStart = timeStart;
    this.maxSize = maxSize;
    this.val = new Value[1][maxSize == 0 || maxSize > CHUNK ? CHUNK : maxSize];
    this.dur = new long[1][maxSize == 0 || maxSize > CHUNK ? CHUNK : maxSize];
    this.curSize = 0;
    this.firstIndex = 0;
    extend(initialValue, duration);
  }

  public long omittedDataTime() {
    return curSize == maxSize ? timeStart : 0;
  }

  public long getEndTime() {
    long t = timeStart;
    for (int p = 0; p < curSize; p++) {
      int i = (firstIndex + p) % curSize;
      t += dur[i / CHUNK][i % CHUNK];
    }
    return t;
  }

  public void extend(long duration) {
    if (last == null) {
      timeStart += duration;
    } else {
      int i = (firstIndex + curSize - 1) % curSize;
      dur[i / CHUNK][i % CHUNK] += duration;
    }
  }

  public void extend(Value v, long duration) {
    if (v.getWidth() != info.getWidth())
      System.out.printf(
          "*** notice: value width mismatch for %s: width=%d bits, newVal=%s (%d bits)\n",
          info, info.getWidth(), v, v.getWidth());
    if (last != null && last.equals(v)) {
      int i = (firstIndex + curSize - 1) % curSize;
      dur[i / CHUNK][i % CHUNK] += duration;
      return;
    }
    last = v;
    int c = val.length;
    int cap = CHUNK * (c - 1) + val[c - 1].length;
    if (curSize < cap) {
      // fits in an existing chunk
      val[curSize / CHUNK][curSize % CHUNK] = v;
      dur[curSize / CHUNK][curSize % CHUNK] = duration;
      curSize++;
    } else if (curSize < maxSize || maxSize <= 0) {
      // allocate another chunk
      Value[][] val2 = new Value[c + 1][];
      long[][] dur2 = new long[c + 1][];
      System.arraycopy(val, 0, val2, 0, c);
      System.arraycopy(dur, 0, dur2, 0, c);
      val2[c + 1] = new Value[maxSize == 0 || (maxSize - cap) > CHUNK ? CHUNK : (maxSize - cap)];
      dur2[c + 1] = new long[maxSize == 0 || (maxSize - cap) > CHUNK ? CHUNK : (maxSize - cap)];
      val = val2;
      dur = dur2;
      val[curSize / CHUNK][curSize % CHUNK] = v;
      dur[curSize / CHUNK][curSize % CHUNK] = duration;
      curSize++;
    } else {
      // limited size is filled, wrap around, and adjust start offset
      timeStart += dur[firstIndex / CHUNK][firstIndex % CHUNK];
      val[firstIndex / CHUNK][firstIndex % CHUNK] = v;
      dur[firstIndex / CHUNK][firstIndex % CHUNK] = duration;
      firstIndex++;
      if (firstIndex >= maxSize) firstIndex = 0;
    }
  }

  public void replaceRecent(Value v, long duration) {
    if (last == null || curSize == 0)
      throw new IllegalStateException("signal should have at least " + duration + " ns of data");
    int i = (firstIndex + curSize - 1) % curSize;
    boolean checkMerge = true;
    if (dur[i / CHUNK][i % CHUNK] == duration) {
      val[i / CHUNK][i % CHUNK] = v;
      last = v;
      int j = (i + curSize - 1) % curSize;
      if (curSize > 1 && val[j / CHUNK][j % CHUNK].equals(v)) {
        dur[j / CHUNK][j % CHUNK] += duration;
        curSize--;
        // special case: last chunk is now entirely empty, must be removed
        if (i % CHUNK == 0) {
          int c = val.length - 1;
          Value[][] valueNew = new Value[c][];
          long[][] durNew = new long[c][];
          System.arraycopy(val, 0, valueNew, 0, c);
          System.arraycopy(dur, 0, durNew, 0, c);
          val = valueNew;
          dur = durNew;
        }
      }
    } else if (dur[i / CHUNK][i % CHUNK] > duration) {
      dur[i / CHUNK][i % CHUNK] -= duration;
      extend(v, duration);
    } else if (curSize == 1 && dur[i / CHUNK][i % CHUNK] + timeStart >= duration) {
      timeStart -= (duration - dur[i / CHUNK][i % CHUNK]);
      val[i / CHUNK][i % CHUNK] = v;
      dur[i / CHUNK][i % CHUNK] = duration;
      last = v;
    } else {
      throw new IllegalStateException(
          "signal data should be at least "
              + duration
              + " ns in duration,"
              + " but only "
              + dur[i / CHUNK][i % CHUNK]
              + " in last signal");
    }
  }

  private void retainOnly(int offset, int amt, int cap) {
    // shift all values [from offset to offset+amt] left into new arrays
    // of size appropriate for eventual capacity cap
    int c = (amt + CHUNK - 1) / CHUNK;
    int last = cap == 0 ? CHUNK : Math.min(CHUNK, cap - (c - 1) * CHUNK);
    Value[][] v = new Value[c][];
    long[][] d = new long[c][];
    for (int i = 0; i < c; i++) {
      v[i] = new Value[i < c - 1 ? CHUNK : last];
      d[i] = new long[i < c - 1 ? CHUNK : last];
    }
    for (int p = 0; p < amt; p++) {
      int i = (firstIndex + offset + p) % curSize;
      v[p / CHUNK][p % CHUNK] = val[i / CHUNK][i % CHUNK];
      d[p / CHUNK][p % CHUNK] = dur[i / CHUNK][i % CHUNK];
    }
    val = v;
    dur = d;
    firstIndex = 0;
    curSize = amt;
  }

  public void resize(int newMaxSize) {
    if (newMaxSize == maxSize) return;
    if (newMaxSize == 0 || newMaxSize > maxSize) {
      // growing
      if (firstIndex != 0) retainOnly(0, curSize, newMaxSize); // keeps all data, but shifts it left
    } else {
      // shrinking: newMaxSize < maxSize
      if (curSize <= newMaxSize) {
        // Mostly empty, keep all data, but maybe truncate last chunk if needed
        // to get capacity below new max size.
        // Note: firstIndex must be 0, since otherwize curSize==maxSize,
        // and that would mean curSize > newMaxSize.
        // There are two cases:
        //  very unfull:
        //    cap -----------------------------------------------|
        //    curSize --------------------------|
        //    [ 0+ full large-chunks ] [ partly full large-chunk ] [ not yet allocated... ]
        //  nearly full:
        //    cap ---------------------------------------------|
        //    curSize -------------------------|
        //    [ 0+ full large-chunks ] [ partly full end-chunk ]
        // In the very unfull case, we may be able to do nothing at all
        // (if cap <= newMaxSize), or we may have to shrink that last allocated
        // chunk (if cap > newMaxSize).
        // In the nearly full case, cap > newMaxSize and we need to shrink the
        // last allocated chunk.
        int c = val.length;
        int cap = CHUNK * (c - 1) + val[c - 1].length;
        if (cap > newMaxSize) {
          // Note: # of existing chunks (c) must be equal to # of new chunks
          int last = Math.min(CHUNK, newMaxSize - (c - 1) * CHUNK);
          Value[] v = new Value[last];
          long[] d = new long[last];
          System.arraycopy(val[c - 1], 0, v, 0, last);
          System.arraycopy(dur[c - 1], 0, d, 0, last);
          val[c - 1] = v;
          dur[c - 1] = d;
        }
      } else { // curSize > newMaxSize
        // too much data, keep only most recent data and shift it left
        int discard = (maxSize - newMaxSize);
        for (int p = 0; p < discard; p++) {
          int i = (firstIndex + p) % curSize;
          timeStart += dur[i / CHUNK][i % CHUNK];
        }
        retainOnly(discard, newMaxSize, newMaxSize);
      }
    }
    maxSize = newMaxSize;
  }

  public void reset(Value v, long duration) {
    if (val.length > 1) {
      Value[][] val2 = new Value[1][];
      long[][] dur2 = new long[1][];
      val2[0] = val[0];
      dur2[0] = dur[0];
      val = val2;
      dur = dur2;
    }
    last = null;
    curSize = 0;
    firstIndex = 0;
    extend(v, duration);
  }

  public class Iterator {

    public int position;
    public long time;
    public long duration;
    public Value value;

    public Iterator() {
      position = 0;
      time = timeStart;
      int i = firstIndex;
      int width = info.getWidth();
      value = val[i / CHUNK][i % CHUNK].extendWidth(width, Value.FALSE);
      duration = dur[i / CHUNK][i % CHUNK];
    }

    public Iterator(long t) {
      this();
      if (t > time) advance(t - time);
    }

    public String getFormattedValue() {
      return value == null ? "-" : info.format(value);
    }

    public boolean advance() {
      if (position == curSize - 1) {
        value = null;
        duration = 0;
        return false;
      }
      position++;
      time += duration;
      int i = (firstIndex + position) % curSize;
      int width = info.getWidth();
      value = val[i / CHUNK][i % CHUNK].extendWidth(width, Value.FALSE);
      duration = dur[i / CHUNK][i % CHUNK];
      return true;
    }

    public boolean advance(long timeFwd) {
      if (value == null) return false;
      if (timeFwd <= 0) return true;
      long t = time + timeFwd;
      while (t >= time + duration) {
        if (!advance()) return false;
      }
      // postcondition: t < time + duration
      //                t - time < duration
      duration -= (t - time);
      time = t;
      return true;
    }
  }

  // todo: easily optimized
  public Value getValue(long t) { // always current width, even when width changes
    if (t < timeStart) return null;
    int width = info.getWidth();
    long tt = timeStart;
    for (int p = 0; p < curSize; p++) {
      int i = (firstIndex + p) % curSize;
      long d = dur[i / CHUNK][i % CHUNK];
      if (t < tt + d) return val[i / CHUNK][i % CHUNK].extendWidth(width, Value.FALSE);
      tt += d;
    }
    return null;
  }

  public String getFormattedValue(long t) {
    Value v = getValue(t);
    return v == null ? "-" : info.format(v);
  }

  public String format(Value v) {
    return info.format(v);
  }

  public String getFormattedMaxValue() {
    int width = info.getWidth();
    // todo: signed decimal should maybe use a large positive value?
    return format(Value.createKnown(BitWidth.create(width), -1));
  }

  public String getFormattedMinValue() {
    int width = info.getWidth();
    // todo: signed decimal should maybe use a large negative value?
    return format(Value.createKnown(BitWidth.create(width), 0));
  }

  public String getName() {
    return info.getDisplayName();
  }

  public int getWidth() {
    return info.getWidth();
  }

  // This class is mostly needed because drag-and-drop DataFlavor works easiest
  // with a non-generic non-anonymous class
  public static class List extends ArrayList<Signal> implements Transferable {
    public static final DataFlavor dataFlavor;

    static {
      DataFlavor f = null;
      try {
        f =
            new DataFlavor(
                String.format(
                    "%s;class=\"%s\"",
                    DataFlavor.javaJVMLocalObjectMimeType, List.class.getName()));
      } catch (ClassNotFoundException e) {
        e.printStackTrace();
      }
      dataFlavor = f;
    }

    public static final DataFlavor[] dataFlavors = new DataFlavor[] {dataFlavor};

    @Override
    public Object getTransferData(DataFlavor flavor) throws UnsupportedFlavorException {
      if (!isDataFlavorSupported(flavor)) throw new UnsupportedFlavorException(flavor);
      return this;
    }

    @Override
    public DataFlavor[] getTransferDataFlavors() {
      return dataFlavors;
    }

    @Override
    public boolean isDataFlavorSupported(DataFlavor flavor) {
      return dataFlavor.equals(flavor);
    }
  }
}
