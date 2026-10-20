/*
 * Logisim-evolution - digital logic design tool and simulator
 * Copyright by the Logisim-evolution developers
 *
 * https://github.com/logisim-evolution/
 *
 * This is free software released under GNU GPLv3 license
 */

package com.cburch.logisim.gui.main;

import com.cburch.logisim.circuit.SubcircuitFactory;
import com.cburch.logisim.data.AttributeEvent;
import com.cburch.logisim.data.AttributeListener;
import com.cburch.logisim.file.LibraryEvent;
import com.cburch.logisim.file.LibraryEventSource;
import com.cburch.logisim.file.LibraryListener;
import com.cburch.logisim.file.LogisimFile;
import com.cburch.logisim.file.LogisimFileActions;
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
      for (final var sub : proj.getLogisimFile().getLibraries()) {
        if (sub instanceof BaseLibrary) {
          final var tool = sub.getTool(EditTool._ID);
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
    final var request = event.getTarget();
    if (request instanceof ProjectExplorerLibraryNode libNode) {
      ProjectLibraryActions.doUnloadLibrary(proj, libNode.getValue());
    } else if (request instanceof ProjectExplorerToolNode toolNode) {
      final var tool = toolNode.getValue();
      if (tool instanceof AddTool addTool) {
        final var factory = addTool.getFactory();
        if (factory instanceof SubcircuitFactory circFact) {
          ProjectCircuitActions.doRemoveCircuit(proj, circFact.getSubcircuit());
        }
      }
    }
  }

  @Override
  public void doubleClicked(ProjectExplorer.Event event) {
    final var clicked = event.getTarget();
    if (clicked instanceof ProjectExplorerToolNode clickedNode) {
      clickedNode.fireNodeChanged();
      final var baseTool = ((ProjectExplorerToolNode) clicked).getValue();
      if (baseTool instanceof AddTool tool) {
        final var source = tool.getFactory();
        if (source instanceof SubcircuitFactory circFact) {
          proj.setCurrentCircuit(circFact.getSubcircuit());
          proj.getFrame().setEditorView(Frame.EDIT_LAYOUT);
          setDefaultTool(lastSelected, proj);
        } else if (source instanceof VhdlEntity vhdl) {
          proj.setCurrentHdlModel(vhdl.getContent());
        }
      }
    }
  }

  @Override
  public JPopupMenu menuRequested(ProjectExplorer.Event event) {
    final var clicked = event.getTarget();
    if (clicked instanceof ProjectExplorerToolNode toolNode) {
      final var baseTool = toolNode.getValue();
      if (baseTool instanceof AddTool tool) {
        final var source = tool.getFactory();
        if (source instanceof SubcircuitFactory sub) {
          return Popups.forCircuit(proj, tool, sub.getSubcircuit());
        } else if (source instanceof VhdlEntity vhdlEntity) {
          return Popups.forVhdl(proj, tool, vhdlEntity.getContent());
        } else {
          return null;
        }
      } else {
        return null;
      }
    } else if (clicked instanceof ProjectExplorerLibraryNode libNode) {
      final var lib = libNode.getValue();
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
    final var file = proj.getLogisimFile();
    final var draggedIndex = file.getTools().indexOf(dragged);
    var targetIndex = file.getTools().indexOf(target);
    if (targetIndex > draggedIndex) targetIndex++;
    proj.doAction(LogisimFileActions.moveCircuit(dragged, targetIndex));
  }

  @Override
  public void selectionChanged(ProjectExplorer.Event event) {
    if (proj.getTool() instanceof PokeTool || proj.getTool() instanceof EditTool) {
      lastSelected = proj.getTool();
    }
    final var selected = event.getTarget();
    if (selected instanceof ProjectExplorerToolNode toolNode) {
      toolNode.fireNodeChanged();
      final var tool = toolNode.getValue();
      if (tool instanceof AddTool addTool) {
        final var source = addTool.getFactory();
        if (source instanceof SubcircuitFactory circFact) {
          final var circ = circFact.getSubcircuit();
          if (proj.getCurrentCircuit() == circ) {
            proj.getFrame().setAttrTableModel(new AttrTableCircuitModel(proj, circ));
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
      if (lib instanceof LibraryEventSource src) {
        src.addLibraryListener(this);
      }
      for (final var tool : lib.getTools()) {
        final var attrs = tool.getAttributeSet();
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
        final var tool = (Tool) event.getData();
        final var attrs = tool.getAttributeSet();
        if (attrs != null) attrs.addAttributeListener(this);
      } else if (action == LibraryEvent.REMOVE_TOOL) {
        final var tool = (Tool) event.getData();
        final var attrs = tool.getAttributeSet();
        if (attrs != null) attrs.removeAttributeListener(this);
      }
      explorer.repaint();
    }

    @Override
    public void projectChanged(ProjectEvent event) {
      final var action = event.getAction();
      if (action == ProjectEvent.ACTION_SET_FILE) {
        setFile((LogisimFile) event.getOldData(), (LogisimFile) event.getData());
        explorer.repaint();
      }
    }

    private void removeLibrary(Library lib) {
      if (lib instanceof LibraryEventSource src) {
        src.removeLibraryListener(this);
      }
      for (final var tool : lib.getTools()) {
        final var attrs = tool.getAttributeSet();
        if (attrs != null) attrs.removeAttributeListener(this);
      }
    }

    private void setFile(LogisimFile oldFile, LogisimFile newFile) {
      if (oldFile != null) {
        removeLibrary(oldFile);
        for (final var lib : oldFile.getLibraries()) {
          removeLibrary(lib);
        }
      }
      curFile = newFile;
      if (newFile != null) {
        addLibrary(newFile);
        for (final var lib : newFile.getLibraries()) {
          addLibrary(lib);
        }
      }
    }
  }
}
