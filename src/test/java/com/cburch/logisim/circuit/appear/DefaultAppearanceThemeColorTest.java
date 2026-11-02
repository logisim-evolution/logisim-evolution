/*
 * Logisim-evolution - digital logic design tool and simulator
 * Copyright by the Logisim-evolution developers
 *
 * https://github.com/logisim-evolution/
 *
 * This is free software released under GNU GPLv3 license
 */

package com.cburch.logisim.circuit.appear;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

import com.cburch.draw.model.CanvasObject;
import com.cburch.draw.shapes.DrawAttr;
import com.cburch.draw.shapes.Poly;
import com.cburch.draw.shapes.Rectangle;
import com.cburch.logisim.data.Location;
import com.cburch.logisim.instance.Instance;
import com.cburch.logisim.instance.StdAttr;
import com.cburch.logisim.prefs.AppPreferences;
import com.cburch.logisim.std.wiring.Pin;
import java.awt.Color;
import java.util.List;
import java.util.prefs.Preferences;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/** The default subcircuit appearances must draw their outlines in the theme's component color. */
class DefaultAppearanceThemeColorTest {
  private final Preferences preferences = AppPreferences.getPrefs();
  private final String originalLookAndFeel = AppPreferences.LookAndFeel.get();
  private final String darkTheme = "ApDark" + (System.nanoTime() % 100000);
  private Color componentColor;

  @BeforeEach
  void useDarkTheme() {
    AppPreferences.LookAndFeel.set(darkTheme);
    componentColor = new Color(AppPreferences.COMPONENT_COLOR.get());
    assertFalse(Color.BLACK.equals(componentColor), "test needs a non-black component color");
  }

  @AfterEach
  void restorePreferences() throws Exception {
    AppPreferences.LookAndFeel.set(originalLookAndFeel);
    final var prefix = "themeColors." + darkTheme + ".";
    for (final var key : preferences.keys()) {
      if (key.startsWith(prefix)) preferences.remove(key);
    }
    preferences.flush();
  }

  @Test
  void evolutionClockSymbolUsesComponentColor() {
    final var poly = only(Poly.class, DefaultEvolutionAppearance.build(pins(), "sub", false));
    assertEquals(componentColor, poly.getValue(DrawAttr.STROKE_COLOR));
  }

  @Test
  void classicOutlineUsesComponentColor() {
    final var rect = only(Rectangle.class, DefaultClassicAppearance.build(pins()));
    assertEquals(componentColor, rect.getValue(DrawAttr.STROKE_COLOR));
  }

  @Test
  void holyCrossOutlineUsesComponentColor() {
    final var rect = only(Rectangle.class, DefaultHolyCrossAppearance.build(pins(), "sub"));
    assertEquals(componentColor, rect.getValue(DrawAttr.STROKE_COLOR));
  }

  /** A clock input and a plain input on the west side. */
  private static List<Instance> pins() {
    return List.of(pin(100, 100, "clk"), pin(100, 120, "a"));
  }

  private static Instance pin(int x, int y, String label) {
    final var attrs = Pin.FACTORY.createAttributeSet();
    attrs.setValue(StdAttr.LABEL, label);
    return Instance.getInstanceFor(Pin.FACTORY.createComponent(Location.create(x, y, true), attrs));
  }

  private static <T extends CanvasObject> T only(Class<T> type, List<CanvasObject> objects) {
    final var found = objects.stream().filter(type::isInstance).map(type::cast).toList();
    assertEquals(1, found.size(), () -> "expected one " + type.getSimpleName() + " in " + objects);
    return found.get(0);
  }
}
