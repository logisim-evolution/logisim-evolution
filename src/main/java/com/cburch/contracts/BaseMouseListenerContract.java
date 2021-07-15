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

package com.cburch.contracts;

import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;

/**
 * Dummy implementation of java.awt.event.MouseListener interface. The main purpose of this
 * interface is to provide default (empty) implementation of interface methods as, unfortunately
 * JDKs interfaces do not come with default implementation even they easily could. Implementing this
 * interface instead of the parent one allows skipping the need of implementing all, even unneeded,
 * methods. That's saves some efforts and reduces overall LOC.
 */
public interface BaseMouseListenerContract extends MouseListener {
  @Override
  // No default implementation provided intentionally.
  void mouseClicked(MouseEvent mouseEvent);

  @Override
  default void mousePressed(MouseEvent mouseEvent) {
    // no-op implementation
  }

  @Override
  default void mouseReleased(MouseEvent mouseEvent) {
    // no-op implementation
  }

  @Override
  default void mouseEntered(MouseEvent mouseEvent) {
    // no-op implementation
  }

  @Override
  default void mouseExited(MouseEvent mouseEvent) {
    // no-op implementation
  }
}
