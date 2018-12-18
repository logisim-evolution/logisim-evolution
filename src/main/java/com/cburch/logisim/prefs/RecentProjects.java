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

package com.cburch.logisim.prefs;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.prefs.PreferenceChangeEvent;
import java.util.prefs.PreferenceChangeListener;
import java.util.prefs.Preferences;

class RecentProjects implements PreferenceChangeListener {
	private static class FileTime {
		private long time;
		private File file;

		public FileTime(File file, long time) {
			this.time = time;
			this.file = file;
		}

		@Override
		public boolean equals(Object other) {
			if (other instanceof FileTime) {
				FileTime o = (FileTime) other;
				return this.time == o.time && isSame(this.file, o.file);
			} else {
				return false;
			}
		}
	}

	private static boolean isSame(Object a, Object b) {
		return a == null ? b == null : a.equals(b);
	}

	private static final String BASE_PROPERTY = "recent";

	private static final int NUM_RECENT = 10;
	private File[] recentFiles;

	private long[] recentTimes;

	RecentProjects() {
		recentFiles = new File[NUM_RECENT];
		recentTimes = new long[NUM_RECENT];
		Arrays.fill(recentTimes, System.currentTimeMillis());

		Preferences prefs = AppPreferences.getPrefs();
		prefs.addPreferenceChangeListener(this);

		for (int index = 0; index < NUM_RECENT; index++) {
			getAndDecode(prefs, index);
		}
	}

	private void getAndDecode(Preferences prefs, int index) {
		String encoding = prefs.get(BASE_PROPERTY + index, null);
		if (encoding == null)
			return;
		int semi = encoding.indexOf(';');
		if (semi < 0)
			return;
		try {
			long time = Long.parseLong(encoding.substring(0, semi));
			File file = new File(encoding.substring(semi + 1));
			updateInto(index, time, file);
		} catch (NumberFormatException e) {
		}
	}

	public List<File> getRecentFiles() {
		long now = System.currentTimeMillis();
		long[] ages = new long[NUM_RECENT];
		long[] toSort = new long[NUM_RECENT];
		for (int i = 0; i < NUM_RECENT; i++) {
			if (recentFiles[i] == null) {
				ages[i] = -1;
			} else {
				ages[i] = now - recentTimes[i];
			}
			toSort[i] = ages[i];
		}
		Arrays.sort(toSort);

		List<File> ret = new ArrayList<File>();
		for (long age : toSort) {
			if (age >= 0) {
				int index = -1;
				for (int i = 0; i < NUM_RECENT; i++) {
					if (ages[i] == age) {
						index = i;
						ages[i] = -1;
						break;
					}
				}
				if (index >= 0) {
					ret.add(recentFiles[index]);
				}
			}
		}
		return ret;
	}

	private int getReplacementIndex(long now, File f) {
		long oldestAge = -1;
		int oldestIndex = 0;
		int nullIndex = -1;
		for (int i = 0; i < NUM_RECENT; i++) {
			if (f.equals(recentFiles[i])) {
				return i;
			}
			if (recentFiles[i] == null) {
				nullIndex = i;
			}
			long age = now - recentTimes[i];
			if (age > oldestAge) {
				oldestIndex = i;
				oldestAge = age;
			}
		}
		if (nullIndex != -1) {
			return nullIndex;
		} else {
			return oldestIndex;
		}
	}

	public void preferenceChange(PreferenceChangeEvent event) {
		Preferences prefs = event.getNode();
		String prop = event.getKey();
		if (prop.startsWith(BASE_PROPERTY)) {
			String rest = prop.substring(BASE_PROPERTY.length());
			int index = -1;
			try {
				index = Integer.parseInt(rest);
				if (index < 0 || index >= NUM_RECENT)
					index = -1;
			} catch (NumberFormatException e) {
			}
			if (index >= 0) {
				File oldValue = recentFiles[index];
				long oldTime = recentTimes[index];
				getAndDecode(prefs, index);
				File newValue = recentFiles[index];
				long newTime = recentTimes[index];
				if (!isSame(oldValue, newValue) || oldTime != newTime) {
					AppPreferences.firePropertyChange(
							AppPreferences.RECENT_PROJECTS, new FileTime(
									oldValue, oldTime), new FileTime(newValue,
									newTime));
				}
			}
		}
	}

	private void updateInto(int index, long time, File file) {
		File oldFile = recentFiles[index];
		long oldTime = recentTimes[index];
		if (!isSame(oldFile, file) || oldTime != time) {
			recentFiles[index] = file;
			recentTimes[index] = time;
			try {
				AppPreferences.getPrefs().put(BASE_PROPERTY + index,
						"" + time + ";" + file.getCanonicalPath());
				AppPreferences.firePropertyChange(
						AppPreferences.RECENT_PROJECTS, new FileTime(oldFile,
								oldTime), new FileTime(file, time));
			} catch (IOException e) {
				recentFiles[index] = oldFile;
				recentTimes[index] = oldTime;
			}
		}
	}

	public void updateRecent(File file) {
		File fileToSave = file;
		try {
			fileToSave = file.getCanonicalFile();
		} catch (IOException e) {
		}
		long now = System.currentTimeMillis();
		int index = getReplacementIndex(now, fileToSave);
		updateInto(index, now, fileToSave);
	}
}
