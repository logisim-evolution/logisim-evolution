/*
 * Logisim-evolution - digital logic design tool and simulator
 * Copyright by the Logisim-evolution developers
 *
 * https://github.com/logisim-evolution/
 *
 * This is free software released under GNU GPLv3 license
 */

package com.cburch.logisim.comp;

import com.cburch.logisim.data.Value;
import com.cburch.logisim.gui.theme.ColorSchemeRegistry;
import com.cburch.logisim.instance.InstancePainter;
import com.cburch.logisim.prefs.AppPreferences;
import java.awt.Color;

public interface Palette {
  Color stroke();

  Color valueColor(Value v);

  Color label();

  Color pinOutline();

  Color fill();

  Color blueIndex();

  Color background();

  Color nil();

  Color widthError();

  Color bus();

  boolean isDark();

  Color switchBody();

  Color switchOutline();

  Color switchChannel(boolean on);

  Color switchActuator(boolean on);

  Color switchLabel();

  Color headerFill();

  Color headerText();

  Color accent();

  Color selectionHalo();

  Color selectionMarqueeBorder();

  Color selectionMarqueeFill();

  Color probeFill();

  Color raisedSurface();

  Palette DARK = new DarkPalette();
  Palette LIGHT = new LightPalette();
  Palette PRINT = new PrintPalette();

  ThreadLocal<Integer> FORCE_PRINT_DEPTH = ThreadLocal.withInitial(() -> 0);

  static boolean isForcePrint() {
    return FORCE_PRINT_DEPTH.get() > 0;
  }

  static boolean isForceLight() {
    return isForcePrint();
  }

  @FunctionalInterface
  interface Scope extends AutoCloseable {
    @Override
    void close();
  }

  static Scope enterPrintScope() {
    FORCE_PRINT_DEPTH.set(FORCE_PRINT_DEPTH.get() + 1);
    return () -> {
      final var depth = FORCE_PRINT_DEPTH.get() - 1;
      if (depth <= 0) {
        FORCE_PRINT_DEPTH.remove();
      } else {
        FORCE_PRINT_DEPTH.set(depth);
      }
    };
  }

  static Scope enterLightScope() {
    return enterPrintScope();
  }

  static void withPrintScope(Runnable r) {
    try (var ignored = enterPrintScope()) {
      r.run();
    }
  }

  static void withLightScope(Runnable r) {
    withPrintScope(r);
  }

  static Palette current() {
    if (isForcePrint()) {
      return PRINT;
    }
    return AppPreferences.isDarkTheme(AppPreferences.LookAndFeel.get()) ? DARK : LIGHT;
  }

  static Palette of(ComponentDrawContext context) {
    if (isForcePrint() || (context != null && context.isPrintView())) {
      return PRINT;
    }
    return current();
  }

  static Palette of(InstancePainter painter) {
    if (isForcePrint() || (painter != null && painter.isPrintView())) {
      return PRINT;
    }
    return current();
  }

  class DarkPalette implements Palette {
    private static final Color STROKE = new Color(0xE6E6E6);
    private static final Color WIRE_0 = new Color(0x1B8458);
    private static final Color WIRE_1 = new Color(0x55F08A);
    private static final Color UNKNOWN = new Color(0x5B9BFF);
    private static final Color ERROR = new Color(0xFF6B5E);
    private static final Color MISMATCH = new Color(0xF0A030);
    private static final Color LABEL = new Color(0x7AA7FF);
    private static final Color BUS = new Color(0x9F, 0xA9, 0xB8); // Variation B neutral cool grey
    private static final Color FILL = new Color(0x25, 0x28, 0x2C);
    private static final Color BG = new Color(0x1F1F1F);
    private static final Color NIL = new Color(0x7C, 0x82, 0x8C);
    private static final Color SWITCH_RECESSED = new Color(0x141414);
    private static final Color SWITCH_ACTUATOR_ON = new Color(0xF0F0F0);
    private static final Color SWITCH_ACTUATOR_OFF = new Color(0x9E9E9E);

    @Override
    public boolean isDark() {
      return true;
    }

    @Override
    public Color switchBody() {
      return FILL;
    }

    @Override
    public Color switchOutline() {
      return stroke();
    }

    @Override
    public Color switchChannel(boolean on) {
      return SWITCH_RECESSED;
    }

    @Override
    public Color switchActuator(boolean on) {
      return on ? SWITCH_ACTUATOR_ON : SWITCH_ACTUATOR_OFF;
    }

    private static final Color HEADER_FILL = new Color(0x383838);

    @Override
    public Color headerFill() {
      return HEADER_FILL;
    }

    @Override
    public Color headerText() {
      return stroke();
    }

    @Override
    public Color switchLabel() {
      return stroke();
    }

    @Override
    public Color stroke() {
      if (!AppPreferences.isAdaptComponentColor()) {
        return new Color(AppPreferences.COMPONENT_COLOR.get());
      }
      return STROKE;
    }

    @Override
    public Color bus() {
      return ColorSchemeRegistry.getColorScheme(AppPreferences.COLOR_SCHEME.get(), true).busColor();
    }

    @Override
    public Color valueColor(Value v) {
      final var scheme = ColorSchemeRegistry.getColorScheme(AppPreferences.COLOR_SCHEME.get(), true);
      if (v == null) return scheme.unknownColor();
      if (v.isErrorValue()) return scheme.errorColor();
      if (v.getWidth() == 0) return scheme.unconnectedColor();
      if (v.getWidth() == 1) {
        if (v == Value.UNKNOWN) return scheme.unknownColor();
        if (v == Value.TRUE) return scheme.trueColor();
        if (v == Value.FALSE) return scheme.falseColor();
      }
      if (v.isFullyDefined()) return scheme.busColor();
      return scheme.mismatchColor();
    }

    @Override
    public Color label() {
      return LABEL;
    }

    @Override
    public Color pinOutline() {
      return stroke();
    }

    private static final Color ACCENT = new Color(0x35, 0x84, 0xE4);
    private static final Color HALO = new Color(0xC0, 0x61, 0xCB);
    private static final Color RADIX = new Color(0x62, 0xA0, 0xEA);
    private static final Color MARQUEE_FILL = new Color(0x35, 0x84, 0xE4, 41);

    @Override
    public Color accent() {
      return ACCENT;
    }

    @Override
    public Color selectionHalo() {
      return HALO;
    }

    @Override
    public Color selectionMarqueeBorder() {
      return ACCENT;
    }

    @Override
    public Color selectionMarqueeFill() {
      return MARQUEE_FILL;
    }

    @Override
    public Color probeFill() {
      return FILL;
    }

    @Override
    public Color raisedSurface() {
      return FILL;
    }

    @Override
    public Color fill() {
      return FILL;
    }

    @Override
    public Color blueIndex() {
      return RADIX;
    }

    @Override
    public Color background() {
      return BG;
    }

    @Override
    public Color nil() {
      return ColorSchemeRegistry.getColorScheme(AppPreferences.COLOR_SCHEME.get(), true).unconnectedColor();
    }

    @Override
    public Color widthError() {
      return ColorSchemeRegistry.getColorScheme(AppPreferences.COLOR_SCHEME.get(), true).mismatchColor();
    }
  }

  class LightPalette implements Palette {
    private static final Color STROKE = new Color(0x26, 0x28, 0x2C);
    private static final Color BG = new Color(0xFF, 0xFF, 0xFF);
    private static final Color WIRE_0 = new Color(0x0B, 0x4A, 0x2C);
    private static final Color WIRE_1 = new Color(0x16, 0xA0, 0x5A);
    private static final Color UNKNOWN = new Color(0x1C, 0x71, 0xD8);
    private static final Color ERROR = new Color(0xC0, 0x1C, 0x28);
    private static final Color MISMATCH = new Color(0xB3, 0x5C, 0x00);
    private static final Color BUS = new Color(0x4A, 0x53, 0x61); // Variation B neutral cool grey
    private static final Color LABEL = new Color(0x1A, 0x5F, 0xB4);
    private static final Color FILL = Color.WHITE;
    private static final Color NIL = new Color(0x8A, 0x90, 0x99);
    private static final Color HALO = new Color(0x91, 0x41, 0xAC);
    private static final Color ACCENT = new Color(0x1C, 0x71, 0xD8);
    private static final Color SELECTION_FILL = new Color(0x1C, 0x71, 0xD8, 36); // 14% alpha
    private static final Color HEADER_FILL = new Color(0xE4, 0xE6, 0xEA);
    private static final Color HEADER_TEXT = new Color(0x26, 0x28, 0x2C);
    private static final Color SWITCH_BODY = new Color(0xE4, 0xE6, 0xEA);
    private static final Color SWITCH_ACTUATOR_ON = new Color(0x4A, 0x53, 0x61);
    private static final Color SWITCH_ACTUATOR_OFF = new Color(0x8A, 0x90, 0x99);
    private static final Color RAISED_SURFACE = new Color(0xF2, 0xF3, 0xF5);

    @Override
    public boolean isDark() {
      return false;
    }

    @Override
    public Color stroke() {
      if (!AppPreferences.isAdaptComponentColor()) {
        return new Color(AppPreferences.COMPONENT_COLOR.get());
      }
      return STROKE;
    }

    @Override
    public Color bus() {
      return ColorSchemeRegistry.getColorScheme(AppPreferences.COLOR_SCHEME.get(), false).busColor();
    }

    @Override
    public Color valueColor(Value v) {
      final var scheme = ColorSchemeRegistry.getColorScheme(AppPreferences.COLOR_SCHEME.get(), false);
      if (v == null) return scheme.unknownColor();
      if (v.isErrorValue()) return scheme.errorColor();
      if (v.getWidth() == 0) return scheme.unconnectedColor();
      if (v.getWidth() == 1) {
        if (v == Value.UNKNOWN) return scheme.unknownColor();
        if (v == Value.TRUE) return scheme.trueColor();
        if (v == Value.FALSE) return scheme.falseColor();
      }
      if (v.isFullyDefined()) return scheme.busColor();
      return scheme.mismatchColor();
    }

    @Override
    public Color label() {
      return LABEL;
    }

    @Override
    public Color pinOutline() {
      return stroke();
    }

    @Override
    public Color fill() {
      return FILL;
    }

    @Override
    public Color blueIndex() {
      return LABEL;
    }

    @Override
    public Color background() {
      return BG;
    }

    @Override
    public Color nil() {
      return ColorSchemeRegistry.getColorScheme(AppPreferences.COLOR_SCHEME.get(), false).unconnectedColor();
    }

    @Override
    public Color widthError() {
      return ColorSchemeRegistry.getColorScheme(AppPreferences.COLOR_SCHEME.get(), false).mismatchColor();
    }

    @Override
    public Color switchBody() {
      return SWITCH_BODY;
    }

    @Override
    public Color switchOutline() {
      return stroke();
    }

    @Override
    public Color switchChannel(boolean on) {
      return on ? WIRE_1 : Color.WHITE;
    }

    @Override
    public Color switchActuator(boolean on) {
      return on ? SWITCH_ACTUATOR_ON : SWITCH_ACTUATOR_OFF;
    }

    @Override
    public Color switchLabel() {
      return stroke();
    }

    @Override
    public Color headerFill() {
      return HEADER_FILL;
    }

    @Override
    public Color headerText() {
      return HEADER_TEXT;
    }

    @Override
    public Color accent() {
      return ACCENT;
    }

    @Override
    public Color selectionHalo() {
      return HALO;
    }

    @Override
    public Color selectionMarqueeBorder() {
      return ACCENT;
    }

    @Override
    public Color selectionMarqueeFill() {
      return SELECTION_FILL;
    }

    @Override
    public Color probeFill() {
      return BG;
    }

    @Override
    public Color raisedSurface() {
      return RAISED_SURFACE;
    }
  }

  class PrintPalette implements Palette {
    private static final Color STROKE = Color.BLACK;
    private static final Color WIRE_0 = new Color(0x006400);
    private static final Color WIRE_1 = new Color(0x00D200);
    private static final Color UNKNOWN = new Color(0x2828FF);
    private static final Color ERROR = new Color(0xC00000);
    private static final Color MISMATCH = new Color(0xFF7B00);
    private static final Color LABEL = Color.BLACK;
    private static final Color FILL = Color.WHITE;
    private static final Color BG = Color.WHITE;
    private static final Color NIL = Color.GRAY;

    @Override
    public Color stroke() {
      if (!AppPreferences.isAdaptComponentColor()) {
        return new Color(AppPreferences.COMPONENT_COLOR.get());
      }
      return STROKE;
    }

    @Override
    public Color bus() {
      final var schemeId = AppPreferences.COLOR_SCHEME.get();
      if (ColorSchemeRegistry.SCHEME_MODERN.equalsIgnoreCase(schemeId)
          || ColorSchemeRegistry.SCHEME_CLASSIC.equalsIgnoreCase(schemeId)) {
        return stroke();
      }
      return ColorSchemeRegistry.getActiveScheme().busColor();
    }

    @Override
    public Color valueColor(Value v) {
      final var schemeId = AppPreferences.COLOR_SCHEME.get();
      if (ColorSchemeRegistry.SCHEME_MODERN.equalsIgnoreCase(schemeId)
          || ColorSchemeRegistry.SCHEME_CLASSIC.equalsIgnoreCase(schemeId)) {
        if (v == null) return UNKNOWN;
        if (v.isErrorValue()) return ERROR;
        if (v.getWidth() == 0) return NIL;
        if (v.getWidth() == 1) {
          if (v == Value.UNKNOWN) return UNKNOWN;
          if (v == Value.TRUE) return WIRE_1;
          if (v == Value.FALSE) return WIRE_0;
        }
        if (v.isFullyDefined()) return Color.BLACK;
        return MISMATCH;
      }
      final var scheme = ColorSchemeRegistry.getActiveScheme();
      if (v == null) return scheme.unknownColor();
      if (v.isErrorValue()) return scheme.errorColor();
      if (v.getWidth() == 0) return scheme.unconnectedColor();
      if (v.getWidth() == 1) {
        if (v == Value.UNKNOWN) return scheme.unknownColor();
        if (v == Value.TRUE) return scheme.trueColor();
        if (v == Value.FALSE) return scheme.falseColor();
      }
      if (v.isFullyDefined()) return scheme.busColor();
      return scheme.mismatchColor();
    }

    @Override
    public Color label() {
      return LABEL;
    }

    @Override
    public Color pinOutline() {
      return stroke();
    }

    @Override
    public boolean isDark() {
      return false;
    }

    @Override
    public Color switchBody() {
      return Color.DARK_GRAY;
    }

    @Override
    public Color switchOutline() {
      return null;
    }

    @Override
    public Color switchChannel(boolean on) {
      return on ? Value.trueColor : Color.white;
    }

    @Override
    public Color switchActuator(boolean on) {
      return on ? Color.DARK_GRAY : Color.GRAY;
    }

    @Override
    public Color headerFill() {
      return Color.BLACK;
    }

    @Override
    public Color headerText() {
      return Color.WHITE;
    }

    @Override
    public Color switchLabel() {
      return Color.white;
    }

    private static final Color ACCENT = new Color(0x35, 0x84, 0xE4);
    private static final Color RADIX = new Color(0x00, 0x66, 0xCC);
    private static final Color PROBE_FILL = new Color(0xFF, 0xF0, 0x99);
    private static final Color RAISED_SURFACE = new Color(230, 230, 230);
    private static final Color MARQUEE_BORDER = new Color(0, 64, 128, 255);
    private static final Color MARQUEE_FILL = new Color(192, 192, 255, 192);

    @Override
    public Color accent() {
      return ACCENT;
    }

    @Override
    public Color selectionHalo() {
      return Color.MAGENTA;
    }

    @Override
    public Color selectionMarqueeBorder() {
      return MARQUEE_BORDER;
    }

    @Override
    public Color selectionMarqueeFill() {
      return MARQUEE_FILL;
    }

    @Override
    public Color probeFill() {
      return PROBE_FILL;
    }

    @Override
    public Color raisedSurface() {
      return RAISED_SURFACE;
    }

    @Override
    public Color fill() {
      return FILL;
    }

    @Override
    public Color blueIndex() {
      return RADIX;
    }

    @Override
    public Color background() {
      return BG;
    }

    @Override
    public Color nil() {
      final var schemeId = AppPreferences.COLOR_SCHEME.get();
      if (ColorSchemeRegistry.SCHEME_MODERN.equalsIgnoreCase(schemeId)
          || ColorSchemeRegistry.SCHEME_CLASSIC.equalsIgnoreCase(schemeId)) {
        return NIL;
      }
      return ColorSchemeRegistry.getActiveScheme().unconnectedColor();
    }

    @Override
    public Color widthError() {
      final var schemeId = AppPreferences.COLOR_SCHEME.get();
      if (ColorSchemeRegistry.SCHEME_MODERN.equalsIgnoreCase(schemeId)
          || ColorSchemeRegistry.SCHEME_CLASSIC.equalsIgnoreCase(schemeId)) {
        return MISMATCH;
      }
      return ColorSchemeRegistry.getActiveScheme().mismatchColor();
    }
  }
}
