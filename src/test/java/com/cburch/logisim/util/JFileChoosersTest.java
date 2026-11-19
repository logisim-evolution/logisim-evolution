/*
 * Logisim-evolution - digital logic design tool and simulator
 * Copyright by the Logisim-evolution developers
 *
 * https://github.com/logisim-evolution/
 *
 * This is free software released under GNU GPLv3 license
 */

package com.cburch.logisim.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.cburch.logisim.prefs.AppPreferences;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class JFileChoosersTest {

  /**
   * Mimics the shell link entries that Windows shows in its Recent Items folder: the object claims
   * to be a directory, but the same path does not resolve to one on the file system.
   */
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
  void usableDirectoryRejectsNull() {
    assertFalse(JFileChoosers.isUsableDirectory(null));
  }

  @Test
  void usableDirectoryAcceptsReadableDirectory(@TempDir Path tempDir) {
    assertTrue(JFileChoosers.isUsableDirectory(tempDir.toFile()));
  }

  @Test
  void usableDirectoryRejectsRegularFile(@TempDir Path tempDir) throws IOException {
    final var file = Files.createFile(tempDir.resolve("design.txt")).toFile();

    assertFalse(JFileChoosers.isUsableDirectory(file));
  }

  @Test
  void usableDirectoryRejectsMissingPath(@TempDir Path tempDir) {
    assertFalse(JFileChoosers.isUsableDirectory(tempDir.resolve("missing").toFile()));
  }

  @Test
  void usableDirectoryRejectsShellLink(@TempDir Path tempDir) throws IOException {
    final var target = Files.createFile(tempDir.resolve("linked.txt")).toFile();
    final var link = new ShellLinkFile(target);

    assertTrue(link.isDirectory());
    assertFalse(JFileChoosers.isUsableDirectory(link));
  }

  @Test
  void setCurrentDirectoryRemembersUsableDirectory(@TempDir Path tempDir) {
    final var previous = AppPreferences.DIALOG_DIRECTORY.get();
    try {
      JFileChoosers.setCurrentDirectory(JFileChoosers.DirectoryScope.CIRCUITS, tempDir.toFile());

      assertEquals(tempDir.toFile().getAbsolutePath(), JFileChoosers.getCurrentDirectory());
      assertEquals(tempDir.toFile().getAbsolutePath(), AppPreferences.DIALOG_DIRECTORY.get());
    } finally {
      AppPreferences.DIALOG_DIRECTORY.set(previous);
    }
  }

  @Test
  void setCurrentDirectoryIgnoresShellLink(@TempDir Path tempDir) throws IOException {
    final var previous = AppPreferences.DIALOG_DIRECTORY.get();
    final var target = Files.createFile(tempDir.resolve("linked.txt")).toFile();
    try {
      final var before = JFileChoosers.getCurrentDirectory();
      JFileChoosers.setCurrentDirectory(
          JFileChoosers.DirectoryScope.CIRCUITS, new ShellLinkFile(target));

      assertEquals(before, JFileChoosers.getCurrentDirectory());
    } finally {
      AppPreferences.DIALOG_DIRECTORY.set(previous);
    }
  }

  @Test
  void getSelectedFileDoesNotRememberCurrentDirectory(@TempDir Path first, @TempDir Path second) {
    final var previous = AppPreferences.DIALOG_DIRECTORY.get();
    try {
      final var chooser = JFileChoosers.createAt(first.toFile());
      chooser.setCurrentDirectory(second.toFile());

      chooser.getSelectedFile();

      assertEquals(previous, AppPreferences.DIALOG_DIRECTORY.get());
    } finally {
      AppPreferences.DIALOG_DIRECTORY.set(previous);
    }
  }
}
