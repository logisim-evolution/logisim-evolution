/*
 * This file is part of logisim-evolution.
 *
 * Logisim-evolution is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by the
 * Free Software Foundation, either version 3 of the License, or (at your
 * option) any later version.
 *
 * Logisim-evolution is distributed in the hope that it will be useful, but
 * WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 * or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
 * for more details.
 *
 * You should have received a copy of the GNU General Public License along
 * with logisim-evolution. If not, see <http://www.gnu.org/licenses/>.
 *
 * Original code by Carl Burch (http://www.cburch.com), 2011.
 * Subsequent modifications by:
 *   + College of the Holy Cross
 *     http://www.holycross.edu
 *   + Haute École Spécialisée Bernoise/Berner Fachhochschule
 *     http://www.bfh.ch
 *   + Haute École du paysage, d'ingénierie et d'architecture de Genève
 *     http://hepia.hesge.ch/
 *   + Haute École d'Ingénierie et de Gestion du Canton de Vaud
 *     http://www.heig-vd.ch/
 */

package com.cburch.logisim.gui.main;

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
import com.cburch.logisim.gui.generic.ProjectExplorerLibraryNode;
import com.cburch.logisim.gui.generic.ProjectExplorerToolNode;
import com.cburch.logisim.gui.menu.Popups;
import com.cburch.logisim.gui.menu.ProjectCircuitActions;
import com.cburch.logisim.gui.menu.ProjectLibraryActions;
import com.cburch.logisim.proj.Project;
import com.cburch.logisim.proj.ProjectEvent;
import com.cburch.logisim.proj.ProjectListener;
import com.cburch.logisim.std.base.BaseLibrary;
import com.cburch.logisim.tools.AddTool;
import com.cburch.logisim.tools.EditTool;
import com.cburch.logisim.tools.Library;
import com.cburch.logisim.tools.PokeTool;
import com.cburch.logisim.tools.Tool;
import com.cburch.logisim.vhdl.base.VhdlContent;
import com.cburch.logisim.vhdl.base.VhdlEntity;
import javax.swing.JPopupMenu;

class ToolboxManip implements ProjectExplorer.Listener {
  private final Project proj;
  private final ProjectExplorer explorer;
  private final MyListener myListener = new MyListener();
  private Tool lastSelected = null;

  ToolboxManip(Project proj, ProjectExplorer explorer) {
    this.proj = proj;
    this.explorer = explorer;
    proj.addProjectListener(myListener);
    myListener.setFile(null, proj.getLogisimFile());
  }

  private static void setDefaultTool(Tool lastSelected, Project proj) {
    if (lastSelected != null) {
      proj.setTool(lastSelected);
    } else {
      for (Library sub : proj.getLogisimFile().getLibraries()) {
        if (sub instanceof BaseLibrary) {
          Tool tool = sub.getTool(EditTool._ID);
          if (tool != null) {
            proj.setTool(tool);
            break;
          }
        }
      }
    }
  }

  @Override
  public void deleteRequested(ProjectExplorer.Event event) {
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
          ProjectCircuitActions.doRemoveCircuit(proj, circFact.getSubcircuit());
        }
      }
    }
  }

  @Override
  public void doubleClicked(ProjectExplorer.Event event) {
    Object clicked = event.getTarget();
    if (clicked instanceof ProjectExplorerToolNode) {
      ((ProjectExplorerToolNode) clicked).fireNodeChanged();
      Tool baseTool = ((ProjectExplorerToolNode) clicked).getValue();
      if (baseTool instanceof AddTool) {
        AddTool tool = (AddTool) baseTool;
        ComponentFactory source = tool.getFactory();
        if (source instanceof SubcircuitFactory) {
          SubcircuitFactory circFact = (SubcircuitFactory) source;
          proj.setCurrentCircuit(circFact.getSubcircuit());
          proj.getFrame().setEditorView(Frame.EDIT_LAYOUT);
          setDefaultTool(lastSelected, proj);
        } else if (source instanceof VhdlEntity) {
          VhdlEntity vhdl = (VhdlEntity) source;
          proj.setCurrentHdlModel(vhdl.getContent());
        }
      }
    }
  }

  @Override
  public JPopupMenu menuRequested(ProjectExplorer.Event event) {
    Object clicked = event.getTarget();
    if (clicked instanceof ProjectExplorerToolNode) {
      Tool baseTool = ((ProjectExplorerToolNode) clicked).getValue();
      if (baseTool instanceof AddTool) {
        AddTool tool = (AddTool) baseTool;
        ComponentFactory source = tool.getFactory();
        if (source instanceof SubcircuitFactory) {
          Circuit circ = ((SubcircuitFactory) source).getSubcircuit();
          return Popups.forCircuit(proj, tool, circ);
        } else if (source instanceof VhdlEntity) {
          VhdlContent vhdl = ((VhdlEntity) source).getContent();
          return Popups.forVhdl(proj, tool, vhdl);
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
        final var isTop = event.getTreePath().getPathCount() <= 2;
        return Popups.forLibrary(proj, lib, isTop);
      }
    } else {
      return null;
    }
  }

  @Override
  public void moveRequested(ProjectExplorer.Event event, AddTool dragged, AddTool target) {
    LogisimFile file = proj.getLogisimFile();
    int draggedIndex = file.getTools().indexOf(dragged);
    int targetIndex = file.getTools().indexOf(target);
    if (targetIndex > draggedIndex) targetIndex++;
    proj.doAction(LogisimFileActions.moveCircuit(dragged, targetIndex));
  }

  @Override
  public void selectionChanged(ProjectExplorer.Event event) {
    if (proj.getTool() instanceof PokeTool || proj.getTool() instanceof EditTool) {
      lastSelected = proj.getTool();
    }
    Object selected = event.getTarget();
    if (selected instanceof ProjectExplorerToolNode) {
      ((ProjectExplorerToolNode) selected).fireNodeChanged();
      Tool tool = ((ProjectExplorerToolNode) selected).getValue();
      if (tool instanceof AddTool) {
        AddTool addTool = (AddTool) tool;
        ComponentFactory source = addTool.getFactory();
        if (source instanceof SubcircuitFactory) {
          SubcircuitFactory circFact = (SubcircuitFactory) source;
          Circuit circ = circFact.getSubcircuit();
          if (proj.getCurrentCircuit() == circ) {
            AttrTableModel m = new AttrTableCircuitModel(proj, circ);
            proj.getFrame().setAttrTableModel(m);
            setDefaultTool(lastSelected, proj);
            return;
          }
        }
      }
      proj.setTool(tool);
      proj.getFrame().viewAttributes(tool);
    }
  }

  private class MyListener implements ProjectListener, LibraryListener, AttributeListener {
    private LogisimFile curFile = null;

    private void addLibrary(Library lib) {
      if (lib instanceof LibraryEventSource) {
        ((LibraryEventSource) lib).addLibraryListener(this);
      }
      for (Tool tool : lib.getTools()) {
        AttributeSet attrs = tool.getAttributeSet();
        if (attrs != null) attrs.addAttributeListener(this);
      }
    }

    @Override
    public void attributeValueChanged(AttributeEvent e) {
      explorer.repaint();
    }

    @Override
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
        if (attrs != null) attrs.addAttributeListener(this);
      } else if (action == LibraryEvent.REMOVE_TOOL) {
        Tool tool = (Tool) event.getData();
        AttributeSet attrs = tool.getAttributeSet();
        if (attrs != null) attrs.removeAttributeListener(this);
      }
      explorer.repaint();
    }

    @Override
    public void projectChanged(ProjectEvent event) {
      int action = event.getAction();
      if (action == ProjectEvent.ACTION_SET_FILE) {
        setFile((LogisimFile) event.getOldData(), (LogisimFile) event.getData());
        explorer.repaint();
      }
    }

    private void removeLibrary(Library lib) {
      if (lib instanceof LibraryEventSource) {
        ((LibraryEventSource) lib).removeLibraryListener(this);
      }
      for (Tool tool : lib.getTools()) {
        AttributeSet attrs = tool.getAttributeSet();
        if (attrs != null) attrs.removeAttributeListener(this);
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
}
