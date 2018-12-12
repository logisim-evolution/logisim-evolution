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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;

import com.cburch.logisim.circuit.Circuit;
import com.cburch.logisim.circuit.CircuitMutation;
import com.cburch.logisim.circuit.SubcircuitFactory;
import com.cburch.logisim.comp.Component;
import com.cburch.logisim.comp.ComponentFactory;
import com.cburch.logisim.data.Attribute;
import com.cburch.logisim.data.AttributeSet;
import com.cburch.logisim.proj.Project;
import com.cburch.logisim.proj.Projects;
import com.cburch.logisim.tools.AddTool;
import com.cburch.logisim.tools.Library;
import com.cburch.logisim.tools.Tool;
import com.cburch.logisim.util.EventSourceWeakSupport;

public class LoadedLibrary extends Library implements LibraryEventSource {
	private class MyListener implements LibraryListener {
		public void libraryChanged(LibraryEvent event) {
			fireLibraryEvent(event);
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

	private static AttributeSet createAttributes(ComponentFactory factory,
			AttributeSet src) {
		AttributeSet dest = factory.createAttributeSet();
		copyAttributes(dest, src);
		return dest;
	}

	private static void replaceAll(Circuit circuit,
			Map<ComponentFactory, ComponentFactory> compMap) {
		ArrayList<Component> toReplace = null;
		for (Component comp : circuit.getNonWires()) {
			if (compMap.containsKey(comp.getFactory())) {
				if (toReplace == null)
					toReplace = new ArrayList<Component>();
				toReplace.add(comp);
			}
		}
		if (toReplace != null) {
			CircuitMutation xn = new CircuitMutation(circuit);
			for (Component comp : toReplace) {
				xn.remove(comp);
				ComponentFactory factory = compMap.get(comp.getFactory());
				if (factory != null) {
					AttributeSet newAttrs = createAttributes(factory,
							comp.getAttributeSet());
					xn.add(factory.createComponent(comp.getLocation(), newAttrs));
				}
			}
			xn.execute();
		}
	}

	private static void replaceAll(LogisimFile file,
			Map<ComponentFactory, ComponentFactory> compMap,
			Map<Tool, Tool> toolMap) {
		file.getOptions().getToolbarData().replaceAll(toolMap);
		file.getOptions().getMouseMappings().replaceAll(toolMap);
		for (Circuit circuit : file.getCircuits()) {
			replaceAll(circuit, compMap);
		}
	}

	private static void replaceAll(
			Map<ComponentFactory, ComponentFactory> compMap,
			Map<Tool, Tool> toolMap) {
		for (Project proj : Projects.getOpenProjects()) {
			Tool oldTool = proj.getTool();
			Circuit oldCircuit = proj.getCurrentCircuit();
			if (toolMap.containsKey(oldTool)) {
				proj.setTool(toolMap.get(oldTool));
			}
			SubcircuitFactory oldFactory = oldCircuit.getSubcircuitFactory();
			if (compMap.containsKey(oldFactory)) {
				SubcircuitFactory newFactory;
				newFactory = (SubcircuitFactory) compMap.get(oldFactory);
				proj.setCurrentCircuit(newFactory.getSubcircuit());
			}
			replaceAll(proj.getLogisimFile(), compMap, toolMap);
		}
		for (LogisimFile file : LibraryManager.instance.getLogisimLibraries()) {
			replaceAll(file, compMap, toolMap);
		}
	}

	private Library base;

	private boolean dirty;

	private MyListener myListener;

	private EventSourceWeakSupport<LibraryListener> listeners;

	LoadedLibrary(Library base) {
		dirty = false;
		myListener = new MyListener();
		listeners = new EventSourceWeakSupport<LibraryListener>();

		while (base instanceof LoadedLibrary)
			base = ((LoadedLibrary) base).base;
		this.base = base;
		if (base instanceof LibraryEventSource) {
			((LibraryEventSource) base).addLibraryListener(myListener);
		}
	}

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
		for (LibraryListener l : listeners) {
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

	public boolean removeLibrary(String Name) {
		return base.removeLibrary(Name);
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

	public void removeLibraryListener(LibraryListener l) {
		listeners.remove(l);
	}

	private void resolveChanges(Library old) {
		if (listeners.isEmpty())
			return;

		if (!base.getDisplayName().equals(old.getDisplayName())) {
			fireLibraryEvent(LibraryEvent.SET_NAME, base.getDisplayName());
		}

		HashSet<Library> changes = new HashSet<Library>(old.getLibraries());
		changes.removeAll(base.getLibraries());
		for (Library lib : changes) {
			fireLibraryEvent(LibraryEvent.REMOVE_LIBRARY, lib);
		}

		changes.clear();
		changes.addAll(base.getLibraries());
		changes.removeAll(old.getLibraries());
		for (Library lib : changes) {
			fireLibraryEvent(LibraryEvent.ADD_LIBRARY, lib);
		}

		HashMap<ComponentFactory, ComponentFactory> componentMap;
		HashMap<Tool, Tool> toolMap;
		componentMap = new HashMap<ComponentFactory, ComponentFactory>();
		toolMap = new HashMap<Tool, Tool>();
		for (Tool oldTool : old.getTools()) {
			Tool newTool = base.getTool(oldTool.getName());
			toolMap.put(oldTool, newTool);
			if (oldTool instanceof AddTool) {
				ComponentFactory oldFactory = ((AddTool) oldTool).getFactory();
				if (newTool != null && newTool instanceof AddTool) {
					ComponentFactory newFactory = ((AddTool) newTool)
							.getFactory();
					componentMap.put(oldFactory, newFactory);
				} else {
					componentMap.put(oldFactory, null);
				}
			}
		}
		replaceAll(componentMap, toolMap);

		HashSet<Tool> toolChanges = new HashSet<Tool>(old.getTools());
		toolChanges.removeAll(toolMap.keySet());
		for (Tool tool : toolChanges) {
			fireLibraryEvent(LibraryEvent.REMOVE_TOOL, tool);
		}

		toolChanges = new HashSet<Tool>(base.getTools());
		toolChanges.removeAll(toolMap.values());
		for (Tool tool : toolChanges) {
			fireLibraryEvent(LibraryEvent.ADD_TOOL, tool);
		}
	}

	void setBase(Library value) {
		if (base instanceof LibraryEventSource) {
			((LibraryEventSource) base).removeLibraryListener(myListener);
		}
		Library old = base;
		base = value;
		resolveChanges(old);
		if (base instanceof LibraryEventSource) {
			((LibraryEventSource) base).addLibraryListener(myListener);
		}
	}

	void setDirty(boolean value) {
		if (dirty != value) {
			dirty = value;
			fireLibraryEvent(LibraryEvent.DIRTY_STATE, isDirty() ? Boolean.TRUE
					: Boolean.FALSE);
		}
	}
}
