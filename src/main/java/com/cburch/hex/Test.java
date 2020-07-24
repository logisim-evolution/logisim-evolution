/**
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

package com.cburch.hex;

import java.util.ArrayList;
import java.util.Arrays;
import javax.swing.JFrame;
import javax.swing.JScrollPane;

public class Test {
  private static class Model implements HexModel {
    private ArrayList<HexModelListener> listeners = new ArrayList<HexModelListener>();
    private long[] data = new long[924];

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

  public static void main(String[] args) {
    JFrame frame = new JFrame();
    HexModel model = new Model();
    HexEditor editor = new HexEditor(model);
    frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
    frame.getContentPane().add(new JScrollPane(editor));
    frame.pack();
    frame.setVisible(true);
  }
}
