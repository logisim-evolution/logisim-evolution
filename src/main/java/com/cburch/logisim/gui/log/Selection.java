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

import com.cburch.logisim.circuit.CircuitState;
import com.cburch.logisim.data.Location;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class Selection {

  private final CircuitState root;
  private final Model model;
  private final ArrayList<SelectionItem> signals = new ArrayList<>();

  public Selection(CircuitState root, Model model) {
    this.root = root;
    this.model = model;
  }

  public void sort() {
    Location.sortHorizontal(signals);
    model.fireSelectionChanged(new Model.Event());
  }

  public void add(SelectionItem item) {
    if (!signals.contains(item)) {
      signals.add(item);
      model.fireSelectionChanged(new Model.Event());
    }
  }

  public void addModelListener(Model.Listener l) {
    model.addModelListener(l);
  }

  public boolean contains(SelectionItem item) {
    return signals.contains(item);
  }

  public SelectionItem get(int index) {
    return signals.get(index);
  }

  public CircuitState getCircuitState() {
    return root;
  }

  public int indexOf(SelectionItem value) {
    return signals.indexOf(value);
  }

  public void addOrMove(List<SelectionItem> items, int idx) {
    int changed = items.size();
    for (SelectionItem item : items) {
      int i = signals.indexOf(item);
      if (i < 0)
        signals.add(idx++, item); // put new item at idx
      else if (i > idx)
        signals.add(idx++, signals.remove(i)); // move later item up
      else if (i < idx)
        signals.add(idx-1, signals.remove(i)); // move earlier item down
      else
        changed--;
    }
    if (changed > 0)
      model.fireSelectionChanged(new Model.Event());
  }

  public int remove(List<SelectionItem> items) {
    int count = 0;
    for (SelectionItem item : items) {
      if (signals.remove(item))
        count++;
    }
    if (count > 0)
      model.fireSelectionChanged(new Model.Event());
    return count;
  } 
  
  public void move(int[] fromIndex, int toIndex) {
    int n = fromIndex.length;
    if (n == 0)
      return;
    Arrays.sort(fromIndex);
    int a = fromIndex[0];
    int b = fromIndex[n-1];
    if (a < 0 || b > signals.size())
      return; // invalid selection
    if (a <= toIndex && toIndex <= b && b-a+1 == n)
      return; // no-op
    ArrayList<SelectionItem> items = new ArrayList<>();
    for (int i = n-1; i >= 0; i--) {
      if (fromIndex[i] < toIndex)
        toIndex--;
      items.add(signals.remove(fromIndex[i]));
    }
    for (int i = n-1; i >= 0; i--)
      signals.add(toIndex++, items.get(i));
    model.fireSelectionChanged(new Model.Event());
  }

  public void remove(int index) {
    if (signals.remove(index) != null) model.fireSelectionChanged(new Model.Event());
  }

  public void remove(SelectionItem item) {
    if (signals.remove(item))
      model.fireSelectionChanged(new Model.Event());
  }


  public void removeModelListener(Model.Listener l) {
    model.removeModelListener(l);
  }

  public int size() {
    return signals.size();
  }
}
