/* Copyright (c) 2010, Carl Burch. License information is located in the
 * com.cburch.logisim.Main source code and at www.cburch.com/logisim/. */

package com.ita.logisim.ttl;

import java.util.Arrays;

import com.cburch.logisim.data.BitWidth;
import com.cburch.logisim.data.Value;
import com.cburch.logisim.instance.InstanceData;

public class ShiftRegisterData extends ClockState implements InstanceData {
	private BitWidth width;
	private Value[] vs;
	private int vsPos;

	public ShiftRegisterData(BitWidth width, int len) {
		this.width = width;
		this.vs = new Value[len];
		Arrays.fill(this.vs, Value.createKnown(width, 0));
		this.vsPos = 0;
	}

	public void clear() {
		Arrays.fill(vs, Value.createKnown(width, 0));
		vsPos = 0;
	}

	@Override
	public ShiftRegisterData clone() {
		ShiftRegisterData ret = (ShiftRegisterData) super.clone();
		ret.vs = this.vs.clone();
		return ret;
	}

	public Value get(int index) {
		int i = vsPos + index;
		Value[] v = vs;
		if (i >= v.length)
			i -= v.length;
		return v[i];
	}

	public int getLength() {
		return vs.length;
	}

	public void push(Value v) {
		int pos = vsPos;
		vs[pos] = v;
		vsPos = pos >= vs.length - 1 ? 0 : pos + 1;
	}

	public void set(int index, Value val) {
		int i = vsPos + index;
		Value[] v = vs;
		if (i >= v.length)
			i -= v.length;
		v[i] = val;
	}

	public void setDimensions(BitWidth newWidth, int newLength) {
		Value[] v = vs;
		BitWidth oldWidth = width;
		int oldW = oldWidth.getWidth();
		int newW = newWidth.getWidth();
		if (v.length != newLength) {
			Value[] newV = new Value[newLength];
			int j = vsPos;
			int copy = Math.min(newLength, v.length);
			for (int i = 0; i < copy; i++) {
				newV[i] = v[j];
				j++;
				if (j == v.length)
					j = 0;
			}
			Arrays.fill(newV, copy, newLength, Value.createKnown(newWidth, 0));
			v = newV;
			vsPos = 0;
			vs = newV;
		}
		if (oldW != newW) {
			for (int i = 0; i < v.length; i++) {
				Value vi = v[i];
				if (vi.getWidth() != newW) {
					v[i] = vi.extendWidth(newW, Value.FALSE);
				}
			}
			width = newWidth;
		}
	}
}