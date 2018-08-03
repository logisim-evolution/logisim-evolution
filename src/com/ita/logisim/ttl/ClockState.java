/* Copyright (c) 2010, Carl Burch. License information is located in the
 * com.cburch.logisim.Main source code and at www.cburch.com/logisim/. */

package com.ita.logisim.ttl;

import com.cburch.logisim.data.BitWidth;
import com.cburch.logisim.data.Value;
import com.cburch.logisim.instance.StdAttr;

class ClockState implements Cloneable {
	private Value lastClock;

	public ClockState() {
		lastClock = Value.FALSE;
	}

	@Override
	public ClockState clone() {
		try {
			return (ClockState) super.clone();
		} catch (CloneNotSupportedException e) {
			return null;
		}
	}

	public boolean updateClock(Value newClock, Object trigger) {
		Value oldClock = lastClock;
		lastClock = newClock;
		if (trigger == null || trigger == StdAttr.TRIG_RISING) {
			return oldClock == Value.FALSE && newClock == Value.TRUE;
		} else if (trigger == StdAttr.TRIG_FALLING) {
			return oldClock == Value.TRUE && newClock == Value.FALSE;
		} else if (trigger == StdAttr.TRIG_HIGH) {
			return newClock == Value.TRUE;
		} else if (trigger == StdAttr.TRIG_LOW) {
			return newClock == Value.FALSE;
		} else {
			return oldClock == Value.FALSE && newClock == Value.TRUE;
		}
	}

	public boolean updateClock(Value newClock) {
		Value oldClock = lastClock;
		lastClock = newClock;
		return oldClock == Value.FALSE && newClock == Value.TRUE;
	}
	public boolean updateClock(Value newClock,int which) {
		Value[] values = lastClock.getAll();
		if (values.length <= which) {
			Value[] nvalue = (Value.createKnown(BitWidth.create(which+1), 0)).getAll();
			for (int i=0; i<values.length;i++)
				nvalue[i]=values[i];
			values = nvalue;
		}
		Value oldClock = values[which];
		values[which] = newClock;
		lastClock = Value.create(values);
		return oldClock == Value.FALSE && newClock == Value.TRUE;
	}
}
