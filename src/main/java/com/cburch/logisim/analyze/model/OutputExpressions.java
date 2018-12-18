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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;

public class OutputExpressions {
	private class MyListener implements VariableListListener,
			TruthTableListener {
		public void cellsChanged(TruthTableEvent event) {
			String output = model.getOutputs().get(event.getColumn());
			invalidate(output, false);
		}

		private void inputsChanged(VariableListEvent event) {
			int type = event.getType();
			if (type == VariableListEvent.ALL_REPLACED && !outputData.isEmpty()) {
				outputData.clear();
				fireModelChanged(OutputExpressionsEvent.ALL_VARIABLES_REPLACED);
			} else if (type == VariableListEvent.REMOVE) {
				String input = event.getVariable();
				for (String output : outputData.keySet()) {
					OutputData data = getOutputData(output, false);
					if (data != null)
						data.removeInput(input);
				}
			} else if (type == VariableListEvent.REPLACE) {
				String input = event.getVariable();
				int inputIndex = ((Integer) event.getData()).intValue();
				String newName = event.getSource().get(inputIndex);
				for (String output : outputData.keySet()) {
					OutputData data = getOutputData(output, false);
					if (data != null)
						data.replaceInput(input, newName);
				}
			} else if (type == VariableListEvent.MOVE
					|| type == VariableListEvent.ADD) {
				for (String output : outputData.keySet()) {
					OutputData data = getOutputData(output, false);
					if (data != null)
						data.invalidate(false, false);
				}
			}
		}

		public void listChanged(VariableListEvent event) {
			if (event.getSource() == model.getInputs())
				inputsChanged(event);
			else
				outputsChanged(event);
		}

		private void outputsChanged(VariableListEvent event) {
			int type = event.getType();
			if (type == VariableListEvent.ALL_REPLACED && !outputData.isEmpty()) {
				outputData.clear();
				fireModelChanged(OutputExpressionsEvent.ALL_VARIABLES_REPLACED);
			} else if (type == VariableListEvent.REMOVE) {
				outputData.remove(event.getVariable());
			} else if (type == VariableListEvent.REPLACE) {
				String oldName = event.getVariable();
				if (outputData.containsKey(oldName)) {
					OutputData toMove = outputData.remove(oldName);
					int inputIndex = ((Integer) event.getData()).intValue();
					String newName = event.getSource().get(inputIndex);
					toMove.output = newName;
					outputData.put(newName, toMove);
				}
			}
		}

		public void structureChanged(TruthTableEvent event) {
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
				if (expr == null)
					invalidate(false, false);
				exprString = expr == null ? "" : expr.toString();
			}
			return exprString;
		}

		Expression getMinimalExpression() {
			if (minimalExpr == null)
				invalidate(false, false);
			return minimalExpr;
		}

		List<Implicant> getMinimalImplicants() {
			return minimalImplicants;
		}

		int getMinimizedFormat() {
			return format;
		}

		private void invalidate(boolean initializing, boolean formatChanged) {
			if (invalidating)
				return;
			invalidating = true;
			try {
				List<Implicant> oldImplicants = minimalImplicants;
				Expression oldMinExpr = minimalExpr;
				minimalImplicants = Implicant.computeMinimal(format, model,
						output);
				minimalExpr = Implicant.toExpression(format, model,
						minimalImplicants);
				boolean minChanged = !implicantsSame(oldImplicants,
						minimalImplicants);

				if (!updatingTable) {
					// see whether the expression is still consistent with the
					// truth table
					TruthTable table = model.getTruthTable();
					Entry[] outputColumn = computeColumn(model.getTruthTable(),
							expr);
					int outputIndex = model.getOutputs().indexOf(output);

					Entry[] currentColumn = table.getOutputColumn(outputIndex);
					if (!columnsMatch(currentColumn, outputColumn)
							|| isAllUndefined(outputColumn) || formatChanged) {
						// if not, then we need to change the expression to
						// maintain consistency
						boolean exprChanged = expr != oldMinExpr || minChanged;
						expr = minimalExpr;
						if (exprChanged) {
							exprString = null;
							if (!initializing) {
								fireModelChanged(
										OutputExpressionsEvent.OUTPUT_EXPRESSION,
										output);
							}
						}
					}
				}

				if (!initializing && minChanged) {
					fireModelChanged(OutputExpressionsEvent.OUTPUT_MINIMAL,
							output);
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
				Expression oldExpr = expr;
				Expression newExpr;
				if (oldExpr == oldMinExpr) {
					newExpr = getMinimalExpression();
					expr = newExpr;
				} else {
					newExpr = expr.removeVariable(input);
				}
				if (newExpr == null || !newExpr.equals(oldExpr)) {
					expr = newExpr;
					fireModelChanged(OutputExpressionsEvent.OUTPUT_EXPRESSION,
							output, expr);
				}
			}
			fireModelChanged(OutputExpressionsEvent.OUTPUT_MINIMAL, output,
					minimalExpr);
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
					fireModelChanged(OutputExpressionsEvent.OUTPUT_EXPRESSION,
							output);
				}
			} else {
				fireModelChanged(OutputExpressionsEvent.OUTPUT_EXPRESSION,
						output);
			}
			fireModelChanged(OutputExpressionsEvent.OUTPUT_MINIMAL, output);
		}

		void setExpression(Expression newExpr, String newExprString) {
			expr = newExpr;
			exprString = newExprString;

			if (expr != minimalExpr) { // for efficiency to avoid recomputation
				Entry[] values = computeColumn(model.getTruthTable(), expr);
				int outputColumn = model.getOutputs().indexOf(output);
				updatingTable = true;
				try {
					model.getTruthTable().setOutputColumn(outputColumn, values);
				} finally {
					updatingTable = false;
				}
			}

			fireModelChanged(OutputExpressionsEvent.OUTPUT_EXPRESSION, output,
					getExpression());
		}

		void setMinimizedFormat(int value) {
			if (format != value) {
				format = value;
				this.invalidate(false, true);
			}
		}
	}

	private static boolean columnsMatch(Entry[] a, Entry[] b) {
		if (a.length != b.length)
			return false;
		for (int i = 0; i < a.length; i++) {
			if (a[i] != b[i]) {
				boolean bothDefined = (a[i] == Entry.ZERO || a[i] == Entry.ONE)
						&& (b[i] == Entry.ZERO || b[i] == Entry.ONE);
				if (bothDefined)
					return false;
			}
		}
		return true;
	}

	private static Entry[] computeColumn(TruthTable table, Expression expr) {
		int rows = table.getRowCount();
		int cols = table.getInputColumnCount();
		Entry[] values = new Entry[rows];
		if (expr == null) {
			Arrays.fill(values, Entry.DONT_CARE);
		} else {
			Assignments assn = new Assignments();
			for (int i = 0; i < rows; i++) {
				for (int j = 0; j < cols; j++) {
					assn.put(table.getInputHeader(j),
							TruthTable.isInputSet(i, j, cols));
				}
				values[i] = expr.evaluate(assn) ? Entry.ONE : Entry.ZERO;
			}
		}
		return values;
	}

	private static boolean implicantsSame(List<Implicant> a, List<Implicant> b) {
		if (a == null) {
			return b == null || b.size() == 0;
		} else if (b == null) {
			return a == null || a.size() == 0;
		} else if (a.size() != b.size()) {
			return false;
		} else {
			Iterator<Implicant> ait = a.iterator();
			for (Implicant bi : b) {
				if (!ait.hasNext())
					return false; // should never happen
				Implicant ai = ait.next();
				if (!ai.equals(bi))
					return false;
			}
			return true;
		}
	}

	private static boolean isAllUndefined(Entry[] a) {
		for (int i = 0; i < a.length; i++) {
			if (a[i] == Entry.ZERO || a[i] == Entry.ONE)
				return false;
		}
		return true;
	}

	private MyListener myListener = new MyListener();

	private AnalyzerModel model;

	private HashMap<String, OutputData> outputData = new HashMap<String, OutputData>();

	private ArrayList<OutputExpressionsListener> listeners = new ArrayList<OutputExpressionsListener>();

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
		OutputExpressionsEvent event = new OutputExpressionsEvent(model, type,
				variable, data);
		for (OutputExpressionsListener l : listeners) {
			l.expressionChanged(event);
		}
	}

	//
	// access methods
	//
	public Expression getExpression(String output) {
		if (output == null)
			return null;
		return getOutputData(output, true).getExpression();
	}

	public String getExpressionString(String output) {
		if (output == null)
			return "";
		return getOutputData(output, true).getExpressionString();
	}

	public Expression getMinimalExpression(String output) {
		if (output == null)
			return Expressions.constant(0);
		return getOutputData(output, true).getMinimalExpression();
	}

	public List<Implicant> getMinimalImplicants(String output) {
		if (output == null)
			return Implicant.MINIMAL_LIST;
		return getOutputData(output, true).getMinimalImplicants();
	}

	public int getMinimizedFormat(String output) {
		if (output == null)
			return AnalyzerModel.FORMAT_SUM_OF_PRODUCTS;
		return getOutputData(output, true).getMinimizedFormat();
	}

	private OutputData getOutputData(String output, boolean create) {
		if (output == null)
			throw new IllegalArgumentException("null output name");
		OutputData ret = outputData.get(output);
		if (ret == null && create) {
			if (model.getOutputs().indexOf(output) < 0) {
				throw new IllegalArgumentException("unrecognized output "
						+ output);
			}
			ret = new OutputData(output);
			outputData.put(output, ret);
		}
		return ret;
	}

	private void invalidate(String output, boolean formatChanged) {
		OutputData data = getOutputData(output, false);
		if (data != null)
			data.invalidate(false, false);
	}

	public boolean isExpressionMinimal(String output) {
		OutputData data = getOutputData(output, false);
		return data == null ? true : data.isExpressionMinimal();
	}

	public void removeOutputExpressionsListener(OutputExpressionsListener l) {
		listeners.remove(l);
	}

	public void setExpression(String output, Expression expr) {
		setExpression(output, expr, null);
	}

	public void setExpression(String output, Expression expr, String exprString) {
		if (output == null)
			return;
		getOutputData(output, true).setExpression(expr, exprString);
	}

	//
	// modifier methods
	//
	public void setMinimizedFormat(String output, int format) {
		int oldFormat = getMinimizedFormat(output);
		if (format != oldFormat) {
			getOutputData(output, true).setMinimizedFormat(format);
			invalidate(output, true);
		}
	}
}
