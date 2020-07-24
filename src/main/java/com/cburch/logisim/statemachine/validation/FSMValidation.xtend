package com.cburch.logisim.statemachine.validation

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
import com.cburch.logisim.statemachine.editor.editpanels.FSMCommandListEditPanel
import com.cburch.logisim.statemachine.bdd.BDDOptimizer
import javax.swing.JOptionPane
import java.util.HashSet
import java.util.List
import java.util.ArrayList
import java.util.HashMap
import javax.management.RuntimeErrorException
import com.cburch.logisim.statemachine.fSMDSL.BoolExpr
import org.eclipse.emf.common.util.EList
import com.cburch.logisim.statemachine.bdd.BitWidthAnalyzer
import com.cburch.logisim.statemachine.editor.view.FSMCustomFactory
import com.cburch.logisim.statemachine.fSMDSL.Transition
import java.util.regex.Pattern

class FSMValidation{

	FSM fsm
	
	HashSet<State> targets = new HashSet<State>();
	List<String> warnings= new ArrayList<String>()
	List<String> errors= new ArrayList<String>();
	BitWidthAnalyzer analyzer = new BitWidthAnalyzer

	static HashSet<String> keywords = new HashSet<String>(#["CLK","RST","EN","if", "then", "while", "for", "do", "end","begin", "entity", "component"]);

    private static final Pattern FQCN = Pattern.compile("(?:\\b[_a-zA-Z]|\\B\\$)[_$a-zA-Z0-9]*+");

 
	new(FSM fsm) {
		this.fsm=fsm;
	}
	
	public def getErrors() {errors}
	public def getWarnings() {warnings}

	public def dispatch validate(FSM e) {  
		if(e.start==null) {
			error("No initial state")
		}
		if(e.states.size==0) {
			error("The FSM has no states !")
		}
		if(e.out==0) {
			warning("The FSM has no output pins !")
		}
		if(e.in==0) {
			warning("The FSM has no input pins !")
		}
		val map = new HashMap<String, State>

		for(s : e.states) {
			if (map.containsKey(s.code)) {
				error('''The FSM has two states («s.name», «map.get(s.code).name») with the same encoding''')
			} else {
				map.put(s.code,s);
			}
		}
		
		checkNames();
		for(s : e.states) {
			validate(s)
			for(t: s.transition) {
				targets.add(t.dst);
			}
		}  
		for(s : e.states) {
			if (s!=fsm.start && !targets.contains(s)) {
				warning("State "+PrettyPrinter.pp(s)+" is not reachable from initial state "+PrettyPrinter.pp(e.start));
			}
		}
	}
	
	def public static isValidBinaryString(String s, int width) {
			s.matches("\"[0-1]+\"") && (s.length()==width+2);
	}

	def public static boolean isValidIdentifier(String identifier) {
		FQCN.matcher(identifier).matches()
    }
	def public static boolean isReservedKeyword(String identifier) {
		(keywords.contains(identifier)) 
    }
	def boolean validateIdentifier(String identifier) {
		if (!isValidIdentifier(identifier)) {
			error('''Ilegal identifier : «identifier»''');
		} else if (isReservedKeyword(identifier)) {
			error('''Reserved keyword : «identifier»''');
        }
    }
	
	def checkNames() {
	
		val nameMap = new HashMap<String, FSMElement>
		for(s : fsm.states) {
			validateIdentifier(s.name)
			if (nameMap.containsKey(s.name)) {
				error('''The FSM has two states with the same Label «s.name»''')
			} else {
				nameMap.put(s.name,s);
			}
		}
		for(s : fsm.in) {
			validateIdentifier(s.name)
			if (nameMap.containsKey(s.name)) {
				error('''The FSM has two elements using a same identifier («PrettyPrinter.pp(s)», «PrettyPrinter.pp(nameMap.get(s.name))») ''')
			} else {
				nameMap.put(s.name,s);
			}
		}
		for(s : fsm.out) {
			validateIdentifier(s.name)
			if (nameMap.containsKey(s.name)) {
				error('''The FSM has two elements using a same identifier («PrettyPrinter.pp(s)», «PrettyPrinter.pp(nameMap.get(s.name))») ''')
			} else {
				nameMap.put(s.name,s);
			}
		}
		
		
	}
	
	
	public def warning(String string) {
		warnings.add(string);
	}
	public def error(String string) {
		errors.add(string);
	}

	public def dispatch validate(FSMElement e) {
		
	}
	public def dispatch validate(CommandList cl) {
		for(c : cl.commands) {
			try {
				analyzer.computeBitwidth(c.value)
			} catch (RuntimeException e) {
				 error(e.message)
			}
			validateExpr(c.value,false)
			val optimizer = new BDDOptimizer(c.value);
			optimizer.simplify
			
			if (optimizer.isAlwaysFalse()) {
				warning("command "+PrettyPrinter.pp(c)+" is always evaluated to 0");
			}
			if (optimizer.isAlwaysTrue() && (!(c instanceof Constant) )) {
				warning("command "+PrettyPrinter.pp(c)+" is always evaluated to 1");
			}
		}
	}

	public def dispatch validate(Transition t) {
		val p = t.predicate
		if(t.predicate==null) {
			throw new RuntimeException("null Predicate");
		}
		
		validateExpr(t.predicate,true)
		if(!(t.predicate instanceof DefaultPredicate)) {
			try {
				val optimizer = new BDDOptimizer(p);
				optimizer.simplify
				if (optimizer.isAlwaysFalse()) {
					error("Transition  "+PrettyPrinter.pp(t)+" can never be taken (evaluated to 0)");
				}
				if (optimizer.isAlwaysTrue() && (!(t.predicate instanceof DefaultPredicate) )) {
					warning("Transition "+PrettyPrinter.pp(t)+" is always taken (evaluated to 1)");
				}		
			} catch(Exception e) {
				error("BDD analysis for "+PrettyPrinter.pp(t)+" failed : "+e.message+"\n"+e.stackTrace);
			}
		}
	}
	
	public def dispatch validate(State e) {
		var int i=0;
		var int j=0
		if (e.eContainer instanceof FSM) {
			val fsm = e.eContainer as FSM
			if(e.code.length!=(fsm.width+2)) {
				error("State "+PrettyPrinter.pp(e)+" code is not consistent with FSM configuration ("+fsm.width+" bits expected");
			}
		}
		if(e.transition.size==0) {
			warning("State "+PrettyPrinter.pp(e)+" has no output transition");
		}
		val nonDefaultTransitions = e.transition.filter[t|!(t.predicate instanceof DefaultPredicate)].toList
		if ((e.transition.size-nonDefaultTransitions.length)>1) {
			error("State "+PrettyPrinter.pp(e)+" has multiple default transitions");
		}
		for (a: nonDefaultTransitions) {
			validate(a)
			j=0;
			for (b: nonDefaultTransitions) {
				if(i<j) {
					val pa= a.predicate
					val pb= b.predicate
					val and = FSMCustomFactory.and(EcoreUtil.copy(pa),EcoreUtil.copy(pb)) 
					val optimizer = new BDDOptimizer(and);
					if (!optimizer.isAlwaysFalse()) {
						error("Transitions predicates "+PrettyPrinter.pp(pa)+" and "+PrettyPrinter.pp(pb)+" are not mutually exclusive");
					}
				}
				j+=1
			}
			i+=1
		}
		
	}

	public def dispatch  validateExpr(BoolExpr b, boolean predicate) {
		
	}

	public def  dispatch validateExpr(OrExpr b, boolean predicate) {
		b.args.forEach[a|validateExpr(a,predicate)]
	}
	
	public def  dispatch validateExpr(CmpExpr b, boolean predicate) {
		if (b.args.size!=2) error("Inconsistent number of arguments for "+PrettyPrinter.pp(b)+" ");
		b.args.forEach[a|validateExpr(a,predicate)]
	}
	
	

	public def dispatch validateExpr(AndExpr b, boolean predicate) {
		b.args.forEach[a|validateExpr(a,predicate)]
	}
	
	public def dispatch validateExpr(NotExpr b, boolean predicate) {
		b.args.forEach[a|validateExpr(a,predicate)]
	}
	
	public def dispatch validateExpr(Constant b, boolean predicate) {

	}
	
	public def dispatch validateExpr(PortRef b, boolean predicate) {
		if (b.range!=null) {
			if (b.range.ub>b.port.width-1) {
				error("Inconsistent range ["+b.range.ub+":"+b.range.lb+"] for port "+b.port.name+"["+(b.port.width-1)+":0]");
			}
			if (b.range.lb>b.port.width-1) {
				error("Inconsistent range ["+b.range.ub+":"+b.range.lb+"] for port "+b.port.name+"["+(b.port.width-1)+":0]");
			}
			if (b.range.lb>b.range.lb) {
				error("Inconsistent range ["+b.range.ub+":"+b.range.lb+"] ");
			}
		}
	}
	
	public def dispatch validateExpr(DefaultPredicate b, boolean predicate) {
		if (!predicate) {
			error("keyword \"default\" not allowed in command expressions, use \"0\" or \"1\" instead");
		}
	}

}