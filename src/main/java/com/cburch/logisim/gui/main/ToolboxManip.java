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

import javax.swing.JPopupMenu;

import com.cburch.logisim.circuit.Circuit;
import com.cburch.logisim.circuit.SubcircuitFactory;
import com.cburch.logisim.comp.ComponentFactory;
import com.cburch.logisim.data.AttributeEvent;
import com.cburch.logisim.data.AttributeListener;
import com.cburch.logisim.data.AttributeSet;
import com.cburch.logisim.file.LibraryEvent;
import com.cburch.logisim.file.LibraryEventSource;
import com.cburch.logisim.file.LibraryListener;
import com.cburch.logisim.file.LogisimFile;
import com.cburch.logisim.file.LogisimFileActions;
import com.cburch.logisim.gui.generic.AttrTableModel;
import com.cburch.logisim.gui.generic.ProjectExplorer;
import com.cburch.logisim.gui.generic.ProjectExplorerEvent;
import com.cburch.logisim.gui.generic.ProjectExplorerLibraryNode;
import com.cburch.logisim.gui.generic.ProjectExplorerListener;
import com.cburch.logisim.gui.generic.ProjectExplorerToolNode;
import com.cburch.logisim.gui.menu.Popups;
import com.cburch.logisim.gui.menu.ProjectCircuitActions;
import com.cburch.logisim.gui.menu.ProjectLibraryActions;
import com.cburch.logisim.proj.Project;
import com.cburch.logisim.proj.ProjectEvent;
import com.cburch.logisim.proj.ProjectListener;
import com.cburch.logisim.tools.AddTool;
import com.cburch.logisim.tools.Library;
import com.cburch.logisim.tools.Tool;

class ToolboxManip implements ProjectExplorerListener {
	private class MyListener implements ProjectListener, LibraryListener,
			AttributeListener {
		private LogisimFile curFile = null;

		private void addLibrary(Library lib) {
			if (lib instanceof LibraryEventSource) {
				((LibraryEventSource) lib).addLibraryListener(this);
			}
			for (Tool tool : lib.getTools()) {
				AttributeSet attrs = tool.getAttributeSet();
				if (attrs != null)
					attrs.addAttributeListener(this);
			}
		}

		public void attributeListChanged(AttributeEvent e) {
		}

		public void attributeValueChanged(AttributeEvent e) {
			explorer.repaint();
		}

		public void libraryChanged(LibraryEvent event) {
			int action = event.getAction();
			if (action == LibraryEvent.ADD_LIBRARY) {
				if (event.getSource() == curFile) {
					addLibrary((Library) event.getData());
				}
			} else if (action == LibraryEvent.REMOVE_LIBRARY) {
				if (event.getSource() == curFile) {
					removeLibrary((Library) event.getData());
				}
			} else if (action == LibraryEvent.ADD_TOOL) {
				Tool tool = (Tool) event.getData();
				AttributeSet attrs = tool.getAttributeSet();
				if (attrs != null)
					attrs.addAttributeListener(this);
			} else if (action == LibraryEvent.REMOVE_TOOL) {
				Tool tool = (Tool) event.getData();
				AttributeSet attrs = tool.getAttributeSet();
				if (attrs != null)
					attrs.removeAttributeListener(this);
			}
			explorer.repaint();
		}

		public void projectChanged(ProjectEvent event) {
			int action = event.getAction();
			if (action == ProjectEvent.ACTION_SET_FILE) {
				setFile((LogisimFile) event.getOldData(),
						(LogisimFile) event.getData());
				explorer.repaint();
			}
		}

		private void removeLibrary(Library lib) {
			if (lib instanceof LibraryEventSource) {
				((LibraryEventSource) lib).removeLibraryListener(this);
			}
			for (Tool tool : lib.getTools()) {
				AttributeSet attrs = tool.getAttributeSet();
				if (attrs != null)
					attrs.removeAttributeListener(this);
			}
		}

		private void setFile(LogisimFile oldFile, LogisimFile newFile) {
			if (oldFile != null) {
				removeLibrary(oldFile);
				for (Library lib : oldFile.getLibraries()) {
					removeLibrary(lib);
				}
			}
			curFile = newFile;
			if (newFile != null) {
				addLibrary(newFile);
				for (Library lib : newFile.getLibraries()) {
					addLibrary(lib);
				}
			}
		}

	}

	private Project proj;
	private ProjectExplorer explorer;
	private MyListener myListener = new MyListener();
	private Tool lastSelected = null;

	ToolboxManip(Project proj, ProjectExplorer explorer) {
		this.proj = proj;
		this.explorer = explorer;
		proj.addProjectListener(myListener);
		myListener.setFile(null, proj.getLogisimFile());
	}

	public void deleteRequested(ProjectExplorerEvent event) {
		Object request = event.getTarget();
		if (request instanceof ProjectExplorerLibraryNode) {
			Library lib = ((ProjectExplorerLibraryNode) request).getValue();
			ProjectLibraryActions.doUnloadLibrary(proj, lib);
		} else if (request instanceof ProjectExplorerToolNode) {
			Tool tool = ((ProjectExplorerToolNode) request).getValue();
			if (tool instanceof AddTool) {
				ComponentFactory factory = ((AddTool) tool).getFactory();
				if (factory instanceof SubcircuitFactory) {
					SubcircuitFactory circFact = (SubcircuitFactory) factory;
					ProjectCircuitActions.doRemoveCircuit(proj,
							circFact.getSubcircuit());
				}
			}
		}
	}

	public void doubleClicked(ProjectExplorerEvent event) {
		Object clicked = event.getTarget();
		if (clicked instanceof ProjectExplorerToolNode) {
			Tool baseTool = ((ProjectExplorerToolNode) clicked).getValue();
			if (baseTool instanceof AddTool) {
				AddTool tool = (AddTool) baseTool;
				ComponentFactory source = tool.getFactory();
				if (source instanceof SubcircuitFactory) {
					SubcircuitFactory circFact = (SubcircuitFactory) source;
					proj.setCurrentCircuit(circFact.getSubcircuit());
					proj.getFrame().setEditorView(Frame.EDIT_LAYOUT);
					if (lastSelected != null){
						proj.setTool(lastSelected);
					} else {
						Library base = proj.getLogisimFile().getLibrary("Base");
						if (base != null)
							proj.setTool(base.getTool("Edit Tool"));
					}
				}
			}
		}
	}

	public JPopupMenu menuRequested(ProjectExplorerEvent event) {
		Object clicked = event.getTarget();
		if (clicked instanceof ProjectExplorerToolNode) {
			Tool baseTool = ((ProjectExplorerToolNode) clicked).getValue();
			if (baseTool instanceof AddTool) {
				AddTool tool = (AddTool) baseTool;
				ComponentFactory source = tool.getFactory();
				if (source instanceof SubcircuitFactory) {
					Circuit circ = ((SubcircuitFactory) source).getSubcircuit();
					return Popups.forCircuit(proj, tool, circ);
				} else {
					return null;
				}
			} else {
				return null;
			}
		} else if (clicked instanceof ProjectExplorerLibraryNode) {
			Library lib = ((ProjectExplorerLibraryNode) clicked).getValue();
			if (lib == proj.getLogisimFile()) {
				return Popups.forProject(proj);
			} else {
				boolean is_top = event.getTreePath().getPathCount() <= 2;
				return Popups.forLibrary(proj, lib, is_top);
			}
		} else {
			return null;
		}
	}

	public void moveRequested(ProjectExplorerEvent event, AddTool dragged,
			AddTool target) {
		LogisimFile file = proj.getLogisimFile();
		int draggedIndex = file.getTools().indexOf(dragged);
		int targetIndex = file.getTools().indexOf(target);
		if (targetIndex > draggedIndex)
			targetIndex++;
		proj.doAction(LogisimFileActions.moveCircuit(dragged, targetIndex));
	}

	public void selectionChanged(ProjectExplorerEvent event) {
		Object selected = event.getTarget();
		if (selected instanceof ProjectExplorerToolNode) {
			Tool tool = ((ProjectExplorerToolNode) selected).getValue();
			if (selected instanceof AddTool) {
				AddTool addTool = (AddTool) tool;
				ComponentFactory source = addTool.getFactory();
				if (source instanceof SubcircuitFactory) {
					SubcircuitFactory circFact = (SubcircuitFactory) source;
					Circuit circ = circFact.getSubcircuit();
					if (proj.getCurrentCircuit() == circ) {
						AttrTableModel m = new AttrTableCircuitModel(proj, circ);
						proj.getFrame().setAttrTableModel(m);
						return;
					}
				}
			}

			// This was causing the selection to lag behind double-clicks,
			// commented-out
			// lastSelected = proj.getTool();
			proj.setTool(tool);
			proj.getFrame().viewAttributes(tool);
		}
	}

}
