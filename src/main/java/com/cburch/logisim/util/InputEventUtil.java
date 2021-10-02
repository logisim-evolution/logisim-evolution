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

import java.awt.event.InputEvent;
import java.util.ArrayList;
import java.util.StringTokenizer;

public final class InputEventUtil {

  public static final String CTRL = "Ctrl";
  public static final String SHIFT = "Shift";
  public static final String ALT = "Alt";
  public static final String BUTTON1 = "Button1";
  public static final String BUTTON2 = "Button2";
  public static final String BUTTON3 = "Button3";

  private InputEventUtil() {
    throw new IllegalStateException("Utility class. No instantiation allowed.");
  }

  public static int fromDisplayString(String str) {
    var ret = 0;
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
    return switch (s) {
      case CTRL -> InputEvent.CTRL_DOWN_MASK;
      case SHIFT -> InputEvent.SHIFT_DOWN_MASK;
      case ALT -> InputEvent.ALT_DOWN_MASK;
      case BUTTON1 -> InputEvent.BUTTON1_DOWN_MASK;
      case BUTTON2 -> InputEvent.BUTTON2_DOWN_MASK;
      case BUTTON3 -> InputEvent.BUTTON3_DOWN_MASK;
      default -> throw new NumberFormatException("InputEventUtil");
    };
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

}
