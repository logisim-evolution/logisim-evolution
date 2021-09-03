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
import java.io.File;
import java.io.IOException;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.WeakHashMap;

class LibraryManager {
  private static class JarDescriptor extends LibraryDescriptor {
    private final File file;
    private final String className;

    JarDescriptor(File file, String className) {
      this.file = file;
      this.className = className;
    }

    @Override
    boolean concernsFile(File query) {
      return file.equals(query);
    }

    @Override
    public boolean equals(Object other) {
      if (!(other instanceof JarDescriptor)) return false;
      final var o = (JarDescriptor) other;
      return this.file.equals(o.file) && this.className.equals(o.className);
    }

    @Override
    public int hashCode() {
      return file.hashCode() * 31 + className.hashCode();
    }

    @Override
    void setBase(Loader loader, LoadedLibrary lib) throws LoadFailedException {
      lib.setBase(loader.loadJarFile(file, className));
    }

    @Override
    String toDescriptor(Loader loader) {
      return "jar#" + toRelative(loader, file) + DESC_SEP + className;
    }
  }

  private abstract static class LibraryDescriptor {
    abstract boolean concernsFile(File query);

    abstract void setBase(Loader loader, LoadedLibrary lib) throws LoadFailedException;

    abstract String toDescriptor(Loader loader);
  }

  private static class LogisimProjectDescriptor extends LibraryDescriptor {
    private final File file;

    LogisimProjectDescriptor(File file) {
      this.file = file;
    }

    @Override
    boolean concernsFile(File query) {
      return file.equals(query);
    }

    @Override
    public boolean equals(Object other) {
      if (!(other instanceof LogisimProjectDescriptor)) return false;
      final var o = (LogisimProjectDescriptor) other;
      return this.file.equals(o.file);
    }

    @Override
    public int hashCode() {
      return file.hashCode();
    }

    @Override
    void setBase(Loader loader, LoadedLibrary lib) throws LoadFailedException {
      lib.setBase(loader.loadLogisimFile(file));
    }

    @Override
    String toDescriptor(Loader loader) {
      return "file#" + toRelative(loader, file);
    }
  }

  private static String toRelative(Loader loader, File file) {
    final var currentDirectory = loader.getCurrentDirectory();
    if (currentDirectory == null) {
      try {
        return file.getCanonicalPath();
      } catch (IOException e) {
        return file.toString();
      }
    }

    final var fileDir = file.getParentFile();
    if (fileDir != null) {
      if (currentDirectory.equals(fileDir)) {
        return file.getName();
      } else if (currentDirectory.equals(fileDir.getParentFile())) {
        return fileDir.getName() + File.separator + file.getName();
      } else if (fileDir.equals(currentDirectory.getParentFile())) {
        return ".." + File.separator + file.getName();
      }
    }
    try {
      return file.getCanonicalPath();
    } catch (IOException e) {
      return file.toString();
    }
  }

  public static final LibraryManager instance = new LibraryManager();

  private static final char DESC_SEP = '#';
  private final HashMap<LibraryDescriptor, WeakReference<LoadedLibrary>> fileMap;

  private final WeakHashMap<LoadedLibrary, LibraryDescriptor> invMap;

  private LibraryManager() {
    fileMap = new HashMap<>();
    invMap = new WeakHashMap<>();
    ProjectsDirty.initialize();
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
      if (lib instanceof LoadedLibrary) {
        final var loadedLib = (LoadedLibrary) lib;
        if (loadedLib.getBase() instanceof LogisimFile) {
          final var loadedProj = (LogisimFile) loadedLib.getBase();
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
      if (lib.getBase() instanceof LogisimFile) {
        ret.add((LogisimFile) lib.getBase());
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

  public Library loadLibrary(Loader loader, String desc) {
    // It may already be loaded.
    // Otherwise we'll have to decode it.
    int sep = desc.indexOf(DESC_SEP);
    if (sep < 0) {
      loader.showError(S.get("fileDescriptorError", desc));
      return null;
    }
    final var type = desc.substring(0, sep);
    final var name = desc.substring(sep + 1);

    switch (type) {
      case "":
        final var ret = loader.getBuiltin().getLibrary(name);
        if (ret == null) {
          loader.showError(S.get("fileBuiltinMissingError", name));
          return null;
        }
        return ret;
      case "file": {
        final var toRead = loader.getFileFor(name, Loader.LOGISIM_FILTER);
        return loadLogisimLibrary(loader, toRead);
      }
      case "jar": {
        final var sepLoc = name.lastIndexOf(DESC_SEP);
        final var fileName = name.substring(0, sepLoc);
        final var className = name.substring(sepLoc + 1);
        final var toRead = loader.getFileFor(fileName, Loader.JAR_FILTER);
        return loadJarLibrary(loader, toRead, className);
      }
      default:
        loader.showError(S.get("fileTypeError", type, desc));
        return null;
    }
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
}
