package com.cburch.logisim.statemachine.editor.view

import com.cburch.logisim.statemachine.fSMDSL.*
import com.cburch.logisim.statemachine.fSMDSL.FSMDSLFactory
import com.cburch.logisim.statemachine.fSMDSL.BoolExpr
import java.util.List

class FSMCustomFactory {
	
	public static FSMDSLFactory factory = FSMDSLFactory.eINSTANCE
	
	public static int CMD_OFFSETX = 45;
	public static int CMD_OFFSETY = 15;
	public static int CMD_WIDTH = 30;
	public static int CMD_HEIGHT= 20;

	public static int PRED_WIDTH = 20;
	public static int PRED_HEIGHT= 15;

	public static int PORT_HEIGHT= 30;
	public static int PORT_WIDTH = 20;

	public static int FSM_HEIGHT= 500;
	public static int FSM_WIDTH = 500;

	public static int STATE_RADIUS = 30;


	def static state(String label, String code, int x, int y) {
		val s = factory.createState;
		s.name=label
		s.code=code
		s.commandList=factory.createCommandList
		s.commandList.layout = factory.createLayoutInfo
		s.commandList.layout.x = x+CMD_OFFSETX;
		s.commandList.layout.y = y+CMD_OFFSETY;
		s.commandList.layout.width = CMD_WIDTH;
		s.commandList.layout.height = CMD_HEIGHT;
		s.layout = factory.createLayoutInfo
		s.layout.x=x
		s.layout.y=y
		s.layout.width=STATE_RADIUS
		s.layout.height=STATE_RADIUS
		
		s
	}
	def static fsm(String label) {
		val s = factory.createFSM;
		s.name=label
		s.layout = factory.createLayoutInfo
		s.layout.x=15
		s.layout.y=15
		s.layout.width=FSM_WIDTH
		s.layout.height=FSM_WIDTH
		s
	}



	
	def static transition(State src, State dst, int x, int y) {
		val factory = FSMDSLFactory.eINSTANCE
		val t = factory.createTransition;
		t.dst=dst
		t.src=src
		t.layout = factory.createLayoutInfo
		t.layout.x=x
		t.layout.y=y
		t.layout.width=PRED_WIDTH
		t.layout.height=PRED_HEIGHT
		src.transition.add(t);
		t.predicate=defaultPred()
		t
	}
	
	def static defaultPred() {
		val factory = FSMDSLFactory.eINSTANCE
		val t = factory.createDefaultPredicate
		t
	}

	
	def static inport(String label, int width, int x, int y) {
		val factory = FSMDSLFactory.eINSTANCE
		val s = factory.createInputPort;
		s.name=label
		s.width=width
		s.layout = factory.createLayoutInfo
		s.layout.x=x
		s.layout.y=y
		s.layout.width=PORT_WIDTH
		s.layout.height=PORT_HEIGHT
		s
	}

	def static outport(String label, int width, int x, int y) {
		val factory = FSMDSLFactory.eINSTANCE
		val s = factory.createOutputPort;
		s.name=label
		s.width=width
		s.layout = factory.createLayoutInfo
		s.layout.width=PORT_WIDTH
		s.layout.height=PORT_HEIGHT
		s.layout.x=x
		s.layout.y=y
		s
	}
	
	def static pref(Port p) {
		val factory = FSMDSLFactory.eINSTANCE
		val s = factory.createPortRef;
		s.port=p
		s
	}

	def static pref(Port p, int lb, int ub) {
		val factory = FSMDSLFactory.eINSTANCE
		val s = factory.createPortRef;
	
		s.range= factory.createRange;
		s.range.lb=lb
		s.range.ub=ub
		
		s.port=p
		s
	}

	def static negpref(Port p) {
		not(pref(p))
	}

	def static and(BoolExpr a, BoolExpr b) {
		val factory = FSMDSLFactory.eINSTANCE
		val s = factory.createAndExpr;
		s.args+=a
		s.args+=b
		s
	}
	def static and(List<BoolExpr> list) {
		val factory = FSMDSLFactory.eINSTANCE
		val s = factory.createAndExpr;
		for (BoolExpr bexp : list) {
			if (bexp != null)
				s.getArgs().add(bexp);
		}
		s
	}
	def static and(BoolExpr[] list) {
		val factory = FSMDSLFactory.eINSTANCE
		val s = factory.createAndExpr;
		for (BoolExpr bexp : list) {
			if (bexp != null)
				s.getArgs().add(bexp);
		}
		s
	}
	def static or(BoolExpr a, BoolExpr b) {
		val factory = FSMDSLFactory.eINSTANCE
		val s = factory.createOrExpr;
		s.args+=a
		s.args+=b
		s
	}
	def static or(List<BoolExpr> list) {
		val factory = FSMDSLFactory.eINSTANCE
		val s = factory.createOrExpr;
		for (BoolExpr bexp : list) {
			if (bexp != null)
				s.getArgs().add(bexp);
		}
		s
	}
	def static or(BoolExpr[] list) {
		val factory = FSMDSLFactory.eINSTANCE
		val s = factory.createOrExpr;
		for (BoolExpr bexp : list) {
			if (bexp != null)
				s.getArgs().add(bexp);
		}
		s
	}
	def static not(BoolExpr args) {
		val factory = FSMDSLFactory.eINSTANCE
		val s = factory.createNotExpr;
		s.args+=args
		s
	}

	def static cst(String v) {
		val factory = FSMDSLFactory.eINSTANCE
		val p =factory.createConstant
		p.value=v;
		p
	}
	
	def static cst(boolean b) {
		if(b) cst("\"1\"") else cst("\"0\"")
	}
	


}