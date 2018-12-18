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

package com.cburch.logisim.gui.log;

import java.io.File;
import java.util.HashMap;
import java.util.Iterator;

import javax.swing.JFrame;

import com.cburch.logisim.circuit.CircuitState;
import com.cburch.logisim.data.Value;
import com.cburch.logisim.util.EventSourceWeakSupport;

public class Model {
	private EventSourceWeakSupport<ModelListener> listeners;
	private Selection selection;
	private HashMap<SelectionItem, ValueLog> log;
	private boolean fileEnabled = false;
	private File file = null;
	private boolean fileHeader = true;
	private boolean selected = false;
	private LogThread logger = null;

	public Model(CircuitState circuitState) {
		listeners = new EventSourceWeakSupport<ModelListener>();
		selection = new Selection(circuitState, this);
		log = new HashMap<SelectionItem, ValueLog>();
	}

	public void addModelListener(ModelListener l) {
		listeners.add(l);
	}

	private void fireEntryAdded(ModelEvent e, Value[] values) {
		for (ModelListener l : listeners) {
			l.entryAdded(e, values);
		}
	}

	private void fireFilePropertyChanged(ModelEvent e) {
		for (ModelListener l : listeners) {
			l.filePropertyChanged(e);
		}
	}

	void fireSelectionChanged(ModelEvent e) {
		for (Iterator<SelectionItem> it = log.keySet().iterator(); it.hasNext();) {
			SelectionItem i = it.next();
			if (selection.indexOf(i) < 0) {
				it.remove();
			}
		}

		for (ModelListener l : listeners) {
			l.selectionChanged(e);
		}
	}

	public CircuitState getCircuitState() {
		return selection.getCircuitState();
	}

	public File getFile() {
		return file;
	}

	public boolean getFileHeader() {
		return fileHeader;
	}

	public Selection getSelection() {
		return selection;
	}

	public ValueLog getValueLog(SelectionItem item) {
		ValueLog ret = log.get(item);
		if (ret == null && selection.indexOf(item) >= 0) {
			ret = new ValueLog();
			log.put(item, ret);
		}
		return ret;
	}

	public boolean isFileEnabled() {
		return fileEnabled;
	}

	public boolean isSelected() {
		return selected;
	}

	public void propagationCompleted() {
		CircuitState circuitState = getCircuitState();
		Value[] vals = new Value[selection.size()];
		boolean changed = false;
		for (int i = selection.size() - 1; i >= 0; i--) {
			SelectionItem item = selection.get(i);
			vals[i] = item.fetchValue(circuitState);
			if (!changed) {
				Value v = getValueLog(item).getLast();
				changed = v == null ? vals[i] != null : !v.equals(vals[i]);
			}
		}
		if (changed) {
			for (int i = selection.size() - 1; i >= 0; i--) {
				SelectionItem item = selection.get(i);
				getValueLog(item).append(vals[i]);
			}
			fireEntryAdded(new ModelEvent(), vals);
		}
	}

	public void removeModelListener(ModelListener l) {
		listeners.remove(l);
	}

	public void setFile(File value) {
		if (file == null ? value == null : file.equals(value))
			return;
		file = value;
		fileEnabled = file != null;
		fireFilePropertyChanged(new ModelEvent());
	}

	public void setFileEnabled(boolean value) {
		if (fileEnabled == value)
			return;
		fileEnabled = value;
		fireFilePropertyChanged(new ModelEvent());
	}

	public void setFileHeader(boolean value) {
		if (fileHeader == value)
			return;
		fileHeader = value;
		fireFilePropertyChanged(new ModelEvent());
	}

	public void setSelected(JFrame frame, boolean value) {
		if (selected == value)
			return;
		selected = value;
		if (selected) {
			logger = new LogThread(this);
			logger.start();
		} else {
			if (logger != null)
				logger.cancel();
			logger = null;
			fileEnabled = false;
		}
		fireFilePropertyChanged(new ModelEvent());
	}
}
