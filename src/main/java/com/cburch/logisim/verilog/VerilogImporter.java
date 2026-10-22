/*
 * Logisim-evolution - digital logic design tool and simulator
 * Copyright by the Logisim-evolution developers
 * https://github.com/logisim-evolution/
 * This is free software released under GNU GPLv3 license
 */
package com.cburch.logisim.verilog;

import static com.cburch.logisim.verilog.YosysJson.checkCancelled;

import com.cburch.logisim.circuit.Circuit;
import com.cburch.logisim.file.LogisimFile;
import com.cburch.logisim.proj.Action;
import com.cburch.logisim.proj.Project;
import com.cburch.logisim.tools.Library;
import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.function.Consumer;

/** Converts detached circuits first, then commits the entire hierarchy in one project action. */
public final class VerilogImporter {
  public record Result(List<Circuit> circuits, Circuit top, LogisimFile destination) {
    public Result {
      circuits = List.copyOf(circuits);
    }

    public int componentCount() {
      return circuits.stream().mapToInt(c -> c.getNonWires().size()).sum();
    }

    public Action action(String description) {
      // Project.doAction records its undo entry before invoking the action.
      // Reject stale names here, before that history can be changed.
      var names = reservedNames(destination);
      for (var circuit : circuits) {
        if (names.contains(circuit.getName().toLowerCase(Locale.ROOT))) {
          throw new ImportException(
              "Circuit name became unavailable during import: " + circuit.getName());
        }
      }
      return new ImportAction(this, description);
    }
  }

  private VerilogImporter() {}

  public static Set<String> reservedNames(LogisimFile file) {
    var names = new HashSet<String>();
    collectNames(file, names, new HashSet<>());
    collectNames(file.getLoader().getBuiltin(), names, new HashSet<>());
    return names;
  }

  private static void collectNames(Library library, Set<String> names, Set<Library> visited) {
    if (!visited.add(library)) {
      return;
    }
    for (var tool : library.getTools()) {
      names.add(tool.getName().toLowerCase(Locale.ROOT));
    }
    for (var child : library.getLibraries()) {
      collectNames(child, names, visited);
    }
  }

  public static Result synthesize(YosysRunner.Options options, LogisimFile destination,
      Set<String> reserved, Consumer<String> messages) throws IOException {
    var design = YosysRunner.synthesize(options, messages);
    return convert(design, destination, reserved, options.undefined(), messages);
  }

  public static Result readJson(Path path, String top, String undefined, LogisimFile destination,
      Set<String> reserved, Consumer<String> messages) throws IOException {
    if (!Set.of("error", "0", "1").contains(undefined)) {
      throw new ImportException("Invalid undefined policy");
    }
    return convert(
        new YosysDesign(YosysJson.read(path), top), destination, reserved, undefined, messages);
  }

  private static Result convert(YosysDesign design, LogisimFile destination, Set<String> reserved,
      String undefined, Consumer<String> messages) {
    var used = new HashSet<>(reserved);
    var names = new LinkedHashMap<String, String>();
    for (var name : design.order) {
      names.put(name, WordNetlist.uniqueName(name, used));
    }
    var circuits = new LinkedHashMap<String, Circuit>();
    var layout = new CircuitLayout(destination, circuits);
    for (var name : design.order) {
      checkCancelled();
      messages.accept("Creating circuit " + name + "...");
      var netlist = new WordNetlist(design, name, undefined, names);
      var circuit = layout.build(netlist, names.get(name));
      circuits.put(names.get(name), circuit);
      messages.accept(name + ": " + circuit.getNonWires().size() + " components, "
          + circuit.getWires().size() + " wires; connectivity verified");
    }
    return new Result(
        new ArrayList<>(circuits.values()), circuits.get(names.get(design.top)), destination);
  }

  private static final class ImportAction extends Action {
    private final Result result;
    private final String description;
    private final List<Library> addedLibraries = new ArrayList<>();
    private Circuit previous;

    ImportAction(Result result, String description) {
      this.result = result;
      this.description = description;
    }

    @Override
    public String getName() {
      return description;
    }

    @Override
    public void doIt(Project project) {
      if (project.getLogisimFile() != result.destination()) {
        throw new ImportException("Destination project changed during import");
      }
      // Recheck on the event thread: the user may have edited while synthesis was running.
      var names = reservedNames(result.destination());
      for (var circuit : result.circuits()) {
        if (names.contains(circuit.getName().toLowerCase(Locale.ROOT))) {
          throw new ImportException(
              "Circuit name became unavailable during import: " + circuit.getName());
        }
      }
      previous = project.getCurrentCircuit();
      addedLibraries.clear();
      var file = result.destination();
      for (String name : List.of("Wiring", "Gates", "Plexers", "Arithmetic", "Memory")) {
        if (file.getLibrary(name) == null) {
          var library = file.getLoader().getBuiltin().getLibrary(name);
          file.addLibrary(library);
          addedLibraries.add(library);
        }
      }
      for (var circuit : result.circuits()) {
        circuit.setProject(project);
        file.addCircuit(circuit);
      }
      project.setCurrentCircuit(result.top());
    }

    @Override
    public void undo(Project project) {
      project.setCurrentCircuit(previous);
      for (var circuit : result.circuits().reversed()) {
        result.destination().removeCircuit(circuit);
      }
      for (var library : addedLibraries.reversed()) {
        result.destination().removeLibrary(library);
      }
    }
  }
}
