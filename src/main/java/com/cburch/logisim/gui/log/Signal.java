/*
 * Logisim-evolution - digital logic design tool and simulator
 * Copyright by the Logisim-evolution developers
 *
 * https://github.com/logisim-evolution/
 *
 * This is free software released under GNU GPLv3 license
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
    var t = timeStart;
    for (var p = 0; p < curSize; p++) {
      var i = (firstIndex + p) % curSize;
      t += dur[i / CHUNK][i % CHUNK];
    }
    return t;
  }

  public void extend(long duration) {
    if (last == null) {
      timeStart += duration;
    } else {
      final var i = (firstIndex + curSize - 1) % curSize;
      dur[i / CHUNK][i % CHUNK] += duration;
    }
  }

  public void extend(Value v, long duration) {
    if (v.getWidth() != info.getWidth())
      System.out.printf(
          "*** notice: value width mismatch for %s: width=%d bits, newVal=%s (%d bits)\n",
          info, info.getWidth(), v, v.getWidth());
    if (last != null && last.equals(v)) {
      final var i = (firstIndex + curSize - 1) % curSize;
      dur[i / CHUNK][i % CHUNK] += duration;
      return;
    }
    last = v;
    final var c = val.length;
    final var cap = CHUNK * (c - 1) + val[c - 1].length;
    if (curSize < cap) {
      // fits in an existing chunk
      val[curSize / CHUNK][curSize % CHUNK] = v;
      dur[curSize / CHUNK][curSize % CHUNK] = duration;
      curSize++;
    } else if (curSize < maxSize || maxSize <= 0) {
      // allocate another chunk
      final var val2 = new Value[c + 1][];
      final var dur2 = new long[c + 1][];
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
    final var i = (firstIndex + curSize - 1) % curSize;
    if (dur[i / CHUNK][i % CHUNK] == duration) {
      val[i / CHUNK][i % CHUNK] = v;
      last = v;
      final var j = (i + curSize - 1) % curSize;
      if (curSize > 1 && val[j / CHUNK][j % CHUNK].equals(v)) {
        dur[j / CHUNK][j % CHUNK] += duration;
        curSize--;
        // special case: last chunk is now entirely empty, must be removed
        if (i % CHUNK == 0) {
          int c = val.length - 1;
          final var valueNew = new Value[c][];
          final var durNew = new long[c][];
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
    final var c = (amt + CHUNK - 1) / CHUNK;
    final var last = cap == 0 ? CHUNK : Math.min(CHUNK, cap - (c - 1) * CHUNK);
    final var v = new Value[c][];
    final var d = new long[c][];
    for (var i = 0; i < c; i++) {
      v[i] = new Value[i < c - 1 ? CHUNK : last];
      d[i] = new long[i < c - 1 ? CHUNK : last];
    }
    for (var p = 0; p < amt; p++) {
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
        final var c = val.length;
        final var cap = CHUNK * (c - 1) + val[c - 1].length;
        if (cap > newMaxSize) {
          // Note: # of existing chunks (c) must be equal to # of new chunks
          final var last = Math.min(CHUNK, newMaxSize - (c - 1) * CHUNK);
          final var v = new Value[last];
          final var d = new long[last];
          System.arraycopy(val[c - 1], 0, v, 0, last);
          System.arraycopy(dur[c - 1], 0, d, 0, last);
          val[c - 1] = v;
          dur[c - 1] = d;
        }
      } else { // curSize > newMaxSize
        // too much data, keep only most recent data and shift it left
        final var discard = (maxSize - newMaxSize);
        for (var p = 0; p < discard; p++) {
          final var i = (firstIndex + p) % curSize;
          timeStart += dur[i / CHUNK][i % CHUNK];
        }
        retainOnly(discard, newMaxSize, newMaxSize);
      }
    }
    maxSize = newMaxSize;
  }

  public void reset(Value v, long duration) {
    if (val.length > 1) {
      final var val2 = new Value[1][];
      final var dur2 = new long[1][];
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
      final var i = firstIndex;
      final var width = info.getWidth();
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
      final var i = (firstIndex + position) % curSize;
      final var width = info.getWidth();
      value = val[i / CHUNK][i % CHUNK].extendWidth(width, Value.FALSE);
      duration = dur[i / CHUNK][i % CHUNK];
      return true;
    }

    public boolean advance(long timeFwd) {
      if (value == null) return false;
      if (timeFwd <= 0) return true;
      final var t = time + timeFwd;
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

  // TODO: easily optimized
  public Value getValue(long t) { // always current width, even when width changes
    if (t < timeStart) return null;
    final var width = info.getWidth();
    var tt = timeStart;
    for (int p = 0; p < curSize; p++) {
      final var i = (firstIndex + p) % curSize;
      final var d = dur[i / CHUNK][i % CHUNK];
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
    final var width = info.getWidth();
    // TODO: signed decimal should maybe use a large positive value?
    return format(Value.createKnown(BitWidth.create(width), -1));
  }

  public String getFormattedMinValue() {
    final var width = info.getWidth();
    // TODO: signed decimal should maybe use a large negative value?
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
