package com.cburch.logisim.statemachine.editor.view

import com.cburch.logisim.statemachine.editor.editpanels.FSMCommandListEditPanel
import com.cburch.logisim.statemachine.editor.editpanels.FSMEditPanel
import com.cburch.logisim.statemachine.editor.editpanels.FSMPortEditPanel
import com.cburch.logisim.statemachine.editor.editpanels.FSMStateEditPanel
import com.cburch.logisim.statemachine.editor.editpanels.FSMTransitionEditPanel
import com.cburch.logisim.statemachine.fSMDSL.CommandList
import com.cburch.logisim.statemachine.fSMDSL.FSM
import com.cburch.logisim.statemachine.fSMDSL.InputPort
import com.cburch.logisim.statemachine.fSMDSL.OutputPort
import com.cburch.logisim.statemachine.fSMDSL.State
import com.cburch.logisim.statemachine.fSMDSL.Transition
import org.eclipse.emf.ecore.EObject

class FSMEdit {

	def dispatch edit(EObject e) {
	}

	def dispatch edit(CommandList e) {
		// FIXME ref names
		val panel = new FSMCommandListEditPanel(e);
		panel.configure();
		System.out.println(e);
	}

	def dispatch edit(State e) {
		// FIXME ref names
		val panel = new FSMStateEditPanel(e);
		panel.configure();
		System.out.println(e);
	}

	def dispatch edit(FSM e) {
		// FIXME ref names
		val panel = new FSMEditPanel(e);
		panel.configure();
		System.out.println(e);
	}

	def dispatch edit(Transition e) {
		val panel = new FSMTransitionEditPanel(e);
		panel.configure();
		System.out.println(e);

	}

	def dispatch edit(InputPort e) {
		val panel = new FSMPortEditPanel(e);
		panel.configure();
		System.out.println(e);
	}

	def dispatch edit(OutputPort e) {
		val panel = new FSMPortEditPanel(e);
		panel.configure();
		System.out.println(e);
	}
}