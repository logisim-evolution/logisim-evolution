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

package com.cburch.draw.undo;

import java.util.LinkedList;

import com.cburch.logisim.util.EventSourceWeakSupport;

public class UndoLog {
	private static final int MAX_UNDO_SIZE = 64;

	private EventSourceWeakSupport<UndoLogListener> listeners;
	private LinkedList<Action> undoLog;
	private LinkedList<Action> redoLog;
	private int modCount;

	public UndoLog() {
		this.listeners = new EventSourceWeakSupport<UndoLogListener>();
		this.undoLog = new LinkedList<Action>();
		this.redoLog = new LinkedList<Action>();
		this.modCount = 0;
	}

	//
	// listening methods
	//
	public void addProjectListener(UndoLogListener what) {
		listeners.add(what);
	}

	public void clearModified() {
		modCount = 0;
	}

	//
	// mutator methods
	//
	public void doAction(Action act) {
		if (act == null)
			return;
		act.doIt();
		logAction(act);
	}

	private void fireEvent(int action, Action actionObject) {
		UndoLogEvent e = null;
		for (UndoLogListener listener : listeners) {
			if (e == null)
				e = new UndoLogEvent(this, action, actionObject);
			listener.undoLogChanged(e);
		}
	}

	public Action getRedoAction() {
		if (redoLog.isEmpty()) {
			return null;
		} else {
			return redoLog.getLast();
		}
	}

	//
	// accessor methods
	//
	public Action getUndoAction() {
		if (undoLog.isEmpty()) {
			return null;
		} else {
			return undoLog.getLast();
		}
	}

	public boolean isModified() {
		return modCount != 0;
	}

	public void logAction(Action act) {
		redoLog.clear();
		if (!undoLog.isEmpty()) {
			Action prev = undoLog.getLast();
			if (act.shouldAppendTo(prev)) {
				if (prev.isModification())
					--modCount;
				Action joined = prev.append(act);
				if (joined == null) {
					fireEvent(UndoLogEvent.ACTION_DONE, act);
					return;
				}
				act = joined;
			}
			while (undoLog.size() > MAX_UNDO_SIZE) {
				undoLog.removeFirst();
			}
		}
		undoLog.add(act);
		if (act.isModification())
			++modCount;
		fireEvent(UndoLogEvent.ACTION_DONE, act);
	}

	public void redoAction() {
		if (!redoLog.isEmpty()) {
			Action action = redoLog.removeLast();
			if (action.isModification())
				++modCount;
			action.doIt();
			undoLog.add(action);
			fireEvent(UndoLogEvent.ACTION_DONE, action);
		}
	}

	public void removeProjectListener(UndoLogListener what) {
		listeners.remove(what);
	}

	public void undoAction() {
		if (!undoLog.isEmpty()) {
			Action action = undoLog.removeLast();
			if (action.isModification())
				--modCount;
			action.undo();
			redoLog.add(action);
			fireEvent(UndoLogEvent.ACTION_UNDONE, action);
		}
	}
}
