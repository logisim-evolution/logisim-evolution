/*
 * Logisim-evolution - digital logic design tool and simulator
 * Copyright by the Logisim-evolution developers
 *
 * https://github.com/logisim-evolution/
 *
 * This is free software released under GNU GPLv3 license
 */

package com.cburch.logisim.file;

import static com.cburch.logisim.file.Strings.S;

import com.cburch.logisim.tools.Library;
import com.cburch.logisim.util.LineBuffer;

import java.io.File;
import java.io.IOException;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;
import java.util.WeakHashMap;
import java.util.regex.Pattern;

public final class LibraryManager {

  public static final LibraryManager instance = new LibraryManager();

  public static final char DESC_SEP = '#';
  private final HashMap<LibraryDescriptor, WeakReference<LoadedLibrary>> fileMap;
  private final WeakHashMap<LoadedLibrary, LibraryDescriptor> invMap;

  private static class JarDescriptor implements LibraryDescriptor {
    private final File file;
    private final String className;

    JarDescriptor(File file, String className) {
      this.file = file;
      this.className = className;
    }

    @Override
    public boolean concernsFile(File query) {
      return file.equals(query);
    }

    @Override
    public boolean equals(Object other) {
      return (other instanceof JarDescriptor o)
             ? this.file.equals(o.file) && this.className.equals(o.className)
             : false;
    }

    @Override
    public int hashCode() {
      return file.hashCode() * 31 + className.hashCode();
    }

    @Override
    public void setBase(Loader loader, LoadedLibrary lib) throws LoadFailedException {
      lib.setBase(loader.loadJarFile(file, className));
    }

    @Override
    public String toDescriptor(Loader loader) {
      return "jar#" + toRelative(loader, file) + DESC_SEP + className;
    }
  }

  private interface LibraryDescriptor {
    boolean concernsFile(File query);

    void setBase(Loader loader, LoadedLibrary lib) throws LoadFailedException;

    String toDescriptor(Loader loader);
  }

  private static class LogisimProjectDescriptor implements LibraryDescriptor {
    private final File file;

    public LogisimProjectDescriptor(File file) {
      this.file = file;
    }

    @Override
    public boolean concernsFile(File query) {
      return file.equals(query);
    }

    @Override
    public boolean equals(Object other) {
      return (other instanceof LogisimProjectDescriptor o)
             ? this.file.equals(o.file)
             : false;
    }

    @Override
    public int hashCode() {
      return file.hashCode();
    }

    @Override
    public void setBase(Loader loader, LoadedLibrary lib) throws LoadFailedException {
      lib.setBase(loader.loadLogisimFile(file));
    }

    @Override
    public String toDescriptor(Loader loader) {
      return "file#" + toRelative(loader, file);
    }
  }

  private LibraryManager() {
    fileMap = new HashMap<>();
    invMap = new WeakHashMap<>();
    ProjectsDirty.initialize();
  }

  private static String toRelative(Loader loader, File file) {
    final var currentDirectory = loader.getCurrentDirectory();
    var fileName = file.toString();
    try {
      fileName = file.getCanonicalPath();
    } catch (IOException e) {
      // Do nothing as we already have defined the default above
    }
    if (currentDirectory != null) {
      final var currentParts = currentDirectory.toString().split(Pattern.quote(File.separator));
      final var newParts = fileName.split(Pattern.quote(File.separator));
      final var nrOfNewParts = newParts.length;
      // note that the newParts includes the filename, whilst the old doesn't
      var nrOfPartsEqual = 0;
      while ((nrOfPartsEqual < currentParts.length) && (nrOfPartsEqual < (nrOfNewParts - 1))
          && (currentParts[nrOfPartsEqual].equals(newParts[nrOfPartsEqual]))) {
        nrOfPartsEqual++;
      }
      final var nrOfLevelsToGoDown = currentParts.length - nrOfPartsEqual;
      final var relativeFile = new StringBuilder();
      relativeFile.append(String.format("..%s", File.separator).repeat(nrOfLevelsToGoDown));
      for (var restingPartId = nrOfPartsEqual; restingPartId < nrOfNewParts; restingPartId++) {
        relativeFile.append(newParts[restingPartId]);
        if (restingPartId < (nrOfNewParts - 1)) relativeFile.append(File.separator);
      }
      return relativeFile.toString();
    }
    return fileName;
  }


  public void fileSaved(Loader loader, File dest, File oldFile, LogisimFile file) {
    final var old = findKnown(oldFile);
    if (old != null) {
      old.setDirty(false);
    }

    final var lib = findKnown(dest);
    if (lib != null) {
      final var clone = file.cloneLogisimFile(loader);
      clone.setName(file.getName());
      clone.setDirty(false);
      lib.setBase(clone);
    }
  }

  private LoadedLibrary findKnown(Object key) {
    final var retLibRef = fileMap.get(key);
    if (retLibRef == null) {
      return null;
    } else {
      final var retLib = retLibRef.get();
      if (retLib == null) {
        fileMap.remove(key);
        return null;
      } else {
        return retLib;
      }
    }
  }

  public Library findReference(LogisimFile file, File query) {
    for (final var lib : file.getLibraries()) {
      final var desc = invMap.get(lib);
      if (desc != null && desc.concernsFile(query)) {
        return lib;
      }
      if (lib instanceof LoadedLibrary loadedLib) {
        if (loadedLib.getBase() instanceof LogisimFile loadedProj) {
          final var ret = findReference(loadedProj, query);
          if (ret != null) return lib;
        }
      }
    }
    return null;
  }

  public String getDescriptor(Loader loader, Library lib) {
    if (loader.getBuiltin().getLibraries().contains(lib)) {
      return DESC_SEP + lib.getName();
    } else {
      final var desc = invMap.get(lib);
      if (desc != null) {
        return desc.toDescriptor(loader);
      } else {
        throw new LoaderException(
            S.get("fileDescriptorUnknownError", lib.getDisplayName()));
      }
    }
  }

  Collection<LogisimFile> getLogisimLibraries() {
    final var ret = new ArrayList<LogisimFile>();
    for (final var lib : invMap.keySet()) {
      if (lib.getBase() instanceof LogisimFile lsFile) {
        ret.add(lsFile);
      }
    }
    return ret;
  }

  public LoadedLibrary loadJarLibrary(Loader loader, File toRead, String className) {
    final var jarDescriptor = new JarDescriptor(toRead, className);
    var ret = findKnown(jarDescriptor);
    if (ret != null) return ret;

    try {
      ret = new LoadedLibrary(loader.loadJarFile(toRead, className));
    } catch (LoadFailedException e) {
      loader.showError(e.getMessage());
      return null;
    }

    fileMap.put(jarDescriptor, new WeakReference<>(ret));
    invMap.put(ret, jarDescriptor);
    return ret;
  }

  public static Set<String> getBuildinNames(Loader loader) {
    final var buildinNames = new HashSet<String>();
    for (final var lib : loader.getBuiltin().getLibraries()) {
      buildinNames.add(lib.getName());
    }
    return buildinNames;
  }

  public Library loadLibrary(Loader loader, String desc) {
    // It may already be loaded.
    // Otherwise we'll have to decode it.
    final var sep = desc.indexOf(DESC_SEP);
    if (sep < 0) {
      loader.showError(S.get("fileDescriptorError", desc));
      return null;
    }
    final var type = desc.substring(0, sep);
    final var name = desc.substring(sep + 1);

    switch (type) {
      case "" -> {
        final var ret = loader.getBuiltin().getLibrary(name);
        if (ret == null) {
          loader.showError(S.get("fileBuiltinMissingError", name));
          return null;
        }
        return ret;
      }
      case "file" -> {
        final var toRead = loader.getFileFor(name, Loader.LOGISIM_FILTER);
        return loadLogisimLibrary(loader, toRead);
      }
      case "jar" -> {
        final var sepLoc = name.lastIndexOf(DESC_SEP);
        final var fileName = name.substring(0, sepLoc);
        final var className = name.substring(sepLoc + 1);
        final var toRead = loader.getFileFor(fileName, Loader.JAR_FILTER);
        return loadJarLibrary(loader, toRead, className);
      }
      default -> {
        loader.showError(S.get("fileTypeError", type, desc));
        return null;
      }
    }
  }

  public static String getLibraryFilePath(Loader loader, String desc) {
    final var sep = desc.indexOf(DESC_SEP);
    if (sep < 0) {
      loader.showError(S.get("fileDescriptorError", desc));
      return null;
    }
    final var type = desc.substring(0, sep);
    final var name = desc.substring(sep + 1);
    return switch (type) {
      case "file" -> loader.getFileFor(name, Loader.LOGISIM_FILTER).getAbsolutePath();
      case "jar" -> loader.getFileFor(name.substring(0, name.lastIndexOf(DESC_SEP)), Loader.JAR_FILTER).getAbsolutePath();
      default -> null;
    };
  }

  public static String getReplacementDescriptor(Loader loader, String desc, String fileName) {
    final var sep = desc.indexOf(DESC_SEP);
    if (sep < 0) {
      loader.showError(S.get("fileDescriptorError", desc));
      return null;
    }
    final var type = desc.substring(0, sep);
    final var name = desc.substring(sep + 1);
    return switch (type) {
      case "file" -> String.format("file#%s", fileName);
      case "jar" -> LineBuffer.format("jar#{{1}}#{{2}}", fileName, name.substring(name.lastIndexOf(DESC_SEP) + 1));
      default -> null;
    };
  }

  public LoadedLibrary loadLogisimLibrary(Loader loader, File toRead) {
    var ret = findKnown(toRead);
    if (ret != null) return ret;

    try {
      ret = new LoadedLibrary(loader.loadLogisimFile(toRead));
    } catch (LoadFailedException e) {
      loader.showError(e.getMessage());
      return null;
    }

    final var desc = new LogisimProjectDescriptor(toRead);
    fileMap.put(desc, new WeakReference<>(ret));
    invMap.put(ret, desc);
    return ret;
  }

  public void reload(Loader loader, LoadedLibrary lib) {
    final var descriptor = invMap.get(lib);
    if (descriptor == null) {
      loader.showError(S.get("unknownLibraryFileError", lib.getDisplayName()));
    } else {
      try {
        descriptor.setBase(loader, lib);
      } catch (LoadFailedException e) {
        loader.showError(e.getMessage());
      }
    }
  }

  void setDirty(File file, boolean dirty) {
    final var lib = findKnown(file);
    if (lib != null) {
      lib.setDirty(dirty);
    }
  }

  public static void removeUnusedLibraries(Library lib) {
    LogisimFile logiLib = null;
    if (lib instanceof LoadedLibrary lib1) {
      if (lib1.getBase() instanceof LogisimFile logi) {
        logiLib = logi;
      }
    } else if (lib instanceof LogisimFile logi) {
      logiLib = logi;
    }
    if (logiLib == null) return;
    final var toBeRemoved = new HashSet<String>();
    for (final var library : logiLib.getLibraries()) {
      var isUsed = false;
      for (final var circ : logiLib.getCircuits()) {
        for (final var tool : circ.getNonWires()) {
          isUsed |= library.contains(tool.getFactory());
        }
      }
      if (!isUsed) {
        toBeRemoved.add(library.getName());
      } else {
        removeUnusedLibraries(library);
      }
    }
    for (final var remove : toBeRemoved) {
      lib.removeLibrary(remove);
    }
  }

  public static Set<String> getUsedBaseLibraries(Library library) {
    final var result = new HashSet<String>();
    for (final var lib : library.getLibraries()) {
      result.addAll(getUsedBaseLibraries(lib));
      if (!(lib instanceof LoadedLibrary) && !(lib instanceof LogisimFile)) {
        result.add(lib.getName());
      }
    }
    return result;
  }

  public static void removeBaseLibraries(Library library, Set<String> baseLibs) {
    final var libIterator = library.getLibraries().iterator();
    while (libIterator.hasNext()) {
      final var lib = libIterator.next();
      if (baseLibs.contains(lib.getName())) {
        libIterator.remove();
      } else {
        removeBaseLibraries(lib, baseLibs);
      }
    }
  }
}
