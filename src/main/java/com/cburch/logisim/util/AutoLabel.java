/*
 * Logisim-evolution - digital logic design tool and simulator
 * Copyright by the Logisim-evolution developers
 *
 * https://github.com/logisim-evolution/
 *
 * This is free software released under GNU GPLv3 license
 */

package com.cburch.logisim.util;

import static com.cburch.logisim.util.Strings.S;

import com.cburch.logisim.circuit.Circuit;
import com.cburch.logisim.comp.Component;
import com.cburch.logisim.comp.ComponentFactory;
import com.cburch.logisim.comp.PositionComparator;
import com.cburch.logisim.data.AttributeSet;
import com.cburch.logisim.fpga.designrulecheck.CorrectLabel;
import com.cburch.logisim.gui.generic.OptionPane;
import com.cburch.logisim.instance.StdAttr;
import com.cburch.logisim.prefs.AppPreferences;
import com.cburch.logisim.prefs.PrefMonitorKeyStroke;
import com.cburch.logisim.std.wiring.Tunnel;
import com.cburch.logisim.tools.SetAttributeAction;
import java.awt.event.KeyEvent;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;

public class AutoLabel {

  public static final Integer[] USED_KEY_STROKES = new Integer[] {
      KeyEvent.VK_L, KeyEvent.VK_T, KeyEvent.VK_V, KeyEvent.VK_H, KeyEvent.VK_A};
  public static final Set<Integer> KEY_STROKES = new HashSet<>(Arrays.asList(USED_KEY_STROKES));

  private final HashMap<Circuit, String> labelBase = new HashMap<>();
  private final HashMap<Circuit, Integer> currentIndex = new HashMap<>();
  private final HashMap<Circuit, Boolean> useLabelBaseOnly = new HashMap<>();
  private final HashMap<Circuit, Boolean> useUnderscore = new HashMap<>();
  private final HashMap<Circuit, Boolean> active = new HashMap<>();
  private final HashMap<Circuit, String> currentLabel = new HashMap<>();

  public AutoLabel() {
    this("", null, false);
  }

  public AutoLabel(String label, Circuit circ) {
    this(label, circ, true);
  }

  public AutoLabel(String label, Circuit circ, boolean useFirstLabel) {
    update(circ, label, useFirstLabel, null);
    activate(circ);
  }

  public boolean hasNext(Circuit circ) {
    if (circ == null || !active.containsKey(circ)) {
      return false;
    }
    return active.get(circ);
  }

  public String getCurrent(Circuit circ, ComponentFactory me) {
    if (circ == null || !currentLabel.containsKey(circ) || currentLabel.get(circ).isEmpty()) {
      return "";
    }
    if (Circuit.isCorrectLabel(circ.getName(),
        currentLabel.get(circ), circ.getNonWires(), null, me, false)) {
      return currentLabel.get(circ);
    }

    if (hasNext(circ)) {
      return getNext(circ, me);
    } else {
      setLabel("", circ, me);
    }
    return "";
  }

  public boolean correctMatrixBaseLabel(Circuit circ, ComponentFactory me,
                                        String common, int maxX, int maxY) {
    if (StringUtil.isNullOrEmpty(common) || (maxX < 0) || (maxY < 0)) {
      return true;
    }
    if (!SyntaxChecker.isVariableNameAcceptable(common, true)) {
      return false;
    }
    for (var x = 0; x < maxX; x++) {
      for (var y = 0; y < maxY; y++) {
        if (getMatrixLabel(circ, me, common, x, y).isEmpty()) {
          return false;
        }
      }
    }
    return true;
  }

  public String getMatrixLabel(Circuit circ, ComponentFactory me, String common, int x, int y) {
    if (StringUtil.isNullOrEmpty(common) || (x < 0) || (y < 0)) {
      return "";
    }
    if (circ == null || !currentLabel.containsKey(circ) || currentLabel.get(circ).isEmpty()) {
      return "";
    }
    final var label = common.concat("_X" + x + "_Y" + y);
    if (Circuit.isCorrectLabel(circ.getName(), label, circ.getNonWires(), null, me, false)
        && SyntaxChecker.isVariableNameAcceptable(label, false)) {
      return label;
    }
    return "";
  }

  public String getNext(Circuit circ, ComponentFactory me) {
    if (circ == null) {
      return "";
    }
    if (useLabelBaseOnly.get(circ)) {
      useLabelBaseOnly.put(circ, false);
      return labelBase.get(circ);
    }
    if (me instanceof Tunnel) {
      return labelBase.get(circ);
    }
    var newLabel = "";
    var curIdx = currentIndex.get(circ);
    final var baseLabel = labelBase.get(circ);
    boolean undescore = useUnderscore.get(circ);
    do {
      curIdx++;
      newLabel = baseLabel;
      if (undescore) {
        newLabel = newLabel.concat("_");
      }
      newLabel = newLabel.concat(Integer.toString(curIdx));
    } while (!Circuit.isCorrectLabel(circ.getName(),
        newLabel, circ.getNonWires(), null, me, false));
    currentIndex.put(circ, curIdx);
    currentLabel.put(circ, newLabel);
    return newLabel;
  }

  public boolean isActive(Circuit circ) {
    if (circ != null && active.containsKey(circ)) {
      return active.get(circ);
    }
    return false;
  }

  public void setLabel(String label, Circuit circ, ComponentFactory me) {
    if (circ != null) {
      update(circ, label, true, me);
    }
  }

  public void activate(Circuit circ) {
    if (circ != null) {
      if (labelBase.containsKey(circ)
          && currentIndex.containsKey(circ)
          && useLabelBaseOnly.containsKey(circ)
          && useUnderscore.containsKey(circ)) {
        active.put(circ, !labelBase.get(circ).isEmpty());
      }
    }
  }

  public void stop(Circuit circ) {
    if (circ != null) {
      setLabel("", circ, null);
      active.put(circ, false);
    }
  }

  public static boolean labelEndsWithNumber(String label) {
    return CorrectLabel.NUMBERS.contains(label.substring(label.length() - 1));
  }

  private int getLabelBaseEndIndex(String label) {
    var index = label.length();
    while ((index > 1) && CorrectLabel.NUMBERS.contains(label.substring(index - 1, index))) {
      index--;
    }
    return (index - 1);
  }

  private void update(Circuit circ, String label, boolean useFirstLabel, ComponentFactory me) {
    if (circ == null) {
      return;
    }
    if (label.isEmpty() || !SyntaxChecker.isVariableNameAcceptable(label, false)) {
      labelBase.put(circ, "");
      currentIndex.put(circ, 0);
      useLabelBaseOnly.put(circ, false);
      currentLabel.put(circ, "");
      return;
    }
    useLabelBaseOnly.put(circ, useFirstLabel);
    if (labelEndsWithNumber(label)) {
      int index = getLabelBaseEndIndex(label);
      currentIndex.put(circ, Integer.valueOf(label.substring(index + 1)));
      labelBase.put(circ, label.substring(0, index + 1));
      useUnderscore.put(circ, false);
      useLabelBaseOnly.put(circ, false);
    } else {
      labelBase.put(circ, label);
      currentIndex.put(circ, 0);
      useUnderscore.put(circ, !label.endsWith("_"));
    }
    if (useFirstLabel) {
      currentLabel.put(circ, label);
    } else {
      currentLabel.put(circ, getNext(circ, me));
    }
  }

  public static SortedSet<Component> sort(Set<Component> comps) {
    SortedSet<Component> sorted = new TreeSet<>(new PositionComparator());
    sorted.addAll(comps);
    return sorted;
  }

  public String askAndSetLabel(String componentName, String oldLabel, Circuit circ,
                               Component comp, ComponentFactory compFactory,
                               AttributeSet attrs, SetAttributeAction act, boolean createAction) {
    var correct = false;
    var newLabel = oldLabel;
    while (!correct) {
      newLabel = (String)
          OptionPane.showInputDialog(circ.getProject().getFrame(), S.get("editLabelQuestion") + " " + componentName,
              S.get("editLabelDialog"), OptionPane.QUESTION_MESSAGE, null, null, oldLabel);
      if (newLabel != null) {
        if (Circuit.isCorrectLabel(circ.getName(),
            newLabel, circ.getNonWires(), attrs, compFactory, true)
            && SyntaxChecker.isVariableNameAcceptable(newLabel, true)
            && !CorrectLabel.isKeyword(newLabel, true)) {
          if (createAction) {
            act.set(comp, StdAttr.LABEL, newLabel);
          } else {
            setLabel(newLabel, circ, compFactory);
          }
          correct = true;
        }
      } else {
        correct = true;
        newLabel = oldLabel;
      }
    }
    return newLabel;
  }

  public boolean labelKeyboardHandler(int keyCode, int keyModifiers, AttributeSet attrs,
                                      String componentName, Component comp,
                                      ComponentFactory compFactory, Circuit circ,
                                      SetAttributeAction act, boolean createAction) {
    /* TODO: bind more hotkeys */
    int code = keyCode;
    int modifier = keyModifiers;
    if (((PrefMonitorKeyStroke) AppPreferences.HOTKEY_AUTO_LABEL_OPEN).compare(code, modifier)) {
      if (attrs.containsAttribute(StdAttr.LABEL)) {
        final var oldLabel = attrs.getValue(StdAttr.LABEL);
        final var newLabel = askAndSetLabel(componentName, oldLabel, circ, comp, compFactory,
            attrs, act, createAction);
        if (!newLabel.equals(oldLabel)) {
          if (!newLabel.isEmpty() && labelEndsWithNumber(newLabel)) {
            activate(circ);
          } else {
            active.put(circ, false);
          }
        }
      }
      return true;
    } else if (((PrefMonitorKeyStroke) AppPreferences.HOTKEY_AUTO_LABEL_TOGGLE)
        .compare(code, modifier)) {
      if (attrs.containsAttribute(StdAttr.LABEL_VISIBILITY)) {
        if (createAction) {
          act.set(comp, StdAttr.LABEL_VISIBILITY, !attrs.getValue(StdAttr.LABEL_VISIBILITY));
        } else {
          attrs.setValue(StdAttr.LABEL_VISIBILITY, !attrs.getValue(StdAttr.LABEL_VISIBILITY));
        }
      }
      return true;
    } else if (((PrefMonitorKeyStroke) AppPreferences.HOTKEY_AUTO_LABEL_VIEW)
        .compare(code, modifier)) {
      if (attrs.containsAttribute(StdAttr.LABEL_VISIBILITY)
          && !attrs.getValue(StdAttr.LABEL_VISIBILITY)) {
        if (createAction) {
          act.set(comp, StdAttr.LABEL_VISIBILITY, true);
        } else {
          attrs.setValue(StdAttr.LABEL_VISIBILITY, true);
        }
      }
      return true;
    } else if (((PrefMonitorKeyStroke) AppPreferences.HOTKEY_AUTO_LABEL_HIDE)
        .compare(code, modifier)) {
      if (attrs.containsAttribute(StdAttr.LABEL_VISIBILITY)
          && attrs.getValue(StdAttr.LABEL_VISIBILITY)) {
        if (createAction) {
          act.set(comp, StdAttr.LABEL_VISIBILITY, false);
        } else {
          attrs.setValue(StdAttr.LABEL_VISIBILITY, false);
        }
      }
      return true;
    } else if (((PrefMonitorKeyStroke) AppPreferences.HOTKEY_AUTO_LABEL_SELF_NUMBERED_STOP)
        .compare(code, modifier)) {
      stop(circ);
      return true;
    }
    return false;
  }
}
