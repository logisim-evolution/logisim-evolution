package com.cburch.logisim.statemachine.bdd

import java.io.FileNotFoundException
import java.util.ArrayList
import java.util.List
import java.util.Map.Entry
import org.eclipse.emf.common.util.BasicEList
import org.eclipse.emf.common.util.EList
import org.eclipse.emf.ecore.util.EcoreUtil
import com.cburch.logisim.statemachine.PrettyPrinter
import com.cburch.logisim.statemachine.fSMDSL.AndExpr
import com.cburch.logisim.statemachine.fSMDSL.BoolExpr
import com.cburch.logisim.statemachine.fSMDSL.CmpExpr
import com.cburch.logisim.statemachine.fSMDSL.ConcatExpr
import com.cburch.logisim.statemachine.fSMDSL.Constant
import com.cburch.logisim.statemachine.fSMDSL.FSM
import com.cburch.logisim.statemachine.fSMDSL.InputPort
import com.cburch.logisim.statemachine.fSMDSL.NotExpr
import com.cburch.logisim.statemachine.fSMDSL.OrExpr
import com.cburch.logisim.statemachine.fSMDSL.PortRef
import com.cburch.logisim.statemachine.fSMDSL.Range
import com.cburch.logisim.statemachine.fSMDSL.util.FSMDSLSwitch
import jdd.bdd.BDD
import static com.cburch.logisim.statemachine.editor.view.FSMCustomFactory.*
import com.cburch.logisim.statemachine.fSMDSL.Command

//import jdd.bdd.BDD;
/*  implement with switch visit ... !!! */
class BDDOptimizer extends FSMDSLSwitch<Integer> {
	static final String ZERO = "\"0\""
	static final String ONE = "\"1\""
	static final boolean VERBOSE = true
	package EList<InputPort> in
	BDD bdd
	Integer root
	BDDVariableMapping map
	package List<BoolExpr> list

	def private void debug(String str) {
		if (VERBOSE)
			System.out.println('''«str»''')
	}

	new(BoolExpr bexp) {
		var BoolExpr copy = EcoreUtil.copy(bexp)
		copy = new RemoveBitVectors().replace(copy)
		in = new CollectFlags().collect(copy)
		// FIXME 
		var int size = 0
		for (InputPort ip : in) {
			size += size * ip.getWidth()
		}
		var int bddsize = size * size
		bdd = new BDD(bddsize)
		map = new BDDVariableMapping(bdd)
		for (InputPort icp : in) {
			map.map(icp)
		}
		root = doSwitch(copy)
		debug('''Root is «root»''')
	}



	override Integer caseAndExpr(AndExpr object) {
		var int varBDDAnd = 0
		var boolean first = true
		for (bexp : object.getArgs()) {
			doSwitch(bexp)
			if (first) {
				first = false
				varBDDAnd = map.getBDDVarFor(bexp)
			} else {
				var int old = varBDDAnd
				varBDDAnd = bdd.and(varBDDAnd, map.getBDDVarFor(bexp))
				debug('''New node id=«varBDDAnd»=and(«old»,«map.getBDDVarFor(bexp)»)''')
			}
		}
		map.map(object, varBDDAnd)
		return varBDDAnd
	}

	override Integer caseConcatExpr(ConcatExpr object) {
		throw new UnsupportedOperationException('''BDD analysis dos not support operator «PrettyPrinter.pp(object)»''')
	}

	override Integer caseConstant(Constant object) {
		var String value = object.getValue()
		if (value.equals(ONE)) {
			map.map(object, bdd.getOne())
			return bdd.getOne()
		} else if (value.equals(ZERO)) {
			map.map(object, bdd.getZero())
			return bdd.getZero()
		} else {
			throw new UnsupportedOperationException('''BDD analysis does not support unkown constant value «value»''')
		}
	}

	override Integer casePortRef(PortRef pref) {
		var InputPort icp = (pref.getPort() as InputPort)
		if (!in.contains(icp)) {
			throw new RuntimeException('''Inconsistency in «PrettyPrinter.pp(pref)»''')
		} else {
			var int width = icp.getWidth()
			if (width > 1) {
				var Range range = pref.getRange()
				if (range !== null) {
					var int lb = range.getLb()
					var int ub = range.getUb()
					if ((lb === ub || ub === -1) && lb < width) {
						var int varProduct = map.getBDDVarFor(icp, lb)
						map.map(pref, varProduct)
						return varProduct
					}
				}
				throw new RuntimeException('''BDD analysis dos not support bitvector port references «PrettyPrinter.pp(pref)»''')
			} else {
				var int varProduct = map.getBDDVarFor(icp, 0)
				map.map(pref, varProduct)
				return varProduct
			}
		}
	}

	def private boolean isBitSetAt(int value, int i) {
		return ((value >> i).bitwiseAnd(0x1)) === 0x1
	}

	override Integer caseOrExpr(OrExpr object) {
		var int orBDDExression = 0;
		var boolean first = true
		for (BoolExpr bexp : object.getArgs()) {
			doSwitch(bexp)
			if (first) {
				first = false
				orBDDExression = map.getBDDVarFor(bexp)
			} else {
				var int old = orBDDExression
				orBDDExression = bdd.or(orBDDExression, map.getBDDVarFor(bexp))
				debug('''New node id=«orBDDExression»=or(«old»,«map.getBDDVarFor(bexp)»)''')
			}
		}
		map.map(object, orBDDExression)
		return orBDDExression
	}

	override Integer caseNotExpr(NotExpr object) {
		var int notBDDExpr0
		var BoolExpr bexp = object.getArgs().get(0)
		doSwitch(bexp)
		notBDDExpr0 = bdd.not(map.getBDDVarFor(bexp))
		map.map(object, notBDDExpr0)
		return notBDDExpr0
	}

	def BoolExpr simplify() {
		list = new ArrayList<BoolExpr>()
		rebuildPredicateFromSimplifiedBDD(bdd, root, null)
		if (list.size() === 0) {
			debug("No solution -> always false")
			return cst(false)
		}
		var OrExpr orExp = or(list)
		return orExp
	}

	def boolean isAlwaysTrue() {
		return root === 1
	}

	def boolean isAlwaysFalse() {
		return root === 0
	}

	/** 
	 * Translates the simplified BDD boolean expression into a FSM predicates 
	 */
	def private void rebuildPredicateFromSimplifiedBDD(BDD bdd, int root, BoolExpr current_finalParam_) {
		var current = current_finalParam_
		var BDDDotExport dot
		try {
			dot = new BDDDotExport("bdd.dot", bdd, map)
			dot.save(root)
		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace()
		}

		if (root >= 2) {
			var int bddInputVar = bdd.getVar(root)
			debug('''Root=«root» var=«bddInputVar» high=«bdd.getHigh(root)» low=«bdd.getLow(root)»''')
			/** 
			 */
			// if (bddVar<=1) throw new
			// UnsupportedOperationException("Not yet implemented");
			var Entry<InputPort, Integer> entry = map.getICPortForBDDvar(bddInputVar)
			var InputPort port = entry.getKey()
			var PortRef t
			var BoolExpr nT
			if (port.getWidth() === 1) {
				t = pref(port)
				nT = not(pref(port))
			} else if (port.getWidth() > 1) {
				t = pref(port, entry.getValue(), entry.getValue())
				nT = not(pref(port, entry.getValue(), entry.getValue()))
			} else {
				throw new RuntimeException('''Illegal port width «port.getWidth()» for port «port.getName()»''')
			}
			/** 
			 */
			if (current !== null) {
				var BoolExpr ResHigh = (EcoreUtil.copy(current) as BoolExpr)
				rebuildPredicateFromSimplifiedBDD(bdd, bdd.getHigh(root), and(t, ResHigh))
			} else {
				rebuildPredicateFromSimplifiedBDD(bdd, bdd.getHigh(root), t)
			}
			/** 
			 */
			if (current !== null) {
				var BoolExpr ResLow = (EcoreUtil.copy(current) as BoolExpr)
				rebuildPredicateFromSimplifiedBDD(bdd, bdd.getLow(root), and(nT, ResLow))
			} else {
				rebuildPredicateFromSimplifiedBDD(bdd, bdd.getLow(root), nT)
			}
		} else if (root === 1) {
			// BDD is simplified as a constant to true
			if (current === null) {
				current = cst(true)
			}
			list.add(current)
			debug('''Adding «current» to predicate''')
		} else if (root === 0) {
			// BDD is simplified as a constant to false
			debug('''Adding «current» to predicate''')
		}
	}
}
