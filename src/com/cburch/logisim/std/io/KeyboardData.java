/*******************************************************************************
 * This file is part of logisim-evolution.
 *
 *   logisim-evolution is free software: you can redistribute it and/or modify
 *   it under the terms of the GNU General Public License as published by
 *   the Free Software Foundation, either version 3 of the License, or
 *   (at your option) any later version.
 *
 *   logisim-evolution is distributed in the hope that it will be useful,
 *   but WITHOUT ANY WARRANTY; without even the implied warranty of
 *   MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *   GNU General Public License for more details.
 *
 *   You should have received a copy of the GNU General Public License
 *   along with logisim-evolution.  If not, see <http://www.gnu.org/licenses/>.
 *
 *   Original code by Carl Burch (http://www.cburch.com), 2011.
 *   Subsequent modifications by :
 *     + Haute École Spécialisée Bernoise
 *       http://www.bfh.ch
 *     + Haute École du paysage, d'ingénierie et d'architecture de Genève
 *       http://hepia.hesge.ch/
 *     + Haute École d'Ingénierie et de Gestion du Canton de Vaud
 *       http://www.heig-vd.ch/
 *   The project is currently maintained by :
 *     + REDS Institute - HEIG-VD
 *       Yverdon-les-Bains, Switzerland
 *       http://reds.heig-vd.ch
 *******************************************************************************/

package com.cburch.logisim.std.io;

import java.awt.FontMetrics;

import com.cburch.logisim.data.Value;
import com.cburch.logisim.instance.InstanceData;

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
			KeyboardData ret = (KeyboardData) super.clone();
			ret.buffer = this.buffer.clone();
			return ret;
		} catch (CloneNotSupportedException e) {
			return null;
		}
	}

	public boolean delete() {
		char[] buf = buffer;
		int len = bufferLength;
		int pos = cursorPos;
		if (pos >= len)
			return false;
		for (int i = pos + 1; i < len; i++)
			buf[i - 1] = buf[i];
		bufferLength = len - 1;
		str = null;
		dispValid = false;
		return true;
	}

	public char dequeue() {
		char[] buf = buffer;
		int len = bufferLength;
		if (len == 0)
			return '\0';
		char ret = buf[0];
		for (int i = 1; i < len; i++)
			buf[i - 1] = buf[i];
		bufferLength = len - 1;
		int pos = cursorPos;
		if (pos > 0)
			cursorPos = pos - 1;
		str = null;
		dispValid = false;
		return ret;
	}

	private boolean fits(FontMetrics fm, String str, int w0, int w1, int i0,
			int i1, int max) {
		if (i0 >= i1)
			return true;
		int len = str.length();
		if (i0 < 0 || i1 > len)
			return false;
		int w = fm.stringWidth(str.substring(i0, i1));
		if (i0 > 0)
			w += w0;
		if (i1 < str.length())
			w += w1;
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
		char[] buf = buffer;
		int len = bufferLength;
		for (int i = pos; i < len; i++) {
			char c = buf[i];
			if (Character.isISOControl(c))
				return i;
		}
		return -1;
	}

	public boolean insert(char value) {
		char[] buf = buffer;
		int len = bufferLength;
		if (len >= buf.length)
			return false;
		int pos = cursorPos;
		for (int i = len; i > pos; i--)
			buf[i] = buf[i - 1];
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
		int len = bufferLength;
		int pos = cursorPos;
		int newPos = pos + delta;
		if (newPos < 0 || newPos > len)
			return false;
		cursorPos = newPos;
		dispValid = false;
		return true;
	}

	public boolean setCursor(int value) {
		int len = bufferLength;
		if (value > len)
			value = len;
		int pos = cursorPos;
		if (pos == value)
			return false;
		cursorPos = value;
		dispValid = false;
		return true;
	}

	public Value setLastClock(Value newClock) {
		Value ret = lastClock;
		lastClock = newClock;
		return ret;
	}

	@Override
	public String toString() {
		String s = str;
		if (s != null)
			return s;
		StringBuilder build = new StringBuilder();
		char[] buf = buffer;
		int len = bufferLength;
		for (int i = 0; i < len; i++) {
			char c = buf[i];
			build.append(Character.isISOControl(c) ? ' ' : c);
		}
		str = build.toString();
		return str;
	}

	public void updateBufferLength(int len) {
		synchronized (this) {
			char[] buf = buffer;
			int oldLen = buf.length;
			if (oldLen != len) {
				char[] newBuf = new char[len];
				System.arraycopy(buf, 0, newBuf, 0, Math.min(len, oldLen));
				if (len < oldLen) {
					if (bufferLength > len)
						bufferLength = len;
					if (cursorPos > len)
						cursorPos = len;
				}
				buffer = newBuf;
				str = null;
				dispValid = false;
			}
		}
	}

	public void updateDisplay(FontMetrics fm) {
		if (dispValid)
			return;
		int pos = cursorPos;
		int i0 = dispStart;
		int i1 = dispEnd;
		String str = toString();
		int len = str.length();
		int max = Keyboard.WIDTH - 8 - 4;
		if (str.equals("") || fm.stringWidth(str) <= max) {
			i0 = 0;
			i1 = len;
		} else {
			// grow to include end of string if possible
			int w0 = fm.stringWidth(str.charAt(0) + "m");
			int w1 = fm.stringWidth("m");
			int w = i0 == 0 ? fm.stringWidth(str) : w0
					+ fm.stringWidth(str.substring(i0));
			if (w <= max)
				i1 = len;

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
			if (i0 <= 2)
				i0 = 0;

			// resize segment to fit
			if (fits(fm, str, w0, w1, i0, i1, max)) { // maybe should grow
				while (fits(fm, str, w0, w1, i0, i1 + 1, max))
					i1++;
				while (fits(fm, str, w0, w1, i0 - 1, i1, max))
					i0--;
			} else { // should shrink
				if (pos < (i0 + i1) / 2) {
					i1--;
					while (!fits(fm, str, w0, w1, i0, i1, max))
						i1--;
				} else {
					i0++;
					while (!fits(fm, str, w0, w1, i0, i1, max))
						i0++;
				}

			}
			if (i0 == 1)
				i0 = 0;
		}
		dispStart = i0;
		dispEnd = i1;
		dispValid = true;
	}
}