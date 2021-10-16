/*
 * Logisim-evolution - digital logic design tool and simulator
 * Copyright by the Logisim-evolution developers
 *
 * https://github.com/logisim-evolution/
 *
 * This is free software released under GNU GPLv3 license
 */

package com.cburch.hex;

import java.util.ArrayList;
import java.util.Arrays;
import javax.swing.JFrame;
import javax.swing.JScrollPane;

public class Test {
  public static void main(String[] args) {
    final var frame = new JFrame();
    final var model = new Model();
    final var editor = new HexEditor(model);
    frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
    frame.getContentPane().add(new JScrollPane(editor));
    frame.pack();
    frame.setVisible(true);
  }

  private static class Model implements HexModel {
    private final ArrayList<HexModelListener> listeners = new ArrayList<>();
    private final long[] data = new long[924];

    @Override
    public void addHexModelListener(HexModelListener l) {
      listeners.add(l);
    }

    @Override
    public void fill(long start, long len, long value) {
      final var oldValues = new long[(int) len];
      System.arraycopy(data, (int) (start - 11111), oldValues, 0, (int) len);
      Arrays.fill(data, (int) (start - 11111), (int) len, value);
      for (final var listener : listeners) {
        listener.bytesChanged(this, start, len, oldValues);
      }
    }

    @Override
    public long get(long address) {
      return data[(int) (address - 11111)];
    }

    @Override
    public long getFirstOffset() {
      return 11111;
    }

    @Override
    public long getLastOffset() {
      return data.length + 11110;
    }

    @Override
    public int getValueWidth() {
      return 9;
    }

    @Override
    public void removeHexModelListener(HexModelListener l) {
      listeners.remove(l);
    }

    @Override
    public void set(long address, long value) {
      final var oldValues = new long[] {data[(int) (address - 11111)]};
      data[(int) (address - 11111)] = value & 0x1FF;
      for (final var listener : listeners) {
        listener.bytesChanged(this, address, 1, oldValues);
      }
    }

    @Override
    public void set(long start, long[] values) {
      final var oldValues = new long[values.length];
      System.arraycopy(data, (int) (start - 11111), oldValues, 0, values.length);
      System.arraycopy(values, 0, data, (int) (start - 11111), values.length);
      for (final var listener : listeners) {
        listener.bytesChanged(this, start, values.length, oldValues);
      }
    }
  }
}
