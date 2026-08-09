/*
 * Logisim-evolution - digital logic design tool and simulator
 * Copyright by the Logisim-evolution developers
 *
 * https://github.com/logisim-evolution/
 *
 * This is free software released under GNU GPLv3 license
 */

package com.cburch.logisim.file;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import com.cburch.logisim.circuit.Circuit;
import com.cburch.logisim.circuit.CircuitMutation;
import com.cburch.logisim.data.Location;
import com.cburch.logisim.tools.AddTool;
import com.cburch.logisim.tools.Library;
import java.io.File;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class NestedLibraryResolutionTest {

  @TempDir Path tempDir;

  @Test
  void savesAndReloadsComponentsFromNestedLogisimLibraries() throws Exception {
    final var aPath = tempDir.resolve("A.circ").toFile();
    final var bPath = tempDir.resolve("B.circ").toFile();
    final var cPath = tempDir.resolve("C.circ").toFile();

    final var aLoader = new RecordingLoader();
    final var aFile = newProject(aLoader, "Leaf");
    save(aLoader, aFile, aPath);

    final var bLoader = new RecordingLoader();
    final var bFile = newProject(bLoader, "Middle");
    final var aLibrary = bLoader.loadLogisimLibrary(aPath);
    assertNotNull(aLibrary, bLoader.errors());
    bFile.addLibrary(aLibrary);
    addComponent(bFile.getMainCircuit(), tool(aLibrary, "Leaf"));
    save(bLoader, bFile, bPath);

    final var cLoader = new RecordingLoader();
    final var cFile = newProject(cLoader, "Top");
    final var bLibrary = cLoader.loadLogisimLibrary(bPath);
    assertNotNull(bLibrary, cLoader.errors());
    cFile.addLibrary(bLibrary);
    final var nestedALibrary = findLibrary(bLibrary, "A");
    addComponent(cFile.getMainCircuit(), tool(nestedALibrary, "Leaf"));
    save(cLoader, cFile, cPath);

    final var reloadLoader = new RecordingLoader();
    final var reloaded = reloadLoader.openLogisimFile(cPath);
    assertNotNull(reloaded, reloadLoader.errors());
    assertTrue(
        reloaded.getMainCircuit().getNonWires().stream()
            .anyMatch(component -> component.getFactory().getName().equals("Leaf")));
    assertFalse(reloadLoader.hasErrors(), reloadLoader.errors());
  }

  @Test
  void reusesSharedNestedLogisimLibraryAcrossParentLibraries() throws Exception {
    final var sharedPath = tempDir.resolve("Shared.circ").toFile();
    final var aPath = tempDir.resolve("A.circ").toFile();
    final var bPath = tempDir.resolve("B.circ").toFile();

    final var sharedLoader = new RecordingLoader();
    save(sharedLoader, newProject(sharedLoader, "Common"), sharedPath);

    final var aLoader = new RecordingLoader();
    final var aFile = newProject(aLoader, "ParentA");
    final var sharedFromA = aLoader.loadLogisimLibrary(sharedPath);
    assertNotNull(sharedFromA, aLoader.errors());
    aFile.addLibrary(sharedFromA);
    addComponent(aFile.getMainCircuit(), tool(sharedFromA, "Common"));
    save(aLoader, aFile, aPath);

    final var bLoader = new RecordingLoader();
    final var bFile = newProject(bLoader, "ParentB");
    final var sharedFromB = bLoader.loadLogisimLibrary(sharedPath);
    assertNotNull(sharedFromB, bLoader.errors());
    bFile.addLibrary(sharedFromB);
    addComponent(bFile.getMainCircuit(), tool(sharedFromB, "Common"));
    save(bLoader, bFile, bPath);

    final var rootLoader = new RecordingLoader();
    final var aLibrary = rootLoader.loadLogisimLibrary(aPath);
    final var bLibrary = rootLoader.loadLogisimLibrary(bPath);
    assertNotNull(aLibrary, rootLoader.errors());
    assertNotNull(bLibrary, rootLoader.errors());

    final var nestedSharedFromA = findLibrary(aLibrary, "Shared");
    final var nestedSharedFromB = findLibrary(bLibrary, "Shared");
    assertSame(nestedSharedFromA, nestedSharedFromB);
    assertSame(
        tool(nestedSharedFromA, "Common").getFactory(),
        tool(nestedSharedFromB, "Common").getFactory());
    assertFalse(rootLoader.hasErrors(), rootLoader.errors());
  }

  private static void addComponent(Circuit circuit, AddTool tool) {
    final var factory = tool.getFactory();
    final var component =
        factory.createComponent(Location.create(100, 100, true), factory.createAttributeSet());
    final var mutation = new CircuitMutation(circuit);
    mutation.add(component);
    mutation.execute();
  }

  private static Library findLibrary(Library library, String name) {
    for (final var sub : library.getLibraries()) {
      if (sub.getName().equals(name)) return sub;
    }
    return fail("library not found: " + name);
  }

  private static LogisimFile newProject(RecordingLoader loader, String circuitName) {
    final var file = LogisimFile.createNew(loader, null);
    file.getMainCircuit().setName(circuitName);
    return file;
  }

  private static void save(RecordingLoader loader, LogisimFile file, File path) {
    assertTrue(loader.save(file, path), loader.errors());
    assertFalse(loader.hasErrors(), loader.errors());
  }

  private static AddTool tool(Library library, String name) {
    final var tool = library.getTool(name);
    assertTrue(tool instanceof AddTool, "tool not found: " + name);
    return (AddTool) tool;
  }

  private static class RecordingLoader extends Loader {
    private final List<String> errors = new ArrayList<>();

    RecordingLoader() {
      super(null);
    }

    String errors() {
      return String.join("\n", errors);
    }

    boolean hasErrors() {
      return !errors.isEmpty();
    }

    @Override
    public void showError(String description) {
      errors.add(description);
    }
  }
}
