/*
 * Logisim-evolution - digital logic design tool and simulator
 * Copyright by the Logisim-evolution developers
 *
 * https://github.com/logisim-evolution/
 *
 * This is free software released under GNU GPLv3 license
 */

package com.cburch.logisim.analyze.data;

import com.cburch.logisim.prefs.AppPreferences;
import com.cburch.logisim.prefs.PrefMonitor;
import java.awt.Color;
import java.util.ArrayList;
import java.util.List;
import java.util.prefs.PreferenceChangeEvent;
import java.util.prefs.PreferenceChangeListener;

public class CoverColor implements PreferenceChangeListener {
  public static final CoverColor COVER_COLOR = new CoverColor();

  private int index = -1;
  private final List<Color> colors = new ArrayList<>();

  private final List<PrefMonitor<Integer>> availableColors =
      List.of(
          AppPreferences.KMAP1_COLOR,
          AppPreferences.KMAP2_COLOR,
          AppPreferences.KMAP3_COLOR,
          AppPreferences.KMAP4_COLOR,
          AppPreferences.KMAP5_COLOR,
          AppPreferences.KMAP6_COLOR,
          AppPreferences.KMAP7_COLOR,
          AppPreferences.KMAP8_COLOR,
          AppPreferences.KMAP9_COLOR,
          AppPreferences.KMAP10_COLOR,
          AppPreferences.KMAP11_COLOR,
          AppPreferences.KMAP12_COLOR,
          AppPreferences.KMAP13_COLOR,
          AppPreferences.KMAP14_COLOR,
          AppPreferences.KMAP15_COLOR,
          AppPreferences.KMAP16_COLOR);

  public CoverColor() {
    for (final var color : availableColors) {
      colors.add(new Color(color.get()));
    }
  }

  public String getColorName(Color col) {
    if (colors.contains(col)) return "LogisimKMapColor" + colors.indexOf(col);
    return null;
  }

  public int nrOfColors() {
    return colors.size();
  }

  public Color getColor(int index) {
    if (index < 0 || index >= colors.size()) return null;
    return colors.get(index);
  }

  public Color getNext() {
    index++;
    if (index < 0 || index >= colors.size()) index = 0;
    return colors.get(index);
  }

  public void reset() {
    index = 0;
  }

  @Override
  public void preferenceChange(PreferenceChangeEvent evt) {
    var idx = 0;
    Color newColor = null;

    for (final var color : availableColors) {
      if (evt.getKey().contentEquals(color.getIdentifier())) {
        newColor = new Color(color.get());
        break;
      }
      idx++;
    }

    if (newColor != null) {
      colors.remove(idx);
      colors.add(idx, newColor);
    }
  }
}
