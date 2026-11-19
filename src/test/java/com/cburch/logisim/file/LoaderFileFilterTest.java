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
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class LoaderFileFilterTest {

  /** Mimics a Windows shell link that reports itself as a directory but cannot be entered. */
  private static final class ShellLinkFile extends File {
    private static final long serialVersionUID = 1L;

    ShellLinkFile(File target) {
      super(target.getPath());
    }

    @Override
    public boolean isDirectory() {
      return true;
    }
  }

  @Test
  void logisimFilterAcceptsReadableDirectory(@TempDir Path tempDir) {
    assertTrue(Loader.LOGISIM_FILTER.accept(tempDir.toFile()));
  }

  @Test
  void logisimFilterAcceptsCircuitFile(@TempDir Path tempDir) throws IOException {
    final var file = Files.createFile(tempDir.resolve("design.circ")).toFile();

    assertTrue(Loader.LOGISIM_FILTER.accept(file));
  }

  @Test
  void logisimFilterRejectsOtherFiles(@TempDir Path tempDir) throws IOException {
    final var file = Files.createFile(tempDir.resolve("readme.txt")).toFile();

    assertFalse(Loader.LOGISIM_FILTER.accept(file));
  }

  @Test
  void logisimFilterRejectsShellLink(@TempDir Path tempDir) throws IOException {
    final var target = Files.createFile(tempDir.resolve("linked.txt")).toFile();
    final var link = new ShellLinkFile(target);

    assertTrue(link.isDirectory());
    assertFalse(Loader.LOGISIM_FILTER.accept(link));
  }
}
