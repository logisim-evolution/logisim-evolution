package com.cburch.logisim.statemachine.bdd

import com.cburch.logisim.statemachine.fSMDSL.util.FSMDSLSwitch
import org.eclipse.emf.common.util.EList
import com.cburch.logisim.statemachine.fSMDSL.InputPort
import com.cburch.logisim.statemachine.fSMDSL.BoolExpr
import org.eclipse.emf.common.util.BasicEList
import com.cburch.logisim.statemachine.fSMDSL.*

class CollectFlags extends FSMDSLSwitch<Object> {

	EList<InputPort> list

	def EList<InputPort> collect(BoolExpr bexp) {
		list = new BasicEList<InputPort>()
		doSwitch(bexp)
		return list
	}

	override Object caseAndExpr(AndExpr object) {
		for (BoolExpr bexp : object.getArgs()) {
			doSwitch(bexp)
		}
		return super.caseAndExpr(object)
	}

	override Object caseOrExpr(OrExpr object) {
		for (BoolExpr bexp : object.getArgs()) {
			doSwitch(bexp)
		}
		return super.caseOrExpr(object)
	}

	override Object caseNotExpr(NotExpr object) {
		doSwitch(object.getArgs().get(0))
		return super.caseNotExpr(object)
	}

	override Object casePortRef(PortRef object) {
		if(!list.contains(object.getPort())) list.add((object.getPort() as InputPort))
		return super.casePortRef(object)
	}
}
