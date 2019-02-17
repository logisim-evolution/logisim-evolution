package com.cburch.logisim.prefs;

import com.cburch.logisim.data.AttributeOption;

public class ConvertEvent {
	private AttributeOption value;

	public ConvertEvent(AttributeOption value) {
		this.value = value;
	}
	
	public AttributeOption GetValue() {
		return value;
	}
}
