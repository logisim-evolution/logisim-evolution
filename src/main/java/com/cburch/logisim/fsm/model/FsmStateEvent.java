package com.cburch.logisim.fsm.model;

public class FsmStateEvent {
	
	public static int StateAdded = 0;
	public static int StateModified = 1;
	public static int StateRemoved = 2;
	public static int StateIsCurrentState = 3;
	public static int StateIsNoLongerCurrentState = 4;
	
	private FsmState source;
	private int reason;
	private Object data;
	
	public FsmStateEvent(FsmState state , int event, Object data) {
		source = state;
		reason = event;
		this.data = data;
	}
	
	public FsmState getSource() {
		return source;
	}
	
	public int getReason() {
		return reason;
	}
	
	public Object getData() {
		return data;
	}

}
