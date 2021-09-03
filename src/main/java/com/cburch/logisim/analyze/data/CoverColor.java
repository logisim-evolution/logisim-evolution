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
import java.awt.Color;
import java.util.ArrayList;
import java.util.List;
import java.util.prefs.PreferenceChangeEvent;
import java.util.prefs.PreferenceChangeListener;

public class CoverColor implements PreferenceChangeListener {
  public static final CoverColor COVERCOLOR = new CoverColor();

  private int index;
  private final List<Color> colors = new ArrayList<>();

  public CoverColor() {
    index = -1;
    colors.add(new Color(AppPreferences.KMAP1_COLOR.get()));
    colors.add(new Color(AppPreferences.KMAP2_COLOR.get()));
    colors.add(new Color(AppPreferences.KMAP3_COLOR.get()));
    colors.add(new Color(AppPreferences.KMAP4_COLOR.get()));
    colors.add(new Color(AppPreferences.KMAP5_COLOR.get()));
    colors.add(new Color(AppPreferences.KMAP6_COLOR.get()));
    colors.add(new Color(AppPreferences.KMAP7_COLOR.get()));
    colors.add(new Color(AppPreferences.KMAP8_COLOR.get()));
    colors.add(new Color(AppPreferences.KMAP9_COLOR.get()));
    colors.add(new Color(AppPreferences.KMAP10_COLOR.get()));
    colors.add(new Color(AppPreferences.KMAP11_COLOR.get()));
    colors.add(new Color(AppPreferences.KMAP12_COLOR.get()));
    colors.add(new Color(AppPreferences.KMAP13_COLOR.get()));
    colors.add(new Color(AppPreferences.KMAP14_COLOR.get()));
    colors.add(new Color(AppPreferences.KMAP15_COLOR.get()));
    colors.add(new Color(AppPreferences.KMAP16_COLOR.get()));
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
    int idx = -1;
    int colValue = -1;
    if (evt.getKey().contentEquals(AppPreferences.KMAP1_COLOR.getIdentifier())) {
      idx = 0;
      colValue = AppPreferences.KMAP1_COLOR.get();
    } else if (evt.getKey().contentEquals(AppPreferences.KMAP2_COLOR.getIdentifier())) {
      idx = 1;
      colValue = AppPreferences.KMAP2_COLOR.get();
    } else if (evt.getKey().contentEquals(AppPreferences.KMAP3_COLOR.getIdentifier())) {
      idx = 2;
      colValue = AppPreferences.KMAP3_COLOR.get();
    } else if (evt.getKey().contentEquals(AppPreferences.KMAP4_COLOR.getIdentifier())) {
      idx = 3;
      colValue = AppPreferences.KMAP4_COLOR.get();
    } else if (evt.getKey().contentEquals(AppPreferences.KMAP5_COLOR.getIdentifier())) {
      idx = 4;
      colValue = AppPreferences.KMAP5_COLOR.get();
    } else if (evt.getKey().contentEquals(AppPreferences.KMAP6_COLOR.getIdentifier())) {
      idx = 5;
      colValue = AppPreferences.KMAP6_COLOR.get();
    } else if (evt.getKey().contentEquals(AppPreferences.KMAP7_COLOR.getIdentifier())) {
      idx = 6;
      colValue = AppPreferences.KMAP7_COLOR.get();
    } else if (evt.getKey().contentEquals(AppPreferences.KMAP8_COLOR.getIdentifier())) {
      idx = 7;
      colValue = AppPreferences.KMAP8_COLOR.get();
    } else if (evt.getKey().contentEquals(AppPreferences.KMAP9_COLOR.getIdentifier())) {
      idx = 8;
      colValue = AppPreferences.KMAP9_COLOR.get();
    } else if (evt.getKey().contentEquals(AppPreferences.KMAP10_COLOR.getIdentifier())) {
      idx = 9;
      colValue = AppPreferences.KMAP10_COLOR.get();
    } else if (evt.getKey().contentEquals(AppPreferences.KMAP11_COLOR.getIdentifier())) {
      idx = 10;
      colValue = AppPreferences.KMAP11_COLOR.get();
    } else if (evt.getKey().contentEquals(AppPreferences.KMAP12_COLOR.getIdentifier())) {
      idx = 11;
      colValue = AppPreferences.KMAP12_COLOR.get();
    } else if (evt.getKey().contentEquals(AppPreferences.KMAP13_COLOR.getIdentifier())) {
      idx = 12;
      colValue = AppPreferences.KMAP13_COLOR.get();
    } else if (evt.getKey().contentEquals(AppPreferences.KMAP14_COLOR.getIdentifier())) {
      idx = 13;
      colValue = AppPreferences.KMAP14_COLOR.get();
    } else if (evt.getKey().contentEquals(AppPreferences.KMAP15_COLOR.getIdentifier())) {
      idx = 14;
      colValue = AppPreferences.KMAP15_COLOR.get();
    } else if (evt.getKey().contentEquals(AppPreferences.KMAP16_COLOR.getIdentifier())) {
      idx = 15;
      colValue = AppPreferences.KMAP16_COLOR.get();
    }
    if (idx < 0) return;
    colors.remove(idx);
    colors.add(idx, new Color(colValue));
  }
}
