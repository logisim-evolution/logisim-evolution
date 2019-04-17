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

package com.cburch.logisim.std.hdl;

import com.cburch.hdl.HdlModel;
import com.cburch.hdl.HdlModelListener;
import com.cburch.logisim.util.EventSourceWeakSupport;
import java.util.Arrays;

public abstract class HdlContent implements HdlModel, Cloneable {

  protected static <T> T[] concat(T[] first, T[] second) {
    T[] result = Arrays.copyOf(first, first.length + second.length);
    System.arraycopy(second, 0, result, first.length, second.length);
    return result;
  }

  protected EventSourceWeakSupport<HdlModelListener> listeners;

  protected HdlContent() {
    this.listeners = null;
  }

  @Override
  public void addHdlModelListener(HdlModelListener l) {
    if (listeners == null) {
      listeners = new EventSourceWeakSupport<HdlModelListener>();
    }
    listeners.add(l);
  }

  @Override
  public HdlContent clone() throws CloneNotSupportedException {
    HdlContent ret = (HdlContent) super.clone();
    ret.listeners = null;
    return ret;
  }

  @Override
  public abstract boolean compare(HdlModel model);

  @Override
  public abstract boolean compare(String value);

  protected void fireContentSet() {
    if (listeners == null) {
      return;
    }

    boolean found = false;
    for (HdlModelListener l : listeners) {
      found = true;
      l.contentSet(this);
    }

    if (!found) {
      listeners = null;
    }
  }

  @Override
  public abstract String getContent();

  @Override
  public abstract String getName();

  @Override
  public void removeHdlModelListener(HdlModelListener l) {
    if (listeners == null) {
      return;
    }
    listeners.remove(l);
    if (listeners.isEmpty()) {
      listeners = null;
    }
  }

  @Override
  public abstract boolean setContent(String content);
}
