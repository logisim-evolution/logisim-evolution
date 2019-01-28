package com.cburch.logisim.statemachine

import com.cburch.logisim.statemachine.fSMDSL.FSMElement
import java.util.ArrayList
import com.cburch.logisim.statemachine.fSMDSL.FSM
import com.cburch.logisim.statemachine.fSMDSL.State
import com.cburch.logisim.statemachine.fSMDSL.Transition

class FSMUtils {
	
	public def getAllFSMElements(FSM fsm) {
		var l = new ArrayList<FSMElement>();
		l+=fsm
		l+=fsm.in
		l+=fsm.out
		l+=fsm.states
		for (State s : fsm.getStates()) {
			l+=s.getCommandList()
			for (Transition t : s.getTransition()) {
				l+=t
			}
		}
	}
}