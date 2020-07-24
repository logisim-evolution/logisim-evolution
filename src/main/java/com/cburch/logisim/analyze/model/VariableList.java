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

package com.cburch.logisim.analyze.model;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.NoSuchElementException;

public class VariableList {
  private ArrayList<VariableListListener> listeners = new ArrayList<VariableListListener>();
  private int maxSize;
  private ArrayList<Var> data;
  private ArrayList<String> names;
  public final List<Var> vars;
  public final List<String> bits;
  private ArrayList<VariableList> others;

  public VariableList(int maxSize) {
    this.maxSize = maxSize;
    data = maxSize > 16 ? new ArrayList<Var>() : new ArrayList<Var>(maxSize);
    names = maxSize > 16 ? new ArrayList<String>() : new ArrayList<String>(maxSize);
    vars = Collections.unmodifiableList(data);
    bits = Collections.unmodifiableList(names);
    others = new ArrayList<VariableList>();
  }
  
  public void addCompanion(VariableList var) {
	  others.add(var);
  }
  
  public ArrayList<String> getNames() { return names; }
  
  public boolean containsDuplicate(VariableList data, Var oldVar, String name) {
	  boolean found = false;
      for (int i = 0, n = vars.size(); i < n && !found; i++) {
          Var other = vars.get(i);
          if (other != oldVar && name.equals(other.name))
        	  found = true;
      }
      for (int i = 0 ; i < others.size() && !found ; i++) {
        VariableList l = others.get(i);
        if (l.equals(data)) 
          continue;
    	found |= l.containsDuplicate(data, oldVar, name);
      }
	  return found;
  }

  public void add(Var V) {
    if (data.size() + V.width >= maxSize) {
      throw new IllegalArgumentException("maximum size is " + maxSize);
    }
    int index = data.size();
    data.add(V);
    for (String bit : V) names.add(bit);
    int bitIndex = names.size() - 1;
    fireEvent(VariableListEvent.ADD, V, index, bitIndex);
  }

  public void addVariableListListener(VariableListListener l) {
    listeners.add(l);
  }

  private void fireEvent(int type) {
    fireEvent(type, null, null, null);
  }

  private void fireEvent(int type, Var variable, Integer Index, Integer bitIndex) {
    if (listeners.size() == 0) return;
    VariableListEvent event = new VariableListEvent(this, type, variable, Index, bitIndex);
    for (VariableListListener l : listeners) {
      l.listChanged(event);
    }
  }

  public int getMaximumSize() {
    return maxSize;
  }

  public void move(Var var, int delta) {
    int index = data.indexOf(var);
    if (index < 0) throw new NoSuchElementException(var.toString());
    int bitIndex = names.indexOf(var.bitName(0));
    if (bitIndex < 0) throw new NoSuchElementException(var.toString());
    int newIndex = index + delta;
    if (newIndex < 0) {
      throw new IllegalArgumentException("cannot move index " + index + " by " + delta);
    }
    if (newIndex > data.size() - 1) {
      throw new IllegalArgumentException(
          "cannot move index " + index + " by " + delta + ": size " + data.size());
    }
    if (index == newIndex) return;
    data.remove(index);
    data.add(newIndex, var);
    names.subList(bitIndex + 1 - var.width, bitIndex + 1).clear();
    int i = (newIndex == 0 ? 0 : (1 + names.indexOf(data.get(newIndex - 1).bitName(0))));
    for (String bit : var) names.add(i++, bit);
    int bitDelta = names.indexOf(var.bitName(0)) - bitIndex;
    fireEvent(VariableListEvent.MOVE, var, delta, bitDelta);
  }

  public void remove(Var var) {
    int index = data.indexOf(var);
    if (index < 0) throw new NoSuchElementException(var.toString());
    int bitIndex = names.indexOf(var.bitName(0));
    if (bitIndex < 0) throw new NoSuchElementException(var.toString());
    data.remove(index);
    names.subList(bitIndex + 1 - var.width, bitIndex + 1).clear();
    fireEvent(VariableListEvent.REMOVE, var, index, bitIndex);
  }

  public void removeVariableListListener(VariableListListener l) {
    listeners.remove(l);
  }

  public void replace(Var oldVar, Var newVar) {
    int index = data.indexOf(oldVar);
    if (index < 0) throw new NoSuchElementException(oldVar.toString());
    int bitIndex = names.indexOf(oldVar.bitName(0));
    if (bitIndex < 0) throw new NoSuchElementException(oldVar.toString());
    if (oldVar.equals(newVar)) return;
    data.set(index, newVar);
    names.subList(bitIndex + 1 - oldVar.width, bitIndex + 1).clear();
    int i = bitIndex + 1 - oldVar.width;
    for (String bit : newVar) names.add(i++, bit);
    fireEvent(VariableListEvent.REPLACE, oldVar, index, bitIndex);
  }

  public void setAll(List<Var> values) {
    int total = 0;
    for (Var v : values) total += v.width;
    if (total > maxSize) throw new IllegalArgumentException("maximum size is " + maxSize);
    data.clear();
    data.addAll(values);
    names.clear();
    for (Var v : values) {
      for (String bit : v) names.add(bit);
    }
    fireEvent(VariableListEvent.ALL_REPLACED);
  }
}
