package com.cburch.logisim.statemachine.editor.view

import java.awt.Point
import org.eclipse.emf.ecore.EObject
import com.cburch.logisim.statemachine.fSMDSL.*
import java.awt.Color
import java.awt.RenderingHints
import java.awt.Graphics2D
import java.awt.geom.RoundRectangle2D
import com.cburch.logisim.statemachine.PrettyPrinter
 
import org.eclipse.emf.ecore.util.EcoreUtil
import com.cburch.logisim.statemachine.fSMDSL.FSM

class FSMRemoveElement{

	FSM fsm
	
	new(FSM fsm) {
		this.fsm=fsm;
	}
	
	def dispatch remove(EObject e) {

	}


	def dispatch remove(State s) {
		fsm.states.remove(s);
		val deadTransitions = fsm.eAllContents.filter(Transition).filter[t|t.dst==s].toList
		for(t : deadTransitions) {
			remove(t)
		}
	}

	
	def dispatch remove(Transition t) {
		(t.eContainer as State).transition.remove(t);
		t.src=null;
		t.dst=null;
	}

	def dispatch replaceByZero(PortRef pr) {
		val Constant cst = FSMDSLFactory.eINSTANCE.createConstant;
		cst.value = "0";
		if (pr.port.width!=1) {
			throw new UnsupportedOperationException("Support for port width>1 not yet available");
		} 
		val BoolExpr roor = (pr.eContainer as BoolExpr)
		switch(roor) {
			AndExpr:{
				roor.args.replaceAll([x| if(x==pr) cst else x])
			}
			OrExpr:{
				roor.args.replaceAll([x| if(x==pr) cst else x])
				
			}
			NotExpr:{
				roor.args.replaceAll([x| if(x==pr) cst else x])
			}
		}
	}

	def dispatch remove(InputPort e) {
		fsm.in.remove(e);
		val deadRefs= fsm.eAllContents.filter(PortRef).filter[c|c.port.name==e].toList
		for(r : deadRefs) {
			replaceByZero(r)
		}

	}

	def dispatch remove(Command c) {
		((c.eContainer) as CommandList).commands.remove(c)
	}

	def dispatch remove(OutputPort op) {
		fsm.out.remove(op);
		val deadCommands= fsm.eAllContents.filter(Command).filter[c|c.name==op].toList
		for(t : deadCommands) {
			remove(t)
		}
		
	}
}