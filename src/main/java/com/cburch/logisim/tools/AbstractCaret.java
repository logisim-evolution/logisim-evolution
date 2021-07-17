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

package com.cburch.logisim.tools;

import com.cburch.logisim.data.Bounds;
import java.awt.Graphics;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class AbstractCaret implements Caret {
  private final ArrayList<CaretListener> listeners = new ArrayList<>();
  private final List<CaretListener> listenersView;
  private Bounds bounds = Bounds.EMPTY_BOUNDS;

  public AbstractCaret() {
    listenersView = Collections.unmodifiableList(listeners);
  }

  // listener methods
  @Override
  public void addCaretListener(CaretListener e) {
    listeners.add(e);
  }

  @Override
  public Bounds getBounds(Graphics gfx) {
    return bounds;
  }

  protected List<CaretListener> getCaretListeners() {
    return listenersView;
  }

  // query/Graphics methods
  @Override
  public String getText() {
    return "";
  }

  @Override
  public void removeCaretListener(CaretListener e) {
    listeners.remove(e);
  }

  // configuration methods
  public void setBounds(Bounds value) {
    bounds = value;
  }
}
