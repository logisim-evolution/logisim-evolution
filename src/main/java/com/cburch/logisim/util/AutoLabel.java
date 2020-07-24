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

package com.cburch.logisim.util;

import static com.cburch.logisim.util.Strings.S;

import com.cburch.logisim.circuit.Circuit;
import com.cburch.logisim.comp.Component;
import com.cburch.logisim.comp.ComponentFactory;
import com.cburch.logisim.data.AttributeSet;
import com.cburch.logisim.data.Location;
import com.cburch.logisim.fpga.designrulecheck.CorrectLabel;
import com.cburch.logisim.gui.generic.OptionPane;
import com.cburch.logisim.instance.StdAttr;
import com.cburch.logisim.tools.SetAttributeAction;
import java.awt.event.KeyEvent;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;

public class AutoLabel {

  static final Integer[] UsedKeyStrokes =
      new Integer[] {KeyEvent.VK_L, KeyEvent.VK_T, KeyEvent.VK_V, KeyEvent.VK_H, KeyEvent.VK_A};
  public static Set<Integer> KeyStrokes = new HashSet<Integer>(Arrays.asList(UsedKeyStrokes));

  private HashMap<Circuit, String> LabelBase = new HashMap<Circuit, String>();
  private HashMap<Circuit, Integer> CurrentIndex = new HashMap<Circuit, Integer>();
  private HashMap<Circuit, Boolean> UseLabelBaseOnly = new HashMap<Circuit, Boolean>();
  private HashMap<Circuit, Boolean> UseUnderscore = new HashMap<Circuit, Boolean>();
  private HashMap<Circuit, Boolean> active = new HashMap<Circuit, Boolean>();
  private HashMap<Circuit, String> CurrentLabel = new HashMap<Circuit, String>();

  public AutoLabel() {
    this("", null, false);
  }

  public AutoLabel(String Label, Circuit circ) {
    this(Label, circ, true);
  }

  public AutoLabel(String Label, Circuit circ, boolean UseFirstLabel) {
    update(circ, Label, UseFirstLabel, null);
    Activate(circ);
  }

  public boolean hasNext(Circuit circ) {
    if (circ == null || !active.containsKey(circ)) return false;
    return active.get(circ);
  }

  public String GetCurrent(Circuit circ, ComponentFactory me) {
    if (circ == null || !CurrentLabel.containsKey(circ) || CurrentLabel.get(circ).isEmpty())
      return "";
    if (Circuit.IsCorrectLabel(circ.getName(),CurrentLabel.get(circ), circ.getNonWires(), null, me, false))
      return CurrentLabel.get(circ);
    else if (hasNext(circ)) {
      return GetNext(circ, me);
    } else {
      SetLabel("", circ, me);
    }
    return "";
  }

  public boolean CorrectMatrixBaseLabel(
      Circuit circ, ComponentFactory me, String Common, int maxX, int maxY) {
    if ((Common == null) | (Common.isEmpty()) | (maxX < 0) | (maxY < 0)) return true;
    if (!SyntaxChecker.isVariableNameAcceptable(Common, true)) return false;
    for (int x = 0; x < maxX; x++)
      for (int y = 0; y < maxY; y++) {
        if (GetMatrixLabel(circ, me, Common, x, y).isEmpty()) {
          return false;
        }
      }
    return true;
  }

  public String GetMatrixLabel(Circuit circ, ComponentFactory me, String Common, int x, int y) {
    String Label;
    if ((Common == null) | (Common.isEmpty()) | (x < 0) | (y < 0)) return "";
    if (circ == null || !CurrentLabel.containsKey(circ) || CurrentLabel.get(circ).isEmpty())
      return "";
    Label = Common.concat("_X" + Integer.toString(x) + "_Y" + Integer.toString(y));
    if (Circuit.IsCorrectLabel(circ.getName(),Label, circ.getNonWires(), null, me, false)
        & SyntaxChecker.isVariableNameAcceptable(Label, false)) return Label;
    return "";
  }

  public String GetNext(Circuit circ, ComponentFactory me) {
    if (circ == null) return "";
    if (UseLabelBaseOnly.get(circ)) {
      UseLabelBaseOnly.put(circ, false);
      return LabelBase.get(circ);
    }
    String NewLabel = "";
    int CurIdx = CurrentIndex.get(circ);
    String BaseLab = LabelBase.get(circ);
    boolean Undescore = UseUnderscore.get(circ);
    do {
      CurIdx++;
      NewLabel = BaseLab;
      if (Undescore) NewLabel = NewLabel.concat("_");
      NewLabel = NewLabel.concat(Integer.toString(CurIdx));
    } while (!Circuit.IsCorrectLabel(circ.getName(),NewLabel, circ.getNonWires(), null, me, false));
    CurrentIndex.put(circ, CurIdx);
    CurrentLabel.put(circ, NewLabel);
    return NewLabel;
  }

  public boolean IsActive(Circuit circ) {
    if (circ == null) return false;
    if (!active.containsKey(circ)) return false;
    return active.get(circ);
  }

  public void SetLabel(String Label, Circuit circ, ComponentFactory me) {
    if (circ == null) return;
    update(circ, Label, true, me);
  }

  public void Activate(Circuit circ) {
    if (circ == null) return;
    if (LabelBase.containsKey(circ)
        && CurrentIndex.containsKey(circ)
        && UseLabelBaseOnly.containsKey(circ)
        && UseUnderscore.containsKey(circ)) active.put(circ, !LabelBase.get(circ).isEmpty());
  }

  public void Stop(Circuit circ) {
    if (circ == null) return;
    SetLabel("", circ, null);
    active.put(circ, false);
  }

  public static boolean LabelEndsWithNumber(String Label) {
    return CorrectLabel.Numbers.contains(Label.substring(Label.length() - 1));
  }

  private int GetLabelBaseEndIndex(String Label) {
    int index = Label.length();
    while ((index > 1) && CorrectLabel.Numbers.contains(Label.substring(index - 1, index))) index--;
    return (index - 1);
  }

  private void update(Circuit circ, String Label, boolean UseFirstLabel, ComponentFactory me) {
    if (circ == null) return;
    if (Label.isEmpty() || !SyntaxChecker.isVariableNameAcceptable(Label, false)) {
      LabelBase.put(circ, "");
      CurrentIndex.put(circ, 0);
      UseLabelBaseOnly.put(circ, false);
      CurrentLabel.put(circ, "");
      return;
    }
    UseLabelBaseOnly.put(circ, UseFirstLabel);
    if (LabelEndsWithNumber(Label)) {
      int Index = GetLabelBaseEndIndex(Label);
      CurrentIndex.put(circ, Integer.valueOf(Label.substring(Index + 1, Label.length())));
      LabelBase.put(circ, Label.substring(0, Index + 1));
      UseUnderscore.put(circ, false);
      UseLabelBaseOnly.put(circ, false);
    } else {
      LabelBase.put(circ, Label);
      CurrentIndex.put(circ, 0);
      UseUnderscore.put(circ, !Label.substring(Label.length() - 1).equals("_"));
    }
    if (UseFirstLabel) CurrentLabel.put(circ, Label);
    else CurrentLabel.put(circ, GetNext(circ, me));
  }

  private static class ComponentSorter implements Comparator<Component> {

    @Override
    public int compare(Component o1, Component o2) {
      if (o1 == o2) return 0;
      Location l1 = o1.getLocation();
      Location l2 = o2.getLocation();
      if (l2.getY() != l1.getY()) return l1.getY() - l2.getY();
      if (l2.getX() != l1.getX()) return l1.getX() - l2.getX();
      return -1;
    }
  }

  public static SortedSet<Component> Sort(Set<Component> comps) {
    SortedSet<Component> sorted = new TreeSet<Component>(new ComponentSorter());
    sorted.addAll(comps);
    return sorted;
  }

  public String AskAndSetLabel(
      String ComponentName,
      String OldLabel,
      Circuit circ,
      Component comp,
      ComponentFactory compfac,
      AttributeSet attrs,
      SetAttributeAction act,
      boolean CreateAction) {
    boolean correct = false;
    String NewLabel = OldLabel;
    while (!correct) {
      NewLabel =
          (String)
              OptionPane.showInputDialog(
                  null,
                  S.get("editLabelQuestion") + " " + ComponentName,
                  S.get("editLabelDialog"),
                  OptionPane.QUESTION_MESSAGE,
                  null,
                  null,
                  OldLabel);
      if (NewLabel != null) {
        if (Circuit.IsCorrectLabel(circ.getName(),NewLabel, circ.getNonWires(), attrs, compfac, true)
            && SyntaxChecker.isVariableNameAcceptable(NewLabel, true)
            && !CorrectLabel.IsKeyword(NewLabel, true)) {
          if (CreateAction) act.set(comp, StdAttr.LABEL, NewLabel);
          else SetLabel(NewLabel, circ, compfac);
          correct = true;
        }
      } else {
        correct = true;
        NewLabel = OldLabel;
      }
    }
    return NewLabel;
  }

  public boolean LabelKeyboardHandler(
      int KeyCode,
      AttributeSet attrs,
      String ComponentName,
      Component comp,
      ComponentFactory compfac,
      Circuit circ,
      SetAttributeAction act,
      boolean CreateAction) {
    switch (KeyCode) {
      case KeyEvent.VK_L:
        if (attrs.containsAttribute(StdAttr.LABEL)) {
          String OldLabel = attrs.getValue(StdAttr.LABEL);
          String NewLabel =
              AskAndSetLabel(
                  ComponentName, OldLabel, circ, comp, compfac, attrs, act, CreateAction);
          if (!NewLabel.equals(OldLabel)) {
            if (!NewLabel.isEmpty() && LabelEndsWithNumber(NewLabel)) {
              Activate(circ);
            } else {
              active.put(circ, false);
            }
          }
        }
        return true;
      case KeyEvent.VK_T:
        if (attrs.containsAttribute(StdAttr.LABEL_VISIBILITY)) {
          if (CreateAction)
            act.set(comp, StdAttr.LABEL_VISIBILITY, !attrs.getValue(StdAttr.LABEL_VISIBILITY));
          else attrs.setValue(StdAttr.LABEL_VISIBILITY, !attrs.getValue(StdAttr.LABEL_VISIBILITY));
        }
        return true;
      case KeyEvent.VK_V:
        if (attrs.containsAttribute(StdAttr.LABEL_VISIBILITY)
            && !attrs.getValue(StdAttr.LABEL_VISIBILITY)) {
          if (CreateAction) act.set(comp, StdAttr.LABEL_VISIBILITY, true);
          else attrs.setValue(StdAttr.LABEL_VISIBILITY, true);
        }
        return true;
      case KeyEvent.VK_H:
        if (attrs.containsAttribute(StdAttr.LABEL_VISIBILITY)
            && attrs.getValue(StdAttr.LABEL_VISIBILITY)) {
          if (CreateAction) act.set(comp, StdAttr.LABEL_VISIBILITY, false);
          else attrs.setValue(StdAttr.LABEL_VISIBILITY, false);
        }
        return true;
      case KeyEvent.VK_A:
        Stop(circ);
        return true;
    }
    return false;
  }
}
