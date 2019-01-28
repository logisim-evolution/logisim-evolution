package com.cburch.logisim.statemachine.bdd

import com.cburch.logisim.statemachine.PrettyPrinter
import com.cburch.logisim.statemachine.fSMDSL.AndExpr
import com.cburch.logisim.statemachine.fSMDSL.BoolExpr
import com.cburch.logisim.statemachine.fSMDSL.CmpExpr
import com.cburch.logisim.statemachine.fSMDSL.Constant
import com.cburch.logisim.statemachine.fSMDSL.NotExpr
import com.cburch.logisim.statemachine.fSMDSL.OrExpr
import com.cburch.logisim.statemachine.fSMDSL.PortRef
import java.util.HashMap
import javax.management.RuntimeErrorException
import org.eclipse.emf.common.util.EList
import com.cburch.logisim.statemachine.fSMDSL.ConstRef

class BitWidthAnalyzer {
	
	new() {
		
	}
	
	def Integer getBitwidth(BoolExpr b) {
		if (typeMap.containsKey(b)) {
			typeMap.get(b);
		} else {
			computeBitwidth(b);
		}
	}
	HashMap<BoolExpr, Integer> typeMap = new HashMap<BoolExpr, Integer>();
		
	public def checkWidth(EList<BoolExpr> args, BoolExpr b) {
		var width =-1;
		for(a : args) {
			if (typeMap.containsKey(a)) {
				val w= typeMap.get(a)
				if (w!=width){
					if(width!=-1) {
						val string = "Bitwidth mismatch in term "+PrettyPrinter.pp(a)+" in "+PrettyPrinter.pp(a.eContainer as BoolExpr)+ " "+width+" expected, "+w+"found"
						println(string);
						throw new RuntimeException(string);
					} else {
						width=w
					}
				}
			} else {
				throw new RuntimeException("ERROR : bitwidth mismatch in predicate expression "+PrettyPrinter.pp(b));
			}	
		}
		return typeMap.put(b,width);
	}

	public def dispatch  computeBitwidth(BoolExpr b) {}

	public def  dispatch computeBitwidth(OrExpr b) {
		b.args.forEach[a|computeBitwidth(a)]
		checkWidth(b.args,b)
	}
	
	public def  dispatch computeBitwidth(CmpExpr b) {
		if (b.args.size!=2) throw new RuntimeException("Inconsistent number of arguments for "+PrettyPrinter.pp(b)+" ");
		b.args.forEach[a|computeBitwidth(a)]
		checkWidth(b.args,b)
	}
	
	

	public def dispatch computeBitwidth(AndExpr b) {
		b.args.forEach[a|computeBitwidth(a)]
		checkWidth(b.args,b)
	}
	
	public def dispatch computeBitwidth(NotExpr b) {
		b.args.forEach[a|computeBitwidth(a)]
		checkWidth(b.args,b)
		
	}
	
	public def dispatch computeBitwidth(Constant b) {
		typeMap.put(b,b.value.length-2)
		return typeMap.get(b)
	}
	public def dispatch computeBitwidth(ConstRef b) {
		val res = (b.const.value as Constant).value.length-2 
		typeMap.put(b,res as Integer)
		
	}
	
	public def dispatch computeBitwidth(PortRef b) {
		if (b.range!=null) {
			if (b.range.lb>b.port.width-1) {
				throw new IndexOutOfBoundsException("Inconsistent range ["+b.range.ub+":"+b.range.lb+"] for port "+b.port.name+"["+(b.port.width-1)+":0]");
			}
			if(b.range.ub!=-1) {
				if (b.range.ub>b.port.width-1) {
					throw new IndexOutOfBoundsException("Inconsistent range ["+b.range.ub+":"+b.range.lb+"] for port "+b.port.name+"["+(b.port.width-1)+":0]");
				}
				if (b.range.lb>b.range.ub) {
					throw new IndexOutOfBoundsException("Inconsistent range ["+b.range.ub+":"+b.range.lb+"] ");
				}
				typeMap.put(b,b.range.ub-b.range.lb+1);
			} else {
				typeMap.put(b,1);
			}
		} else {
			typeMap.put(b,b.port.width);
		}
		
		
		
	}
	
}