package com.cburch.logisim.statemachine.parser

import com.cburch.logisim.statemachine.fSMDSL.BoolExpr
import com.cburch.logisim.statemachine.fSMDSL.*
import org.eclipse.emf.ecore.EObject

class FSMTextSave {
	
	
	def static dispatch String pp(FSM f) {
		'''
		state_machine «f.name» «pp(f.layout)»{
			«FOR s: f.constants»«pp(s)»«ENDFOR»
			in «FOR s: f.in»«pp(s)»«ENDFOR»
			out «FOR s: f.out»«pp(s)»«ENDFOR»
			codeWidth = «f.width»;
			reset = «f.start.name»;
			
			«FOR s: f.states»«pp(s)»«ENDFOR»
		}
		'''
	}
	def static dispatch String pp(State b) {
		'''
		state «b.name»[«b.code»]:
		 	«IF b.layout!==null»«pp(b.layout)»«ENDIF»
			«IF b.commandList!==null && b.commandList.commands.size>0»set «FOR c: b.commandList.commands»«pp(c)»«ENDFOR»  «pp(b.layout)»«ENDIF»	
			«FOR t: b.transition.filter[t|!(t.predicate instanceof DefaultPredicate)]»«pp(t)»«ENDFOR»	
			«FOR t: b.transition.filter[t|(t.predicate instanceof DefaultPredicate)]»«pp(t)»«ENDFOR»	
		'''
	}

	def static dispatch String pp(Port b) {
		'''«b.name»[«b.width»] «IF b.layout!==null»  «pp(b.layout)»«ENDIF»;'''
	}
	def static dispatch String pp(LayoutInfo l) {
		'''@«layout(l)»'''
	}

	def static String layout(LayoutInfo l) {
		if (l!=null)
		'''[«l.x»,«l.y»,«l.width»,«l.height»]'''
		else
		'''[0,0,0,0]'''
		
	}
	def static dispatch String pp(Transition b) {
		'''goto «b.dst.name»  when «pp(b.predicate)» «IF b.layout!==null»  «pp(b.layout)»«ENDIF»; '''+"\n"
	}

	def static dispatch String pp(BoolExpr b) {
		""
	}
	def static dispatch String pp(Command c) {
		c.name.name+"="+pp(c.value)+";";
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
		'''define «b.name»=«pp(b.value)» ;'''.toString+"\n"
	}
}