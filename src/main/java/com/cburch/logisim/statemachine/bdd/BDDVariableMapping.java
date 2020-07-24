package com.cburch.logisim.statemachine.bdd;

import java.util.ArrayList;
import java.util.List;
import java.util.Map.Entry;

import com.cburch.logisim.statemachine.fSMDSL.BoolExpr;
import com.cburch.logisim.statemachine.fSMDSL.InputPort;

import jdd.bdd.BDD;


public class BDDVariableMapping  {

	BidirHashMap<BoolExpr, Integer> expressionMap;
	BidirHashMap<InputPort, List<Integer>> portMap;
	BDD bdd;
	
	public BDDVariableMapping(BDD bdd) {
		expressionMap = new BidirHashMap<BoolExpr, Integer>();
		portMap = new BidirHashMap<InputPort, List<Integer>>();
		this.bdd=bdd;
	}
	
	public void map(InputPort flag) {
		if (portMap.containsKey(flag)) {
			throw new RuntimeException("Port "+flag.getName()+ "already registered");
		}
		if (flag.getWidth()==0) {
			throw new RuntimeException("Port "+flag.getName()+ " has width=0");
		} else {
			List<Integer> list = new ArrayList<Integer>();
			portMap.link(flag,list);
			for (int i=0; i<flag.getWidth();i++) {
				int createVar = bdd.createVar();
				//System.out.println("New BDD variable "+createVar+"/"+bdd.getVar(createVar) + " for "+flag.getName()+"["+i+"]");
				list.add(createVar);
			}
		}
	}


	public class Pair<A, B> implements Entry<A, B>{
		B value;	A key;

		public Pair(A key,B value) {
			this.key=key;
			this.value= value;
		}
		public void setKey(A icp) {	this.key=icp;}
		public B setValue(B valueInt) {
			this.value=valueInt ;
			return valueInt;}
		public B getValue() {	return value;}
		public A getKey() {	return key;	}
		
	
	};

	public Entry<InputPort, Integer> getICPortForBDDvar(int bddInputVar) {

		Entry<InputPort, Integer> result;
		for (InputPort icp: portMap.keySet()) {
			List<Integer> list = portMap.get(icp);
			for (Integer bddInternalVar : list) {
				int currentBddInputVar = bdd.getVar(bddInternalVar.intValue());
				if (currentBddInputVar==bddInputVar) {
					int indexOf = list.indexOf(bddInternalVar);
					//System.out.println("Found "+icp.getName()+"["+indexOf+"]<->"+bddInternalVar);
					result = new Pair<InputPort, Integer>(icp,indexOf);
					return result;
				}
			}
		}
		throw new RuntimeException("Error, could not find corresponding Port for BDD node "+bddInputVar);
	}

	public BoolExpr getExpressionForBDDvar(Integer var) {

		return expressionMap.getInverse(var);
		
	}

	public int getBDDVarFor(InputPort flag, int i) {
		if (portMap.containsKey(flag)) {
			List<Integer> list = portMap.get(flag);
			if (list!=null && i<list.size()) {
				return list.get(i);
			} else {
				throw new RuntimeException("Cannot find BDD input variable for "+flag.getName()+"["+i+"]");
			}
		} else {
			throw new RuntimeException("Cannot find port "+flag.getName()+" in registerd port list");
		}
	}

	public void map(BoolExpr exp, int BddValue) {
		if (expressionMap.containsKey(exp)) {
			throw new RuntimeException("Expression "+exp+ " is already registered in \n"+expressionMap);
		} else {
			expressionMap.link(exp,BddValue);
		}
	}

	public int getBDDVarFor(BoolExpr exp) {
		if (!expressionMap.containsKey(exp)) {
			throw new RuntimeException("Expression "+exp+ " is not registered in :\n"+expressionMap);
		} else {
			return expressionMap.get(exp);
		}
	}
	

	

}
