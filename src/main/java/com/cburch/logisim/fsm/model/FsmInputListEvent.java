package com.cburch.logisim.fsm.model;

public class FsmInputListEvent {
	public static final int ALL_REPLACED = 0;
	public static final int ADD = 1;
	public static final int REMOVE = 2;
	public static final int MOVE = 3;
	public static final int REPLACE = 4;
	public static final int VALUE_CHANGED = 5;
	
	private FsmInputList source;
	private int reason;
	private Object data;
	
	public FsmInputListEvent(FsmInputList l, int event, Object data) {
		source = l;
		reason = event;
		this.data = data;
	}
	
	public FsmInputList getSource() {
		return source;
	}
	
	public int getReason() {
		return reason;
	}
	
	public Object getData() {
		return data;
	}

}
