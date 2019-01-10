package com.cburch.logisim.fsm.model;

import java.awt.Color;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.util.ArrayList;
import java.util.Iterator;

import com.cburch.logisim.data.AttributeOption;
import com.cburch.logisim.data.Bounds;
import com.cburch.logisim.data.Value;
import com.cburch.logisim.util.StringGetter;
import com.cburch.logisim.util.StringUtil;

public class FsmDataStructure {
	private class PinInfo {
		private String Name;
		private int NrOfBits;
		
		public PinInfo(String Name , int NrOfBits) {
			this.Name = Name;
			this.NrOfBits = NrOfBits;
		}
		
		public String getName() {
			return Name;
		}
		
		public int getNrOfBits() {
			return NrOfBits;
		}
		
		public PinInfo clone() {
			PinInfo Clone = new PinInfo(Name,NrOfBits);
			return Clone;
		}
	}
	
	private FsmInputList inputs = new FsmInputList();
	private ArrayList<PinInfo> outputs = new ArrayList<PinInfo>();
	private ArrayList<FsmState> states = new ArrayList<FsmState>();
	private ArrayList<FsmTransition> transitions = new ArrayList<FsmTransition>();
	private FsmState resetState = null;
	private AttributeOption FsmType = FsmAttributes.FSM_MEDVEDEV;
	private AttributeOption FsmCoding = FsmAttributes.FSM_BINARYCODED;
	private AttributeOption FsmReset = FsmAttributes.FSM_RESETSYNCHRON;
	private Bounds StateDiagramSize = Bounds.create(0, 0, 0, 0);
	private Bounds InputBlockSize = Bounds.create(0, 0, 0, 0);
	private Bounds OutputBlockSize = Bounds.create(0, 0, 0, 0);
	private Bounds MySize = Bounds.create(0, 0, 0, 0);
	private FontMetrics MyFontMetrics = null;
	private FsmInput Clock = new FsmInput(Strings.getter("FsmClock"));
	private FsmInput Reset = new FsmInput(Strings.getter("FsmReset"));
	private FsmAttributes parent = null;
	
	public FsmDataStructure(FsmAttributes p) {
		parent = p;
	}
	
	public FsmDataStructure(FsmAttributes p ,AttributeOption type) {
		parent = p;
		FsmType = type;
	}
	
	public FsmDataStructure clone(FsmAttributes p) {
		FsmDataStructure Copy = new FsmDataStructure(p,FsmType);
		Copy.SetCoding(FsmCoding);
		Copy.SetResetBehavior(FsmReset);
		Copy.SetFontMetrics(MyFontMetrics);
		Copy.SetInputPins(inputs.clone());
		for (int i = 0 ; i < outputs.size(); i++)
			Copy.AddOutputPin(outputs.get(i).clone());
		for (int i = 0 ; i < states.size(); i++)
			Copy.AddState(states.get(i).clone());
		if (resetState!= null)
			Copy.SetResetState(resetState.GetName());
		for (FsmTransition t : transitions) {
			FsmState source = Copy.GetState(t.getSourceState().GetName());
			FsmState dest = Copy.GetState(t.getNextState().GetName());
			if (t.IsUnconditional()) {
				if ((source!=null)&(dest!=null))
					Copy.AddUnconditionalTransition(source, dest);
			} else {
				// TODO: add clone for conditional transitions
			}
		}
		Copy.CalculateBounds();
		return Copy;
	}
	
	private void fireChange() {
		parent.setValue(FsmAttributes.FSMCONTENT_ATTR, this);
	}
	
	public void SetInputPins(FsmInputList Info) {
		inputs = Info;
	}
	
	public void AddInputPin(String Name) {
		inputs.add(StringUtil.constantGetter(Name));
		fireChange();
	}
	
	public boolean AddOutputPin(PinInfo Pin) {
		return AddOutputPin(Pin.getName(),Pin.NrOfBits);
	}
	
	public boolean AddOutputPin(String Name) {
		return AddOutputPin(Name , 1);
	}
	
	public boolean AddOutputPin(String Name , int NrOfBits) {
		for (int i = 0 ; i < outputs.size() ; i++)
			if (outputs.get(i).getName().toUpperCase().equals(Name.toUpperCase()))
				return false;
		outputs.add(new PinInfo(Name,NrOfBits));
		fireChange();
		return true;
	}
	
	public void SetFontMetrics(FontMetrics value) {
		if (MyFontMetrics == null) {
			MyFontMetrics = value;
			CalculateBounds();
			fireChange();
		}
	}
	
	public boolean HasFontMetrics() {
		return MyFontMetrics != null;
	}
	
	public boolean IsCompletelyDefined() {
		/* TODO: implement */
		return true;
	}
	
	public int NrOfInputs() {
		return inputs.size()+2;
	}
	
	public int NrOfOutputs() {
		return outputs.size();
	}
	
	public StringGetter getInputName(int index) {
		if (index < inputs.size())
			return inputs.get(index);
		else if (index == inputs.size()) {
			return Reset.Name();
		} else if (index == (inputs.size()+1)) {
			return Clock.Name();
		} else return null;
	}
	
	public StringGetter getOutputName(int index) {
		if (index < outputs.size()) 
			return StringUtil.constantGetter(outputs.get(index).getName());
		else return null;
	}
	
	public int getOutputBitWidth(int index) {
		if (index < outputs.size())
			return outputs.get(index).getNrOfBits();
		else return 1;
	}
	
	public AttributeOption GetType () {
		return FsmType;
	}
	
	public void SetType(AttributeOption value) {
		/* TODO: Update data structure */
		FsmType = value;
		fireChange();
	}
	
	public AttributeOption GetCoding() {
		return FsmCoding;
	}
	
	public void SetCoding(AttributeOption value) {
		/* TODO: Update data structure */
		FsmCoding = value;
		fireChange();
	}
	
	public AttributeOption GetResetBehavior() {
		return FsmReset;
	}
	
	public void SetResetBehavior(AttributeOption value) {
		FsmReset = value;
		fireChange();
	}
	
	public String getResetStateName() {
		if (resetState == null)
			return Strings.get("FsmStateUnknown");
		if (!states.contains(resetState)) {
			resetState = null;
			return Strings.get("FsmStateUnknown");
		}
		return states.get(states.indexOf(resetState)).GetName();
	}
	
	public int GetResetStateIndex() {
		if (resetState == null)
			return -1;
		if (!states.contains(resetState)) {
			resetState = null;
			return -1;
		}
		return states.indexOf(resetState);
	}
	
	public void SetResetState(String value) {
		resetState = null;
		for (int i = 0 ; i < states.size() & resetState == null ; i++) {
			if (states.get(i).GetName().equals(value)) {
				resetState = states.get(i);
				fireChange();
			}
		}
	}
	
	public ArrayList<String> GetStateNames() {
		ArrayList<String> ret = new ArrayList<String>();
		ret.add(Strings.get("FsmStateUnknown")); 
		for (int i = 0 ; i < states.size() ; i++)
			ret.add(states.get(i).GetName());
		return ret;
	}
	
	public void AddState(FsmState state) {
		/* TODO: Update data structure */
		if (state.GetLocation()==null)
			return;
		states.add(state);
		CalculateBounds();
		fireChange();
	}
	
	public boolean HasStates() {
		return !states.isEmpty();
	}
	
	public FsmState GetState(String Name) {
		for (FsmState s : states) {
			if (s.GetName().equals(Name))
				return s;
		}
		return null;
	}
	
	public void RemoveState() {
		/* TODO: Dummy Handle for testing REMOVE! */
		if (states.size() > 0) {
			FsmState state = states.get(0);
			state.fireEvent(FsmStateEvent.StateRemoved);
			if (resetState != null)
				if (resetState.equals(state))
					resetState = null;
			states.remove(state);
			Iterator<FsmTransition> iter = transitions.iterator();
			while (iter.hasNext()) {
				FsmTransition t = iter.next();
				if (t.ToBeRemoved())
					iter.remove();
			}
			CalculateBounds();
			fireChange();
		}
	}
	
	public void AddUnconditionalTransition(FsmState source , FsmState dest) {
		if (!states.contains(source) |!states.contains(dest))
			throw new IllegalArgumentException("States should be contained in data structure!");
		FsmTransition obj = new FsmTransition(source,dest);
		source.addFsmStateListener(obj);
		dest.addFsmStateListener(obj);
		transitions.add(obj);
		CalculateBounds();
		fireChange();
	}
	
	/* Here the simulation stuff is defined */
	public void setInputValue( int index , Value val) {
		if ((index >= NrOfInputs()) | (index < 0))
			return;
		if (index < inputs.size())
			inputs.setValue(index, val);
		if (index == inputs.size())
			Reset.setValue(val);
		if (index == (inputs.size()+1))
			Clock.setValue(val);
	}
	
	public void propagate() {
		inputs.propagate();
		if (FsmReset == FsmAttributes.FSM_RESETASYNCHRON & Reset.getValue() == Value.TRUE) {
			if (resetState != null) {
				FsmState current = getCurrentState();
				if (current!=null)
					current.RemoveAsCurrentState();
				resetState.SetAsCurrentState();
				return;
			}
		}
		if (Clock.PosEdge())
			tick();
	}
	
	private FsmState getCurrentState() {
		for (FsmState s : states)
			if (s.IsCurrentState())
				return s;
		return null;
	}
	
	private FsmState getNextState() {
		for (FsmTransition t : transitions)
			if (t.IsActive())
				return t.getNextState();
		return null;
	}
	
	public void tick() {
		FsmState current = getCurrentState();
		FsmState next = getNextState();
		if (FsmReset == FsmAttributes.FSM_RESETSYNCHRON & Reset.getValue() == Value.TRUE) {
			if (resetState != null) {
				if (current != null)
					current.RemoveAsCurrentState();
				resetState.SetAsCurrentState();
				fireChange();
				return;
			}
		}
		boolean change = false;
		if (current != null) {
			if (next != null) {
				if (!current.equals(next)) {
					current.RemoveAsCurrentState();
					next.SetAsCurrentState();
					change = true;
				}
			} else {
				current.RemoveAsCurrentState();
				change = true;
			}
		} else if (next != null) {
			next.SetAsCurrentState();
			change = true;
		}
		if (change)
			fireChange();
	}
	
	
	/* Here some graphics stuff is defined */
	
	public void CalculateBounds() {
		int minx = (states.size() == 0) ? 0 : Integer.MAX_VALUE;
		int miny = (states.size() == 0) ? 0 : Integer.MAX_VALUE;
		int maxx = (states.size() == 0) ? 60 : Integer.MIN_VALUE;
		int maxy = (states.size() == 0) ? 40 : Integer.MIN_VALUE;
		for (int i = 0 ; i < states.size() ; i++) {
			Bounds bds = states.get(i).getSize(MyFontMetrics);
			if (bds.getX() < minx)
				minx = bds.getX();
			if ((bds.getX()+bds.getWidth()) > maxx)
				maxx = bds.getX()+bds.getWidth();
			if (bds.getY() < miny)
				miny = bds.getY();
			if ((bds.getY()+bds.getHeight()) > maxy) 
				maxy = bds.getY()+bds.getHeight();
		}
		for (FsmTransition t : transitions) {
			Bounds bds = t.getBounds(MyFontMetrics);
			if (bds.getX() < minx)
				minx = bds.getX();
			if ((bds.getX()+bds.getWidth()) > maxx)
				maxx = bds.getX()+bds.getWidth();
			if (bds.getY() < miny)
				miny = bds.getY();
			if ((bds.getY()+bds.getHeight()) > maxy) 
				maxy = bds.getY()+bds.getHeight();
		}
		StateDiagramSize = Bounds.create(minx, miny, maxx-minx, maxy-miny);
		minx = miny = maxx = 0;
		maxy = (NrOfInputs()+1)*FsmAbstractFactory.PinDistance;
		for (int i = 0 ; i < NrOfInputs() ; i++) {
			int size = (MyFontMetrics==null) ? getInputName(i).toString().length()*10 : 
				MyFontMetrics.stringWidth(getInputName(i).toString());
			if (size > maxx)
				maxx = size;
		}
		maxx += 20;
		maxx /= 10;
		maxx *= 10;
		InputBlockSize = Bounds.create(0, 0, maxx, maxy);
		maxy = (NrOfOutputs()+1)*FsmAbstractFactory.PinDistance;
		maxx = 0;
		for (int i = 0 ; i < NrOfOutputs() ; i++) {
			int size = (MyFontMetrics==null) ? getOutputName(i).toString().length()*10 :
				MyFontMetrics.stringWidth(getOutputName(i).toString());
			if (size > maxx)
				maxx = size;
		}
		maxx += 20;
		maxx /= 10;
		maxx *= 10;
		OutputBlockSize = Bounds.create(0,0,maxx,maxy);
		int height = Math.max(Math.max(InputBlockSize.getHeight(), OutputBlockSize.getHeight()), StateDiagramSize.getHeight()+10);
		MySize = Bounds.create(0,0,
				InputBlockSize.getWidth()+StateDiagramSize.getWidth()+10+OutputBlockSize.getWidth(),
				height);
	}
	
	public Bounds GetBounds() {
		if (MyFontMetrics == null)
			return Bounds.create(0, 0, 160, 60);
		else
			return MySize;
	}
	
	public void DrawStateDiagram(Graphics g , int x , int y) {
		Color col = g.getColor();
		g.setColor(Color.YELLOW);
		g.fillRoundRect(x+InputBlockSize.getWidth()-3,
				        y-3, 
				        StateDiagramSize.getWidth()+6,
				        StateDiagramSize.getHeight()+6, 10, 10);
		g.setColor(Color.BLUE);
		g.drawRoundRect(x+InputBlockSize.getWidth()-3,
		        y-3, 
		        StateDiagramSize.getWidth()+6,
		        StateDiagramSize.getHeight()+6, 10, 10);
		g.setColor(col);
		int xoff = x+InputBlockSize.getWidth()-StateDiagramSize.getX();
		int yoff = y-StateDiagramSize.getY();
		for (FsmState s : states)
			s.DrawState(g, xoff , yoff , !IsCompletelyDefined());
		for (FsmTransition t : transitions)
			t.DrawTransition(g, xoff, yoff, !IsCompletelyDefined());
	}
	
}
