package com.cburch.logisim.statemachine

import com.cburch.logisim.statemachine.fSMDSL.BoolExpr
import com.cburch.logisim.statemachine.fSMDSL.*
import org.eclipse.emf.ecore.EObject

class PrettyPrinter {
	
	
	def static dispatch String pp(FSMElement b) {
		""
	}
	def static dispatch String pp(State b) {
		b.name+"["+b.code+"]"
	}
	def static dispatch String pp(Transition b) {
		if(b.src!=null) pp(b.src)+"->"+pp(b.dst)+" when "+pp(b.predicate) else "goto"+pp(b.dst)+" when "+pp(b.predicate)
	}
	def static dispatch String pp(CommandList b) {
		b.commands.map[c|pp(c)].reduce[p1, p2|p1+';'+p1]
	}


	def static dispatch String pp(BoolExpr b) {
		""
	}
	def static dispatch String pp(Command c) {
		c.name.name+"="+pp(c.value);
	}
	
	def static dispatch pp(OrExpr b) {
//		if(b.args.size<=1) {
//			throw new RuntimeException("Error in "+b)
//		}
		'''(«FOR i:b.args SEPARATOR "+"»«pp(i)»«ENDFOR»)'''.toString
	}

	def static dispatch pp(ConcatExpr b) {
		'''{«FOR i:b.args SEPARATOR ","»«pp(i)»«ENDFOR»}'''.toString
	}

	def static dispatch pp(AndExpr b) {
		'''(«FOR i:b.args SEPARATOR "."»«pp(i)»«ENDFOR»)'''.toString
	}

	def static dispatch pp(CmpExpr b) {
		switch(b.op) {
			case "==" : {
				"("+pp(b.args.get(0))+"=="+pp(b.args.get(1))+")" 
			}
			case "/=" : {
				"("+pp(b.args.get(0))+"/="+pp(b.args.get(1))+")" 
			}
			default : {
				"????"
			}
			
		}
	}

	def static dispatch pp(NotExpr b) {
		"(/"+pp(b.args.get(0))+")";
		
	}
	def static dispatch pp(PortRef b) {
		if(b.range!=null) {
			if(b.range.ub!=-1)
				b.port.name + "["+b.range.ub+":"+b.range.lb+"]"
			else
				b.port.name + "["+b.range.lb+"]"
		} else {
			b.port.name
		}
	}
	def static  dispatch pp(ConstRef b) {
		"#"+b.const.name
	}
	def static dispatch pp(DefaultPredicate b) {
		"default"
	}
	def static dispatch pp(Constant b) {
		b.value
	}
	def static dispatch pp(ConstantDef b) {
		'''define «b.name»=«pp(b.value)»'''.toString
	}
}