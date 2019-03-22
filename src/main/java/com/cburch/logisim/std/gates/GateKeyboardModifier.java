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

package com.cburch.logisim.std.gates;

import com.cburch.logisim.comp.Component;
import com.cburch.logisim.data.AttributeSet;
import com.cburch.logisim.gui.main.Canvas;
import com.cburch.logisim.tools.SetAttributeAction;
import java.awt.event.KeyEvent;

public class GateKeyboardModifier {
  public static boolean TookKeyboardStrokes(
      int KeyCode,
      Component comp,
      AttributeSet attrs,
      Canvas canvas,
      SetAttributeAction act,
      boolean CreateAction) {
    switch (KeyCode) {
      case KeyEvent.VK_N:
      case KeyEvent.VK_S:
        if (attrs.containsAttribute(GateAttributes.ATTR_SIZE)) {
          if (CreateAction) act.set(comp, GateAttributes.ATTR_SIZE, GateAttributes.SIZE_NARROW);
          else {
            attrs.setValue(GateAttributes.ATTR_SIZE, GateAttributes.SIZE_NARROW);
            canvas.repaint();
          }
        }
        return true;
      case KeyEvent.VK_M:
        if (attrs.containsAttribute(GateAttributes.ATTR_SIZE)) {
          if (CreateAction) act.set(comp, GateAttributes.ATTR_SIZE, GateAttributes.SIZE_MEDIUM);
          else {
            attrs.setValue(GateAttributes.ATTR_SIZE, GateAttributes.SIZE_MEDIUM);
            canvas.repaint();
          }
        }
        return true;
      case KeyEvent.VK_W:
        if (attrs.containsAttribute(GateAttributes.ATTR_SIZE)) {
          if (CreateAction) act.set(comp, GateAttributes.ATTR_SIZE, GateAttributes.SIZE_WIDE);
          else {
            attrs.setValue(GateAttributes.ATTR_SIZE, GateAttributes.SIZE_WIDE);
            canvas.repaint();
          }
        }
        return true;
      case KeyEvent.VK_EQUALS:
      case KeyEvent.VK_PLUS:
      case KeyEvent.VK_ADD:
        if (attrs.containsAttribute(GateAttributes.ATTR_INPUTS)) {
          int NrOfInputs = attrs.getValue(GateAttributes.ATTR_INPUTS);
          if (NrOfInputs < GateAttributes.MAX_INPUTS) {
            if (CreateAction) act.set(comp, GateAttributes.ATTR_INPUTS, NrOfInputs + 1);
            else {
              attrs.setValue(GateAttributes.ATTR_INPUTS, NrOfInputs + 1);
              canvas.repaint();
            }
          }
        }
        return true;
      case KeyEvent.VK_MINUS:
      case KeyEvent.VK_SUBTRACT:
        if (attrs.containsAttribute(GateAttributes.ATTR_INPUTS)) {
          int NrOfInputs = attrs.getValue(GateAttributes.ATTR_INPUTS);
          if (NrOfInputs > 2) {
            if (CreateAction) act.set(comp, GateAttributes.ATTR_INPUTS, NrOfInputs - 1);
            else {
              attrs.setValue(GateAttributes.ATTR_INPUTS, NrOfInputs - 1);
              canvas.repaint();
            }
          }
        }
        return true;
      default:
        return false;
    }
  }
}
