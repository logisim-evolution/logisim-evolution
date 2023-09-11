/*
 * Logisim-evolution - digital logic design tool and simulator
 * Copyright by the Logisim-evolution developers
 *
 * https://github.com/logisim-evolution/
 *
 * This is free software released under GNU GPLv3 license
 */

package com.cburch.logisim.file;

import com.cburch.logisim.circuit.Circuit;
import com.cburch.logisim.circuit.CircuitMutation;
import com.cburch.logisim.circuit.SubcircuitFactory;
import com.cburch.logisim.comp.Component;
import com.cburch.logisim.comp.ComponentFactory;
import com.cburch.logisim.data.Attribute;
import com.cburch.logisim.data.AttributeSet;
import com.cburch.logisim.proj.Projects;
import com.cburch.logisim.tools.AddTool;
import com.cburch.logisim.tools.Library;
import com.cburch.logisim.tools.Tool;
import com.cburch.logisim.util.EventSourceWeakSupport;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;

public class LoadedLibrary extends Library implements LibraryEventSource {
  private class MyListener implements LibraryListener {
    @Override
    public void libraryChanged(LibraryEvent event) {
      fireLibraryEvent(event);
    }
  }

  private Library base;
  private boolean dirty;
  private final MyListener myListener;
  private final EventSourceWeakSupport<LibraryListener> listeners;

  LoadedLibrary(Library base) {
    dirty = false;
    myListener = new MyListener();
    listeners = new EventSourceWeakSupport<>();

    while (base instanceof LoadedLibrary lib) base = lib.base;
    this.base = base;
    if (base instanceof LibraryEventSource src) {
      src.addLibraryListener(myListener);
    }
  }


  static void copyAttributes(AttributeSet dest, AttributeSet src) {
    for (Attribute<?> destAttr : dest.getAttributes()) {
      Attribute<?> srcAttr = src.getAttribute(destAttr.getName());
      if (srcAttr != null) {
        @SuppressWarnings("unchecked")
        Attribute<Object> destAttr2 = (Attribute<Object>) destAttr;
        dest.setValue(destAttr2, src.getValue(srcAttr));
      }
    }
  }

  private static AttributeSet createAttributes(ComponentFactory factory, AttributeSet src) {
    final var dest = factory.createAttributeSet();
    copyAttributes(dest, src);
    return dest;
  }

  private static void replaceAll(Circuit circuit, Map<ComponentFactory, ComponentFactory> compMap) {
    ArrayList<Component> toReplace = null;
    for (final var comp : circuit.getNonWires()) {
      if (compMap.containsKey(comp.getFactory())) {
        if (toReplace == null) toReplace = new ArrayList<>();
        toReplace.add(comp);
      }
    }
    if (toReplace != null) {
      final var xn = new CircuitMutation(circuit);
      for (final var comp : toReplace) {
        xn.remove(comp);
        final var factory = compMap.get(comp.getFactory());
        if (factory != null) {
          final var newAttrs = createAttributes(factory, comp.getAttributeSet());
          xn.add(factory.createComponent(comp.getLocation(), newAttrs));
        }
      }
      xn.execute();
    }
  }

  private static void replaceAll(LogisimFile file, Map<ComponentFactory, ComponentFactory> compMap, Map<Tool, Tool> toolMap) {
    file.getOptions().getToolbarData().replaceAll(toolMap);
    file.getOptions().getMouseMappings().replaceAll(toolMap);
    for (final var circuit : file.getCircuits()) {
      replaceAll(circuit, compMap);
    }
  }

  private static void replaceAll(Map<ComponentFactory, ComponentFactory> compMap, Map<Tool, Tool> toolMap) {
    for (final var proj : Projects.getOpenProjects()) {
      final var oldTool = proj.getTool();
      final var oldCircuit = proj.getCurrentCircuit();
      if (toolMap.containsKey(oldTool)) {
        proj.setTool(toolMap.get(oldTool));
      }
      final var oldFactory = oldCircuit.getSubcircuitFactory();
      if (compMap.containsKey(oldFactory)) {
        final var newFactory = (SubcircuitFactory) compMap.get(oldFactory);
        proj.setCurrentCircuit(newFactory.getSubcircuit());
      }
      replaceAll(proj.getLogisimFile(), compMap, toolMap);
    }
    for (final var file : LibraryManager.instance.getLogisimLibraries()) {
      replaceAll(file, compMap, toolMap);
    }
  }

  @Override
  public void addLibraryListener(LibraryListener l) {
    listeners.add(l);
  }

  private void fireLibraryEvent(int action, Object data) {
    fireLibraryEvent(new LibraryEvent(this, action, data));
  }

  private void fireLibraryEvent(LibraryEvent event) {
    if (event.getSource() != this) {
      event = new LibraryEvent(this, event.getAction(), event.getData());
    }
    for (final var l : listeners) {
      l.libraryChanged(event);
    }
  }

  public Library getBase() {
    return base;
  }

  @Override
  public String getDisplayName() {
    return base.getDisplayName();
  }

  @Override
  public List<Library> getLibraries() {
    return base.getLibraries();
  }

  @Override
  public boolean removeLibrary(String name) {
    return base.removeLibrary(name);
  }

  @Override
  public String getName() {
    return base.getName();
  }

  @Override
  public List<? extends Tool> getTools() {
    return base.getTools();
  }

  @Override
  public boolean isDirty() {
    return dirty || base.isDirty();
  }

  @Override
  public void removeLibraryListener(LibraryListener l) {
    listeners.remove(l);
  }

  private void resolveChanges(Library old) {
    if (listeners.isEmpty()) return;

    if (!base.getDisplayName().equals(old.getDisplayName())) {
      fireLibraryEvent(LibraryEvent.SET_NAME, base.getDisplayName());
    }

    final var changes = new HashSet<>(old.getLibraries());
    base.getLibraries().forEach(changes::remove);
    for (final var lib : changes) {
      fireLibraryEvent(LibraryEvent.REMOVE_LIBRARY, lib);
    }

    changes.clear();
    changes.addAll(base.getLibraries());
    old.getLibraries().forEach(changes::remove);
    for (final var lib : changes) {
      fireLibraryEvent(LibraryEvent.ADD_LIBRARY, lib);
    }

    final var componentMap = new HashMap<ComponentFactory, ComponentFactory>();
    final var toolMap = new HashMap<Tool, Tool>();
    for (final var oldTool : old.getTools()) {
      final var newTool = base.getTool(oldTool.getName());
      toolMap.put(oldTool, newTool);
      if (oldTool instanceof AddTool tool) {
        final var oldFactory = tool.getFactory();
        if (newTool instanceof AddTool) {
          final var newFactory = tool.getFactory();
          componentMap.put(oldFactory, newFactory);
        } else {
          componentMap.put(oldFactory, null);
        }
      }
    }
    replaceAll(componentMap, toolMap);

    var toolChanges = new HashSet<Tool>(old.getTools());
    toolChanges.removeAll(toolMap.keySet());
    for (Tool tool : toolChanges) {
      fireLibraryEvent(LibraryEvent.REMOVE_TOOL, tool);
    }

    toolChanges = new HashSet<>(base.getTools());
    toolChanges.removeAll(toolMap.values());
    for (Tool tool : toolChanges) {
      fireLibraryEvent(LibraryEvent.ADD_TOOL, tool);
    }
  }

  void setBase(Library value) {
    if (base instanceof LibraryEventSource src) {
      src.removeLibraryListener(myListener);
    }
    final var old = base;
    base = value;
    resolveChanges(old);
    if (base instanceof LibraryEventSource src) {
      src.addLibraryListener(myListener);
    }
  }

  void setDirty(boolean value) {
    if (dirty != value) {
      dirty = value;
      fireLibraryEvent(LibraryEvent.DIRTY_STATE, isDirty() ? Boolean.TRUE : Boolean.FALSE);
    }
  }
}
