/*
 * Logisim-evolution - digital logic design tool and simulator
 * Copyright by the Logisim-evolution developers
 *
 * https://github.com/logisim-evolution/
 *
 * This is free software released under GNU GPLv3 license
 */

package com.cburch.logisim.file;

import java.nio.file.Path;

/** Utilities for portable and safe paths inside Logisim project bundles. */
public final class ProjectBundlePaths {
  private static final String ARCHIVE_SEPARATOR = "/";
  private static final String LIBRARY_DIRECTORY =
      Loader.LOGISIM_LIBRARY_DIR + ARCHIVE_SEPARATOR;

  private ProjectBundlePaths() {}

  /** Returns the portable ZIP entry name for the project bundle's library directory. */
  public static String libraryDirectoryEntry() {
    return LIBRARY_DIRECTORY;
  }

  /** Returns a portable ZIP entry name for a direct child of the bundle's library directory. */
  public static String libraryEntry(String fileName) {
    requireDirectFileName(fileName);
    return LIBRARY_DIRECTORY + fileName;
  }

  /** Returns the portable relative descriptor for a bundled external library. */
  public static String libraryDescriptor(String fileName, boolean fromBundledLibrary) {
    requireDirectFileName(fileName);
    return fromBundledLibrary
        ? "." + ARCHIVE_SEPARATOR + fileName
        : "." + ARCHIVE_SEPARATOR + LIBRARY_DIRECTORY + fileName;
  }

  /** Returns a portable path when reading library descriptors written on another platform. */
  public static String normalizeLibraryDescriptorPath(String path) {
    return path.replace('\\', '/');
  }

  /** Returns whether an entry is the library directory, accepting legacy Windows separators. */
  public static boolean isLibraryDirectory(String entryName) {
    if (entryName == null) return false;
    final var normalized = normalizeArchiveEntry(entryName);
    return normalized.equals(Loader.LOGISIM_LIBRARY_DIR)
        || normalized.equals(LIBRARY_DIRECTORY);
  }

  /**
   * Resolves a direct bundled library entry below the extraction directory.
   *
   * <p>Both portable forward slashes and legacy Windows backslashes are accepted. Entries outside
   * the direct {@code library/} directory return {@code null}.
   */
  public static Path resolveLibraryFile(Path extractionDirectory, String entryName) {
    if (extractionDirectory == null || entryName == null) return null;
    final var normalized = normalizeArchiveEntry(entryName);
    if (!normalized.startsWith(LIBRARY_DIRECTORY)) return null;

    final var fileName = normalized.substring(LIBRARY_DIRECTORY.length());
    if (!isDirectFileName(fileName)) return null;

    final var libraryDirectory =
        extractionDirectory.toAbsolutePath().normalize().resolve(Loader.LOGISIM_LIBRARY_DIR);
    final var target = libraryDirectory.resolve(fileName).normalize();
    return target.startsWith(libraryDirectory) ? target : null;
  }

  /** Resolves the manifest's root-level main circuit file without allowing path traversal. */
  public static Path resolveMainFile(Path extractionDirectory, String entryName) {
    if (extractionDirectory == null || !isDirectFileName(entryName)) return null;
    final var directory = extractionDirectory.toAbsolutePath().normalize();
    final var target = directory.resolve(entryName).normalize();
    return target.startsWith(directory) ? target : null;
  }

  private static String normalizeArchiveEntry(String entryName) {
    return entryName.replace('\\', '/');
  }

  private static boolean isDirectFileName(String fileName) {
    return fileName != null
        && !fileName.isEmpty()
        && !fileName.equals(".")
        && !fileName.equals("..")
        && fileName.indexOf('/') < 0
        && fileName.indexOf('\\') < 0;
  }

  private static void requireDirectFileName(String fileName) {
    if (!isDirectFileName(fileName)) {
      throw new IllegalArgumentException("Project bundle file name must not contain a path");
    }
  }
}
