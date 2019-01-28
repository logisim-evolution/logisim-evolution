package com.cburch.logisim.statemachine.simulator

import com.cburch.logisim.statemachine.PrettyPrinter
import com.cburch.logisim.statemachine.fSMDSL.*
import com.cburch.logisim.statemachine.fSMDSL.Constant
import com.cburch.logisim.statemachine.fSMDSL.FSM
import com.cburch.logisim.statemachine.fSMDSL.InputPort
import java.util.ArrayList
import java.util.HashMap
import java.util.List

import com.cburch.logisim.instance.InstanceData

class FSMSimulator extends ClockState implements InstanceData {
	
	FSM fsm;
	
	State current  
	HashMap<Port,String> inputs;
	HashMap<Port,String> outputs;
	
	static final char ONE_C = '1';
	static final char ZERO_C = '0';
	
	
	new(FSM fsm) {
		if (fsm!=null) {
			this.fsm=fsm;
			inputs =  new HashMap<Port,String>();
			outputs =  new HashMap<Port,String>();
			current=fsm.start
			refreshInputPorts
			restoreOutputPorts
		} else {
			throw new RuntimeException("Cannot simulate null FSM");
		}
	}
	


	def setCurrentState(State s) {
		current = s;
	}
	
	def getCurrentState() {
		current ;
	}
	
	def quote(String s) {
		"\""+s+ "\"";
	}

	def zeros(int width) {
		var res = "";
		for (int i:0..width-1) {
			res+="0"
		}
		quote(res)
	}
	
	def ones(int width) {
		var res = "";
		for (int i:0..width-1) {
			res+="1"
		}
		quote(res)
	}
	
	static final String ZERO = "\"0\"";
	static final String ONE = "\"1\"";
	
	final boolean VERBOSE = true
	
	def isTrue(String s) {
		s.equals(ONE);
	}
	
	def public refreshInputPorts() {
		var HashMap<Port,String> newInputs = new HashMap<Port,String>();
		for (newIp : fsm.in) {
			newInputs.put(newIp,zeros(newIp.width))
			for (oldIp : inputs.keySet) {
				val boolean nameEqu= newIp.name.equals(oldIp.name)
				val boolean witdhEqu = (newIp.width == oldIp.width)
				if (nameEqu && witdhEqu) {
					newInputs.put(newIp,inputs.get(oldIp))
				}
			}
			debug('''«newIp.name»:«newIp.hashCode» -> «newInputs.get(newIp)»''')
		}
		inputs=newInputs;
	}
	def public restoreOutputPorts() {
		var HashMap<Port,String> newOutputs = new HashMap<Port,String>();
		for (newOp : fsm.out) {
			newOutputs.put(newOp,zeros(newOp.width))
			for (oldIp : outputs.keySet) {
				val boolean nameEqu= newOp.name.equals(oldIp.name)
				val boolean witdhEqu = (newOp.width == oldIp.width)
				if (nameEqu && witdhEqu) {
					newOutputs.put(newOp,outputs.get(oldIp))
				}
			}
			debug('''«newOp.name»:«newOp.hashCode» -> «newOutputs.get(newOp)»''')
		}
		outputs=newOutputs;
	}
	
	def public resetOutputPorts() {
		var HashMap<Port,String> newOutputs = new HashMap<Port,String>();
		for (newOp : fsm.out) {
			newOutputs.put(newOp,zeros(newOp.width))
			debug('''Reset «newOp.name»:«newOp.hashCode» -> «newOutputs.get(newOp)»''')
		}
		outputs=newOutputs;
	}
	
	def getFSM() {
		fsm
	}
	def reset() {
		current=fsm.start
		refreshInputPorts
		restoreOutputPorts
	}

	def getOutput(int i) {
		try {
			val op = fsm.out.get(i)
			printIOMap
			if(!outputs.containsKey(op)) {
				
				throw new RuntimeException("output  "+op.name+":"+op.hashCode +" at "+i+" is not a known output in fsm "+fsm.name+ " "+fsm.out.map[p| "Port["+fsm.out.indexOf(p)+"]="+p.name+":"+p.hashCode])
			}
			return outputs.get(op);
		} catch (Exception e) {
			throw new RuntimeException(e.message)
		}
	}

	def updateInput(InputPort ip,String b) {
		debug("-> setting "+ip.name+":"+ip.hashCode+" to "+b)
		printIOMap
		if (inputs.containsKey(ip)) {
			if((b.length-2)!=ip.width) throw new RuntimeException("port datawidth mismatch"+ip.name+"["+ip.width+"]  in "+fsm.name)
			inputs.put(ip,b)
		} else {
			throw new RuntimeException("Unregistered input port "+ip.name+":"+ip.hashCode +"  in "+fsm.name)
		}
	}
	
	def updateState() { 
		
		
		debug("FSM "+fsm.name+" current state "+current.name)
		if(fsm.in.size!=inputs.keySet.size) {
			throw new RuntimeException("inconsistent state for input port mapping ")
		}
		if(fsm.out.size!=outputs.keySet.size) {
			throw new RuntimeException("inconsistent state for output port mapping ")
		}
		for (Port e : inputs.keySet) {
			debug("\t- In "+e.name+"=>"+inputs.get(e))
		}  
		for (Port e : outputs.keySet) {
			debug("\t- Out "+e.name+"=>"+outputs.get(e))
		}  
		
		var State defaultDst =null;
		var State nextDst =null;
		for (Transition t : current.transition) {
			debug("\tTransition= "+PrettyPrinter.pp(t))
			if(t.predicate instanceof DefaultPredicate) {
				defaultDst = t.dst
			} else {
				val res = (eval(t.predicate))
				debug("\t\t"+PrettyPrinter.pp(t.predicate)+"="+res+"" );
				if (isTrue(res)) {
					nextDst= t.dst
					debug("=> transition fired : next state is "+nextDst.name)
				} else {
					debug("=> transition not fired")
				}
			}
		}
		if (nextDst!=null) {
			current = nextDst
		} else if (defaultDst!=null) {
			current = defaultDst
			debug("\t\tDefault transition fired "+defaultDst.name)
		}		
		return current;
	}
	
	def updateCommands() {
		resetOutputPorts
		for (Command c : current.commandList.commands) {
			val res= eval(c.value)
			outputs.replace(c.getName(), res);
			debug("\tSet "+c.name.name+":"+ c.name.hashCode+" to "+res)
		}  
	}

	def dispatch String eval(BoolExpr exp) {
		 throw new RuntimeException("Unsupported operation"+PrettyPrinter.pp(exp));
	}

	def dispatch String eval(ConcatExpr exp) {
		val r  = new StringBuffer()
		for(arg : exp.args) {
			r.append(unquote(eval(arg)))
		}
		quote(r.toString)
	}

	def unquote(String s) {
		s.substring(1,s.length-1)		
	}


	def dispatch String eval(DefaultPredicate exp) {
		ZERO
	}
	
	
	def dispatch String eval(CmpExpr b) {
		if (b.args.size!=2) {
		 		throw new RuntimeException("Inconsistent arity for expression "+PrettyPrinter.pp(b));
		}
		val opA = eval(b.args.get(0))
		val opB = eval(b.args.get(1))
		val equ = opA.equals(opB)
		var res = ""
		switch(b.op) {
			case "==" :{
				if (equ) {
					res = quote("1")
				} else {
					res= quote("0")
				}
			}
			case "/=" :{
				if (equ) {
					res= quote("0")
				} else {
					res= quote("1")
				}
			}
			default : {
				throw new RuntimeException("Inconsistent operator "+b.op+" for expression "+PrettyPrinter.pp(b));
			}
		}
		debug('''eval(«PrettyPrinter.pp(b)»)=«res»''')
		res
	}
	
	def debug(String string) {
		if (VERBOSE) println(string);
	}


	def dispatch String eval(Constant exp) {
		exp.value
	}

	def setCharAt(String s, char c, int i) {
		val StringBuilder myName = new StringBuilder(s);
		myName.setCharAt(i, c);
		//println('''\t«s»->«myName.toString»''')
		myName.toString;
	}	

	def dispatch String eval(OrExpr b) {
		var width = -1;
		var List<String> l= new ArrayList<String>(); 
		var andRes = ""
		for(arg : b.args) {
			val res = unquote((eval(arg)))
			l+= res
			if (width==-1) {
				width = res.length
			}
		}		
		
		andRes = unquote(zeros(width));
		
		for(r : l) {
			
		 	 if (width!=r.length) {
		 		throw new RuntimeException("Inconsistent width in expression "+PrettyPrinter.pp(b));
		 	}
		 	for(i:0..width-1) {
		 		val opA = andRes.charAt(i);
		 		val opB = r.charAt(i);
		 		val and1 = or(opA,opB)
		 		andRes= andRes.setCharAt(and1,i)
				//println('''\nres[«i»]=«and1»=«andRes.charAt(i)» ''')
		 	}
		}
		val res  = quote(andRes)
		res
	}
	def dispatch String eval(AndExpr b) {
		var width = -1;
		var List<String> l= new ArrayList<String>(); 
		var andRes = ""
		for(arg : b.args) {
			val res = unquote((eval(arg)))
			l+= res
			if (width==-1) {
				width = res.length
			}
		}		
		
		andRes = unquote(ones(width));
		
		for(r : l) {
			
		 	 if (width!=r.length) {
		 		throw new RuntimeException("Inconsistent width in expression "+PrettyPrinter.pp(b));
		 	}
		 	for(i:0..width-1) {
		 		val opA = andRes.charAt(i);
		 		val opB = r.charAt(i);
		 		val and1 = and(opA,opB)
		 		andRes= andRes.setCharAt(and1,i)
				//println('''\nres[«i»]=«and1»=«andRes.charAt(i)» ''')
		 	}
		}
		val res  = quote(andRes)
		res
	}
	
	def dispatch String eval(NotExpr b) {
		if (b.args.size!=1) {
		 		throw new RuntimeException("Inconsistent arity for expression "+PrettyPrinter.pp(b));
		 }
		var width = -1;
		var List<String> l= new ArrayList<String>(); 
		var notExpr = ""
		
		var res = eval(b.args.get(0))
		res= res.replace('0', '@');
		res= res.replace('1', '0');
		res= res.replace('@', '1');

		debug("eval("+PrettyPrinter.pp(b)+")="+res)
		res
	}

	def char or(char c, char b) {
		val in = ""+c+b
		var char res = '0'
		switch(in) {
			case "00" : { res= '0' }
			case "01" : { res= '1' }
			case "10" : { res= '1' }
			case "11" : { res= '1' }
			default  : { 
				throw new RuntimeException("Unsupported value \""+in+"\","+b+" only '0' or '1' supported");
			}
		}
		debug('''eval(or(«b»,«c»))=«res»''');
		res
	}
	
	def char and(char c, char b) {
		val in = ""+c+b
		var char res = '0'
		switch(in) {
			case "00" : { res= '0' }
			case "01" : { res= '0' }
			case "10" : { res= '0' }
			case "11" : { res= '1' }
			default  : { 
				throw new RuntimeException("Unsupported value "+c+","+b+" only '0' or '1' supported");
			}
		}
		debug('''eval(and(«b»,«c»))=«res»''');
		res
	}

	def char not(char c) {
		var char res = '0'
		switch(c) { 
			case '0' : res = '1' 
			case '1' : res = '0' 
			default : {
				throw new RuntimeException("Unsupported value "+c+", only '0' or '1' supported");
			}
		}
		debug('''eval(not(«c»))=«res»''');
		res
	
	}

	def dispatch String eval(ConstRef b) {
		eval(b.const.value)
	}	
	def dispatch String eval(PortRef b) {
		if (b.port==null) 
			throw new RuntimeException("Invalid expression "+PrettyPrinter.pp(b)+" ");
		var res = ""
		if (inputs.containsKey(b.port)) {
			res = inputs.get(b.port)
		} else {
			printIOMap()
			throw new RuntimeException("Port  "+b.port.name+":"+ b.port.hashCode+ " has no value");
		}
		
		if(b.range!=null) {
			// reverse to account for LSB on the rightmost digit 
			if(b.range.ub==-1) {
				val lb = b.range.lb+1 
				res = new StringBuilder(res).reverse().toString()
				res= quote(res.substring(lb,lb+1))
			} else {
			
				val lb = b.range.lb+1 
				val ub = b.range.ub+1 
				res = new StringBuilder(res).reverse().toString()
				res= quote(res.substring(lb,ub+1))
				res = new StringBuilder(res).reverse().toString()
			}
			
			
		} else {
		}
		debug("eval("+PrettyPrinter.pp(b)+")="+res)
		res
	}
	
	def printIOMap() {
//		for (ip : inputs.keySet) {
//			println('''in «ip.name»:«ip.hashCode» -> «inputs.get(ip)»''');
//		}
//		for (op : outputs.keySet) {
//			println('''out «op.name»:«op.hashCode» -> «outputs.get(op)»''');
//		}
	}
		
	

	
}