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
import java.util.List;
import java.util.Map;

public class TruthTable {
	private class MyListener implements VariableListListener {
		private void inputsChanged(VariableListEvent event) {
			int action = event.getType();
			if (action == VariableListEvent.ADD) {
				for (Map.Entry<String, Entry[]> curEntry : outputColumns
						.entrySet()) {
					String output = curEntry.getKey();
					Entry[] column = curEntry.getValue();
					Entry[] newColumn = new Entry[2 * column.length];
					for (int i = 0; i < column.length; i++) {
						newColumn[2 * i] = column[i];
						newColumn[2 * i + 1] = column[i];
					}
					outputColumns.put(output, newColumn);
				}
			} else if (action == VariableListEvent.REMOVE) {
				int index = ((Integer) event.getData()).intValue();
				for (Map.Entry<String, Entry[]> curEntry : outputColumns
						.entrySet()) {
					String output = curEntry.getKey();
					Entry[] column = curEntry.getValue();
					Entry[] newColumn = removeInput(column, index);
					outputColumns.put(output, newColumn);
				}
			} else if (action == VariableListEvent.MOVE) {
				int delta = ((Integer) event.getData()).intValue();
				int newIndex = model.getInputs().indexOf(event.getVariable());
				for (Map.Entry<String, Entry[]> curEntry : outputColumns
						.entrySet()) {
					String output = curEntry.getKey();
					Entry[] column = curEntry.getValue();
					Entry[] newColumn = moveInput(column, newIndex - delta,
							newIndex);
					outputColumns.put(output, newColumn);
				}
			}
		}

		public void listChanged(VariableListEvent event) {
			if (event.getSource() == model.getInputs()) {
				inputsChanged(event);
			} else {
				outputsChanged(event);
			}
			fireStructureChanged(event);
		}

		private Entry[] moveInput(Entry[] old, int oldIndex, int newIndex) {
			int inputs = model.getInputs().size();
			oldIndex = inputs - 1 - oldIndex;
			newIndex = inputs - 1 - newIndex;
			Entry[] ret = new Entry[old.length];
			int sameMask = (old.length - 1)
					^ ((1 << (1 + Math.max(oldIndex, newIndex))) - 1)
					^ ((1 << Math.min(oldIndex, newIndex)) - 1); // bits that
																	// don't
																	// change
			int moveMask = 1 << oldIndex; // bit that moves
			int moveDist = Math.abs(newIndex - oldIndex);
			boolean moveLeft = newIndex > oldIndex;
			int blockMask = (old.length - 1) ^ sameMask ^ moveMask; // bits that
																	// move by
																	// one
			for (int i = 0; i < old.length; i++) {
				int j; // new index
				if (moveLeft) {
					j = (i & sameMask) | ((i & moveMask) << moveDist)
							| ((i & blockMask) >> 1);
				} else {
					j = (i & sameMask) | ((i & moveMask) >> moveDist)
							| ((i & blockMask) << 1);
				}
				ret[j] = old[i];
			}
			return ret;
		}

		private void outputsChanged(VariableListEvent event) {
			int action = event.getType();
			if (action == VariableListEvent.ALL_REPLACED) {
				outputColumns.clear();
			} else if (action == VariableListEvent.REMOVE) {
				outputColumns.remove(event.getVariable());
			} else if (action == VariableListEvent.REPLACE) {
				Entry[] column = outputColumns.remove(event.getVariable());
				if (column != null) {
					int index = ((Integer) event.getData()).intValue();
					String newVariable = model.getOutputs().get(index);
					outputColumns.put(newVariable, column);
				}
			}
		}

		private Entry[] removeInput(Entry[] old, int index) {
			int oldInputCount = model.getInputs().size() + 1;
			Entry[] ret = new Entry[old.length / 2];
			int j = 0;
			int mask = 1 << (oldInputCount - 1 - index);
			for (int i = 0; i < old.length; i++) {
				if ((i & mask) == 0) {
					Entry e0 = old[i];
					Entry e1 = old[i | mask];
					ret[j] = (e0 == e1 ? e0 : Entry.DONT_CARE);
					j++;
				}
			}
			return ret;
		}
	}

	public static boolean isInputSet(int row, int column, int inputs) {
		return ((row >> (inputs - 1 - column)) & 0x1) == 1;
	}

	private static final Entry DEFAULT_ENTRY = Entry.DONT_CARE;
	private MyListener myListener = new MyListener();
	private List<TruthTableListener> listeners = new ArrayList<TruthTableListener>();
	private AnalyzerModel model;

	private HashMap<String, Entry[]> outputColumns = new HashMap<String, Entry[]>();

	public TruthTable(AnalyzerModel model) {
		this.model = model;
		model.getInputs().addVariableListListener(myListener);
		model.getOutputs().addVariableListListener(myListener);
	}

	public void addTruthTableListener(TruthTableListener l) {
		listeners.add(l);
	}

	private void fireCellsChanged(int column) {
		TruthTableEvent event = new TruthTableEvent(this, column);
		for (TruthTableListener l : listeners) {
			l.cellsChanged(event);
		}
	}

	private void fireStructureChanged(VariableListEvent cause) {
		TruthTableEvent event = new TruthTableEvent(this, cause);
		for (TruthTableListener l : listeners) {
			l.structureChanged(event);
		}
	}

	public int getInputColumnCount() {
		return model.getInputs().size();
	}

	public Entry getInputEntry(int row, int column) {
		int rows = getRowCount();
		int inputs = model.getInputs().size();
		if (row < 0 || row >= rows) {
			throw new IllegalArgumentException("row index: " + row + " size: "
					+ rows);
		}
		if (column < 0 || column >= inputs) {
			throw new IllegalArgumentException("column index: " + column
					+ " size: " + inputs);
		}

		return isInputSet(row, column, inputs) ? Entry.ONE : Entry.ZERO;
	}

	public String getInputHeader(int column) {
		return model.getInputs().get(column);
	}

	public int getInputIndex(String input) {
		return model.getInputs().indexOf(input);
	}

	public Entry[] getOutputColumn(int column) {
		int outputs = model.getOutputs().size();
		if (column < 0 || column >= outputs) {
			throw new IllegalArgumentException("index: " + column + " size: "
					+ outputs);
		}

		String outputName = model.getOutputs().get(column);
		Entry[] columnData = outputColumns.get(outputName);
		if (columnData == null) {
			columnData = new Entry[getRowCount()];
			Arrays.fill(columnData, DEFAULT_ENTRY);
			outputColumns.put(outputName, columnData);
		}
		return columnData;
	}

	public int getOutputColumnCount() {
		return model.getOutputs().size();
	}

	public Entry getOutputEntry(int row, int column) {
		int outputs = model.getOutputs().size();
		if (row < 0 || row >= getRowCount() || column < 0 || column >= outputs) {
			return Entry.DONT_CARE;
		} else {
			String outputName = model.getOutputs().get(column);
			Entry[] columnData = outputColumns.get(outputName);
			if (columnData == null)
				return DEFAULT_ENTRY;
			if (row < 0 || row >= columnData.length)
				return Entry.DONT_CARE;
			return columnData[row];
		}
	}

	public String getOutputHeader(int column) {
		return model.getOutputs().get(column);
	}

	public int getOutputIndex(String output) {
		return model.getOutputs().indexOf(output);
	}

	public int getRowCount() {
		int sz = model.getInputs().size();
		return 1 << sz;
	}

	public void removeTruthTableListener(TruthTableListener l) {
		listeners.remove(l);
	}

	public void setOutputColumn(int column, Entry[] values) {
		if (values != null && values.length != getRowCount()) {
			throw new IllegalArgumentException(
					"argument to setOutputColumn is wrong length");
		}

		int outputs = model.getOutputs().size();
		if (column < 0 || column >= outputs) {
			throw new IllegalArgumentException("index: " + column + " size: "
					+ outputs);
		}

		String outputName = model.getOutputs().get(column);
		Entry[] oldValues = outputColumns.get(outputName);
		if (oldValues == values)
			return;
		else if (values == null)
			outputColumns.remove(outputName);
		else
			outputColumns.put(outputName, values);
		fireCellsChanged(column);
	}

	public void setOutputEntry(int row, int column, Entry value) {
		int rows = getRowCount();
		int outputs = model.getOutputs().size();
		if (row < 0 || row >= rows) {
			throw new IllegalArgumentException("row index: " + row + " size: "
					+ rows);
		}
		if (column < 0 || column >= outputs) {
			throw new IllegalArgumentException("column index: " + column
					+ " size: " + outputs);
		}

		String outputName = model.getOutputs().get(column);
		Entry[] columnData = outputColumns.get(outputName);

		if (columnData == null) {
			if (value == DEFAULT_ENTRY)
				return;
			columnData = new Entry[getRowCount()];
			outputColumns.put(outputName, columnData);
			Arrays.fill(columnData, DEFAULT_ENTRY);
			columnData[row] = value;
		} else {
			if (columnData[row] == value)
				return;
			columnData[row] = value;
		}

		fireCellsChanged(column);
	}
}
