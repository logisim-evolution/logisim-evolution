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

package com.cburch.logisim.gui.menu;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.JFrame;
import javax.swing.JMenu;
import javax.swing.JMenuItem;
import javax.swing.JPopupMenu;
import javax.swing.SwingUtilities;

import com.cburch.logisim.Main;
import com.cburch.logisim.circuit.Circuit;
import com.cburch.logisim.file.LoadedLibrary;
import com.cburch.logisim.file.Loader;
import com.cburch.logisim.file.LogisimFile;
import com.cburch.logisim.gui.main.Frame;
import com.cburch.logisim.gui.main.StatisticsDialog;
import com.cburch.logisim.proj.Project;
import com.cburch.logisim.tools.AddTool;
import com.cburch.logisim.tools.Library;
import com.cburch.logisim.tools.Tool;

public class Popups {
	@SuppressWarnings("serial")
	private static class CircuitPopup extends JPopupMenu implements
			ActionListener {
		Project proj;
		/* Tool tool; */
		Circuit circuit;
		JMenuItem analyze = new JMenuItem(
				Strings.get("projectAnalyzeCircuitItem"));
		JMenuItem stats = new JMenuItem(
				Strings.get("projectGetCircuitStatisticsItem"));
		JMenuItem main = new JMenuItem(Strings.get("projectSetAsMainItem"));
		JMenuItem remove = new JMenuItem(
				Strings.get("projectRemoveCircuitItem"));
		JMenuItem editLayout = new JMenuItem(
				Strings.get("projectEditCircuitLayoutItem"));
		JMenuItem editAppearance = new JMenuItem(
				Strings.get("projectEditCircuitAppearanceItem"));

		CircuitPopup(Project proj, Tool tool, Circuit circuit) {
			super(Strings.get("circuitMenu"));
			this.proj = proj;
			/* this.tool = tool; */
			this.circuit = circuit;

			add(editLayout);
			editLayout.addActionListener(this);
			add(editAppearance);
			editAppearance.addActionListener(this);
			if (Main.ANALYZE) {
				add(analyze);
				analyze.addActionListener(this);
			}
			add(stats);
			stats.addActionListener(this);
			addSeparator();
			add(main);
			main.addActionListener(this);
			add(remove);
			remove.addActionListener(this);

			boolean canChange = proj.getLogisimFile().contains(circuit);
			LogisimFile file = proj.getLogisimFile();
			if (circuit == proj.getCurrentCircuit()) {
				if (proj.getFrame().getEditorView()
						.equals(Frame.EDIT_APPEARANCE)) {
					editAppearance.setEnabled(false);
				} else {
					editLayout.setEnabled(false);
				}
			}
			main.setEnabled(canChange && file.getMainCircuit() != circuit);
			remove.setEnabled(canChange && file.getCircuitCount() > 1
					&& proj.getDependencies().canRemove(circuit));
		}

		public void actionPerformed(ActionEvent e) {
			Object source = e.getSource();
			if (source == editLayout) {
				proj.setCurrentCircuit(circuit);
				proj.getFrame().setEditorView(Frame.EDIT_LAYOUT);
			} else if (source == editAppearance) {
				proj.setCurrentCircuit(circuit);
				proj.getFrame().setEditorView(Frame.EDIT_APPEARANCE);
			} else if (source == analyze && Main.ANALYZE) {
				ProjectCircuitActions.doAnalyze(proj, circuit);
			} else if (source == stats) {
				JFrame frame = (JFrame) SwingUtilities.getRoot(this);
				StatisticsDialog.show(frame, proj.getLogisimFile(), circuit);
			} else if (source == main) {
				ProjectCircuitActions.doSetAsMainCircuit(proj, circuit);
			} else if (source == remove) {
				ProjectCircuitActions.doRemoveCircuit(proj, circuit);
			}
		}
	}

	@SuppressWarnings("serial")
	private static class LibraryPopup extends JPopupMenu implements
			ActionListener {
		Project proj;
		Library lib;
		JMenuItem unload = new JMenuItem(
				Strings.get("projectUnloadLibraryItem"));
		JMenuItem reload = new JMenuItem(
				Strings.get("projectReloadLibraryItem"));

		LibraryPopup(Project proj, Library lib, boolean is_top) {
			super(Strings.get("libMenu"));
			this.proj = proj;
			this.lib = lib;

			add(unload);
			unload.addActionListener(this);
			add(reload);
			reload.addActionListener(this);
			unload.setEnabled(is_top);
			reload.setEnabled(is_top && lib instanceof LoadedLibrary);
		}

		public void actionPerformed(ActionEvent e) {
			Object src = e.getSource();
			if (src == unload) {
				ProjectLibraryActions.doUnloadLibrary(proj, lib);
			} else if (src == reload) {
				Loader loader = proj.getLogisimFile().getLoader();
				loader.reload((LoadedLibrary) lib);
			}
		}
	}

	@SuppressWarnings("serial")
	private static class ProjectPopup extends JPopupMenu implements
			ActionListener {
		Project proj;
		JMenuItem add = new JMenuItem(Strings.get("projectAddCircuitItem"));
		JMenu load = new JMenu(Strings.get("projectLoadLibraryItem"));
		JMenuItem loadBuiltin = new JMenuItem(
				Strings.get("projectLoadBuiltinItem"));
		JMenuItem loadLogisim = new JMenuItem(
				Strings.get("projectLoadLogisimItem"));
		JMenuItem loadJar = new JMenuItem(Strings.get("projectLoadJarItem"));

		ProjectPopup(Project proj) {
			super(Strings.get("projMenu"));
			this.proj = proj;

			load.add(loadBuiltin);
			loadBuiltin.addActionListener(this);
			load.add(loadLogisim);
			loadLogisim.addActionListener(this);
			load.add(loadJar);
			loadJar.addActionListener(this);

			add(add);
			add.addActionListener(this);
			add(load);
		}

		public void actionPerformed(ActionEvent e) {
			Object src = e.getSource();
			if (src == add) {
				ProjectCircuitActions.doAddCircuit(proj);
			} else if (src == loadBuiltin) {
				ProjectLibraryActions.doLoadBuiltinLibrary(proj);
			} else if (src == loadLogisim) {
				ProjectLibraryActions.doLoadLogisimLibrary(proj);
			} else if (src == loadJar) {
				ProjectLibraryActions.doLoadJarLibrary(proj);
			}
		}
	}

	public static JPopupMenu forCircuit(Project proj, AddTool tool, Circuit circ) {
		return new CircuitPopup(proj, tool, circ);
	}

	public static JPopupMenu forLibrary(Project proj, Library lib, boolean isTop) {
		return new LibraryPopup(proj, lib, isTop);
	}

	public static JPopupMenu forProject(Project proj) {
		return new ProjectPopup(proj);
	}

	public static JPopupMenu forTool(Project proj, Tool tool) {
		return null;
	}

}
