package com.cburch.logisim.fsm.model;

import com.cburch.logisim.data.AttributeSet;

public class FsmMooreFactory  extends FsmAbstractFactory {
	
	public FsmMooreFactory() {
		super("FSMMoore");
	}

	@Override
	public AttributeSet createAttributeSet() {
		return new FsmAttributes(FsmAttributes.FSM_MOORE);
	}
}
