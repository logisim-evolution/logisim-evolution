/*
 * Logisim-evolution - digital logic design tool and simulator
 * Copyright by the Logisim-evolution developers
 *
 * https://github.com/logisim-evolution/
 *
 * This is free software released under GNU GPLv3 license
 */

package com.cburch.logisim.analyze.model;

import com.cburch.logisim.util.CollectionUtil;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;

public class OutputExpressions {
  private class MyListener implements VariableListListener, TruthTableListener {

    @Override
    public void rowsChanged(TruthTableEvent event) {
      // Do nothing
    }

    @Override
    public void cellsChanged(TruthTableEvent event) {
      final var output = model.getOutputs().bits.get(event.getColumn());
      invalidate(output);
    }

    private void inputsChanged(VariableListEvent event) {
      final var v = event.getVariable();
      final var type = event.getType();
      if (type == VariableListEvent.ALL_REPLACED && !outputData.isEmpty()) {
        outputData.clear();
        fireModelChanged(OutputExpressionsEvent.ALL_VARIABLES_REPLACED);
      } else if (type == VariableListEvent.REMOVE) {
        for (final var input : v) {
          for (final var output : outputData.keySet()) {
            final var data = getOutputData(output, false);
            if (data != null) data.removeInput(input);
          }
        }
      } else if (type == VariableListEvent.REPLACE) {
        final var newVar = model.getInputs().vars.get(event.getIndex());
        for (final var output : outputData.keySet()) {
          for (int b = 0; b < v.width && b < newVar.width; b++) {
            final var data = getOutputData(output, false);
            if (data != null) data.replaceInput(v.bitName(b), newVar.bitName(b));
          }
          for (int b = newVar.width; b < v.width; b++) {
            final var data = getOutputData(output, false);
            if (data != null) data.removeInput(v.bitName(b));
          }
          if (v.width < newVar.width) {
            final var data = getOutputData(output, false);
            if (data != null) data.invalidate(false, false);
          }
        }
      } else if (type == VariableListEvent.MOVE || type == VariableListEvent.ADD) {
        for (final var output : outputData.keySet()) {
          final var data = getOutputData(output, false);
          if (data != null) data.invalidate(false, false);
        }
      }
    }

    @Override
    public void listChanged(VariableListEvent event) {
      if (event.getSource() == model.getInputs()) inputsChanged(event);
      else outputsChanged(event);
    }

    private void outputsChanged(VariableListEvent event) {
      final var type = event.getType();
      if (type == VariableListEvent.ALL_REPLACED && !outputData.isEmpty()) {
        outputData.clear();
        fireModelChanged(OutputExpressionsEvent.ALL_VARIABLES_REPLACED);
      } else if (type == VariableListEvent.REMOVE) {
        for (final var bit : event.getVariable()) outputData.remove(bit);
      } else if (type == VariableListEvent.REPLACE) {
        Var oldVar = event.getVariable();
        Var newVar = model.getOutputs().vars.get(event.getIndex());
        for (var b = 0; b < oldVar.width && b < newVar.width; b++) {
          final var oldName = oldVar.bitName(b);
          final var newName = newVar.bitName(b);
          if (outputData.containsKey(oldName)) {
            OutputData toMove = outputData.remove(oldName);
            toMove.output = newName;
            outputData.put(newName, toMove);
          }
        }
        for (var b = newVar.width; b < oldVar.width; b++) {
          outputData.remove(oldVar.bitName(b));
        }
      }
    }

    @Override
    public void structureChanged(TruthTableEvent event) {
      // Dp nothing.
    }
  }

  private class OutputData {
    String output;
    int format;
    Expression expr = null;
    String exprString = null;
    List<Implicant> minimalImplicants = null;
    Expression minimalExpr = null;

    private boolean invalidating = false;

    OutputData(String output) {
      this.output = output;
      invalidate(true, false);
    }

    Expression getExpression() {
      return expr;
    }

    String getExpressionString() {
      if (exprString == null) {
        if (expr == null) invalidate(false, false);
        exprString = expr == null ? "" : expr.toString();
      }
      return exprString;
    }

    Expression getMinimalExpression() {
      if (minimalExpr == null) invalidate(false, false);
      return minimalExpr;
    }

    List<Implicant> getMinimalImplicants() {
      return minimalImplicants;
    }

    int getMinimizedFormat() {
      return format;
    }

    private void invalidate(boolean initializing, boolean formatChanged) {
      if (invalidating) return;
      invalidating = true;
      try {
        final var oldImplicants = minimalImplicants;
        final var oldMinExpr = minimalExpr;
        minimalImplicants = Implicant.computeMinimal(format, model, output);
        minimalExpr = Implicant.toExpression(format, model, minimalImplicants);
        final var minChanged = !implicantsSame(oldImplicants, minimalImplicants);

        if (!updatingTable) {
          // see whether the expression is still consistent with the
          // truth table
          final var table = model.getTruthTable();
          final var outputColumn = computeColumn(model.getTruthTable(), expr);
          final var outputIndex = model.getOutputs().bits.indexOf(output);
          final var currentColumn = table.getOutputColumn(outputIndex);
          if (!columnsMatch(currentColumn, outputColumn)
              || isAllUndefined(outputColumn)
              || formatChanged) {
            // if not, then we need to change the expression to
            // maintain consistency
            final var exprChanged = expr != oldMinExpr || minChanged;
            expr = minimalExpr;
            if (exprChanged) {
              exprString = null;
              if (!initializing) {
                fireModelChanged(OutputExpressionsEvent.OUTPUT_EXPRESSION, output);
              }
            }
          }
        }

        if (!initializing && minChanged) {
          fireModelChanged(OutputExpressionsEvent.OUTPUT_MINIMAL, output);
        }
      } finally {
        invalidating = false;
      }
    }

    boolean isExpressionMinimal() {
      return expr == minimalExpr;
    }

    private void removeInput(String input) {
      Expression oldMinExpr = minimalExpr;
      minimalImplicants = null;
      minimalExpr = null;

      if (exprString != null) {
        exprString = null; // invalidate it so it recomputes
      }
      if (expr != null) {
        final var oldExpr = expr;
        Expression newExpr;
        if (oldExpr == oldMinExpr) {
          newExpr = getMinimalExpression();
          expr = newExpr;
        } else {
          newExpr = expr.removeVariable(input);
        }
        if (newExpr == null || !newExpr.equals(oldExpr)) {
          expr = newExpr;
          fireModelChanged(OutputExpressionsEvent.OUTPUT_EXPRESSION, output, expr);
        }
      }
      fireModelChanged(OutputExpressionsEvent.OUTPUT_MINIMAL, output, minimalExpr);
    }

    private void replaceInput(String input, String newName) {
      minimalExpr = null;

      if (exprString != null) {
        exprString = Parser.replaceVariable(exprString, input, newName);
      }
      if (expr != null) {
        Expression newExpr = expr.replaceVariable(input, newName);
        if (!newExpr.equals(expr)) {
          expr = newExpr;
          fireModelChanged(OutputExpressionsEvent.OUTPUT_EXPRESSION, output);
        }
      } else {
        fireModelChanged(OutputExpressionsEvent.OUTPUT_EXPRESSION, output);
      }
      fireModelChanged(OutputExpressionsEvent.OUTPUT_MINIMAL, output);
    }

    void setExpression(Expression newExpr, String newExprString) {
      expr = newExpr;
      exprString = newExprString;

      if (expr != minimalExpr) { // for efficiency to avoid recomputation
        final var values = computeColumn(model.getTruthTable(), expr);
        final var outputColumn = model.getOutputs().bits.indexOf(output);
        updatingTable = true;
        try {
          model.getTruthTable().setOutputColumn(outputColumn, values);
        } finally {
          updatingTable = false;
        }
      }

      fireModelChanged(OutputExpressionsEvent.OUTPUT_EXPRESSION, output, getExpression());
    }

    void setMinimizedFormat(int value) {
      if (format != value) {
        format = value;
        this.invalidate(false, true);
      }
    }
  }

  private static boolean columnsMatch(Entry[] a, Entry[] b) {
    if (a.length != b.length) return false;
    for (var i = 0; i < a.length; i++) {
      if (a[i] != b[i]) {
        final var bothDefined =
            (a[i] == Entry.ZERO || a[i] == Entry.ONE) && (b[i] == Entry.ZERO || b[i] == Entry.ONE);
        if (bothDefined) return false;
      }
    }
    return true;
  }

  private static Entry[] computeColumn(TruthTable table, Expression expr) {
    final var rows = table.getRowCount();
    final var cols = table.getInputColumnCount();
    final var values = new Entry[rows];
    if (expr == null) {
      Arrays.fill(values, Entry.DONT_CARE);
    } else {
      final var assn = new Assignments();
      for (var i = 0; i < rows; i++) {
        for (var j = 0; j < cols; j++) {
          assn.put(table.getInputHeader(j), TruthTable.isInputSet(i, j, cols));
        }
        values[i] = expr.evaluate(assn) ? Entry.ONE : Entry.ZERO;
      }
    }
    return values;
  }

  private static boolean implicantsSame(List<Implicant> a, List<Implicant> b) {
    if (a == null) return CollectionUtil.isNullOrEmpty(b);
    if (b == null) return a.isEmpty();
    if (a.size() != b.size()) return false;
    final var ait = a.iterator();
    for (final var bImplicant : b) {
      if (!ait.hasNext()) return false; // should never happen
      final var ai = ait.next();
      if (!ai.equals(bImplicant)) return false;
    }
    return true;
  }

  private static boolean isAllUndefined(Entry[] a) {
    for (final var entry : a) {
      if (entry == Entry.ZERO || entry == Entry.ONE)
        return false;
    }
    return true;
  }

  private final MyListener myListener = new MyListener();

  private final AnalyzerModel model;

  private final HashMap<String, OutputData> outputData = new HashMap<>();

  private final ArrayList<OutputExpressionsListener> listeners = new ArrayList<>();

  private boolean updatingTable = false;

  public OutputExpressions(AnalyzerModel model) {
    this.model = model;
    model.getInputs().addVariableListListener(myListener);
    model.getOutputs().addVariableListListener(myListener);
    model.getTruthTable().addTruthTableListener(myListener);
  }

  //
  // listener methods
  //
  public void addOutputExpressionsListener(OutputExpressionsListener l) {
    listeners.add(l);
  }

  private void fireModelChanged(int type) {
    fireModelChanged(type, null, null);
  }

  private void fireModelChanged(int type, String variable) {
    fireModelChanged(type, variable, null);
  }

  private void fireModelChanged(int type, String variable, Object data) {
    final var event = new OutputExpressionsEvent(model, type, variable, data);
    for (OutputExpressionsListener l : listeners) {
      l.expressionChanged(event);
    }
  }

  //
  // access methods
  //
  public Expression getExpression(String output) {
    if (output == null) return null;
    try {
      return getOutputData(output, true).getExpression();
    } catch (Exception e) {
      return null;
    }
  }

  public String getExpressionString(String output) {
    if (output == null) return "";
    try {
      return getOutputData(output, true).getExpressionString();
    } catch (Exception e) {
      return "";
    }
  }

  public Expression getMinimalExpression(String output) {
    if (output == null) return Expressions.constant(0);
    try {
      return getOutputData(output, true).getMinimalExpression();
    } catch (Exception e) {
      return Expressions.constant(0);
    }
  }

  public List<Implicant> getMinimalImplicants(String output) {
    if (output == null) return Implicant.MINIMAL_LIST;
    try {
      return getOutputData(output, true).getMinimalImplicants();
    } catch (Exception e) {
      return Implicant.MINIMAL_LIST;
    }
  }

  public int getMinimizedFormat(String output) {
    if (output == null) return AnalyzerModel.FORMAT_SUM_OF_PRODUCTS;
    try {
      return getOutputData(output, true).getMinimizedFormat();
    } catch (Exception e) {
      return AnalyzerModel.FORMAT_SUM_OF_PRODUCTS;
    }
  }

  private OutputData getOutputData(String output, boolean create) {
    if (output == null) throw new IllegalArgumentException("null output name");
    var ret = outputData.get(output);
    if (ret == null && create) {
      if (!model.getOutputs().bits.contains(output)) {
        throw new IllegalArgumentException("unrecognized output " + output);
      }
      ret = new OutputData(output);
      outputData.put(output, ret);
    }
    return ret;
  }

  private void invalidate(String output) {
    final var data = getOutputData(output, false);
    if (data != null) {
      if (!allowUpdates) {
        outputData.remove(output);
      } else {
        data.invalidate(false, false);
      }
    }
  }

  public boolean isExpressionMinimal(String output) {
    final var data = getOutputData(output, false);
    return data == null || data.isExpressionMinimal();
  }

  public void removeOutputExpressionsListener(OutputExpressionsListener l) {
    listeners.remove(l);
  }

  public void setExpression(String output, Expression expr) {
    setExpression(output, expr, null);
  }

  public void setExpression(String output, Expression expr, String exprString) {
    if (output == null) return;
    getOutputData(output, true).setExpression(expr, exprString);
  }

  public void setMinimizedFormat(String output, int format) {
    final var oldFormat = getMinimizedFormat(output);
    if (format != oldFormat) {
      getOutputData(output, true).setMinimizedFormat(format);
      invalidate(output);
    }
  }

  private boolean allowUpdates = false;

  public void enableUpdates() {
    allowUpdates = true;
  }

  public void disableUpdates() {
    allowUpdates = false;
  }

  public boolean updatesEnabled() {
    return allowUpdates;
  }
}
