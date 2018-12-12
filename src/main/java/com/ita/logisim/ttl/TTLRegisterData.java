package com.ita.logisim.ttl;

import com.cburch.logisim.data.BitWidth;
import com.cburch.logisim.data.Value;
import com.cburch.logisim.instance.InstanceData;
import com.cburch.logisim.prefs.AppPreferences;

public class TTLRegisterData extends ClockState implements InstanceData {

	private Value value;
	private BitWidth bits;

	public TTLRegisterData(BitWidth width) {
		value = (AppPreferences.Memory_Startup_Unknown.get()) ? Value.createUnknown(width) : Value.createKnown(width, 0);
		bits = width;
	}

	public void setValue(Value value) {
		this.value = value;
	}
	
	public Value getValue() {
		return value;
	}
	
	public BitWidth getWidth() {
		return bits;
	}
}
