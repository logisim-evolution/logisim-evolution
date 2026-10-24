/*
 * Logisim-evolution - digital logic design tool and simulator
 * Copyright by the Logisim-evolution developers
 *
 * https://github.com/logisim-evolution/
 *
 * This is free software released under GNU GPLv3 license
 */

package com.cburch.logisim.prefs;

import static com.cburch.logisim.gui.Strings.S;
import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.ArrayList;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class AppPreferencesTest {
  private String originalCircuitOpenZoom;
  private Double originalLayoutZoom;

  @BeforeEach
  void saveOriginalPreferences() {
    originalCircuitOpenZoom = AppPreferences.CIRCUIT_OPEN_ZOOM.get();
    originalLayoutZoom = AppPreferences.LAYOUT_ZOOM.get();
  }

  @AfterEach
  void restoreOriginalPreferences() {
    AppPreferences.CIRCUIT_OPEN_ZOOM.set(originalCircuitOpenZoom);
    AppPreferences.LAYOUT_ZOOM.set(originalLayoutZoom);
  }

  @Test
  void circuitOpenZoomDefaultsToFitToWindow() {
    assertEquals(AppPreferences.ZOOM_FIT_TO_WINDOW, AppPreferences.CIRCUIT_OPEN_ZOOM.get());
  }

  @Test
  void circuitOpenZoomSupportedOptions() {
    assertArrayEquals(
        new String[] {AppPreferences.ZOOM_FIT_TO_WINDOW, AppPreferences.ZOOM_LAST_USED},
        AppPreferences.CIRCUIT_OPEN_ZOOM_OPTIONS);
  }

  @Test
  void circuitOpenZoomUpdatesAndFiresPropertyChange() {
    final var events = new ArrayList<PropertyChangeEvent>();
    final PropertyChangeListener listener = events::add;
    AppPreferences.CIRCUIT_OPEN_ZOOM.addPropertyChangeListener(listener);
    try {
      AppPreferences.CIRCUIT_OPEN_ZOOM.set(AppPreferences.ZOOM_LAST_USED);
      assertEquals(AppPreferences.ZOOM_LAST_USED, AppPreferences.CIRCUIT_OPEN_ZOOM.get());
      assertEquals(1, events.size());
      assertEquals("circuitOpenZoom", events.get(0).getPropertyName());
      assertEquals(AppPreferences.ZOOM_FIT_TO_WINDOW, events.get(0).getOldValue());
      assertEquals(AppPreferences.ZOOM_LAST_USED, events.get(0).getNewValue());
    } finally {
      AppPreferences.CIRCUIT_OPEN_ZOOM.removePropertyChangeListener(listener);
    }
  }

  @Test
  void circuitOpenZoomFallsBackToDefaultOnInvalidValue() {
    AppPreferences.CIRCUIT_OPEN_ZOOM.set("invalidOption");
    assertEquals(AppPreferences.ZOOM_FIT_TO_WINDOW, AppPreferences.CIRCUIT_OPEN_ZOOM.get());
  }

  @Test
  void resetWindowResetsCircuitOpenZoomToDefault() {
    AppPreferences.CIRCUIT_OPEN_ZOOM.set(AppPreferences.ZOOM_LAST_USED);
    assertEquals(AppPreferences.ZOOM_LAST_USED, AppPreferences.CIRCUIT_OPEN_ZOOM.get());

    AppPreferences.resetWindow();
    assertEquals(AppPreferences.ZOOM_FIT_TO_WINDOW, AppPreferences.CIRCUIT_OPEN_ZOOM.get());
  }

  @Test
  void circuitOpenZoomLocalizedStringsAreDefined() {
    assertEquals("Zoom when opening a circuit:", S.get("windowCircuitOpenZoom"));
    assertEquals("Fit to window", S.get("windowCircuitOpenZoomFit"));
    assertEquals("Last used zoom", S.get("windowCircuitOpenZoomLast"));
  }
}
