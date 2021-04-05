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

import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.Transferable;
import java.awt.datatransfer.UnsupportedFlavorException;
import java.util.ArrayList;

import com.cburch.logisim.data.BitWidth;
import com.cburch.logisim.data.Value;

public class Signal {


  private static final int CHUNK = 512;

  // Signal position in list, name, etc.
  public int idx;
  public final SignalInfo info;
  private int width;

  // Signal data
  private long tStart;
  private Value[][] val; // values, in blocks no larger than CHUNK
  private Value last;
  private long[][] dur; // duration of each value
  private int curSize;
  private int maxSize; // limit, or zero for unlimited
  private short firstIndex; // for wrapping, only when limited

  public Signal(int idx, SignalInfo info, Value initialValue, long duration, long tStart, int maxSize) {
    this.idx = idx;
    this.info = info;
    this.width = info.getWidth();
    this.tStart = tStart;
    this.maxSize = maxSize;
    this.val = new Value[1][maxSize == 0 || maxSize > CHUNK ? CHUNK : maxSize];
    this.dur = new long[1][maxSize == 0 || maxSize > CHUNK ? CHUNK : maxSize];
    this.curSize = 0;
    this.firstIndex = 0;
    extend(initialValue, duration);
  }

  public void extend(Value v, long duration) {
    if (v.getWidth() != width)
      System.out.println("*** notice: value width mismatch for " + info);
    if (last != null && last.equals(v)) {
      int i = (firstIndex + curSize - 1) % curSize;
      dur[i/CHUNK][i%CHUNK] += duration;
      return;
    }
    last = v;
    int c = val.length;
    int cap = CHUNK*(c-1) + val[c-1].length;
    if (curSize < cap) {
      // fits in an existing chunk
      val[curSize/CHUNK][curSize%CHUNK] = v;
      dur[curSize/CHUNK][curSize%CHUNK] = duration;
      curSize++;
    } else if (curSize < maxSize || maxSize <= 0) {
      // allocate another chunk
      Value[][] val2 = new Value[c+1][];
      long[][] dur2 = new long[c+1][];
      System.arraycopy(val, 0, val2, 0, c);
      System.arraycopy(dur, 0, dur2, 0, c);
      val2[c+1] = new Value[maxSize == 0 || (maxSize-cap) > CHUNK ? CHUNK : (maxSize-cap)];
      dur2[c+1] = new long[maxSize == 0 || (maxSize-cap) > CHUNK ? CHUNK : (maxSize-cap)];
      val = val2;
      dur = dur2;
      val[curSize/CHUNK][curSize%CHUNK] = v;
      dur[curSize/CHUNK][curSize%CHUNK] = duration;
      curSize++;
    } else {
   // limited size is filled, wrap around, and adjust start offset
      tStart += dur[firstIndex/CHUNK][firstIndex%CHUNK];
      val[firstIndex/CHUNK][firstIndex%CHUNK] = v;
      dur[firstIndex/CHUNK][firstIndex%CHUNK] = duration;
      firstIndex++;
      if (firstIndex >= maxSize)
        firstIndex = 0;
    }
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
      time = tStart;
      int i = firstIndex;
      value = val[i/CHUNK][i%CHUNK].extendWidth(width, Value.FALSE);
      duration = dur[i/CHUNK][i%CHUNK];
    }

    public Iterator(long t) {
      this();
      advance(t);
    }

    public String getFormattedValue() {
      return value == null ? "-" : info.format(value);
    }

    public boolean advance() {
      if (position == curSize-1) {
        value = null;
        duration = 0;
        return false;
      }
      position++;
      time += duration;
      int i = (firstIndex + position) % curSize;
      value = val[i/CHUNK][i%CHUNK].extendWidth(width, Value.FALSE);
      duration = dur[i/CHUNK][i%CHUNK];
      return true;
    }

    public boolean advance(long tFwd) {
      if (value == null)
        return false;
      if (tFwd <= 0)
        return true;
      long t = time + tFwd;
      while (t >= time + duration) {
        if (!advance())
          return false;
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
    if (t < tStart)
      return null;
    long tt = tStart;
    for (int p = 0; p < curSize; p++) {
      int i = (firstIndex + p) % curSize;
      long d = dur[i/CHUNK][i%CHUNK];
      if (t < tt + d)
        return val[i/CHUNK][i%CHUNK].extendWidth(width, Value.FALSE);
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
    // todo: signed decimal should maybe use a large positive value?
    return format(Value.createKnown(BitWidth.create(width), -1));
  }

  public String getFormattedMinValue() {
    // todo: signed decimal should maybe use a large negative value?
    return format(Value.createKnown(BitWidth.create(width), 0));
  }

  public String getName() {
    return info.getDisplayName();
  }

  public int getWidth() {
    return width;
  }

  private boolean expanded; // todo: this doesn't belong here
  public boolean isExpanded() { return expanded; }
  public void toggleExpanded() { expanded = !expanded; }

  // This class is mostly needed because drag-and-drop DataFlavor works easiest
  // with a regular class (not an inner or generic class).
  public static class Collection extends ArrayList<Signal> implements Transferable {
    public static final DataFlavor dataFlavor;
    static {
      DataFlavor f = null;
      try {
        f = new DataFlavor(
            String.format("%s;class=\"%s\"",
              DataFlavor.javaJVMLocalObjectMimeType,
              Collection.class.getName()));
      } catch (ClassNotFoundException e) {
        e.printStackTrace();
      }
      dataFlavor = f;
    }
    public static final DataFlavor[] dataFlavors = new DataFlavor[] { dataFlavor };

    @Override
    public Object getTransferData(DataFlavor flavor) throws UnsupportedFlavorException {
      if(!isDataFlavorSupported(flavor))
        throw new UnsupportedFlavorException(flavor);
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
