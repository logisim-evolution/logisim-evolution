/*
 * Logisim-evolution - digital logic design tool and simulator
 * Copyright by the Logisim-evolution developers
 * https://github.com/logisim-evolution/
 * This is free software released under GNU GPLv3 license
 */
package com.cburch.logisim.verilog;

import static com.cburch.logisim.verilog.YosysJson.checkCancelled;
import static com.cburch.logisim.verilog.YosysJson.flag;
import static com.cburch.logisim.verilog.YosysJson.map;
import static com.cburch.logisim.verilog.YosysJson.object;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;

/** Validated reachable module hierarchy, with children preceding parents. */
final class YosysDesign {
  final Map<String, Object> json;
  final Map<String, Object> modules;
  final String top;
  final List<String> order = new ArrayList<>();

  YosysDesign(Map<String, Object> json, String requested) {
    this.json = json;
    modules = map(json, "modules");
    if (modules.isEmpty()) {
      throw new ImportException("No modules in Yosys JSON");
    }
    if (requested == null || requested.isBlank()) {
      var candidates = new ArrayList<String>();
      modules.forEach((name, value) -> {
        if (flag(map(object(value), "attributes").getOrDefault("top", 0))) {
          candidates.add(name);
        }
      });
      if (candidates.isEmpty()) {
        var used = new LinkedHashSet<String>();
        for (var value : modules.values()) {
          for (var cell : map(object(value), "cells").values()) {
            used.add(object(cell).get("type").toString());
          }
        }
        for (var name : modules.keySet()) {
          if (!used.contains(name)) {
            candidates.add(name);
          }
        }
      }
      if (candidates.size() != 1) {
        throw new ImportException("Choose a top module. Candidates: " + candidates);
      }
      requested = candidates.getFirst();
    }
    top = requested;
    if (!modules.containsKey(top)) {
      throw new ImportException("Unknown top module: " + top);
    }
    record Visit(String name, boolean leaving) {}
    var state = new HashMap<String, Integer>();
    var stack = new ArrayDeque<Visit>();
    stack.push(new Visit(top, false));
    while (!stack.isEmpty()) {
      checkCancelled();
      var visit = stack.pop();
      if (visit.leaving()) {
        state.put(visit.name(), 2);
        order.add(visit.name());
        continue;
      }
      int status = state.getOrDefault(visit.name(), 0);
      if (status == 2) {
        continue;
      }
      if (status == 1) {
        throw new ImportException("Recursive hierarchy at " + visit.name());
      }
      var module = module(visit.name());
      var attributes = map(module, "attributes");
      if (flag(attributes.getOrDefault("blackbox", 0))
          || flag(attributes.getOrDefault("whitebox", 0))) {
        throw new ImportException("Module " + visit.name() + " has no concrete implementation");
      }
      state.put(visit.name(), 1);
      stack.push(new Visit(visit.name(), true));
      var children = new LinkedHashSet<String>();
      for (var value : map(module, "cells").values()) {
        var type = object(value).get("type").toString();
        if (modules.containsKey(type)) {
          children.add(type);
        }
      }
      for (var child : new ArrayList<>(children).reversed()) {
        stack.push(new Visit(child, false));
      }
    }
  }

  Map<String, Object> module(String name) {
    return object(modules.get(name));
  }

  void preserveOutputlessModules() {
    for (var name : order) {
      var module = module(name);
      boolean output = map(module, "ports")
                           .values()
                           .stream()
                           .anyMatch(p -> !"input".equals(object(p).get("direction")));
      if (!output) {
        for (var value : map(module, "cells").values()) {
          var cell = object(value);
          cell.computeIfAbsent("attributes", key -> new LinkedHashMap<String, Object>());
          map(cell, "attributes").put("keep", 1);
        }
      }
    }
  }
}
