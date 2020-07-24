package com.cburch.logisim.statemachine.bdd;

import java.io.FileNotFoundException;
import java.io.PrintStream;

import jdd.bdd.BDD;

public class BDDDotExport {
	String filename;
	BDD bdd;
	PrintStream st;
	
	public BDDDotExport(String filename, BDD bdd, BDDVariableMapping map) throws FileNotFoundException {
		this.bdd=bdd;
		this.filename=filename;
		st = new PrintStream(filename);
	}
	
	public void save(int root) {
		st.append("digraph G {\n");
		st.append("root[label=\"root\"]\nroot->n"+root+"\n");
		export(root);
		st.append("}");
	}
	
	public void export(int root) {
		if (root>1) {
			
			st.append("n"+root+"[label=\"var_"+bdd.getVar(root)+"\"]\n");
			export(bdd.getHigh(root));
			export(bdd.getLow(root));
			st.append("n"+root+"->n"+bdd.getHigh(root)+"[label=\"true\"]\n");
			st.append("n"+root+"->n"+bdd.getLow(root)+"[label=\"false\"]\n");
		} else {
			st.append("n"+root+"[label=\""+root+"\"]\n");
		}

	}
}
