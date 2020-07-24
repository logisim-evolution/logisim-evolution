package com.cburch.logisim.statemachine.bdd;

import java.util.Iterator;
import java.util.stream.Stream;

import org.eclipse.emf.common.util.BasicEList;
import org.eclipse.emf.common.util.EList;

import com.cburch.logisim.statemachine.fSMDSL.FSM;
import com.cburch.logisim.statemachine.fSMDSL.State;
import com.cburch.logisim.statemachine.fSMDSL.Transition;



public class RemoveUnreachableStates {

	EList<State> reachList;
	FSM fsm;
	
	public RemoveUnreachableStates(FSM fsm) {
		reachList = new BasicEList<State>();
		this.fsm=fsm;
	}

	public void compute() {
		EList<State> removeList = new BasicEList<State>();

		visitSuccessors(fsm.getStart());

		for (State current : fsm.getStates()) {
			if(!reachList.contains(current)) {
				removeList.add(current);
			}
		}
		fsm.getStates().removeAll(removeList);

	}
	
	public void visitSuccessors(State state) {
		reachList.add(state);
		Stream<Transition> stream = state.getTransition().stream();
		Stream<State> map = stream.map((x)->(x.getDst()));
		for (Iterator<State> i = map.iterator(); i.hasNext();) {
			State next = (State) i.next();
			if (!reachList.contains(next))
				visitSuccessors(next);
		}
	}
}
