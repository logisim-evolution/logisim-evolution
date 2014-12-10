/*******************************************************************************
 * This file is part of logisim-evolution.
 *
 *   logisim-evolution is free software: you can redistribute it and/or modify
 *   it under the terms of the GNU General Public License as published by
 *   the Free Software Foundation, either version 3 of the License, or
 *   (at your option) any later version.
 *
 *   logisim-evolution is distributed in the hope that it will be useful,
 *   but WITHOUT ANY WARRANTY; without even the implied warranty of
 *   MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *   GNU General Public License for more details.
 *
 *   You should have received a copy of the GNU General Public License
 *   along with logisim-evolution.  If not, see <http://www.gnu.org/licenses/>.
 *
 *   Original code by Carl Burch (http://www.cburch.com), 2011.
 *   Subsequent modifications by :
 *     + Haute École Spécialisée Bernoise
 *       http://www.bfh.ch
 *     + Haute École du paysage, d'ingénierie et d'architecture de Genève
 *       http://hepia.hesge.ch/
 *     + Haute École d'Ingénierie et de Gestion du Canton de Vaud
 *       http://www.heig-vd.ch/
 *   The project is currently maintained by :
 *     + REDS Institute - HEIG-VD
 *       Yverdon-les-Bains, Switzerland
 *       http://reds.heig-vd.ch
 *******************************************************************************/

package com.cburch.logisim.analyze.model;

import java.util.List;

import com.cburch.logisim.circuit.Circuit;
import com.cburch.logisim.proj.Project;

public class AnalyzerModel {
	public static final int MAX_INPUTS = 12;
	public static final int MAX_OUTPUTS = 12;

	public static final int FORMAT_SUM_OF_PRODUCTS = 0;
	public static final int FORMAT_PRODUCT_OF_SUMS = 1;

	private VariableList inputs = new VariableList(MAX_INPUTS);
	private VariableList outputs = new VariableList(MAX_OUTPUTS);
	private TruthTable table;
	private OutputExpressions outputExpressions;
	private Project currentProject = null;
	private Circuit currentCircuit = null;

	public AnalyzerModel() {
		// the order here is important, because the output expressions
		// need the truth table to exist for listening.
		table = new TruthTable(this);
		outputExpressions = new OutputExpressions(this);
	}

	public Circuit getCurrentCircuit() {
		return currentCircuit;
	}

	//
	// access methods
	//
	public Project getCurrentProject() {
		return currentProject;
	}

	public VariableList getInputs() {
		return inputs;
	}

	public OutputExpressions getOutputExpressions() {
		return outputExpressions;
	}

	public VariableList getOutputs() {
		return outputs;
	}

	public TruthTable getTruthTable() {
		return table;
	}

	//
	// modifier methods
	//
	public void setCurrentCircuit(Project value, Circuit circuit) {
		currentProject = value;
		currentCircuit = circuit;
	}

	public void setVariables(List<String> inputs, List<String> outputs) {
		this.inputs.setAll(inputs);
		this.outputs.setAll(outputs);
	}
}
