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

package com.cburch.logisim.util;

import java.io.File;
import java.io.IOException;

import javax.swing.JFileChooser;

import com.cburch.logisim.prefs.AppPreferences;

public class JFileChoosers {
	/*
	 * A user reported that JFileChooser's constructor sometimes resulted in
	 * IOExceptions when Logisim is installed under a system administrator
	 * account and then is attempted to run as a regular user. This class is an
	 * attempt to be a bit more robust about which directory the JFileChooser
	 * opens up under. (23 Feb 2010)
	 */
	private static class LogisimFileChooser extends JFileChooser {
		private static final long serialVersionUID = 1L;

		LogisimFileChooser() {
			super();
		}

		LogisimFileChooser(File initSelected) {
			super(initSelected);
		}

		@Override
		public File getSelectedFile() {
			File dir = getCurrentDirectory();
			if (dir != null) {
				JFileChoosers.currentDirectory = dir.toString();
			}
			return super.getSelectedFile();
		}
	}

	public static JFileChooser create() {
		RuntimeException first = null;
		for (int i = 0; i < PROP_NAMES.length; i++) {
			String prop = PROP_NAMES[i];
			try {
				String dirname;
				if (prop == null) {
					dirname = currentDirectory;
					if (dirname.equals("")) {
						dirname = AppPreferences.DIALOG_DIRECTORY.get();
					}
				} else {
					dirname = System.getProperty(prop);
				}
				if (dirname.equals("")) {
					return new LogisimFileChooser();
				} else {
					File dir = new File(dirname);
					if (dir.canRead()) {
						return new LogisimFileChooser(dir);
					}
				}
			} catch (RuntimeException t) {
				if (first == null)
					first = t;
				Throwable u = t.getCause();
				if (!(u instanceof IOException))
					throw t;
			}
		}
		throw first;
	}

	public static JFileChooser createAt(File openDirectory) {
		if (openDirectory == null) {
			return create();
		} else {
			try {
				return new LogisimFileChooser(openDirectory);
			} catch (RuntimeException t) {
				if (t.getCause() instanceof IOException) {
					try {
						return create();
					} catch (RuntimeException u) {
					}
				}
				throw t;
			}
		}
	}

	public static JFileChooser createSelected(File selected) {
		if (selected == null) {
			return create();
		} else {
			JFileChooser ret = createAt(selected.getParentFile());
			ret.setSelectedFile(selected);
			return ret;
		}
	}

	public static String getCurrentDirectory() {
		return currentDirectory;
	}

	private static final String[] PROP_NAMES = { null, "user.home", "user.dir",
			"java.home", "java.io.tmpdir" };

	private static String currentDirectory = "";

	private JFileChoosers() {
	}
}
