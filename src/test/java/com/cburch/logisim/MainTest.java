/*
 * Logisim-evolution - digital logic design tool and simulator
 * Copyright by the Logisim-evolution developers
 *
 * https://github.com/logisim-evolution/
 *
 * This is free software released under GNU GPLv3 license
 */

package com.cburch.logisim;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.cburch.logisim.prefs.AppPreferences;
import com.formdev.flatlaf.FlatLaf;
import java.awt.Font;
import java.util.HashMap;
import java.util.Map;
import java.util.prefs.BackingStoreException;
import java.util.prefs.Preferences;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import javax.swing.UnsupportedLookAndFeelException;
import javax.swing.plaf.FontUIResource;
import javax.swing.plaf.metal.MetalLookAndFeel;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

class MainTest {
  // A scratch node, so the real Logisim preferences are never touched.
  private final Preferences node = Preferences.userRoot().node("logisim-test-" + System.nanoTime());

  @AfterEach
  void removeNode() throws BackingStoreException {
    node.removeNode();
  }

  @Test
  void emptyNodeIsNewInstall() {
    assertTrue(Main.isNewInstall(node));
  }

  @Test
  void nodeWithOneKeyIsExistingInstall() {
    node.put("someKey", "value");
    assertFalse(Main.isNewInstall(node));
  }

  @Test
  void checkDoesNotWrite() throws BackingStoreException {
    Main.isNewInstall(node);
    assertEquals(0, node.keys().length);
  }

  @Test
  void unreadableNodeIsExistingInstall() throws BackingStoreException {
    final var broken = mock(Preferences.class);
    when(broken.keys()).thenThrow(new BackingStoreException("test"));
    assertFalse(Main.isNewInstall(broken));
  }

  @Test
  void startupFontsRespectLookAndFeelUnlessExplicitlySelected() throws Exception {
    SwingUtilities.invokeAndWait(() -> {
      final var previousLookAndFeel = UIManager.getLookAndFeel();
      final var previousFamily = FlatLaf.getPreferredFontFamily();
      final var previousScale = AppPreferences.SCALE_FACTOR.get();
      final var previousFonts = new HashMap<Object, Object>();
      final var fonts = Map.of(
          "ToolTip.font", new FontUIResource(Font.SERIF, Font.ITALIC, 15),
          "Tree.font", new FontUIResource(Font.MONOSPACED, Font.BOLD, 17),
          "MenuItem.acceleratorFont", new FontUIResource(Font.DIALOG_INPUT, Font.PLAIN, 11),
          "Label.font", new FontUIResource(Font.SERIF, Font.BOLD, 19));
      try {
        UIManager.setLookAndFeel(new MetalLookAndFeel());
        // UIManager.put returns the developer override, excluding look-and-feel defaults.
        for (final var key : UIManager.getDefaults().keySet().toArray()) {
          if (UIManager.get(key) instanceof FontUIResource) {
            previousFonts.put(key, UIManager.put(key, UIManager.get(key)));
          }
        }
        fonts.forEach(UIManager::put);
        for (final var preference : new String[] {null, "", " \t "}) {
          Main.updateGlobalFont(preference);
          fonts.forEach((key, font) -> {
            final var actual = UIManager.getFont(key);
            assertSame(font, actual, key);
            assertEquals(font.getFamily(), actual.getFamily(), key);
            assertEquals(font.getStyle(), actual.getStyle(), key);
            assertEquals(font.getSize(), actual.getSize(), key);
          });
          assertEquals(previousFamily, FlatLaf.getPreferredFontFamily());
        }

        AppPreferences.SCALE_FACTOR.set(1.5);
        Main.updateGlobalFont(Font.MONOSPACED);
        assertEquals(Font.MONOSPACED, FlatLaf.getPreferredFontFamily());
        fonts.forEach((key, font) -> {
          final var actual = UIManager.getFont(key);
          assertEquals(Font.MONOSPACED, actual.getFamily(), key);
          final var speciallySized = key.equals("ToolTip.font")
              || key.equals("MenuItem.acceleratorFont");
          assertEquals(speciallySized ? Font.BOLD : font.getStyle(), actual.getStyle(), key);
          assertEquals(speciallySized ? 18 : font.getSize(), actual.getSize(), key);
        });
      } catch (UnsupportedLookAndFeelException e) {
        throw new AssertionError(e);
      } finally {
        previousFonts.forEach(UIManager::put);
        FlatLaf.setPreferredFontFamily(previousFamily);
        AppPreferences.SCALE_FACTOR.set(previousScale);
        try {
          UIManager.setLookAndFeel(previousLookAndFeel);
        } catch (UnsupportedLookAndFeelException e) {
          throw new AssertionError(e);
        }
      }
    });
  }
}
