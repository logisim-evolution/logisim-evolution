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

package com.cburch.logisim.std.io;

import com.cburch.logisim.data.Value;
import com.cburch.logisim.instance.InstanceData;
import java.awt.FontMetrics;

class KeyboardData implements InstanceData, Cloneable {
  private Value lastClock;
  private char[] buffer;
  private String str;
  private int bufferLength;
  private int cursorPos;
  private boolean dispValid;
  private int dispStart;
  private int dispEnd;

  public KeyboardData(int capacity) {
    lastClock = Value.UNKNOWN;
    buffer = new char[capacity];
    clear();
  }

  public void clear() {
    bufferLength = 0;
    cursorPos = 0;
    str = "";
    dispValid = false;
    dispStart = 0;
    dispEnd = 0;
  }

  @Override
  public Object clone() {
    try {
      final var ret = (KeyboardData) super.clone();
      ret.buffer = this.buffer.clone();
      return ret;
    } catch (CloneNotSupportedException e) {
      return null;
    }
  }

  public boolean delete() {
    final var buf = buffer;
    final var len = bufferLength;
    final var pos = cursorPos;
    if (pos >= len) return false;
    if (len >= pos + 1)
      System.arraycopy(buf, pos + 1, buf, pos, len - (pos + 1));
    bufferLength = len - 1;
    str = null;
    dispValid = false;
    return true;
  }

  public char dequeue() {
    final var buf = buffer;
    final var len = bufferLength;
    if (len == 0) return '\0';
    final var ret = buf[0];
    if (len >= 1)
      System.arraycopy(buf, 1, buf, 0, len - 1);
    bufferLength = len - 1;
    final var pos = cursorPos;
    if (pos > 0) cursorPos = pos - 1;
    str = null;
    dispValid = false;
    return ret;
  }

  private boolean fits(FontMetrics fm, String str, int w0, int w1, int i0, int i1, int max) {
    if (i0 >= i1) return true;
    var len = str.length();
    if (i0 < 0 || i1 > len) return false;
    var w = fm.stringWidth(str.substring(i0, i1));
    if (i0 > 0) w += w0;
    if (i1 < str.length()) w += w1;
    return w <= max;
  }

  public char getChar(int pos) {
    return pos >= 0 && pos < bufferLength ? buffer[pos] : '\0';
  }

  public int getCursorPosition() {
    return cursorPos;
  }

  public int getDisplayEnd() {
    return dispEnd;
  }

  public int getDisplayStart() {
    return dispStart;
  }

  public int getNextSpecial(int pos) {
    final var buf = buffer;
    final var len = bufferLength;
    for (int i = pos; i < len; i++) {
      char c = buf[i];
      if (Character.isISOControl(c)) return i;
    }
    return -1;
  }

  public boolean insert(char value) {
    final var buf = buffer;
    final var len = bufferLength;
    if (len >= buf.length) return false;
    final var pos = cursorPos;
    if (len >= pos)
      System.arraycopy(buf, pos, buf, pos + 1, len - pos);
    buf[pos] = value;
    bufferLength = len + 1;
    cursorPos = pos + 1;
    str = null;
    dispValid = false;
    return true;
  }

  public boolean isDisplayValid() {
    return dispValid;
  }

  public boolean moveCursorBy(int delta) {
    final var len = bufferLength;
    final var pos = cursorPos;
    final var newPos = pos + delta;
    if (newPos < 0 || newPos > len) return false;
    cursorPos = newPos;
    dispValid = false;
    return true;
  }

  public boolean setCursor(int value) {
    final var len = bufferLength;
    if (value > len) value = len;
    final var pos = cursorPos;
    if (pos == value) return false;
    cursorPos = value;
    dispValid = false;
    return true;
  }

  public Value setLastClock(Value newClock) {
    final var ret = lastClock;
    lastClock = newClock;
    return ret;
  }

  @Override
  public String toString() {
    final var s = str;
    if (s != null) return s;
    final var build = new StringBuilder();
    final var buf = buffer;
    final var len = bufferLength;
    for (var i = 0; i < len; i++) {
      final var c = buf[i];
      build.append(Character.isISOControl(c) ? ' ' : c);
    }
    str = build.toString();
    return str;
  }

  public void updateBufferLength(int len) {
    synchronized (this) {
      final var buf = buffer;
      final var oldLen = buf.length;
      if (oldLen != len) {
        final var newBuf = new char[len];
        System.arraycopy(buf, 0, newBuf, 0, Math.min(len, oldLen));
        if (len < oldLen) {
          if (bufferLength > len) bufferLength = len;
          if (cursorPos > len) cursorPos = len;
        }
        buffer = newBuf;
        str = null;
        dispValid = false;
      }
    }
  }

  public void updateDisplay(FontMetrics fm) {
    if (dispValid) return;
    final var pos = cursorPos;
    var i0 = dispStart;
    var i1 = dispEnd;
    final var str = toString();
    final var len = str.length();
    final var max = Keyboard.WIDTH - 8 - 4;
    if (str.equals("") || fm.stringWidth(str) <= max) {
      i0 = 0;
      i1 = len;
    } else {
      // grow to include end of string if possible
      final var w0 = fm.stringWidth(str.charAt(0) + "m");
      final var w1 = fm.stringWidth("m");
      final var w = i0 == 0 ? fm.stringWidth(str) : w0 + fm.stringWidth(str.substring(i0));
      if (w <= max) i1 = len;

      // rearrange start/end so as to include cursor
      if (pos <= i0) {
        if (pos < i0) {
          i1 += pos - i0;
          i0 = pos;
        }
        if (pos == i0 && i0 > 0) {
          i0--;
          i1--;
        }
      }
      if (pos >= i1) {
        if (pos > i1) {
          i0 += pos - i1;
          i1 = pos;
        }
        if (pos == i1 && i1 < len) {
          i0++;
          i1++;
        }
      }
      if (i0 <= 2) i0 = 0;

      // resize segment to fit
      if (fits(fm, str, w0, w1, i0, i1, max)) { // maybe should grow
        while (fits(fm, str, w0, w1, i0, i1 + 1, max)) i1++;
        while (fits(fm, str, w0, w1, i0 - 1, i1, max)) i0--;
      } else { // should shrink
        if (pos < (i0 + i1) / 2) {
          i1--;
          while (!fits(fm, str, w0, w1, i0, i1, max)) i1--;
        } else {
          i0++;
          while (!fits(fm, str, w0, w1, i0, i1, max)) i0++;
        }
      }
      if (i0 == 1) i0 = 0;
    }
    dispStart = i0;
    dispEnd = i1;
    dispValid = true;
  }
}
