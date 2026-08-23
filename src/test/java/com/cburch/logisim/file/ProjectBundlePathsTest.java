/*
 * Logisim-evolution - digital logic design tool and simulator
 * Copyright by the Logisim-evolution developers
 *
 * https://github.com/logisim-evolution/
 *
 * This is free software released under GNU GPLv3 license
 */

package com.cburch.logisim.file;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class ProjectBundlePathsTest {
  @TempDir Path tempDir;

  @Test
  void writesPortableArchivePathsAndDescriptors() {
    assertEquals("library/", ProjectBundlePaths.libraryDirectoryEntry());
    assertEquals("library/dependency.circ", ProjectBundlePaths.libraryEntry("dependency.circ"));
    assertEquals(
        "./library/dependency.circ",
        ProjectBundlePaths.libraryDescriptor("dependency.circ", false));
    assertEquals(
        "./dependency.circ", ProjectBundlePaths.libraryDescriptor("dependency.circ", true));
  }

  @Test
  void readsPortableAndLegacyWindowsLibraryDescriptors() {
    assertEquals(
        "./library/dependency.circ",
        ProjectBundlePaths.normalizeLibraryDescriptorPath("./library/dependency.circ"));
    assertEquals(
        "./library/dependency.circ",
        ProjectBundlePaths.normalizeLibraryDescriptorPath(".\\library\\dependency.circ"));
    assertEquals(
        "./dependency.circ",
        ProjectBundlePaths.normalizeLibraryDescriptorPath(".\\dependency.circ"));
  }

  @Test
  void loaderResolvesLegacyWindowsLibraryDescriptor() throws IOException {
    final var expected = tempDir.resolve("library").resolve("dependency.circ");
    Files.createDirectories(expected.getParent());
    Files.createFile(expected);

    final var loader =
        new Loader(null) {
          @Override
          public File getCurrentDirectory() {
            return tempDir.toFile();
          }
        };

    assertEquals(
        expected.toFile().getCanonicalFile(),
        loader.getFileFor(".\\library\\dependency.circ", null).getCanonicalFile());
  }

  @Test
  void acceptsPortableAndLegacyWindowsLibraryEntries() {
    final var expected = tempDir.resolve("library").resolve("dependency.circ").toAbsolutePath();

    assertEquals(
        expected,
        ProjectBundlePaths.resolveLibraryFile(tempDir, "library/dependency.circ"));
    assertEquals(
        expected,
        ProjectBundlePaths.resolveLibraryFile(tempDir, "library\\dependency.circ"));
    assertTrue(ProjectBundlePaths.isLibraryDirectory("library/"));
    assertTrue(ProjectBundlePaths.isLibraryDirectory("library\\"));
  }

  @Test
  void rejectsNestedAndEscapingLibraryEntries() {
    assertNull(ProjectBundlePaths.resolveLibraryFile(tempDir, "other/dependency.circ"));
    assertNull(ProjectBundlePaths.resolveLibraryFile(tempDir, "library/nested/dependency.circ"));
    assertNull(ProjectBundlePaths.resolveLibraryFile(tempDir, "library\\..\\dependency.circ"));
    assertNull(ProjectBundlePaths.resolveLibraryFile(tempDir, "library/.."));
    assertNull(ProjectBundlePaths.resolveLibraryFile(tempDir, "library/"));
    assertFalse(ProjectBundlePaths.isLibraryDirectory("library/dependency.circ"));
  }

  @Test
  void keepsMainCircuitAtBundleRoot() {
    assertEquals(
        tempDir.resolve("main.circ").toAbsolutePath(),
        ProjectBundlePaths.resolveMainFile(tempDir, "main.circ"));
    assertNull(ProjectBundlePaths.resolveMainFile(tempDir, "../main.circ"));
    assertNull(ProjectBundlePaths.resolveMainFile(tempDir, "nested/main.circ"));
    assertNull(ProjectBundlePaths.resolveMainFile(tempDir, "nested\\main.circ"));
  }

  @Test
  void refusesPathsWhenWritingBundleEntries() {
    assertThrows(
        IllegalArgumentException.class,
        () -> ProjectBundlePaths.libraryEntry("nested/dependency.circ"));
    assertThrows(
        IllegalArgumentException.class,
        () -> ProjectBundlePaths.libraryDescriptor("..", false));
  }
}
