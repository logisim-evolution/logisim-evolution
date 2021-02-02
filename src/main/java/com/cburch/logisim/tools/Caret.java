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
import java.awt.event.KeyEvent;
import java.awt.event.MouseEvent;

public interface Caret {
  // listener methods
  void addCaretListener(CaretListener e);

  void cancelEditing();

  // finishing
  void commitText(String text);

  void draw(Graphics g);

  Bounds getBounds(Graphics g);

  // query/Graphics methods
  String getText();

  void keyPressed(KeyEvent e);

  void keyReleased(KeyEvent e);

  void keyTyped(KeyEvent e);

  void mouseDragged(MouseEvent e);

  // events to handle
  void mousePressed(MouseEvent e);

  void mouseReleased(MouseEvent e);

  void removeCaretListener(CaretListener e);

  void stopEditing();
}
