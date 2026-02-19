package com.cburch.logisim.prefs;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

public class CanvasDefaultColorTest {

  @Test
  public void defaultCanvasBackgroundColor_isDarkGray() {
    assertEquals(0xFF3C3C3C, AppPreferences.DEFAULT_CANVAS_BG_COLOR);
  }

  @Test
  public void defaultGridBackgroundColor_isDarkGray() {
    assertEquals(0xFF3C3C3C, AppPreferences.DEFAULT_GRID_BG_COLOR);
  }
}
