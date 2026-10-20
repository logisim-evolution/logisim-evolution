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
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.cburch.logisim.circuit.CircuitMutation;
import com.cburch.logisim.data.Location;
import com.cburch.logisim.tools.AddTool;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class ProjectBundleExportTest {
  @TempDir Path tempDir;

  @Test
  void exportsPortableLibraryEntryAndDescriptor() throws Exception {
    final var dependencyPath = tempDir.resolve("dependency.circ").toFile();
    final var dependencyLoader = new RecordingLoader();
    save(dependencyLoader, newProject(dependencyLoader, "Leaf"), dependencyPath);

    final var mainLoader = new RecordingLoader();
    final var mainFile = newProject(mainLoader, "Top");
    final var dependency = mainLoader.loadLogisimLibrary(dependencyPath);
    assertNotNull(dependency, mainLoader.errors());
    mainFile.addLibrary(dependency);
    final var tool = dependency.getTool("Leaf");
    assertTrue(tool instanceof AddTool);
    final var factory = ((AddTool) tool).getFactory();
    final var mutation = new CircuitMutation(mainFile.getMainCircuit());
    mutation.add(
        factory.createComponent(Location.create(100, 100, true), factory.createAttributeSet()));
    mutation.execute();
    save(mainLoader, mainFile, tempDir.resolve("main.circ").toFile());

    final var bundleBytes = new ByteArrayOutputStream();
    try (var zip = new ZipOutputStream(bundleBytes)) {
      zip.putNextEntry(new ZipEntry(ProjectBundlePaths.libraryDirectoryEntry()));
      assertTrue(mainLoader.export(mainFile, zip, "bundle.circ"));
    }

    final Map<String, String> entries = new HashMap<>();
    try (var zip = new ZipInputStream(new ByteArrayInputStream(bundleBytes.toByteArray()))) {
      ZipEntry entry;
      while ((entry = zip.getNextEntry()) != null) {
        entries.put(entry.getName(), new String(zip.readAllBytes(), StandardCharsets.UTF_8));
      }
    }

    assertTrue(entries.containsKey("library/"), entries.keySet().toString());
    assertTrue(entries.containsKey("library/dependency.circ"), entries.keySet().toString());
    assertFalse(
        entries.keySet().stream().anyMatch(name -> name.indexOf('\\') >= 0),
        entries.keySet().toString());
    final var exportedMain = entries.get("bundle.circ");
    assertNotNull(exportedMain, entries.keySet().toString());
    assertTrue(
        exportedMain.contains("desc=\"file#./library/dependency.circ\""), exportedMain);
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
