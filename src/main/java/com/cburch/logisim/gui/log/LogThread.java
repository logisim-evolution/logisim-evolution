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

import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import com.cburch.logisim.util.UniquelyNamedThread;

import com.cburch.logisim.data.Value;

class LogThread extends UniquelyNamedThread implements ModelListener {
	// file will be flushed with at least this frequency
	private static final int FLUSH_FREQUENCY = 500;

	// file will be closed after waiting this many milliseconds between writes
	private static final int IDLE_UNTIL_CLOSE = 10000;

	private Model model;
	private boolean canceled = false;
	private Object lock = new Object();
	private PrintWriter writer = null;
	private boolean headerDirty = true;
	private long lastWrite = 0;

	public LogThread(Model model) {
		super("LogThread");
		this.model = model;
		model.addModelListener(this);
	}

	// Should hold lock and have verified that isFileEnabled() before
	// entering this method.
	private void addEntry(Value[] values) {
		if (writer == null) {
			try {
				writer = new PrintWriter(new FileWriter(model.getFile(), true));
			} catch (IOException e) {
				model.setFile(null);
				return;
			}
		}
		Selection sel = model.getSelection();
		if (headerDirty) {
			if (model.getFileHeader()) {
				StringBuilder buf = new StringBuilder();
				for (int i = 0; i < sel.size(); i++) {
					if (i > 0)
						buf.append("\t");
					buf.append(sel.get(i).toString());
				}
				writer.println(buf.toString());
			}
			headerDirty = false;
		}
		StringBuilder buf = new StringBuilder();
		for (int i = 0; i < values.length; i++) {
			if (i > 0)
				buf.append("\t");
			if (values[i] != null) {
				int radix = sel.get(i).getRadix();
				buf.append(values[i].toDisplayString(radix));
			}
		}
		writer.println(buf.toString());
		lastWrite = System.currentTimeMillis();
	}

	public void cancel() {
		synchronized (lock) {
			canceled = true;
			if (writer != null) {
				writer.close();
				writer = null;
			}
		}
	}

	public void entryAdded(ModelEvent event, Value[] values) {
		synchronized (lock) {
			if (isFileEnabled())
				addEntry(values);
		}
	}

	public void filePropertyChanged(ModelEvent event) {
		synchronized (lock) {
			if (isFileEnabled()) {
				if (writer == null) {
					Selection sel = model.getSelection();
					Value[] values = new Value[sel.size()];
					boolean found = false;
					for (int i = 0; i < values.length; i++) {
						values[i] = model.getValueLog(sel.get(i)).getLast();
						if (values[i] != null)
							found = true;
					}
					if (found)
						addEntry(values);
				}
			} else {
				if (writer != null) {
					writer.close();
					writer = null;
				}
			}
		}
	}

	private boolean isFileEnabled() {
		return !canceled && model.isSelected() && model.isFileEnabled()
				&& model.getFile() != null;
	}

	@Override
	public void run() {
		while (!canceled) {
			synchronized (lock) {
				if (writer != null) {
					if (System.currentTimeMillis() - lastWrite > IDLE_UNTIL_CLOSE) {
						writer.close();
						writer = null;
					} else {
						writer.flush();
					}
				}
			}
			try {
				Thread.sleep(FLUSH_FREQUENCY);
			} catch (InterruptedException e) {
			}
		}
		synchronized (lock) {
			if (writer != null) {
				writer.close();
				writer = null;
			}
		}
	}

	public void selectionChanged(ModelEvent event) {
		headerDirty = true;
	}
}
