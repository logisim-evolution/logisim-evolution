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

package com.cburch.logisim.file;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.File;
import java.util.ArrayList;

import com.cburch.logisim.proj.Project;
import com.cburch.logisim.proj.Projects;

class ProjectsDirty {
	private static class DirtyListener implements LibraryListener {
		Project proj;

		DirtyListener(Project proj) {
			this.proj = proj;
		}

		public void libraryChanged(LibraryEvent event) {
			if (event.getAction() == LibraryEvent.DIRTY_STATE) {
				LogisimFile lib = proj.getLogisimFile();
				File file = lib.getLoader().getMainFile();
				LibraryManager.instance.setDirty(file, lib.isDirty());
			}
		}
	}

	private static class ProjectListListener implements PropertyChangeListener {
		public synchronized void propertyChange(PropertyChangeEvent event) {
			for (DirtyListener l : listeners) {
				l.proj.removeLibraryListener(l);
			}
			listeners.clear();
			for (Project proj : Projects.getOpenProjects()) {
				DirtyListener l = new DirtyListener(proj);
				proj.addLibraryListener(l);
				listeners.add(l);

				LogisimFile lib = proj.getLogisimFile();
				LibraryManager.instance.setDirty(lib.getLoader().getMainFile(),
						lib.isDirty());
			}
		}
	}

	public static void initialize() {
		Projects.addPropertyChangeListener(Projects.projectListProperty,
				projectListListener);
	}

	private static ProjectListListener projectListListener = new ProjectListListener();
	private static ArrayList<DirtyListener> listeners = new ArrayList<DirtyListener>();

	private ProjectsDirty() {
	}
}
