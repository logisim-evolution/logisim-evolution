package com.cburch.logisim.fsm.model;

import java.util.ArrayList;

import com.cburch.logisim.data.Value;
import com.cburch.logisim.util.StringGetter;

public class FsmInputList {

	private ArrayList<FsmInput> inputs = new ArrayList<FsmInput>(); 
	private ArrayList<FsmInputListListener> listeners = new ArrayList<FsmInputListListener>();
	
	/* here the event handlers are defined */
	public void addListener(FsmInputListListener l) {
		listeners.add(l);
	}
	
	public void removeListener(FsmInputListListener l) {
		if (listeners.contains(l))
			listeners.remove(l);
	}
	
	private void fireEvent(int event) {
		fireEvent(event,null);
	}
	
	private void fireEvent(int event , Object data) {
		if (listeners.isEmpty())
			return;
		FsmInputListEvent e = new FsmInputListEvent(this,event,data);
		for (FsmInputListListener l : listeners)
			l.FsmInputListChanged(e);
	}
	
	/* here all other handlers are defined */
	public FsmInputList clone() {
		FsmInputList clone = new FsmInputList();
		for (FsmInput inp : inputs)
			clone.add(inp.Name());
		return clone;
	}
	
	public int size() {
		return inputs.size();
	}
	
	public StringGetter get(int index) {
		if ((index < 0)||(index>=inputs.size()))
			throw new IllegalArgumentException("Invalid index ("+index+") for get method");
		return inputs.get(index).Name();
	}
	
	public Value getValue(int index) {
		if ((index < 0)||(index>=inputs.size()))
			throw new IllegalArgumentException("Invalid index ("+index+") for getValue method");
		return inputs.get(index).getValue();
	}
	
	public void setValue(int index , Value val) {
		if ((index < 0)||(index>=inputs.size()))
			throw new IllegalArgumentException("Invalid index ("+index+") for getValue method");
		inputs.get(index).setValue(val);
	}
	
	public void add(StringGetter Name) {
		FsmInput inp = new FsmInput(Name);
		inputs.add(inp);
		fireEvent(FsmInputListEvent.ADD);
	}
	
	public void propagate() {
		boolean change = false;
		for (FsmInput i : inputs)
			change |= i.hasChanged();
		if (change)
			fireEvent(FsmInputListEvent.VALUE_CHANGED);
	}
	
}
