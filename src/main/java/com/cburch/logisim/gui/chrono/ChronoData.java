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

package com.cburch.logisim.gui.chrono;

import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.Transferable;
import java.awt.datatransfer.UnsupportedFlavorException;
import java.util.ArrayList;

import com.cburch.logisim.circuit.CircuitState;
import com.cburch.logisim.data.BitWidth;
import com.cburch.logisim.data.Value;
import com.cburch.logisim.gui.log.Selection;
import com.cburch.logisim.gui.log.SelectionItem;

/** Contains all data to be plotted */
public class ChronoData {

  public static class Signals extends ArrayList<Signal> implements Transferable {
    private static final long serialVersionUID = 1L;

    public static final DataFlavor dataFlavor;
    static {
      DataFlavor f = null;
      try {
        f = new DataFlavor( String.format("%s;class=\"%s\"", DataFlavor.javaJVMLocalObjectMimeType, Signals.class.getName()));
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
  
  public static class Signal {

    public int idx;
    public final SelectionItem info;
    private final ArrayList<Value> vals;
    private int offset;
    private int width;

    private Signal(int idx, SelectionItem info, int offset, Value initialValue) {
      this.idx = idx;
      this.info = info;
      this.offset = offset;
      this.width = info.getWidth();
      this.vals = new ArrayList<>();
      extend(initialValue);
    }

    private void extend(Value v) {
      vals.add(v);
    }

    private void reset(Value v) {
      offset = 0;
      vals.clear();
      extend(v);
    }

    public Value getValue(int t) {
      int idx = t - offset;
      return (idx < 0 || idx >= vals.size()) ? null : vals.get(idx).extendWidth(width, Value.FALSE);
    }

    public String getFormattedValue(int t) {
      int idx = t - offset;
      return (idx < 0 || idx >= vals.size()) ? "-" : info.format(vals.get(idx));
    }

    public String format(Value v) {
      return info.format(v);
    }

    public String getFormattedMaxValue() {
      // well, signed decimal should maybe use a large positive value?
      return format(Value.createKnown(BitWidth.create(width), -1));
    }

    public String getFormattedMinValue() {
      // well, signed decimal should maybe use a large negative value?
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
  }
  
  private ArrayList<Signal> signals = new ArrayList<>();
  private int count = 1;

  public ChronoData() {}
  
  public int getSignalCount() {
    return signals.size();
  }
  
  public int getValueCount() {
    return count;
  }
  
  public Signal getSignal(int idx) {
    return signals.get(idx);
  }

  public void clear() {
    spotlight = null;
    count = 1;
    signals.clear();
  }


  public void setSignals(Selection sel, CircuitState circuitState) {
    int n = sel.size();
    for (int i = 0; i < n; i++) {
      SelectionItem id = sel.get(i);
      Value value = id.fetchValue(circuitState);
      addSignal(id, value, i);
    }
    if (spotlight != null && signals.indexOf(spotlight)  >= n)
      spotlight = null;
    if (signals.size() > n)
      signals.subList(n, signals.size()).clear();
  }

  private void addSignal(SelectionItem info, Value initialValue, int idx) {  
    for (Signal s : signals) {
      if (s.info.equals(info)) {
        if (s.idx == idx)
          return;
        signals.remove(s.idx);
        signals.add(idx, s);
        int a = Math.min(idx, s.idx);
        int b = Math.max(idx, s.idx);
        for (int i = a; i <= b; i++)
          signals.get(i).idx = i;
        return;
      }
    }
    Signal s = new Signal(idx, info, count - 1, initialValue);
    signals.add(idx, s);
  }

  public void addSignalValues(Value[] vals) {
    for (int i = 0; i < signals.size() && i < vals.length; i++)
      signals.get(i).extend(vals[i]);
    count++;
  }
  
  public void resetSignalValues(Value[] vals) {
    count = 0;
    for (int i = 0; i < signals.size() && i < vals.length; i++)
      signals.get(i).reset(vals[i]);
    count++;
  }
  
  private Signal spotlight;
  
  public Signal getSpotlight() {
    return spotlight;
  }
  
  public Signal setSpotlight(Signal s) {
    Signal old = spotlight;
    spotlight = s;
    return old;
  }
}
