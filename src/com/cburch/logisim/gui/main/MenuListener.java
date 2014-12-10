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

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.ArrayList;
import java.util.List;

import com.cburch.draw.model.CanvasModelEvent;
import com.cburch.draw.model.CanvasModelListener;
import com.cburch.logisim.Main;
import com.cburch.logisim.circuit.Circuit;
import com.cburch.logisim.circuit.CircuitState;
import com.cburch.logisim.circuit.Simulator;
import com.cburch.logisim.file.LibraryEvent;
import com.cburch.logisim.file.LibraryListener;
import com.cburch.logisim.file.LogisimFile;
import com.cburch.logisim.gui.appear.RevertAppearanceAction;
import com.cburch.logisim.gui.generic.CardPanel;
import com.cburch.logisim.gui.menu.LogisimMenuBar;
import com.cburch.logisim.gui.menu.LogisimMenuItem;
import com.cburch.logisim.gui.menu.ProjectCircuitActions;
import com.cburch.logisim.gui.menu.SimulateListener;
import com.cburch.logisim.proj.Project;
import com.cburch.logisim.proj.ProjectEvent;
import com.cburch.logisim.proj.ProjectListener;

class MenuListener {
	private class EditListener implements ActionListener, EditHandler.Listener {
		private EditHandler handler = null;

		public void actionPerformed(ActionEvent e) {
			Object src = e.getSource();
			EditHandler h = handler;
			if (src == LogisimMenuBar.CUT) {
				if (h != null)
					h.cut();
			} else if (src == LogisimMenuBar.COPY) {
				if (h != null)
					h.copy();
			} else if (src == LogisimMenuBar.PASTE) {
				if (h != null)
					h.paste();
			} else if (src == LogisimMenuBar.DELETE) {
				if (h != null)
					h.delete();
			} else if (src == LogisimMenuBar.DUPLICATE) {
				if (h != null)
					h.duplicate();
			} else if (src == LogisimMenuBar.SELECT_ALL) {
				if (h != null)
					h.selectAll();
			} else if (src == LogisimMenuBar.RAISE) {
				if (h != null)
					h.raise();
			} else if (src == LogisimMenuBar.LOWER) {
				if (h != null)
					h.lower();
			} else if (src == LogisimMenuBar.RAISE_TOP) {
				if (h != null)
					h.raiseTop();
			} else if (src == LogisimMenuBar.LOWER_BOTTOM) {
				if (h != null)
					h.lowerBottom();
			} else if (src == LogisimMenuBar.ADD_CONTROL) {
				if (h != null)
					h.addControlPoint();
			} else if (src == LogisimMenuBar.REMOVE_CONTROL) {
				if (h != null)
					h.removeControlPoint();
			}
		}

		public void enableChanged(EditHandler handler, LogisimMenuItem action,
				boolean value) {
			if (handler == this.handler) {
				menubar.setEnabled(action, value);
				fireEnableChanged();
			}
		}

		private void register() {
			menubar.addActionListener(LogisimMenuBar.CUT, this);
			menubar.addActionListener(LogisimMenuBar.COPY, this);
			menubar.addActionListener(LogisimMenuBar.PASTE, this);
			menubar.addActionListener(LogisimMenuBar.DELETE, this);
			menubar.addActionListener(LogisimMenuBar.DUPLICATE, this);
			menubar.addActionListener(LogisimMenuBar.SELECT_ALL, this);
			menubar.addActionListener(LogisimMenuBar.RAISE, this);
			menubar.addActionListener(LogisimMenuBar.LOWER, this);
			menubar.addActionListener(LogisimMenuBar.RAISE_TOP, this);
			menubar.addActionListener(LogisimMenuBar.LOWER_BOTTOM, this);
			menubar.addActionListener(LogisimMenuBar.ADD_CONTROL, this);
			menubar.addActionListener(LogisimMenuBar.REMOVE_CONTROL, this);
			if (handler != null)
				handler.computeEnabled();
		}

		private void setHandler(EditHandler value) {
			handler = value;
			value.setListener(this);
			handler.computeEnabled();
		}
	}

	interface EnabledListener {
		public void menuEnableChanged(MenuListener source);
	}

	private class FileListener implements ActionListener {
		public void actionPerformed(ActionEvent event) {
			Object src = event.getSource();
			Project proj = frame.getProject();
			if (src == LogisimMenuBar.EXPORT_IMAGE) {
				ExportImage.doExport(proj);
			} else if (src == LogisimMenuBar.PRINT) {
				Print.doPrint(proj);
			}
		}

		private void register() {
			menubar.addActionListener(LogisimMenuBar.EXPORT_IMAGE, this);
			menubar.addActionListener(LogisimMenuBar.PRINT, this);
		}
	}

	class ProjectMenuListener implements ProjectListener, LibraryListener,
			ActionListener, PropertyChangeListener, CanvasModelListener {
		public void actionPerformed(ActionEvent event) {
			Object src = event.getSource();
			Project proj = frame.getProject();
			Circuit cur = proj == null ? null : proj.getCurrentCircuit();
			if (src == LogisimMenuBar.ADD_CIRCUIT) {
				ProjectCircuitActions.doAddCircuit(proj);
			} else if (src == LogisimMenuBar.MOVE_CIRCUIT_UP) {
				ProjectCircuitActions.doMoveCircuit(proj, cur, -1);
			} else if (src == LogisimMenuBar.MOVE_CIRCUIT_DOWN) {
				ProjectCircuitActions.doMoveCircuit(proj, cur, 1);
			} else if (src == LogisimMenuBar.SET_MAIN_CIRCUIT) {
				ProjectCircuitActions.doSetAsMainCircuit(proj, cur);
			} else if (src == LogisimMenuBar.REMOVE_CIRCUIT) {
				ProjectCircuitActions.doRemoveCircuit(proj, cur);
			} else if (src == LogisimMenuBar.EDIT_LAYOUT) {
				frame.setEditorView(Frame.EDIT_LAYOUT);
			} else if (src == LogisimMenuBar.EDIT_APPEARANCE) {
				frame.setEditorView(Frame.EDIT_APPEARANCE);
			} else if (src == LogisimMenuBar.VIEW_TOOLBOX) {
				frame.setExplorerView(Frame.VIEW_TOOLBOX);
			} else if (src == LogisimMenuBar.VIEW_SIMULATION) {
				frame.setExplorerView(Frame.VIEW_SIMULATION);
			} else if (src == LogisimMenuBar.REVERT_APPEARANCE) {
				proj.doAction(new RevertAppearanceAction(cur));
			} else if (src == LogisimMenuBar.ANALYZE_CIRCUIT && Main.ANALYZE) {
				ProjectCircuitActions.doAnalyze(proj, cur);
			} else if (src == LogisimMenuBar.CIRCUIT_STATS) {
				StatisticsDialog.show(frame, proj.getLogisimFile(), cur);
			}
		}

		private void computeEnabled() {
			Project proj = frame.getProject();
			LogisimFile file = proj.getLogisimFile();
			Circuit cur = proj.getCurrentCircuit();
			int curIndex = file.getCircuits().indexOf(cur);
			boolean isProjectCircuit = curIndex >= 0;
			String editorView = frame.getEditorView();
			String explorerView = frame.getExplorerView();
			boolean canSetMain = false;
			boolean canMoveUp = false;
			boolean canMoveDown = false;
			boolean canRemove = false;
			boolean canRevert = false;
			boolean viewAppearance = editorView.equals(Frame.EDIT_APPEARANCE);
			boolean viewLayout = editorView.equals(Frame.EDIT_LAYOUT);
			boolean viewToolbox = explorerView.equals(Frame.VIEW_TOOLBOX);
			boolean viewSimulation = explorerView.equals(Frame.VIEW_SIMULATION);
			if (isProjectCircuit) {
				List<?> tools = proj.getLogisimFile().getTools();

				canSetMain = proj.getLogisimFile().getMainCircuit() != cur;
				canMoveUp = curIndex > 0;
				canMoveDown = curIndex < tools.size() - 1;
				canRemove = tools.size() > 1;
				canRevert = viewAppearance
						&& !cur.getAppearance().isDefaultAppearance();
			}

			menubar.setEnabled(LogisimMenuBar.ADD_CIRCUIT, true);
			menubar.setEnabled(LogisimMenuBar.MOVE_CIRCUIT_UP, canMoveUp);
			menubar.setEnabled(LogisimMenuBar.MOVE_CIRCUIT_DOWN, canMoveDown);
			menubar.setEnabled(LogisimMenuBar.SET_MAIN_CIRCUIT, canSetMain);
			menubar.setEnabled(LogisimMenuBar.REMOVE_CIRCUIT, canRemove);
			menubar.setEnabled(LogisimMenuBar.VIEW_TOOLBOX, !viewToolbox);
			menubar.setEnabled(LogisimMenuBar.VIEW_SIMULATION, !viewSimulation);
			menubar.setEnabled(LogisimMenuBar.EDIT_LAYOUT, !viewLayout);
			menubar.setEnabled(LogisimMenuBar.EDIT_APPEARANCE, !viewAppearance);
			menubar.setEnabled(LogisimMenuBar.REVERT_APPEARANCE, canRevert);
			menubar.setEnabled(LogisimMenuBar.ANALYZE_CIRCUIT, true);
			menubar.setEnabled(LogisimMenuBar.CIRCUIT_STATS, true);
			fireEnableChanged();
		}

		private void computeRevertEnabled() {
			// do this separately since it can happen rather often
			Project proj = frame.getProject();
			LogisimFile file = proj.getLogisimFile();
			Circuit cur = proj.getCurrentCircuit();
			boolean isProjectCircuit = file.contains(cur);
			boolean viewAppearance = frame.getEditorView().equals(
					Frame.EDIT_APPEARANCE);
			boolean canRevert = isProjectCircuit && viewAppearance
					&& !cur.getAppearance().isDefaultAppearance();
			boolean oldValue = menubar
					.isEnabled(LogisimMenuBar.REVERT_APPEARANCE);
			if (canRevert != oldValue) {
				menubar.setEnabled(LogisimMenuBar.REVERT_APPEARANCE, canRevert);
				fireEnableChanged();
			}
		}

		public void libraryChanged(LibraryEvent event) {
			computeEnabled();
		}

		public void modelChanged(CanvasModelEvent event) {
			computeRevertEnabled();
		}

		public void projectChanged(ProjectEvent event) {
			int action = event.getAction();
			if (action == ProjectEvent.ACTION_SET_CURRENT) {
				Circuit old = (Circuit) event.getOldData();
				if (old != null) {
					old.getAppearance().removeCanvasModelListener(this);
				}
				Circuit circ = (Circuit) event.getData();
				if (circ != null) {
					circ.getAppearance().addCanvasModelListener(this);
				}
				computeEnabled();
			} else if (action == ProjectEvent.ACTION_SET_FILE) {
				computeEnabled();
			}
		}

		public void propertyChange(PropertyChangeEvent e) {
			computeEnabled();
		}

		void register() {
			Project proj = frame.getProject();
			if (proj == null) {
				return;
			}

			proj.addProjectListener(this);
			proj.addLibraryListener(this);
			frame.addPropertyChangeListener(Frame.EDITOR_VIEW, this);
			frame.addPropertyChangeListener(Frame.EXPLORER_VIEW, this);
			Circuit circ = proj.getCurrentCircuit();
			if (circ != null) {
				circ.getAppearance().addCanvasModelListener(this);
			}

			menubar.addActionListener(LogisimMenuBar.ADD_CIRCUIT, this);
			menubar.addActionListener(LogisimMenuBar.MOVE_CIRCUIT_UP, this);
			menubar.addActionListener(LogisimMenuBar.MOVE_CIRCUIT_DOWN, this);
			menubar.addActionListener(LogisimMenuBar.SET_MAIN_CIRCUIT, this);
			menubar.addActionListener(LogisimMenuBar.REMOVE_CIRCUIT, this);
			menubar.addActionListener(LogisimMenuBar.EDIT_LAYOUT, this);
			menubar.addActionListener(LogisimMenuBar.EDIT_APPEARANCE, this);
			menubar.addActionListener(LogisimMenuBar.VIEW_TOOLBOX, this);
			menubar.addActionListener(LogisimMenuBar.VIEW_SIMULATION, this);
			menubar.addActionListener(LogisimMenuBar.REVERT_APPEARANCE, this);
			menubar.addActionListener(LogisimMenuBar.ANALYZE_CIRCUIT, this);
			menubar.addActionListener(LogisimMenuBar.CIRCUIT_STATS, this);

			computeEnabled();
		}
	}

	class SimulateMenuListener implements ProjectListener, SimulateListener {
		public void projectChanged(ProjectEvent event) {
			if (event.getAction() == ProjectEvent.ACTION_SET_STATE) {
				menubar.setCircuitState(frame.getProject().getSimulator(),
						frame.getProject().getCircuitState());
			}
		}

		void register() {
			Project proj = frame.getProject();
			proj.addProjectListener(this);
			menubar.setSimulateListener(this);
			menubar.setCircuitState(proj.getSimulator(), proj.getCircuitState());
		}

		public void stateChangeRequested(Simulator sim, CircuitState state) {
			if (state != null)
				frame.getProject().setCircuitState(state);
		}
	}

	private Frame frame;
	private LogisimMenuBar menubar;
	private ArrayList<EnabledListener> listeners;
	private FileListener fileListener = new FileListener();
	private EditListener editListener = new EditListener();
	private ProjectMenuListener projectListener = new ProjectMenuListener();
	private SimulateMenuListener simulateListener = new SimulateMenuListener();

	public MenuListener(Frame frame, LogisimMenuBar menubar) {
		this.frame = frame;
		this.menubar = menubar;
		this.listeners = new ArrayList<EnabledListener>();
	}

	public void addEnabledListener(EnabledListener listener) {
		listeners.add(listener);
	}

	public void doAction(LogisimMenuItem item) {
		menubar.doAction(item);
	}

	private void fireEnableChanged() {
		for (EnabledListener listener : listeners) {
			listener.menuEnableChanged(this);
		}
	}

	LogisimMenuBar getMenuBar() {
		return menubar;
	}

	public boolean isEnabled(LogisimMenuItem item) {
		return menubar.isEnabled(item);
	}

	public void register(CardPanel mainPanel) {
		fileListener.register();
		editListener.register();
		projectListener.register();
		simulateListener.register();
	}

	public void removeEnabledListener(EnabledListener listener) {
		listeners.remove(listener);
	}

	public void setEditHandler(EditHandler handler) {
		editListener.setHandler(handler);
	}
}
