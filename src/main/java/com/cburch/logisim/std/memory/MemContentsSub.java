/*
 * Logisim-evolution - digital logic design tool and simulator
 * Copyright by the Logisim-evolution developers
 *
 * https://github.com/logisim-evolution/
 *
 * This is free software released under GNU GPLv3 license
 */

package com.cburch.logisim.std.memory;

import com.cburch.logisim.prefs.AppPreferences;

class MemContentsSub {
  private static class BytePage extends MemContents.Page {
    private byte[] data;
    private final long mask;

    public BytePage(int size, long mask) {
      this.mask = mask;
      data = new byte[size];
      if (AppPreferences.Memory_Startup_Unknown.get()) {
        final var generator = new java.util.Random();
        for (var i = 0; i < size; i++) {
          data[i] = (byte) (generator.nextInt(256) & mask);
        }
      }
    }

    @Override
    public BytePage clone() {
      final var ret = (BytePage) super.clone();
      ret.data = new byte[this.data.length];
      System.arraycopy(this.data, 0, ret.data, 0, this.data.length);
      return ret;
    }

    @Override
    long get(long addr) {
      return addr >= 0 && addr < data.length ? data[(int) addr] : 0;
    }

    //
    // methods for accessing data within memory
    //
    @Override
    int getLength() {
      return data.length;
    }

    @Override
    void load(long start, long[] values, long mask) {
      final var n = Math.min(values.length, data.length - (int) start);
      for (var i = 0; i < n; i++) {
        data[(int) start + i] = (byte) (values[i] & mask);
      }
    }

    @Override
    void set(long addr, long value) {
      if (addr >= 0 && addr < data.length) {
        final var oldValue = data[(int) addr];
        if (value != oldValue) {
          data[(int) addr] = (byte) value;
        }
      }
    }
  }

  private static class IntPage extends MemContents.Page {
    private int[] data;
    private final long mask;

    public IntPage(int size, long mask) {
      this.mask = mask;
      data = new int[size];
      if (AppPreferences.Memory_Startup_Unknown.get()) {
        final var generator = new java.util.Random();
        for (var i = 0; i < size; i++) data[i] = (int) (generator.nextInt() & mask);
      }
    }

    @Override
    public IntPage clone() {
      final var ret = (IntPage) super.clone();
      ret.data = new int[this.data.length];
      System.arraycopy(this.data, 0, ret.data, 0, this.data.length);
      return ret;
    }

    @Override
    long get(long addr) {
      return addr >= 0 && addr < data.length ? data[(int) addr] : 0;
    }

    //
    // methods for accessing data within memory
    //
    @Override
    int getLength() {
      return data.length;
    }

    @Override
    void load(long start, long[] values, long mask) {
      final var n = Math.min(values.length, data.length - (int) start);
      for (var i = 0; i < n; i++) {
        data[(int) start + i] = (int) (values[i] & mask);
      }
    }

    @Override
    void set(long addr, long value) {
      if (addr >= 0 && addr < data.length) {
        int oldValue = data[(int) addr];
        if (value != oldValue) {
          data[(int) addr] = (int) value;
        }
      }
    }
  }

  private static class ShortPage extends MemContents.Page {
    private short[] data;
    private final long mask;

    public ShortPage(int size, long mask) {
      data = new short[size];
      this.mask = mask;
      if (AppPreferences.Memory_Startup_Unknown.get()) {
        final var generator = new java.util.Random();
        for (var i = 0; i < size; i++) data[i] = (short) (generator.nextInt(1 << 16) & mask);
      }
    }

    @Override
    public ShortPage clone() {
      final var ret = (ShortPage) super.clone();
      ret.data = new short[this.data.length];
      System.arraycopy(this.data, 0, ret.data, 0, this.data.length);
      return ret;
    }

    @Override
    long get(long addr) {
      return addr >= 0 && addr < data.length ? data[(int) addr] : 0;
    }

    //
    // methods for accessing data within memory
    //
    @Override
    int getLength() {
      return data.length;
    }

    @Override
    void load(long start, long[] values, long mask) {
      final var n = Math.min(values.length, data.length - (int) start);
      /*
       * Bugfix in memory writing (by Roy77)
       * https://github.com/roy77
       */
      for (var i = (int) start; i < n; i++) {
        data[(int) start + i] = (short) (values[i] & mask);
      }
    }

    @Override
    void set(long addr, long value) {
      if (addr >= 0 && addr < data.length) {
        final var oldValue = data[(int) addr];
        if (value != oldValue) {
          data[(int) addr] = (short) value;
        }
      }
    }
  }

  private static class LongPage extends MemContents.Page {
    private long[] data;
    private final long mask;

    public LongPage(int size, long mask) {
      this.mask = mask;
      data = new long[size];
      if (AppPreferences.Memory_Startup_Unknown.get()) {
        final var generator = new java.util.Random();
        for (var i = 0; i < size; i++) data[i] = (int) generator.nextLong() & mask;
      }
    }

    @Override
    public LongPage clone() {
      final var ret = (LongPage) super.clone();
      ret.data = new long[this.data.length];
      System.arraycopy(this.data, 0, ret.data, 0, this.data.length);
      return ret;
    }

    @Override
    long get(long addr) {
      return addr >= 0 && addr < data.length ? data[(int) addr] : 0;
    }

    //
    // methods for accessing data within memory
    //
    @Override
    int getLength() {
      return data.length;
    }

    @Override
    void load(long start, long[] values, long mask) {
      final var n = Math.min(values.length, data.length - (int) start);
      for (var i = 0; i < n; i++) {
        data[(int) start + i] = (values[i] & mask);
      }
    }

    @Override
    void set(long addr, long value) {
      if (addr >= 0 && addr < data.length) {
        final var oldValue = data[(int) addr];
        if (value != oldValue) {
          data[(int) addr] = value;
        }
      }
    }
  }

  static MemContents.Page createPage(int size, int bits) {
    long mask = (bits == 64) ? 0xffffffffffffffffL : (1L << bits) - 1;
    if (bits <= 8) return new BytePage(size, mask);
    else if (bits <= 16) return new ShortPage(size, mask);
    else if (bits <= 32) return new IntPage(size, mask);
    else return new LongPage(size, mask);
  }

  private MemContentsSub() {}
}
