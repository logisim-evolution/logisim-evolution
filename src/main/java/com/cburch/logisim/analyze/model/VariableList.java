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

package com.cburch.logisim.analyze.model;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.NoSuchElementException;

public class VariableList {
  private final ArrayList<VariableListListener> listeners = new ArrayList<>();
  private final int maxSize;
  private final ArrayList<Var> data;
  private final ArrayList<String> names;
  public final List<Var> vars;
  public final List<String> bits;
  private final ArrayList<VariableList> others;

  public VariableList(int maxSize) {
    this.maxSize = maxSize;
    data = maxSize > 16 ? new ArrayList<>() : new ArrayList<>(maxSize);
    names = maxSize > 16 ? new ArrayList<>() : new ArrayList<>(maxSize);
    vars = Collections.unmodifiableList(data);
    bits = Collections.unmodifiableList(names);
    others = new ArrayList<>();
  }

  public void addCompanion(VariableList var) {
    others.add(var);
  }

  public ArrayList<String> getNames() {
    return names;
  }

  public boolean containsDuplicate(VariableList data, Var oldVar, String name) {
    var found = false;
    for (int i = 0, n = vars.size(); i < n && !found; i++) {
      final var other = vars.get(i);
      if (other != oldVar && name.equals(other.name)) {
        found = true;
        break;
      }
    }
    for (int i = 0; i < others.size() && !found; i++) {
      final var l = others.get(i);
      if (l.equals(data)) continue;
      found |= l.containsDuplicate(data, oldVar, name);
    }
    return found;
  }

  public void add(Var variable) {
    if (data.size() + variable.width >= maxSize) {
      throw new IllegalArgumentException("maximum size is " + maxSize);
    }
    final var index = data.size();
    data.add(variable);
    for (final var bit : variable) names.add(bit);
    final var bitIndex = names.size() - 1;
    fireEvent(VariableListEvent.ADD, variable, index, bitIndex);
  }

  public void addVariableListListener(VariableListListener l) {
    listeners.add(l);
  }

  private void fireEvent(int type) {
    fireEvent(type, null, null, null);
  }

  private void fireEvent(int type, Var variable, Integer index, Integer bitIndex) {
    if (listeners.size() == 0) return;
    final var event = new VariableListEvent(this, type, variable, index, bitIndex);
    for (VariableListListener l : listeners) {
      l.listChanged(event);
    }
  }

  public int getMaximumSize() {
    return maxSize;
  }

  public void move(Var var, int delta) {
    final var index = data.indexOf(var);
    if (index < 0) throw new NoSuchElementException(var.toString());
    final var bitIndex = names.indexOf(var.bitName(0));
    if (bitIndex < 0) throw new NoSuchElementException(var.toString());
    final var newIndex = index + delta;
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
    var i = (newIndex == 0 ? 0 : (1 + names.indexOf(data.get(newIndex - 1).bitName(0))));
    for (final var bit : var) names.add(i++, bit);
    final var bitDelta = names.indexOf(var.bitName(0)) - bitIndex;
    fireEvent(VariableListEvent.MOVE, var, delta, bitDelta);
  }

  public void remove(Var var) {
    final var index = data.indexOf(var);
    if (index < 0) throw new NoSuchElementException(var.toString());
    final var bitIndex = names.indexOf(var.bitName(0));
    if (bitIndex < 0) throw new NoSuchElementException(var.toString());
    data.remove(index);
    names.subList(bitIndex + 1 - var.width, bitIndex + 1).clear();
    fireEvent(VariableListEvent.REMOVE, var, index, bitIndex);
  }

  public void removeVariableListListener(VariableListListener l) {
    listeners.remove(l);
  }

  public void replace(Var oldVar, Var newVar) {
    final var index = data.indexOf(oldVar);
    if (index < 0) throw new NoSuchElementException(oldVar.toString());
    final var bitIndex = names.indexOf(oldVar.bitName(0));
    if (bitIndex < 0) throw new NoSuchElementException(oldVar.toString());
    if (oldVar.equals(newVar)) return;
    data.set(index, newVar);
    names.subList(bitIndex + 1 - oldVar.width, bitIndex + 1).clear();
    var i = bitIndex + 1 - oldVar.width;
    for (final var bit : newVar) {
      names.add(i++, bit);
    }
    fireEvent(VariableListEvent.REPLACE, oldVar, index, bitIndex);
  }

  public void setAll(List<Var> values) {
    var total = 0;
    for (Var v : values) total += v.width;
    if (total > maxSize) throw new IllegalArgumentException("maximum size is " + maxSize);
    data.clear();
    data.addAll(values);
    names.clear();
    for (final var var : values) {
      for (final var bit : var) names.add(bit);
    }
    fireEvent(VariableListEvent.ALL_REPLACED);
  }
}
