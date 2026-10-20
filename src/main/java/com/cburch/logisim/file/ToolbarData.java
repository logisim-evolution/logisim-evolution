/*
 * Logisim-evolution - digital logic design tool and simulator
 * Copyright by the Logisim-evolution developers
 *
 * https://github.com/logisim-evolution/
 *
 * This is free software released under GNU GPLv3 license
 */

package com.cburch.logisim.file;

import com.cburch.logisim.data.AttributeListener;
import com.cburch.logisim.data.AttributeSets;
import com.cburch.logisim.tools.Tool;
import com.cburch.logisim.util.EventSourceWeakSupport;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class ToolbarData {
  public interface ToolbarListener {
    void toolbarChanged();
  }

  private final EventSourceWeakSupport<ToolbarListener> listeners;
  private final EventSourceWeakSupport<AttributeListener> toolListeners;
  private final ArrayList<Tool> contents;

  public ToolbarData() {
    listeners = new EventSourceWeakSupport<>();
    toolListeners = new EventSourceWeakSupport<>();
    contents = new ArrayList<>();
  }

  private void addAttributeListeners(Tool tool) {
    for (final var l : toolListeners) {
      final var attrs = tool.getAttributeSet();
      if (attrs != null) attrs.addAttributeListener(l);
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
    for (final var tool : contents) {
      if (tool != null) {
        final var attrs = tool.getAttributeSet();
        if (attrs != null) attrs.addAttributeListener(l);
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
    if (this == other) return;
    for (final var tool : contents) {
      if (tool != null) {
        removeAttributeListeners(tool);
      }
    }
    this.contents.clear();
    for (final var srcTool : other.contents) {
      if (srcTool == null) {
        this.addSeparator();
      } else {
        final var toolCopy = file.findTool(srcTool);
        if (toolCopy != null) {
          final var dstTool = toolCopy.cloneTool();
          AttributeSets.copy(srcTool.getAttributeSet(), dstTool.getAttributeSet());
          this.addTool(dstTool);
          addAttributeListeners(toolCopy);
        }
      }
    }
    fireToolbarChanged();
  }

  public void fireToolbarChanged() {
    for (final var l : listeners) {
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
    for (final var tool : contents) {
      if (tool != null) return tool;
    }
    return null;
  }

  public Object move(int from, int to) {
    final var moved = contents.remove(from);
    contents.add(to, moved);
    fireToolbarChanged();
    return moved;
  }

  public Object remove(int pos) {
    Object ret = contents.remove(pos);
    if (ret instanceof Tool tool) removeAttributeListeners(tool);
    fireToolbarChanged();
    return ret;
  }

  private void removeAttributeListeners(Tool tool) {
    for (final var l : toolListeners) {
      final var attrs = tool.getAttributeSet();
      if (attrs != null) attrs.removeAttributeListener(l);
    }
  }

  public void removeToolAttributeListener(AttributeListener l) {
    for (final var tool : contents) {
      if (tool != null) {
        final var attrs = tool.getAttributeSet();
        if (attrs != null) attrs.removeAttributeListener(l);
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
    var changed = false;
    for (final var it = contents.listIterator(); it.hasNext(); ) {
      Object old = it.next();
      if (toolMap.containsKey(old)) {
        changed = true;
        removeAttributeListeners((Tool) old);
        final var newTool = toolMap.get(old);
        if (newTool == null) {
          it.remove();
        } else {
          final var addedTool = newTool.cloneTool();
          addAttributeListeners(addedTool);
          LoadedLibrary.copyAttributes(addedTool.getAttributeSet(), ((Tool) old).getAttributeSet());
          it.set(addedTool);
        }
      }
    }
    if (changed) fireToolbarChanged();
  }

  public int size() {
    return contents.size();
  }

  boolean usesToolFromSource(Tool query) {
    for (final var tool : contents) {
      if (tool != null && tool.sharesSource(query)) return true;
    }
    return false;
  }
}
