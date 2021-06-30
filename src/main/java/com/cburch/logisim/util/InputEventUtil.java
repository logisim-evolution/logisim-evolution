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

package com.cburch.logisim.util;

import static com.cburch.logisim.util.Strings.S;

import java.awt.event.InputEvent;
import java.util.ArrayList;
import java.util.StringTokenizer;

public class InputEventUtil {
  public static int fromDisplayString(String str) {
    int ret = 0;
    final var toks = new StringTokenizer(str);
    while (toks.hasMoreTokens()) {
      final var s = toks.nextToken();
      if (s.equals(S.get("ctrlMod"))) ret |= InputEvent.CTRL_DOWN_MASK;
      else if (s.equals(S.get("altMod"))) ret |= InputEvent.ALT_DOWN_MASK;
      else if (s.equals(S.get("shiftMod"))) ret |= InputEvent.SHIFT_DOWN_MASK;
      else if (s.equals(S.get("button1Mod"))) ret |= InputEvent.BUTTON1_DOWN_MASK;
      else if (s.equals(S.get("button2Mod"))) ret |= InputEvent.BUTTON2_DOWN_MASK;
      else if (s.equals(S.get("button3Mod"))) ret |= InputEvent.BUTTON3_DOWN_MASK;
      else throw new NumberFormatException("InputEventUtil");
    }
    return ret;
  }

  private static int parseInput(String s) {
    switch (s) {
      case CTRL:
        return InputEvent.CTRL_DOWN_MASK;
      case SHIFT:
        return InputEvent.SHIFT_DOWN_MASK;
      case ALT:
        return InputEvent.ALT_DOWN_MASK;
      case BUTTON1:
        return InputEvent.BUTTON1_DOWN_MASK;
      case BUTTON2:
        return InputEvent.BUTTON2_DOWN_MASK;
      case BUTTON3:
        return InputEvent.BUTTON3_DOWN_MASK;
      default:
        throw new NumberFormatException("InputEventUtil");
    }
  }

  public static int fromString(String str) {
    var ret = 0;
    final var toks = new StringTokenizer(str);
    while (toks.hasMoreTokens()) {
      final var s = toks.nextToken();
      ret |= parseInput(s);
    }
    return ret;
  }

  public static String toDisplayString(int mods) {
    final var arr = new ArrayList<String>();
    if ((mods & InputEvent.CTRL_DOWN_MASK) != 0) arr.add(S.get("ctrlMod"));
    if ((mods & InputEvent.ALT_DOWN_MASK) != 0) arr.add(S.get("altMod"));
    if ((mods & InputEvent.SHIFT_DOWN_MASK) != 0) arr.add(S.get("shiftMod"));
    if ((mods & InputEvent.BUTTON1_DOWN_MASK) != 0) arr.add(S.get("button1Mod"));
    if ((mods & InputEvent.BUTTON2_DOWN_MASK) != 0) arr.add(S.get("button2Mod"));
    if ((mods & InputEvent.BUTTON3_DOWN_MASK) != 0) arr.add(S.get("button3Mod"));

    if (arr.isEmpty()) return "";

    final var it = arr.iterator();
    if (it.hasNext()) {
      final var ret = new StringBuilder();
      ret.append(it.next());
      while (it.hasNext()) {
        ret.append(" ");
        ret.append(it.next());
      }
      return ret.toString();
    } else {
      return "";
    }
  }

  public static String toKeyDisplayString(int mods) {
    final var arr = new ArrayList<String>();
    if ((mods & InputEvent.META_DOWN_MASK) != 0) arr.add(S.get("metaMod"));
    if ((mods & InputEvent.CTRL_DOWN_MASK) != 0) arr.add(S.get("ctrlMod"));
    if ((mods & InputEvent.ALT_DOWN_MASK) != 0) arr.add(S.get("altMod"));
    if ((mods & InputEvent.SHIFT_DOWN_MASK) != 0) arr.add(S.get("shiftMod"));

    final var it = arr.iterator();
    if (it.hasNext()) {
      final var ret = new StringBuilder();
      ret.append(it.next());
      while (it.hasNext()) {
        ret.append(" ");
        ret.append(it.next());
      }
      return ret.toString();
    } else {
      return "";
    }
  }

  public static String toString(int mods) {
    final var arr = new ArrayList<String>();
    if ((mods & InputEvent.CTRL_DOWN_MASK) != 0) arr.add(CTRL);
    if ((mods & InputEvent.ALT_DOWN_MASK) != 0) arr.add(ALT);
    if ((mods & InputEvent.SHIFT_DOWN_MASK) != 0) arr.add(SHIFT);
    if ((mods & InputEvent.BUTTON1_DOWN_MASK) != 0) arr.add(BUTTON1);
    if ((mods & InputEvent.BUTTON2_DOWN_MASK) != 0) arr.add(BUTTON2);
    if ((mods & InputEvent.BUTTON3_DOWN_MASK) != 0) arr.add(BUTTON3);

    final var it = arr.iterator();
    if (it.hasNext()) {
      final var ret = new StringBuilder();
      ret.append(it.next());
      while (it.hasNext()) {
        ret.append(" ");
        ret.append(it.next());
      }
      return ret.toString();
    } else {
      return "";
    }
  }

  public static final String CTRL = "Ctrl";

  public static final String SHIFT = "Shift";

  public static final String ALT = "Alt";

  public static final String BUTTON1 = "Button1";

  public static final String BUTTON2 = "Button2";

  public static final String BUTTON3 = "Button3";

  private InputEventUtil() {}
}
