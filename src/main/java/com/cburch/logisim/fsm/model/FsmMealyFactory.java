package com.cburch.logisim.fsm.model;

import com.cburch.logisim.data.AttributeSet;

public class FsmMealyFactory extends FsmAbstractFactory {
	
	public FsmMealyFactory() {
		super("FSMMealy");
	}

	@Override
	public AttributeSet createAttributeSet() {
		return new FsmAttributes(FsmAttributes.FSM_MEALY);
	}
}
