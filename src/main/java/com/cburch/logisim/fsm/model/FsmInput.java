package com.cburch.logisim.fsm.model;

import com.cburch.logisim.data.BitWidth;
import com.cburch.logisim.data.Value;
import com.cburch.logisim.util.StringGetter;

public class FsmInput {

	private StringGetter Name;
	private Value value = Value.createUnknown(BitWidth.ONE);
	private boolean ValueChanged = false;
	private boolean PosEdge = false;
	private boolean NegEdge = false;
	
	public FsmInput(StringGetter value) {
		Name = value;
	}
	
	public FsmInput clone() {
		return new FsmInput(Name);
	}
	
	public void setValue( Value val ) {
		if (value.equals(val)) {
			ValueChanged = false;
		} else {
			ValueChanged = true;
			PosEdge = (value == Value.FALSE)&(val == Value.TRUE);
			NegEdge = (val == Value.FALSE)&(value == Value.TRUE);
			value = val;
		}
	}
	
	public boolean hasChanged() {
		return ValueChanged;
	}
	
	public boolean PosEdge() {
		return ValueChanged&PosEdge;
	}
	
	public boolean NegEdge() {
		return ValueChanged&NegEdge;
	}
	
	public StringGetter Name() {
		return Name;
	}
	
	public Value getValue() {
		return value;
	}
	
}
