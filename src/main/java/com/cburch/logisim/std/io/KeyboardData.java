/*
 * Logisim-evolution - digital logic design tool and simulator
 * Copyright by the Logisim-evolution developers
 *
 * https://github.com/logisim-evolution/
 *
 * This is free software released under GNU GPLv3 license
 */

package com.cburch.logisim.std.io;

import com.cburch.logisim.data.Value;
import com.cburch.logisim.instance.InstanceData;
import java.awt.FontMetrics;
import lombok.Getter;

class KeyboardData implements InstanceData, Cloneable {
  private Value lastClock;
  private char[] buffer;
  private String str;
  private int bufferLength;
  @Getter private int cursorPosition;
  @Getter private boolean displayValid;
  @Getter private int displayStart;
  @Getter private int displayEnd;

  public KeyboardData(int capacity) {
    lastClock = Value.UNKNOWN;
    buffer = new char[capacity];
    clear();
  }

  public void clear() {
    bufferLength = 0;
    cursorPosition = 0;
    str = "";
    displayValid = false;
    displayStart = 0;
    displayEnd = 0;
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
    final var pos = cursorPosition;
    if (pos >= len) return false;
    if (len >= pos + 1)
      System.arraycopy(buf, pos + 1, buf, pos, len - (pos + 1));
    bufferLength = len - 1;
    str = null;
    displayValid = false;
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
    final var pos = cursorPosition;
    if (pos > 0) cursorPosition = pos - 1;
    str = null;
    displayValid = false;
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
    return (pos >= 0 && pos < bufferLength) ? buffer[pos] : '\0';
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
    final var pos = cursorPosition;
    if (len >= pos)
      System.arraycopy(buf, pos, buf, pos + 1, len - pos);
    buf[pos] = value;
    bufferLength = len + 1;
    cursorPosition = pos + 1;
    str = null;
    displayValid = false;
    return true;
  }

  public boolean moveCursorBy(int delta) {
    final var len = bufferLength;
    final var pos = cursorPosition;
    final var newPos = pos + delta;
    if (newPos < 0 || newPos > len) return false;
    cursorPosition = newPos;
    displayValid = false;
    return true;
  }

  public boolean setCursor(int value) {
    final var len = bufferLength;
    if (value > len) value = len;
    final var pos = cursorPosition;
    if (pos == value) return false;
    cursorPosition = value;
    displayValid = false;
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
          if (cursorPosition > len) cursorPosition = len;
        }
        buffer = newBuf;
        str = null;
        displayValid = false;
      }
    }
  }

  public void updateDisplay(FontMetrics fm) {
    if (displayValid) return;
    final var pos = cursorPosition;
    var i0 = displayStart;
    var i1 = displayEnd;
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
    displayStart = i0;
    displayEnd = i1;
    displayValid = true;
  }
}
