package com.cburch.logisim.statemachine.bdd

import com.cburch.logisim.statemachine.fSMDSL.BoolExpr
import com.cburch.logisim.statemachine.fSMDSL.AndExpr
import com.cburch.logisim.statemachine.fSMDSL.OrExpr
import com.cburch.logisim.statemachine.fSMDSL.NotExpr
import com.cburch.logisim.statemachine.fSMDSL.Constant
import com.cburch.logisim.statemachine.fSMDSL.FSMDSLFactory
import com.cburch.logisim.statemachine.fSMDSL.PortRef
import org.eclipse.emf.ecore.util.EcoreUtil
import com.cburch.logisim.statemachine.fSMDSL.CmpExpr
import com.cburch.logisim.statemachine.PrettyPrinter
import com.cburch.logisim.statemachine.fSMDSL.ConcatExpr
import com.cburch.logisim.statemachine.fSMDSL.ConstRef

class RemoveBitVectors {

	final static boolean VERBOSE = true;
		BitWidthAnalyzer analyzer = new BitWidthAnalyzer
	
	new() {
		
	}
	
	def dispatch replace(BoolExpr e) {
		print("replace "+e+ " by ")
		
		val list = EcoreUtil.getAllContents(e,false).filter(typeof(CmpExpr)).toList
		for (n:list) {
			println("")
			EcoreUtil.replace(n,slice(n))
		}
		println(" "+PrettyPrinter.pp(e))
		e
	}
	def dispatch replace(CmpExpr  e) {
		print("replace "+PrettyPrinter.pp(e)+ " by ")

		val list = EcoreUtil.getAllContents(e,false).filter(typeof(CmpExpr)).toList
		for (n:list) {
			println("Replace ")
			EcoreUtil.replace(n,slice(n))
		}
		val res= slice(e)
		println(" "+PrettyPrinter.pp(res))
		res;
	}

	def dispatch BoolExpr slice(BoolExpr e, int offset) {
		throw new RuntimeException("Cannot bitslice expression "+e);
	}
	
	def BoolExpr and(BoolExpr a, BoolExpr b) {
		var and= FSMDSLFactory.eINSTANCE.createAndExpr
		and.args+=#{a,b}
		and
	}

	def BoolExpr or(BoolExpr a, BoolExpr b) {
		var or= FSMDSLFactory.eINSTANCE.createOrExpr
		or.args+=#{a,b}
		or
	}
	
	def BoolExpr not(BoolExpr a) {
		var not= FSMDSLFactory.eINSTANCE.createNotExpr
		not.args+=a
		not
	}
	
	def BoolExpr equ(BoolExpr a, BoolExpr b) {
		or(and(EcoreUtil.copy(a),EcoreUtil.copy(b)), not(or(EcoreUtil.copy(a),EcoreUtil.copy(b))))
	}
	
	def BoolExpr nequ(BoolExpr a, BoolExpr b) {
		or(and(not(EcoreUtil.copy(a)),EcoreUtil.copy(b)), and(EcoreUtil.copy(a),not(EcoreUtil.copy(b))))
	}
	

	def dispatch BoolExpr slice(CmpExpr e) {
		var and= FSMDSLFactory.eINSTANCE.createAndExpr
		var canDoIt =true;
		var i = 0;
		analyzer.computeBitwidth(e);
		val bw = analyzer.getBitwidth(e)
		for(i= 0; i< bw; i++) {
			var BoolExpr slice = null
			val left= slice(e.args.get(0), i)
			val right= slice(e.args.get(1), i)
			
			switch(e.op) {
				case "==" : {
					slice = equ(left,right);
				}
				case "/=" : {
					slice = nequ(left,right);
				}
				default: {
					throw new UnsupportedOperationException("Invalid compare operator "+e.op+" only ==,/= allowed")
				}
			}
			if(slice!=null)
				and.args.add(slice)
				
		}
		and
	}
	
	def dispatch BoolExpr slice(AndExpr e, int offset) {
		var and= FSMDSLFactory.eINSTANCE.createAndExpr
		and.args.addAll(e.args.map[arg| slice(arg,offset)])
		and
	}

	def dispatch BoolExpr slice(ConcatExpr e, int offset) {
		analyzer.computeBitwidth(e)
		var current = offset
		for (arg : e.args) {
			val width = analyzer.getBitwidth(arg); 
			if(current<width) {
				return slice(arg,offset)
			}
			current = current - width;			
		}
		throw new IndexOutOfBoundsException("Offset "+offset+" is out of bound w.r.t to expression "+e)
		
	}

	def dispatch BoolExpr slice(OrExpr e, int offset) {
		var or= FSMDSLFactory.eINSTANCE.createOrExpr
		or.args.addAll(e.args.map[arg| slice(arg,offset)])
		or
	}
	def dispatch BoolExpr slice(NotExpr e, int offset) {
		var not= FSMDSLFactory.eINSTANCE.createNotExpr
		not.args.addAll(e.args.map[arg| slice(arg,offset)])
		not
	}

	def dispatch BoolExpr slice(Constant e, int offset) {
		if(offset<=e.value.length-2) {
			var c= FSMDSLFactory.eINSTANCE.createConstant
			c.value='''"«e.value.charAt(offset+1)»"'''
			c
		} else {
			throw new IndexOutOfBoundsException("Offset "+offset+" is out of bound w.r.t to expression "+e)
		}
	}
	
	def dispatch BoolExpr slice(ConstRef e, int offset) {
		return slice(e.const.value,offset)
	}
	
	def dispatch BoolExpr slice(PortRef e, int offset) {
		if (e.range!=null) {
			if(e.range.ub!=-1 && offset+e.range.lb>e.range.ub) {
				throw new IndexOutOfBoundsException("Offset "+offset+" is out of bound w.r.t to port "+e.port)
			}
			var pref= FSMDSLFactory.eINSTANCE.createPortRef
			pref.port=e.port
			pref.range = FSMDSLFactory.eINSTANCE.createRange
			pref.range.lb = offset+e.range.lb
			pref.range.ub = offset+e.range.lb
			pref
		} else {
			if(offset>=e.port.width) {
				throw new IndexOutOfBoundsException("Offset "+offset+" is out of bound w.r.t to port "+e.port)
			}
			var pref= FSMDSLFactory.eINSTANCE.createPortRef
			pref.port=e.port
			pref.range = FSMDSLFactory.eINSTANCE.createRange
			pref.range.lb = offset
			pref.range.ub = offset
			pref
		}
	}
	
}