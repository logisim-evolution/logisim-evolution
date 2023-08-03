/*
 * Logisim-evolution - digital logic design tool and simulator
 * Copyright by the Logisim-evolution developers
 *
 * https://github.com/logisim-evolution/
 *
 * This is free software released under GNU GPLv3 license
 */

package com.cburch.logisim.std.gates;

import com.cburch.logisim.comp.Component;
import com.cburch.logisim.data.AttributeSet;
import com.cburch.logisim.gui.main.Canvas;
import com.cburch.logisim.prefs.AppPreferences;
import com.cburch.logisim.prefs.PrefMonitorKeyStroke;
import com.cburch.logisim.tools.SetAttributeAction;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;

public class GateKeyboardModifier {
  public static boolean tookKeyboardStrokes(
      int KeyCode,
      int modifier,
      Component comp,
      AttributeSet attrs,
      Canvas canvas,
      SetAttributeAction act,
      boolean CreateAction) {
    String compare = InputEvent.getModifiersExText(modifier) + " + " + KeyEvent.getKeyText(KeyCode);
    /* Here originally have VK_N VK_EQUALS VK_PLUS VK_MINUS but not-used */
    if (compare.equals(compare.equals(((PrefMonitorKeyStroke) AppPreferences.HOTKEY_GATE_MODIFIER_SIZE_SMALL).getCompareString()))) {
      if (attrs.containsAttribute(GateAttributes.ATTR_SIZE)) {
        if (CreateAction) act.set(comp, GateAttributes.ATTR_SIZE, GateAttributes.SIZE_NARROW);
        else {
          attrs.setValue(GateAttributes.ATTR_SIZE, GateAttributes.SIZE_NARROW);
          canvas.repaint();
        }
      }
      return true;
    } else if (compare.equals(compare.equals(((PrefMonitorKeyStroke) AppPreferences.HOTKEY_GATE_MODIFIER_SIZE_MEDIUM).getCompareString()))) {
      if (attrs.containsAttribute(GateAttributes.ATTR_SIZE)) {
        if (CreateAction) act.set(comp, GateAttributes.ATTR_SIZE, GateAttributes.SIZE_MEDIUM);
        else {
          attrs.setValue(GateAttributes.ATTR_SIZE, GateAttributes.SIZE_MEDIUM);
          canvas.repaint();
        }
      }
      return true;
    } else if (compare.equals(compare.equals(((PrefMonitorKeyStroke) AppPreferences.HOTKEY_GATE_MODIFIER_SIZE_WIDE).getCompareString()))) {
      if (attrs.containsAttribute(GateAttributes.ATTR_SIZE)) {
        if (CreateAction) act.set(comp, GateAttributes.ATTR_SIZE, GateAttributes.SIZE_WIDE);
        else {
          attrs.setValue(GateAttributes.ATTR_SIZE, GateAttributes.SIZE_WIDE);
          canvas.repaint();
        }
      }
      return true;
    } else if (compare.equals(compare.equals(((PrefMonitorKeyStroke) AppPreferences.HOTKEY_GATE_MODIFIER_INPUT_ADD).getCompareString()))) {
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
    } else if (compare.equals(compare.equals(((PrefMonitorKeyStroke) AppPreferences.HOTKEY_GATE_MODIFIER_INPUT_SUB).getCompareString()))) {
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
    } else {
      return false;
    }
  }
}
