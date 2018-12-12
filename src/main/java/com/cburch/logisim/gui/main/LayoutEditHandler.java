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

package com.cburch.logisim.gui.main;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;

import com.cburch.logisim.circuit.Circuit;
import com.cburch.logisim.file.LibraryEvent;
import com.cburch.logisim.file.LibraryListener;
import com.cburch.logisim.gui.menu.LogisimMenuBar;
import com.cburch.logisim.proj.Action;
import com.cburch.logisim.proj.Project;
import com.cburch.logisim.proj.ProjectEvent;
import com.cburch.logisim.proj.ProjectListener;
import com.cburch.logisim.std.base.Base;
import com.cburch.logisim.tools.Library;
import com.cburch.logisim.tools.Tool;

public class LayoutEditHandler extends EditHandler implements ProjectListener,
		LibraryListener, PropertyChangeListener {
	private Frame frame;

	LayoutEditHandler(Frame frame) {
		this.frame = frame;

		Project proj = frame.getProject();
		Clipboard.addPropertyChangeListener(Clipboard.contentsProperty, this);
		proj.addProjectListener(this);
		proj.addLibraryListener(this);
	}

	@Override
	public void addControlPoint() {
		; // not yet supported in layout mode
	}

	@Override
	public void computeEnabled() {
		Project proj = frame.getProject();
		Selection sel = proj == null ? null : proj.getSelection();
		boolean selEmpty = (sel == null ? true : sel.isEmpty());
		boolean canChange = proj != null
				&& proj.getLogisimFile().contains(proj.getCurrentCircuit());

		boolean selectAvailable = false;
		for (Library lib : proj.getLogisimFile().getLibraries()) {
			if (lib instanceof Base)
				selectAvailable = true;
		}

		setEnabled(LogisimMenuBar.CUT, !selEmpty && selectAvailable
				&& canChange);
		setEnabled(LogisimMenuBar.COPY, !selEmpty && selectAvailable);
		setEnabled(LogisimMenuBar.PASTE, selectAvailable && canChange
				&& !Clipboard.isEmpty());
		setEnabled(LogisimMenuBar.DELETE, !selEmpty && selectAvailable
				&& canChange);
		setEnabled(LogisimMenuBar.DUPLICATE, !selEmpty && selectAvailable
				&& canChange);
		setEnabled(LogisimMenuBar.SELECT_ALL, selectAvailable);
		setEnabled(LogisimMenuBar.RAISE, false);
		setEnabled(LogisimMenuBar.LOWER, false);
		setEnabled(LogisimMenuBar.RAISE_TOP, false);
		setEnabled(LogisimMenuBar.LOWER_BOTTOM, false);
		setEnabled(LogisimMenuBar.ADD_CONTROL, false);
		setEnabled(LogisimMenuBar.REMOVE_CONTROL, false);
	}

	@Override
	public void copy() {
		Project proj = frame.getProject();
		Selection sel = frame.getCanvas().getSelection();
		proj.doAction(SelectionActions.copy(sel));
	}

	@Override
	public void cut() {
		Project proj = frame.getProject();
		Selection sel = frame.getCanvas().getSelection();
		proj.doAction(SelectionActions.cut(sel));
	}

	@Override
	public void delete() {
		Project proj = frame.getProject();
		Selection sel = frame.getCanvas().getSelection();
		proj.doAction(SelectionActions.clear(sel));
	}

	@Override
	public void duplicate() {
		Project proj = frame.getProject();
		Selection sel = frame.getCanvas().getSelection();
		proj.doAction(SelectionActions.duplicate(sel));
	}

	public void libraryChanged(LibraryEvent e) {
		int action = e.getAction();
		if (action == LibraryEvent.ADD_LIBRARY) {
			computeEnabled();
		} else if (action == LibraryEvent.REMOVE_LIBRARY) {
			computeEnabled();
		}
	}

	@Override
	public void lower() {
		; // not yet supported in layout mode
	}

	@Override
	public void lowerBottom() {
		; // not yet supported in layout mode
	}

	@Override
	public void paste() {
		Project proj = frame.getProject();
		Selection sel = frame.getCanvas().getSelection();
		selectSelectTool(proj);
		Action action = SelectionActions.pasteMaybe(proj, sel);
		if (action != null) {
			proj.doAction(action);
		}
	}

	public void projectChanged(ProjectEvent e) {
		int action = e.getAction();
		if (action == ProjectEvent.ACTION_SET_FILE) {
			computeEnabled();
		} else if (action == ProjectEvent.ACTION_SET_CURRENT) {
			computeEnabled();
		} else if (action == ProjectEvent.ACTION_SELECTION) {
			computeEnabled();
		}
	}

	public void propertyChange(PropertyChangeEvent event) {
		if (event.getPropertyName().equals(Clipboard.contentsProperty)) {
			computeEnabled();
		}
	}

	@Override
	public void raise() {
		; // not yet supported in layout mode
	}

	@Override
	public void raiseTop() {
		; // not yet supported in layout mode
	}

	@Override
	public void removeControlPoint() {
		; // not yet supported in layout mode
	}

	@Override
	public void selectAll() {
		Project proj = frame.getProject();
		Selection sel = frame.getCanvas().getSelection();
		selectSelectTool(proj);
		Circuit circ = proj.getCurrentCircuit();
		sel.addAll(circ.getWires());
		sel.addAll(circ.getNonWires());
		proj.repaintCanvas();
	}

	private void selectSelectTool(Project proj) {
		for (Library sub : proj.getLogisimFile().getLibraries()) {
			if (sub instanceof Base) {
				Base base = (Base) sub;
				Tool tool = base.getTool("Edit Tool");
				if (tool != null)
					proj.setTool(tool);
			}
		}
	}
}
