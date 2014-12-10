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
import java.util.List;
import java.util.ListIterator;
import java.util.Map;

import com.cburch.logisim.data.AttributeListener;
import com.cburch.logisim.data.AttributeSet;
import com.cburch.logisim.data.AttributeSets;
import com.cburch.logisim.tools.Tool;
import com.cburch.logisim.util.EventSourceWeakSupport;

public class ToolbarData {
	public static interface ToolbarListener {
		public void toolbarChanged();
	}

	private EventSourceWeakSupport<ToolbarListener> listeners;
	private EventSourceWeakSupport<AttributeListener> toolListeners;
	private ArrayList<Tool> contents;

	public ToolbarData() {
		listeners = new EventSourceWeakSupport<ToolbarListener>();
		toolListeners = new EventSourceWeakSupport<AttributeListener>();
		contents = new ArrayList<Tool>();
	}

	private void addAttributeListeners(Tool tool) {
		for (AttributeListener l : toolListeners) {
			AttributeSet attrs = tool.getAttributeSet();
			if (attrs != null)
				attrs.addAttributeListener(l);
		}
	}

	public void addSeparator() {
		contents.add(null);
		fireToolbarChanged();
	}

	public void addSeparator(int pos) {
		contents.add(pos, null);
		fireToolbarChanged();
	}

	public void addTool(int pos, Tool tool) {
		contents.add(pos, tool);
		addAttributeListeners(tool);
		fireToolbarChanged();
	}

	public void addTool(Tool tool) {
		contents.add(tool);
		addAttributeListeners(tool);
		fireToolbarChanged();
	}

	public void addToolAttributeListener(AttributeListener l) {
		for (Tool tool : contents) {
			if (tool != null) {
				AttributeSet attrs = tool.getAttributeSet();
				if (attrs != null)
					attrs.addAttributeListener(l);
			}
		}
		toolListeners.add(l);
	}

	//
	// listener methods
	//
	public void addToolbarListener(ToolbarListener l) {
		listeners.add(l);
	}

	//
	// modification methods
	//
	public void copyFrom(ToolbarData other, LogisimFile file) {
		if (this == other)
			return;
		for (Tool tool : contents) {
			if (tool != null) {
				removeAttributeListeners(tool);
			}
		}
		this.contents.clear();
		for (Tool srcTool : other.contents) {
			if (srcTool == null) {
				this.addSeparator();
			} else {
				Tool toolCopy = file.findTool(srcTool);
				if (toolCopy != null) {
					Tool dstTool = toolCopy.cloneTool();
					AttributeSets.copy(srcTool.getAttributeSet(),
							dstTool.getAttributeSet());
					this.addTool(dstTool);
					addAttributeListeners(toolCopy);
				}
			}
		}
		fireToolbarChanged();
	}

	public void fireToolbarChanged() {
		for (ToolbarListener l : listeners) {
			l.toolbarChanged();
		}
	}

	public Object get(int index) {
		return contents.get(index);
	}

	//
	// query methods
	//
	public List<Tool> getContents() {
		return contents;
	}

	public Tool getFirstTool() {
		for (Tool tool : contents) {
			if (tool != null)
				return tool;
		}
		return null;
	}

	public Object move(int from, int to) {
		Tool moved = contents.remove(from);
		contents.add(to, moved);
		fireToolbarChanged();
		return moved;
	}

	public Object remove(int pos) {
		Object ret = contents.remove(pos);
		if (ret instanceof Tool)
			removeAttributeListeners((Tool) ret);
		fireToolbarChanged();
		return ret;
	}

	private void removeAttributeListeners(Tool tool) {
		for (AttributeListener l : toolListeners) {
			AttributeSet attrs = tool.getAttributeSet();
			if (attrs != null)
				attrs.removeAttributeListener(l);
		}
	}

	public void removeToolAttributeListener(AttributeListener l) {
		for (Tool tool : contents) {
			if (tool != null) {
				AttributeSet attrs = tool.getAttributeSet();
				if (attrs != null)
					attrs.removeAttributeListener(l);
			}
		}
		toolListeners.remove(l);
	}

	public void removeToolbarListener(ToolbarListener l) {
		listeners.remove(l);
	}

	//
	// package-protected methods
	//
	void replaceAll(Map<Tool, Tool> toolMap) {
		boolean changed = false;
		for (ListIterator<Tool> it = contents.listIterator(); it.hasNext();) {
			Object old = it.next();
			if (toolMap.containsKey(old)) {
				changed = true;
				removeAttributeListeners((Tool) old);
				Tool newTool = toolMap.get(old);
				if (newTool == null) {
					it.remove();
				} else {
					Tool addedTool = newTool.cloneTool();
					addAttributeListeners(addedTool);
					LoadedLibrary.copyAttributes(addedTool.getAttributeSet(),
							((Tool) old).getAttributeSet());
					it.set(addedTool);
				}
			}
		}
		if (changed)
			fireToolbarChanged();
	}

	public int size() {
		return contents.size();
	}

	boolean usesToolFromSource(Tool query) {
		for (Tool tool : contents) {
			if (tool != null && tool.sharesSource(query))
				return true;
		}
		return false;
	}
}
