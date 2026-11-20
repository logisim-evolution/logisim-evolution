/*
 * Logisim-evolution - digital logic design tool and simulator
 * Copyright by the Logisim-evolution developers
 *
 * https://github.com/logisim-evolution/
 *
 * This is free software released under GNU GPLv3 license
 */

package com.cburch.logisim.comp;

import com.cburch.logisim.instance.InstancePainter;
import com.cburch.logisim.prefs.AppPreferences;
import java.awt.Color;

public interface Palette {
  Color stroke();

  Palette DARK = new DarkPalette();
  Palette LIGHT = new LightPalette();
  Palette PRINT = new PrintPalette();

  ThreadLocal<Integer> FORCE_PRINT_DEPTH = ThreadLocal.withInitial(() -> 0);

  static boolean isForcePrint() {
    return FORCE_PRINT_DEPTH.get() > 0;
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

  static void withPrintScope(Runnable r) {
    try (var ignored = enterPrintScope()) {
      r.run();
    }
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
    private static final Color STROKE = Color.WHITE;

    @Override
    public Color stroke() {
      if (!AppPreferences.isAdaptComponentColor()) {
        return new Color(AppPreferences.COMPONENT_COLOR.get());
      }
      return STROKE;
    }
  }

  class LightPalette implements Palette {
    private static final Color STROKE = new Color(0x26, 0x28, 0x2C);

    @Override
    public Color stroke() {
      if (!AppPreferences.isAdaptComponentColor()) {
        return new Color(AppPreferences.COMPONENT_COLOR.get());
      }
      return STROKE;
    }
  }

  class PrintPalette implements Palette {
    private static final Color STROKE = Color.BLACK;

    @Override
    public Color stroke() {
      if (!AppPreferences.isAdaptComponentColor()) {
        return new Color(AppPreferences.COMPONENT_COLOR.get());
      }
      return STROKE;
    }
  }
}
