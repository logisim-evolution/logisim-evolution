/*
 * This file is part of logisim-evolution.
 *
 * Logisim-evolution is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by the
 * Free Software Foundation, either version 3 of the License, or (at your
 * option) any later version.
 *
 * Logisim-evolution is distributed in the hope that it will be useful, but
 * WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 * or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
 * for more details.
 *
 * You should have received a copy of the GNU General Public License along
 * with logisim-evolution. If not, see <http://www.gnu.org/licenses/>.
 *
 * Original code by Carl Burch (http://www.cburch.com), 2011.
 * Subsequent modifications by:
 *   + College of the Holy Cross
 *     http://www.holycross.edu
 *   + Haute École Spécialisée Bernoise/Berner Fachhochschule
 *     http://www.bfh.ch
 *   + Haute École du paysage, d'ingénierie et d'architecture de Genève
 *     http://hepia.hesge.ch/
 *   + Haute École d'Ingénierie et de Gestion du Canton de Vaud
 *     http://www.heig-vd.ch/
 */

package com.cburch.logisim.prefs;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.prefs.PreferenceChangeEvent;
import java.util.prefs.PreferenceChangeListener;
import java.util.prefs.Preferences;
import lombok.val;

class RecentProjects implements PreferenceChangeListener {
  private static class FileTime {
    private final long time;
    private final File file;

    public FileTime(File file, long time) {
      this.time = time;
      this.file = file;
    }

    @Override
    public boolean equals(Object other) {
      if (other instanceof FileTime) {
        val o = (FileTime) other;
        return this.time == o.time && isSame(this.file, o.file);
      } else {
        return false;
      }
    }
  }

  private static boolean isSame(Object a, Object b) {
    return Objects.equals(a, b);
  }

  private static final String BASE_PROPERTY = "recent";

  private static final int NUM_RECENT = 10;
  private final File[] recentFiles;

  private final long[] recentTimes;

  RecentProjects() {
    recentFiles = new File[NUM_RECENT];
    recentTimes = new long[NUM_RECENT];
    Arrays.fill(recentTimes, System.currentTimeMillis());

    val prefs = AppPreferences.getPrefs();
    prefs.addPreferenceChangeListener(this);

    for (var index = 0; index < NUM_RECENT; index++) {
      getAndDecode(prefs, index);
    }
  }

  private void getAndDecode(Preferences prefs, int index) {
    val encoding = prefs.get(BASE_PROPERTY + index, null);
    if (encoding == null) return;
    int semi = encoding.indexOf(';');
    if (semi < 0) return;
    try {
      val time = Long.parseLong(encoding.substring(0, semi));
      val file = new File(encoding.substring(semi + 1));
      updateInto(index, time, file);
    } catch (NumberFormatException ignored) {
    }
  }

  public List<File> getRecentFiles() {
    val now = System.currentTimeMillis();
    val ages = new long[NUM_RECENT];
    val toSort = new long[NUM_RECENT];
    for (var i = 0; i < NUM_RECENT; i++) {
      ages[i] = (recentFiles[i] == null) ? -1 : now - recentTimes[i];
      toSort[i] = ages[i];
    }
    Arrays.sort(toSort);

    val ret = new ArrayList<File>();
    for (val age : toSort) {
      if (age >= 0) {
        var index = -1;
        for (var i = 0; i < NUM_RECENT; i++) {
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
    var oldestAge = -1L;
    var oldestIndex = 0;
    var nullIndex = -1;
    for (var i = 0; i < NUM_RECENT; i++) {
      if (f.equals(recentFiles[i])) {
        return i;
      }
      if (recentFiles[i] == null) {
        nullIndex = i;
      }
      val age = now - recentTimes[i];
      if (age > oldestAge) {
        oldestIndex = i;
        oldestAge = age;
      }
    }
    return (nullIndex != -1) ? nullIndex : oldestIndex;
  }

  @Override
  public void preferenceChange(PreferenceChangeEvent event) {
    val prefs = event.getNode();
    val prop = event.getKey();
    if (prop.startsWith(BASE_PROPERTY)) {
      val rest = prop.substring(BASE_PROPERTY.length());
      var index = -1;
      try {
        index = Integer.parseInt(rest);
        if (index < 0 || index >= NUM_RECENT) index = -1;
      } catch (NumberFormatException ignored) {
        // do nothing
      }
      if (index >= 0) {
        val oldValue = recentFiles[index];
        val oldTime = recentTimes[index];
        getAndDecode(prefs, index);
        val newValue = recentFiles[index];
        val newTime = recentTimes[index];
        if (!isSame(oldValue, newValue) || oldTime != newTime) {
          AppPreferences.firePropertyChange(
              AppPreferences.RECENT_PROJECTS,
              new FileTime(oldValue, oldTime),
              new FileTime(newValue, newTime));
        }
      }
    }
  }

  private void updateInto(int index, long time, File file) {
    val oldFile = recentFiles[index];
    val oldTime = recentTimes[index];
    if (!isSame(oldFile, file) || oldTime != time) {
      recentFiles[index] = file;
      recentTimes[index] = time;
      try {
        AppPreferences.getPrefs().put(BASE_PROPERTY + index, "" + time + ";" + file.getCanonicalPath());
        AppPreferences.firePropertyChange(
            AppPreferences.RECENT_PROJECTS,
            new FileTime(oldFile, oldTime),
            new FileTime(file, time));
      } catch (IOException e) {
        recentFiles[index] = oldFile;
        recentTimes[index] = oldTime;
      }
    }
  }

  public void updateRecent(File file) {
    var fileToSave = file;
    try {
      fileToSave = file.getCanonicalFile();
    } catch (IOException ignored) {
      // do nothing
    }
    val now = System.currentTimeMillis();
    val index = getReplacementIndex(now, fileToSave);
    updateInto(index, now, fileToSave);
  }
}
