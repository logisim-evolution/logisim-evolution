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
    JFrame frame = new JFrame();
    HexModel model = new Model();
    HexEditor editor = new HexEditor(model);
    frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
    frame.getContentPane().add(new JScrollPane(editor));
    frame.pack();
    frame.setVisible(true);
  }

  private static class Model implements HexModel {
    private final ArrayList<HexModelListener> listeners = new ArrayList<>();
    private final long[] data = new long[924];

    public void addHexModelListener(HexModelListener l) {
      listeners.add(l);
    }

    public void fill(long start, long len, long value) {
      long[] oldValues = new long[(int) len];
      System.arraycopy(data, (int) (start - 11111), oldValues, 0, (int) len);
      Arrays.fill(data, (int) (start - 11111), (int) len, value);
      for (HexModelListener l : listeners) {
        l.bytesChanged(this, start, len, oldValues);
      }
    }

    public long get(long address) {
      return data[(int) (address - 11111)];
    }

    public long getFirstOffset() {
      return 11111;
    }

    public long getLastOffset() {
      return data.length + 11110;
    }

    public int getValueWidth() {
      return 9;
    }

    public void removeHexModelListener(HexModelListener l) {
      listeners.remove(l);
    }

    public void set(long address, long value) {
      long[] oldValues = new long[] {data[(int) (address - 11111)]};
      data[(int) (address - 11111)] = value & 0x1FF;
      for (HexModelListener l : listeners) {
        l.bytesChanged(this, address, 1, oldValues);
      }
    }

    public void set(long start, long[] values) {
      long[] oldValues = new long[values.length];
      System.arraycopy(data, (int) (start - 11111), oldValues, 0, values.length);
      System.arraycopy(values, 0, data, (int) (start - 11111), values.length);
      for (HexModelListener l : listeners) {
        l.bytesChanged(this, start, values.length, oldValues);
      }
    }
  }
}
