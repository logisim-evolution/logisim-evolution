/*
 * Logisim-evolution - digital logic design tool and simulator
 * Copyright by the Logisim-evolution developers
 *
 * https://github.com/logisim-evolution/
 *
 * This is free software released under GNU GPLv3 license
 */

package com.cburch.logisim.analyze.model;

import com.cburch.logisim.circuit.Circuit;
import com.cburch.logisim.proj.Project;
import java.util.List;

public class AnalyzerModel {
  public static final int MAX_INPUTS = 20;
  public static final int MAX_OUTPUTS = 256;

  public static final int FORMAT_SUM_OF_PRODUCTS = 0;
  public static final int FORMAT_PRODUCT_OF_SUMS = 1;

  private final VariableList inputs = new VariableList(MAX_INPUTS);
  private final VariableList outputs = new VariableList(MAX_OUTPUTS);
  private final TruthTable table;
  private final OutputExpressions outputExpressions;
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

  public void setVariables(List<Var> inputs, List<Var> outputs) {
    this.inputs.setAll(inputs);
    this.outputs.setAll(outputs);
  }
}
