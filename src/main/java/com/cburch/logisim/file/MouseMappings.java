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

package com.cburch.logisim.file;

import com.cburch.logisim.data.AttributeSets;
import com.cburch.logisim.tools.AddTool;
import com.cburch.logisim.tools.SelectTool;
import com.cburch.logisim.tools.Tool;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

public class MouseMappings {
  public interface MouseMappingsListener {
    void mouseMappingsChanged();
  }

  private final ArrayList<MouseMappingsListener> listeners;
  private final HashMap<Integer, Tool> map;
  private int cache_mods;
  private Tool cache_tool;

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
    cache_mods = -1;
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
    if (mods == cache_mods) {
      return cache_tool;
    } else {
      Tool ret = map.get(mods);
      cache_mods = mods;
      cache_tool = ret;
      return ret;
    }
  }

  public Tool getToolFor(Integer mods) {
    if (mods == cache_mods) {
      return cache_tool;
    } else {
      Tool ret = map.get(mods);
      cache_mods = mods;
      cache_tool = ret;
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
      final var searchFor = (tool instanceof AddTool) ? ((AddTool) tool).getFactory() : tool;
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
    if (mods == cache_mods) cache_mods = -1;

    if (tool == null) {
      Object old = map.remove(mods);
      if (old != null) fireMouseMappingsChanged();
    } else {
      Object old = map.put(mods, tool);
      if (old != tool) fireMouseMappingsChanged();
    }
  }

  public void setToolFor(Integer mods, Tool tool) {
    if (mods == cache_mods) cache_mods = -1;

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
