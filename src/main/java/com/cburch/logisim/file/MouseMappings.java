/*
 * Logisim-evolution - digital logic design tool and simulator
 * Copyright by the Logisim-evolution developers
 *
 * https://github.com/logisim-evolution/
 *
 * This is free software released under GNU GPLv3 license
 */

package com.cburch.logisim.file;

import com.cburch.logisim.data.AttributeSets;
import com.cburch.logisim.tools.AddTool;
import com.cburch.logisim.tools.SelectTool;
import com.cburch.logisim.tools.Tool;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class MouseMappings {
  public interface MouseMappingsListener {
    void mouseMappingsChanged();
  }

  private final List<MouseMappingsListener> listeners;
  private final HashMap<Integer, Tool> map;
  private int cacheMods;
  private Tool cacheTool;

  public MouseMappings() {
    listeners = new ArrayList<>();
    map = new HashMap<>();
  }

  //
  // listener methods
  //
  public void addMouseMappingsListener(MouseMappingsListener l) {
    listeners.add(l);
  }

  public boolean containsSelectTool() {
    for (Tool tool : map.values()) {
      if (tool instanceof SelectTool) return true;
    }
    return false;
  }

  //
  // modification methods
  //
  public void copyFrom(MouseMappings other, LogisimFile file) {
    if (this == other) return;
    cacheMods = -1;
    this.map.clear();
    for (Integer mods : other.map.keySet()) {
      final var srcTool = other.map.get(mods);
      var dstTool = file.findTool(srcTool);
      if (dstTool != null) {
        dstTool = dstTool.cloneTool();
        AttributeSets.copy(srcTool.getAttributeSet(), dstTool.getAttributeSet());
        this.map.put(mods, dstTool);
      }
    }
    fireMouseMappingsChanged();
  }

  private void fireMouseMappingsChanged() {
    for (MouseMappingsListener l : listeners) {
      l.mouseMappingsChanged();
    }
  }

  public Set<Integer> getMappedModifiers() {
    return map.keySet();
  }

  //
  // query methods
  //
  public Map<Integer, Tool> getMappings() {
    return map;
  }

  public Tool getToolFor(int mods) {
    if (mods == cacheMods) {
      return cacheTool;
    } else {
      Tool ret = map.get(mods);
      cacheMods = mods;
      cacheTool = ret;
      return ret;
    }
  }

  public Tool getToolFor(Integer mods) {
    if (mods == cacheMods) {
      return cacheTool;
    } else {
      Tool ret = map.get(mods);
      cacheMods = mods;
      cacheTool = ret;
      return ret;
    }
  }

  public Tool getToolFor(MouseEvent e) {
    return getToolFor(e.getModifiersEx());
  }

  public void removeMouseMappingsListener(MouseMappingsListener l) {
    listeners.add(l);
  }

  //
  // package-protected methods
  //
  void replaceAll(Map<Tool, Tool> toolMap) {
    var changed = false;
    for (final var entry : map.entrySet()) {
      final var key = entry.getKey();
      final var tool = entry.getValue();
      final var searchFor = (tool instanceof AddTool addTool) ? addTool.getFactory() : tool;
      changed |= replaceInMap(toolMap, tool, searchFor, key);
    }
    if (changed) fireMouseMappingsChanged();
  }

  private boolean replaceInMap(Map<Tool, Tool> toolMap, Tool tool, Object searchFor, Integer key) {
    var changed = false;
    if (toolMap.containsKey(searchFor)) {
      changed = true;
      final var newTool = toolMap.get(searchFor);
      if (newTool == null) {
        map.remove(searchFor);
      } else {
        final var clone = newTool.cloneTool();
        LoadedLibrary.copyAttributes(clone.getAttributeSet(), tool.getAttributeSet());
        map.put(key, clone);
      }
    }

    return changed;
  }

  public void setToolFor(int mods, Tool tool) {
    if (mods == cacheMods) cacheMods = -1;

    if (tool == null) {
      Object old = map.remove(mods);
      if (old != null) fireMouseMappingsChanged();
    } else {
      Object old = map.put(mods, tool);
      if (old != tool) fireMouseMappingsChanged();
    }
  }

  public void setToolFor(Integer mods, Tool tool) {
    if (mods == cacheMods) cacheMods = -1;

    if (tool == null) {
      Object old = map.remove(mods);
      if (old != null) fireMouseMappingsChanged();
    } else {
      Object old = map.put(mods, tool);
      if (old != tool) fireMouseMappingsChanged();
    }
  }

  public void setToolFor(MouseEvent e, Tool tool) {
    setToolFor(e.getModifiersEx(), tool);
  }

  public boolean usesToolFromSource(Tool query) {
    for (final var tool : map.values()) {
      if (tool.sharesSource(query)) {
        return true;
      }
    }
    return false;
  }
}
