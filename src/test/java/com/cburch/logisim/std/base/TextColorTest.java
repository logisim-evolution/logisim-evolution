/*
 * Logisim-evolution - digital logic design tool and simulator
 * Copyright by the Logisim-evolution developers
 *
 * https://github.com/logisim-evolution/
 *
 * This is free software released under GNU GPLv3 license
 */

package com.cburch.logisim.std.base;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;

import com.cburch.logisim.comp.ComponentDrawContext;
import com.cburch.logisim.data.AttributeListener;
import com.cburch.logisim.data.Location;
import com.cburch.logisim.instance.InstanceComponent;
import com.cburch.logisim.instance.InstancePainter;
import com.cburch.logisim.prefs.AppPreferences;
import java.awt.Color;
import java.awt.image.BufferedImage;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

class TextColorTest {
  @ParameterizedTest
  @CsvSource({
    "#000000, #ffffff",
    "#ff0000, #ff9393",
    "#00ff00, #009100",
    "#0000ff, #dadaff",
    "#ffff00, #252500",
    "#404040, #bfbfbf",
    "#808080, #7f7f7f",
    "#ffffff, #000000",
    "#12345678, #b3d5f778"
  })
  void themeSwitchChangesOnlyRenderedColor(String storedValue, String darkValue) {
    final var originalLookAndFeel = AppPreferences.LookAndFeel.get();
    final var storedColor = Text.ATTR_COLOR.parse(storedValue);
    final var darkColor = Text.ATTR_COLOR.parse(darkValue);
    final var attrs = Text.FACTORY.createAttributeSet();
    attrs.setValue(Text.ATTR_TEXT, "First line\nSecond line");
    attrs.setValue(Text.ATTR_COLOR, storedColor);
    final var component =
        (InstanceComponent) Text.FACTORY.createComponent(Location.create(50, 50, false), attrs);
    final var listener = mock(AttributeListener.class);
    attrs.addAttributeListener(listener);

    try {
      AppPreferences.LookAndFeel.set("com.formdev.flatlaf.FlatLightLaf");
      assertEquals(storedColor, paint(component, false));

      AppPreferences.LookAndFeel.set("com.formdev.flatlaf.FlatDarkLaf");
      assertEquals(darkColor, paint(component, false));
      assertEquals(storedColor, paint(component, true));

      AppPreferences.LookAndFeel.set("com.formdev.flatlaf.FlatDarculaLaf");
      assertEquals(darkColor, paint(component, false));
      assertEquals(storedColor, paint(component, true));

      AppPreferences.LookAndFeel.set("com.formdev.flatlaf.FlatLightLaf");
      assertEquals(storedColor, paint(component, false));
      assertEquals(storedColor, attrs.getValue(Text.ATTR_COLOR));
      verifyNoInteractions(listener);
    } finally {
      AppPreferences.LookAndFeel.set(originalLookAndFeel);
    }
  }

  private static Color paint(InstanceComponent component, boolean printView) {
    final var image = new BufferedImage(100, 100, BufferedImage.TYPE_INT_ARGB);
    final var graphics = image.createGraphics();
    try {
      final var context =
          new ComponentDrawContext(null, null, null, graphics, graphics, printView);
      Text.FACTORY.paintInstance(new InstancePainter(context, component));
      return graphics.getColor();
    } finally {
      graphics.dispose();
    }
  }
}
