/*******************************************************************************
 * This file is part of logisim-evolution.
 *
 *   logisim-evolution is free software: you can redistribute it and/or modify
 *   it under the terms of the GNU General Public License as published by
 *   the Free Software Foundation, either version 3 of the License, or
 *   (at your option) any later version.
 *
 *   logisim-evolution is distributed in the hope that it will be useful,
 *   but WITHOUT ANY WARRANTY; without even the implied warranty of
 *   MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *   GNU General Public License for more details.
 *
 *   You should have received a copy of the GNU General Public License
 *   along with logisim-evolution.  If not, see <http://www.gnu.org/licenses/>.
 *
 *   Original code by Carl Burch (http://www.cburch.com), 2011.
 *   Subsequent modifications by :
 *     + Haute École Spécialisée Bernoise
 *       http://www.bfh.ch
 *     + Haute École du paysage, d'ingénierie et d'architecture de Genève
 *       http://hepia.hesge.ch/
 *     + Haute École d'Ingénierie et de Gestion du Canton de Vaud
 *       http://www.heig-vd.ch/
 *   The project is currently maintained by :
 *     + REDS Institute - HEIG-VD
 *       Yverdon-les-Bains, Switzerland
 *       http://reds.heig-vd.ch
 *******************************************************************************/

package com.cburch.logisim.file;

import java.io.File;
import java.io.IOException;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.WeakHashMap;

import com.cburch.logisim.tools.Library;
import com.cburch.logisim.util.StringUtil;

class LibraryManager {
	private static class JarDescriptor extends LibraryDescriptor {
		private File file;
		private String className;

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
			if (!(other instanceof JarDescriptor))
				return false;
			JarDescriptor o = (JarDescriptor) other;
			return this.file.equals(o.file)
					&& this.className.equals(o.className);
		}

		@Override
		public int hashCode() {
			return file.hashCode() * 31 + className.hashCode();
		}

		@Override
		void setBase(Loader loader, LoadedLibrary lib)
				throws LoadFailedException {
			lib.setBase(loader.loadJarFile(file, className));
		}

		@Override
		String toDescriptor(Loader loader) {
			return "jar#" + toRelative(loader, file) + desc_sep + className;
		}
	}

	private static abstract class LibraryDescriptor {
		abstract boolean concernsFile(File query);

		abstract void setBase(Loader loader, LoadedLibrary lib)
				throws LoadFailedException;

		abstract String toDescriptor(Loader loader);
	}

	private static class LogisimProjectDescriptor extends LibraryDescriptor {
		private File file;

		LogisimProjectDescriptor(File file) {
			this.file = file;
		}

		@Override
		boolean concernsFile(File query) {
			return file.equals(query);
		}

		@Override
		public boolean equals(Object other) {
			if (!(other instanceof LogisimProjectDescriptor))
				return false;
			LogisimProjectDescriptor o = (LogisimProjectDescriptor) other;
			return this.file.equals(o.file);
		}

		@Override
		public int hashCode() {
			return file.hashCode();
		}

		@Override
		void setBase(Loader loader, LoadedLibrary lib)
				throws LoadFailedException {
			lib.setBase(loader.loadLogisimFile(file));
		}

		@Override
		String toDescriptor(Loader loader) {
			return "file#" + toRelative(loader, file);
		}
	}

	private static String toRelative(Loader loader, File file) {
		File currentDirectory = loader.getCurrentDirectory();
		if (currentDirectory == null) {
			try {
				return file.getCanonicalPath();
			} catch (IOException e) {
				return file.toString();
			}
		}

		File fileDir = file.getParentFile();
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

	private static char desc_sep = '#';
	private HashMap<LibraryDescriptor, WeakReference<LoadedLibrary>> fileMap;

	private WeakHashMap<LoadedLibrary, LibraryDescriptor> invMap;

	private LibraryManager() {
		fileMap = new HashMap<LibraryDescriptor, WeakReference<LoadedLibrary>>();
		invMap = new WeakHashMap<LoadedLibrary, LibraryDescriptor>();
		ProjectsDirty.initialize();
	}

	public void fileSaved(Loader loader, File dest, File oldFile,
			LogisimFile file) {
		LoadedLibrary old = findKnown(oldFile);
		if (old != null) {
			old.setDirty(false);
		}

		LoadedLibrary lib = findKnown(dest);
		if (lib != null) {
			LogisimFile clone = file.cloneLogisimFile(loader);
			clone.setName(file.getName());
			clone.setDirty(false);
			lib.setBase(clone);
		}
	}

	private LoadedLibrary findKnown(Object key) {
		WeakReference<LoadedLibrary> retLibRef;
		retLibRef = fileMap.get(key);
		if (retLibRef == null) {
			return null;
		} else {
			LoadedLibrary retLib = retLibRef.get();
			if (retLib == null) {
				fileMap.remove(key);
				return null;
			} else {
				return retLib;
			}
		}
	}

	public Library findReference(LogisimFile file, File query) {
		for (Library lib : file.getLibraries()) {
			LibraryDescriptor desc = invMap.get(lib);
			if (desc != null && desc.concernsFile(query)) {
				return lib;
			}
			if (lib instanceof LoadedLibrary) {
				LoadedLibrary loadedLib = (LoadedLibrary) lib;
				if (loadedLib.getBase() instanceof LogisimFile) {
					LogisimFile loadedProj = (LogisimFile) loadedLib.getBase();
					Library ret = findReference(loadedProj, query);
					if (ret != null)
						return lib;
				}
			}
		}
		return null;
	}

	public String getDescriptor(Loader loader, Library lib) {
		if (loader.getBuiltin().getLibraries().contains(lib)) {
			return desc_sep + lib.getName();
		} else {
			LibraryDescriptor desc = invMap.get(lib);
			if (desc != null) {
				return desc.toDescriptor(loader);
			} else {
				throw new LoaderException(StringUtil.format(
						Strings.get("fileDescriptorUnknownError"),
						lib.getDisplayName()));
			}
		}
	}

	Collection<LogisimFile> getLogisimLibraries() {
		ArrayList<LogisimFile> ret = new ArrayList<LogisimFile>();
		for (LoadedLibrary lib : invMap.keySet()) {
			if (lib.getBase() instanceof LogisimFile) {
				ret.add((LogisimFile) lib.getBase());
			}
		}
		return ret;
	}

	public LoadedLibrary loadJarLibrary(Loader loader, File toRead,
			String className) {
		JarDescriptor jarDescriptor = new JarDescriptor(toRead, className);
		LoadedLibrary ret = findKnown(jarDescriptor);
		if (ret != null)
			return ret;

		try {
			ret = new LoadedLibrary(loader.loadJarFile(toRead, className));
		} catch (LoadFailedException e) {
			loader.showError(e.getMessage());
			return null;
		}

		fileMap.put(jarDescriptor, new WeakReference<LoadedLibrary>(ret));
		invMap.put(ret, jarDescriptor);
		return ret;
	}

	public Library loadLibrary(Loader loader, String desc) {
		// It may already be loaded.
		// Otherwise we'll have to decode it.
		int sep = desc.indexOf(desc_sep);
		if (sep < 0) {
			loader.showError(StringUtil.format(
					Strings.get("fileDescriptorError"), desc));
			return null;
		}
		String type = desc.substring(0, sep);
		String name = desc.substring(sep + 1);

		if (type.equals("")) {
			Library ret = loader.getBuiltin().getLibrary(name);
			if (ret == null) {
				loader.showError(StringUtil.format(
						Strings.get("fileBuiltinMissingError"), name));
				return null;
			}
			return ret;
		} else if (type.equals("file")) {
			File toRead = loader.getFileFor(name, Loader.LOGISIM_FILTER);
			return loadLogisimLibrary(loader, toRead);
		} else if (type.equals("jar")) {
			int sepLoc = name.lastIndexOf(desc_sep);
			String fileName = name.substring(0, sepLoc);
			String className = name.substring(sepLoc + 1);
			File toRead = loader.getFileFor(fileName, Loader.JAR_FILTER);
			return loadJarLibrary(loader, toRead, className);
		} else {
			loader.showError(StringUtil.format(Strings.get("fileTypeError"),
					type, desc));
			return null;
		}
	}

	public LoadedLibrary loadLogisimLibrary(Loader loader, File toRead) {
		LoadedLibrary ret = findKnown(toRead);
		if (ret != null)
			return ret;

		try {
			ret = new LoadedLibrary(loader.loadLogisimFile(toRead));
		} catch (LoadFailedException e) {
			loader.showError(e.getMessage());
			return null;
		}

		LogisimProjectDescriptor desc = new LogisimProjectDescriptor(toRead);
		fileMap.put(desc, new WeakReference<LoadedLibrary>(ret));
		invMap.put(ret, desc);
		return ret;
	}

	public void reload(Loader loader, LoadedLibrary lib) {
		LibraryDescriptor descriptor = invMap.get(lib);
		if (descriptor == null) {
			loader.showError(StringUtil.format(
					Strings.get("unknownLibraryFileError"),
					lib.getDisplayName()));
		} else {
			try {
				descriptor.setBase(loader, lib);
			} catch (LoadFailedException e) {
				loader.showError(e.getMessage());
			}
		}
	}

	void setDirty(File file, boolean dirty) {
		LoadedLibrary lib = findKnown(file);
		if (lib != null) {
			lib.setDirty(dirty);
		}
	}
}