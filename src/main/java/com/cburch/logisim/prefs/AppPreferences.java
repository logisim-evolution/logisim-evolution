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

import com.cburch.logisim.Main;
import com.cburch.logisim.circuit.RadixOption;
import com.cburch.logisim.data.AttributeOption;
import com.cburch.logisim.data.Direction;
import com.cburch.logisim.fpga.hdlgenerator.HdlGeneratorFactory;
import com.cburch.logisim.gui.menu.Menu;
import com.cburch.logisim.gui.menu.MenuItemImpl;
import com.cburch.logisim.gui.start.Startup;
import com.cburch.logisim.instance.StdAttr;
import com.cburch.logisim.util.LocaleListener;
import com.cburch.logisim.util.LocaleManager;
import com.cburch.logisim.util.PropertyChangeWeakSupport;
import com.formdev.flatlaf.FlatIntelliJLaf;
import java.awt.Component;
import java.awt.Container;
import java.awt.Font;
import java.awt.GraphicsEnvironment;
import java.awt.Image;
import java.awt.Toolkit;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;
import java.beans.PropertyChangeListener;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.prefs.BackingStoreException;
import java.util.prefs.PreferenceChangeEvent;
import java.util.prefs.PreferenceChangeListener;
import java.util.prefs.Preferences;
import javax.swing.ImageIcon;
import javax.swing.JFrame;
import javax.swing.JMenu;
import javax.swing.KeyStroke;

public class AppPreferences {
  //
  // LocalePreference
  //
  private static class LocalePreference extends PrefMonitorString {
    private static Locale findLocale(String lang) {
      Locale[] check;
      for (int set = 0; set < 2; set++) {
        check = (set == 0)
            ? new Locale[] {Locale.getDefault(), Locale.ENGLISH}
            : Locale.getAvailableLocales();
        for (Locale loc : check) {
          if (loc != null && loc.getLanguage().equals(lang)) {
            return loc;
          }
        }
      }
      return null;
    }

    public LocalePreference() {
      super("locale", "");

      String localeStr = this.get();
      if (!("".equals(localeStr))) {
        LocaleManager.setLocale(Locale.forLanguageTag(localeStr));
      }
      LocaleManager.addLocaleListener(myListener);
      myListener.localeChanged();
    }

    @Override
    public void set(String value) {
      if (findLocale(value) != null) {
        super.set(value);
      }
    }
  }

  //
  // methods for accessing preferences
  //
  private static class MyListener implements PreferenceChangeListener, LocaleListener {
    @Override
    public void localeChanged() {
      final var loc = LocaleManager.getLocale();
      final var lang = loc.getLanguage();
      if (LOCALE != null) {
        LOCALE.set(lang);
      }
    }

    @Override
    public void preferenceChange(PreferenceChangeEvent event) {
      final var prefs = event.getNode();
      final var prop = event.getKey();
      if (prop.equals(TEMPLATE_TYPE)) {
        int oldValue = templateType;
        int value = prefs.getInt(TEMPLATE_TYPE, TEMPLATE_UNKNOWN);
        if (value != oldValue) {
          templateType = value;
          propertySupport.firePropertyChange(TEMPLATE, oldValue, value);
          propertySupport.firePropertyChange(TEMPLATE_TYPE, oldValue, value);
        }
      } else if (prop.equals(TEMPLATE_FILE)) {
        final var oldValue = templateFile;
        final var value = convertFile(prefs.get(TEMPLATE_FILE, null));
        if (!Objects.equals(value, oldValue)) {
          templateFile = value;
          if (templateType == TEMPLATE_CUSTOM) {
            customTemplate = null;
            propertySupport.firePropertyChange(TEMPLATE, oldValue, value);
          }
          propertySupport.firePropertyChange(TEMPLATE_FILE, oldValue, value);
        }
      }
    }
  }

  //
  // PropertyChangeSource methods
  //
  public static void addPropertyChangeListener(PropertyChangeListener listener) {
    propertySupport.addPropertyChangeListener(listener);
  }

  public static void addPropertyChangeListener(
      String propertyName, PropertyChangeListener listener) {
    propertySupport.addPropertyChangeListener(propertyName, listener);
  }

  public static void clear() {
    try {
      getPrefs(true).clear();
    } catch (BackingStoreException ignored) {
    }
  }

  private static File convertFile(String fileName) {
    if (fileName == null || fileName.equals("")) {
      return null;
    } else {
      final var file = new File(fileName);
      return file.canRead() ? file : null;
    }
  }

  private static <E> PrefMonitor<E> create(PrefMonitor<E> monitor) {
    return monitor;
  }

  static void firePropertyChange(String property, boolean oldVal, boolean newVal) {
    propertySupport.firePropertyChange(property, oldVal, newVal);
  }

  static void firePropertyChange(String property, Object oldVal, Object newVal) {
    propertySupport.firePropertyChange(property, oldVal, newVal);
  }

  private static Template getCustomTemplate() {
    File toRead = templateFile;
    if (customTemplateFile == null || !(customTemplateFile.equals(toRead))) {
      if (toRead == null) {
        customTemplate = null;
        customTemplateFile = null;
      } else {
        try (final var reader = new FileInputStream(toRead)) {
          customTemplate = Template.create(reader);
          customTemplateFile = templateFile;
        } catch (Exception t) {
          setTemplateFile(null);
          customTemplate = null;
          customTemplateFile = null;
        }
      }
    }
    return customTemplate == null ? getPlainTemplate() : customTemplate;
  }

  public static Template getEmptyTemplate() {
    if (emptyTemplate == null) {
      emptyTemplate = Template.createEmpty();
    }
    return emptyTemplate;
  }

  private static Template getPlainTemplate() {
    if (plainTemplate == null) {
      final var ld = Startup.class.getClassLoader();
      final var in = ld.getResourceAsStream("resources/logisim/default.templ");
      if (in == null) {
        plainTemplate = getEmptyTemplate();
      } else {
        try {
          try (in) {
            plainTemplate = Template.create(in);
          }
        } catch (Exception e) {
          plainTemplate = getEmptyTemplate();
        }
      }
    }
    return plainTemplate;
  }

  public static Preferences getPrefs() {
    return getPrefs(false);
  }

  private static Preferences getPrefs(boolean shouldClear) {
    if (prefs == null) {
      synchronized (AppPreferences.class) {
        if (prefs == null) {
          final var p = Preferences.userNodeForPackage(Main.class);
          if (shouldClear) {
            try {
              p.clear();
            } catch (BackingStoreException ignored) {
            }
          }
          myListener = new MyListener();
          p.addPreferenceChangeListener(myListener);
          prefs = p;

          setTemplateFile(convertFile(p.get(TEMPLATE_FILE, null)));
          setTemplateType(p.getInt(TEMPLATE_TYPE, TEMPLATE_PLAIN));
        }
      }
    }
    return prefs;
  }

  //
  // recent projects
  //
  public static List<File> getRecentFiles() {
    return recentProjects.getRecentFiles();
  }

  //
  // template methods
  //
  public static Template getTemplate() {
    getPrefs();
    return switch (templateType) {
      case TEMPLATE_EMPTY -> getEmptyTemplate();
      case TEMPLATE_CUSTOM -> getCustomTemplate();
      default -> getPlainTemplate();
    };
  }

  public static File getTemplateFile() {
    getPrefs();
    return templateFile;
  }

  //
  // accessor methods
  //
  public static int getTemplateType() {
    getPrefs();
    int ret = templateType;
    if (ret == TEMPLATE_CUSTOM && templateFile == null) {
      ret = TEMPLATE_UNKNOWN;
    }
    return ret;
  }

  public static void handleGraphicsAcceleration() {
    try {
      final var accel = GRAPHICS_ACCELERATION.get();
      System.setProperty("sun.java2d.opengl", Boolean.toString(accel.equals(ACCEL_OPENGL)));
      System.setProperty("sun.java2d.d3d", Boolean.toString(accel.equals(ACCEL_D3D)));
    } catch (Exception ignored) {
    }
  }

  public static void removePropertyChangeListener(PropertyChangeListener listener) {
    propertySupport.removePropertyChangeListener(listener);
  }

  public static void removePropertyChangeListener(
      String propertyName, PropertyChangeListener listener) {
    propertySupport.removePropertyChangeListener(propertyName, listener);
  }

  public static void setTemplateFile(File value) {
    getPrefs();
    setTemplateFile(value, null);
  }

  public static void setTemplateFile(File value, Template template) {
    getPrefs();
    if (value != null && !value.canRead()) {
      value = null;
    }
    if (!Objects.equals(value, templateFile)) {
      try {
        customTemplateFile = template == null ? null : value;
        customTemplate = template;
        getPrefs().put(TEMPLATE_FILE, value == null ? "" : value.getCanonicalPath());
      } catch (IOException ignored) {
      }
    }
  }

  public static void setTemplateType(int value) {
    getPrefs();
    if (value != TEMPLATE_PLAIN && value != TEMPLATE_EMPTY && value != TEMPLATE_CUSTOM) {
      value = TEMPLATE_UNKNOWN;
    }
    if (value != TEMPLATE_UNKNOWN && templateType != value) {
      getPrefs().putInt(TEMPLATE_TYPE, value);
    }
  }

  public static void setScaledFonts(Component[] comp) {
    for (final var component : comp) {
      if (component instanceof Container) {
        setScaledFonts(((Container) component).getComponents());
      }
      try {
        component.setFont(getScaledFont(component.getFont()));
        component.revalidate();
        component.repaint();
      } catch (Exception ignored) {
      }
    }
  }

  public static int getDownScaled(int value, float extScale) {
    getPrefs();
    float scale = ((float) ((int) (SCALE_FACTOR.get() * 10))) / (float) 10.0;
    scale *= extScale;
    return (int) ((float) value / scale);
  }

  public static int getDownScaled(int value) {
    getPrefs();
    float scale = ((float) ((int) (SCALE_FACTOR.get() * 10))) / (float) 10.0;
    return (int) ((float) value / scale);
  }

  public static double getDownScaled(double value) {
    getPrefs();
    return value / SCALE_FACTOR.get();
  }

  public static int getScaled(int value, float extScale) {
    getPrefs();
    float scale = ((float) ((int) (SCALE_FACTOR.get() * 10))) / (float) 10.0;
    scale *= extScale;
    return (int) ((float) value * scale);
  }

  public static int getScaled(int value) {
    getPrefs();
    float scale = ((float) ((int) (SCALE_FACTOR.get() * 10))) / (float) 10.0;
    return (int) ((float) value * scale);
  }

  public static float getScaled(float value) {
    getPrefs();
    float scale = ((float) ((int) (SCALE_FACTOR.get() * 10))) / (float) 10.0;
    return value * scale;
  }

  public static float getScaled(float value, float extscale) {
    getPrefs();
    float scale = ((float) ((int) (SCALE_FACTOR.get() * 10))) / (float) 10.0;
    scale *= extscale;
    return value * scale;
  }

  public static double getScaled(double value) {
    getPrefs();
    double scale = ((double) ((int) (SCALE_FACTOR.get() * 10))) / 10.0;
    return value * scale;
  }

  public static Font getScaledFont(Font myfont) {
    if (myfont != null) {
      return myfont.deriveFont(getScaled((float) FONT_SIZE));
    } else {
      return null;
    }
  }

  public static Font getScaledFont(Font myfont, float scale) {
    if (myfont != null) {
      return myfont.deriveFont(getScaled((float) FONT_SIZE, scale));
    } else {
      return null;
    }
  }

  public static ImageIcon getScaledImageIcon(ImageIcon icon) {
    Image iconImage = icon.getImage();
    return new ImageIcon(
        iconImage.getScaledInstance(getScaled(IconSize), getScaled(IconSize), Image.SCALE_SMOOTH));
  }

  public static ImageIcon getScaledImageIcon(ImageIcon icon, float scale) {
    final var iconImage = icon.getImage();
    return new ImageIcon(
        iconImage.getScaledInstance(
            getScaled(IconSize, scale), getScaled(IconSize, scale), Image.SCALE_SMOOTH));
  }

  public static void updateRecentFile(File file) {
    recentProjects.updateRecent(file);
  }

  // class variables for maintaining consistency between properties,
  // internal variables, and other classes
  private static Preferences prefs = null;

  private static MyListener myListener = null;
  private static final PropertyChangeWeakSupport propertySupport =
      new PropertyChangeWeakSupport(AppPreferences.class);

  // Template preferences
  public static final int IconSize = 16;
  public static final int FONT_SIZE = 14;
  public static final int ICON_BORDER = 2;
  public static final int BOX_SIZE = IconSize + 2 * ICON_BORDER;
  public static final int TEMPLATE_UNKNOWN = -1;
  public static final int TEMPLATE_EMPTY = 0;
  public static final int TEMPLATE_PLAIN = 1;
  public static final int TEMPLATE_CUSTOM = 2;
  public static final String TEMPLATE = "template";
  public static final String TEMPLATE_TYPE = "templateType";
  public static final String TEMPLATE_FILE = "templateFile";
  private static int templateType = TEMPLATE_PLAIN;

  private static File templateFile = null;

  private static Template plainTemplate = null;
  private static Template emptyTemplate = null;
  private static Template customTemplate = null;
  private static File customTemplateFile = null;

  public static int getIconSize() {
    return getScaled(IconSize);
  }

  public static int getIconBorder() {
    return getScaled(ICON_BORDER);
  }

  // International preferences
  public static final String SHAPE_SHAPED = "shaped"; // ANSI

  public static final String SHAPE_RECTANGULAR = "rectangular"; // IEC
  // public static final String SHAPE_DIN40700 = "din40700";

  public static final PrefMonitor<String> GATE_SHAPE =
      create(
          new PrefMonitorStringOpts(
              "gateShape", new String[] {SHAPE_SHAPED, SHAPE_RECTANGULAR}, SHAPE_SHAPED));
  public static final PrefMonitor<String> LOCALE = create(new LocalePreference());

  // FPGA Commander Preferences
  public static final PrefMonitor<String> FPGA_Workspace =
      create(
          new PrefMonitorString(
              "FPGAWorkspace", System.getProperty("user.home") + "/logisim_evolution_workspace"));
  public static final PrefMonitor<String> HdlType =
      create(
          new PrefMonitorStringOpts(
              "afterAdd",
              new String[] {HdlGeneratorFactory.VHDL, HdlGeneratorFactory.VERILOG},
              HdlGeneratorFactory.VHDL));
  public static final PrefMonitor<String> SelectedBoard =
      create(new PrefMonitorString("SelectedBoard", null));

  public static final FpgaBoards Boards = new FpgaBoards();

  public static final PrefMonitor<Boolean> SupressGatedClockWarnings =
      create(new PrefMonitorBoolean("NoGatedClockWarnings", false));
  public static final PrefMonitor<Boolean> SupressOpenPinWarnings =
      create(new PrefMonitorBoolean("NoOpenPinWarnings", false));
  public static final PrefMonitor<Boolean> VhdlKeywordsUpperCase =
      create(new PrefMonitorBoolean("VhdlKeywordsUpperCase", true));
  //file preferences
  public static final PrefMonitor<Boolean> REMOVE_UNUSED_LIBRARIES =
      create(new PrefMonitorBoolean("removeUnusedLibs", false));
  // Window preferences
  public static final String TOOLBAR_HIDDEN = "hidden";
  public static final PrefMonitor<Boolean> SHOW_TICK_RATE =
      create(new PrefMonitorBoolean("showTickRate", false));
  public static final PrefMonitor<String> TOOLBAR_PLACEMENT =
      create(
          new PrefMonitorStringOpts(
              "toolbarPlacement",
              new String[] {
                  Direction.NORTH.toString(),
                  Direction.SOUTH.toString(),
                  Direction.EAST.toString(),
                  Direction.WEST.toString(),
                  TOOLBAR_HIDDEN
              },
              Direction.NORTH.toString()));

  public static final PrefMonitor<String> CANVAS_PLACEMENT =
      create(
          new PrefMonitorStringOpts(
              "canvasPlacement",
              new String[] {
                  Direction.EAST.toString(),
                  Direction.WEST.toString()},
              Direction.EAST.toString()));

  public static final PrefMonitor<String> LookAndFeel =
      create(new PrefMonitorString("LookAndFeel", FlatIntelliJLaf.class.getName()));

  // defaiult grid colors
  public static final int DEFAULT_CANVAS_BG_COLOR = 0xFFFFFFFF;
  public static final int DEFAULT_GRID_BG_COLOR = 0xFFFFFFFF;
  public static final int DEFAULT_GRID_DOT_COLOR = 0xFF777777;
  public static final int DEFAULT_ZOOMED_DOT_COLOR = 0xFFCCCCCC;
  public static final int DEFAULT_COMPONENT_COLOR = 0x00000000;
  public static final int DEFAULT_COMPONENT_SECONDARY_COLOR = 0x99999999;
  public static final int DEFAULT_COMPONENT_GHOST_COLOR = 0x99999999;

  // restores default grid colors
  public static void setDefaultGridColors() {
    CANVAS_BG_COLOR.set(DEFAULT_CANVAS_BG_COLOR);
    GRID_BG_COLOR.set(DEFAULT_GRID_BG_COLOR);
    GRID_DOT_COLOR.set(DEFAULT_GRID_DOT_COLOR);
    GRID_ZOOMED_DOT_COLOR.set(DEFAULT_ZOOMED_DOT_COLOR);
    COMPONENT_COLOR.set(DEFAULT_COMPONENT_COLOR);
    COMPONENT_SECONDARY_COLOR.set(DEFAULT_COMPONENT_SECONDARY_COLOR);
    COMPONENT_GHOST_COLOR.set(DEFAULT_COMPONENT_GHOST_COLOR);
  }

  public static final PrefMonitor<Integer> CANVAS_BG_COLOR =
      create(new PrefMonitorInt("canvasBgColor", DEFAULT_CANVAS_BG_COLOR));
  public static final PrefMonitor<Integer> GRID_BG_COLOR =
      create(new PrefMonitorInt("gridBgColor", DEFAULT_GRID_BG_COLOR));
  public static final PrefMonitor<Integer> GRID_DOT_COLOR =
      create(new PrefMonitorInt("gridDotColor", DEFAULT_GRID_DOT_COLOR));
  public static final PrefMonitor<Integer> GRID_ZOOMED_DOT_COLOR =
      create(new PrefMonitorInt("gridZoomedDotColor", DEFAULT_ZOOMED_DOT_COLOR));
  public static final PrefMonitor<Integer> COMPONENT_COLOR =
      create(new PrefMonitorInt("componentColor", DEFAULT_COMPONENT_COLOR));
  public static final PrefMonitor<Integer> COMPONENT_SECONDARY_COLOR =
      create(new PrefMonitorInt("componentSecondaryColor", DEFAULT_COMPONENT_SECONDARY_COLOR));
  public static final PrefMonitor<Integer> COMPONENT_GHOST_COLOR =
      create(new PrefMonitorInt("componentGhostColor", DEFAULT_COMPONENT_GHOST_COLOR));


  // Layout preferences
  public static final String ADD_AFTER_UNCHANGED = "unchanged";
  public static final String ADD_AFTER_EDIT = "edit";
  public static final PrefMonitor<Boolean> ATTRIBUTE_HALO =
      create(new PrefMonitorBoolean("attributeHalo", true));
  public static final PrefMonitor<Boolean> COMPONENT_TIPS =
      create(new PrefMonitorBoolean("componentTips", true));
  public static final PrefMonitor<Boolean> MOVE_KEEP_CONNECT =
      create(new PrefMonitorBoolean("keepConnected", true));
  public static final PrefMonitor<Boolean> ADD_SHOW_GHOSTS =
      create(new PrefMonitorBoolean("showGhosts", true));
  public static final PrefMonitor<Boolean> NAMED_CIRCUIT_BOXES_FIXED_SIZE =
      create(new PrefMonitorBoolean("namedBoxesFixed", true));
  public static final PrefMonitor<Boolean> KMAP_LINED_STYLE =
      create(new PrefMonitorBoolean("KmapLinedStyle", false));
  public static final PrefMonitor<String> DefaultAppearance =
      create(
          new PrefMonitorStringOpts(
              "defaultAppearance",
              new String[] {
                  StdAttr.APPEAR_CLASSIC.toString(),
                  StdAttr.APPEAR_FPGA.toString(),
                  StdAttr.APPEAR_EVOLUTION.toString()
              },
              StdAttr.APPEAR_EVOLUTION.toString()));

  public static AttributeOption getDefaultAppearance() {
    if (DefaultAppearance.get().equals(StdAttr.APPEAR_EVOLUTION.toString())) {
      return StdAttr.APPEAR_EVOLUTION;
    } else {
      return StdAttr.APPEAR_CLASSIC;
    }
  }

  public static AttributeOption getDefaultCircuitAppearance() {
    if (DefaultAppearance.get().equals(StdAttr.APPEAR_EVOLUTION.toString())) {
      return StdAttr.APPEAR_EVOLUTION;
    } else if (DefaultAppearance.get().equals(StdAttr.APPEAR_FPGA.toString())) {
      return StdAttr.APPEAR_FPGA;
    } else {
      return StdAttr.APPEAR_CLASSIC;
    }
  }

  public static final PrefMonitor<Boolean> NEW_INPUT_OUTPUT_SHAPES =
      create(new PrefMonitorBooleanConvert("oldIO", true));

  public static double getAutoScaleFactor() {
    return (((!GraphicsEnvironment.isHeadless())
        ? Toolkit.getDefaultToolkit().getScreenSize().getHeight()
        : 0)
        / 1000);
  }

  public static final PrefMonitor<Double> SCALE_FACTOR =
      create(
          new PrefMonitorDouble(
              "Scale",
              Math.max(
                  getAutoScaleFactor(),
                  1.0)));
  public static final PrefMonitor<String> ADD_AFTER =
      create(
          new PrefMonitorStringOpts(
              "afterAdd", new String[] {ADD_AFTER_EDIT, ADD_AFTER_UNCHANGED}, ADD_AFTER_EDIT));

  public static final String PIN_APPEAR_DOT_SMALL = "dot-small";
  public static final String PIN_APPEAR_DOT_MEDIUM = "dot-medium";
  public static final String PIN_APPEAR_DOT_BIG = "dot-big";
  public static final String PIN_APPEAR_DOT_BIGGER = "dot-bigger";
  public static final PrefMonitor<String> PinAppearance =
      create(
          new PrefMonitorStringOpts(
              "pinAppearance",
              new String[] {
                  PIN_APPEAR_DOT_SMALL,
                  PIN_APPEAR_DOT_MEDIUM,
                  PIN_APPEAR_DOT_BIG,
                  PIN_APPEAR_DOT_BIGGER
              },
              PIN_APPEAR_DOT_SMALL));

  public static final PrefMonitor<String> POKE_WIRE_RADIX1;
  public static final PrefMonitor<String> POKE_WIRE_RADIX2;

  static {
    final var radixOptions = RadixOption.OPTIONS;
    final var radixStrings = new String[radixOptions.length];
    for (var i = 0; i < radixOptions.length; i++) {
      radixStrings[i] = radixOptions[i].getSaveString();
    }
    POKE_WIRE_RADIX1 =
        create(
            new PrefMonitorStringOpts(
                "pokeRadix1", radixStrings, RadixOption.RADIX_2.getSaveString()));
    POKE_WIRE_RADIX2 =
        create(
            new PrefMonitorStringOpts(
                "pokeRadix2", radixStrings, RadixOption.RADIX_10_SIGNED.getSaveString()));
  }

  public static final PrefMonitor<Boolean> Memory_Startup_Unknown =
      create(new PrefMonitorBoolean("MemStartUnknown", false));

  // Simulation preferences
  public static final PrefMonitor<Integer> TRUE_COLOR =
      create(new PrefMonitorInt("SimTrueColor", 0x0000D200));
  public static final PrefMonitor<String> TRUE_CHAR =
      create(new PrefMonitorString("SimTrueChar", "1 "));
  public static final PrefMonitor<Integer> FALSE_COLOR =
      create(new PrefMonitorInt("SimFalseColor", 0x00006400));
  public static final PrefMonitor<String> FALSE_CHAR =
      create(new PrefMonitorString("SimFalseChar", "0 "));
  public static final PrefMonitor<Integer> UNKNOWN_COLOR =
      create(new PrefMonitorInt("SimUnknownColor", 0x002828FF));
  public static final PrefMonitor<String> UNKNOWN_CHAR =
      create(new PrefMonitorString("SimUnknownChar", "U "));
  public static final PrefMonitor<Integer> ERROR_COLOR =
      create(new PrefMonitorInt("SimErrorColor", 0x00C00000));
  public static final PrefMonitor<String> ERROR_CHAR =
      create(new PrefMonitorString("SimErrorChar", "E "));
  public static final PrefMonitor<Integer> NIL_COLOR =
      create(new PrefMonitorInt("SimNilColor", 0x808080));
  public static final PrefMonitor<String> DONTCARE_CHAR =
      create(new PrefMonitorString("SimDontCareChar", "- "));
  public static final PrefMonitor<Integer> BUS_COLOR = create(new PrefMonitorInt("SimBusColor", 0));
  public static final PrefMonitor<Integer> STROKE_COLOR =
      create(new PrefMonitorInt("SimStrokeColor", 0xff00ff));
  public static final PrefMonitor<Integer> WIDTH_ERROR_COLOR =
      create(new PrefMonitorInt("SimWidthErrorColor", 0xFF7B00));
  public static final PrefMonitor<Integer> WIDTH_ERROR_CAPTION_COLOR =
      create(new PrefMonitorInt("SimWidthErrorCaptionColor", 0x550000));
  public static final PrefMonitor<Integer> WIDTH_ERROR_HIGHLIGHT_COLOR =
      create(new PrefMonitorInt("SimWidthErrorHighlightColor", 0xFFFF00));
  public static final PrefMonitor<Integer> WIDTH_ERROR_BACKGROUND_COLOR =
      create(new PrefMonitorInt("SimWidthErrorBackgroundColor", 0xFFE6D2));
  public static final PrefMonitor<Integer> CLOCK_FREQUENCY_COLOR =
      create(new PrefMonitorInt("SimClockFrequencyColor", 0xFF00B4));
  public static final PrefMonitor<Integer> KMAP1_COLOR =
      create(new PrefMonitorInt("KMAPColor1", 0x800000));
  public static final PrefMonitor<Integer> KMAP2_COLOR =
      create(new PrefMonitorInt("KMAPColor2", 0xE6194B));
  public static final PrefMonitor<Integer> KMAP3_COLOR =
      create(new PrefMonitorInt("KMAPColor3", 0xFABEBE));
  public static final PrefMonitor<Integer> KMAP4_COLOR =
      create(new PrefMonitorInt("KMAPColor4", 0xAA6E28));
  public static final PrefMonitor<Integer> KMAP5_COLOR =
      create(new PrefMonitorInt("KMAPColor5", 0xF58230));
  public static final PrefMonitor<Integer> KMAP6_COLOR =
      create(new PrefMonitorInt("KMAPColor6", 0xFFD7B4));
  public static final PrefMonitor<Integer> KMAP7_COLOR =
      create(new PrefMonitorInt("KMAPColor7", 0x808000));
  public static final PrefMonitor<Integer> KMAP8_COLOR =
      create(new PrefMonitorInt("KMAPColor8", 0xFFFF19));
  public static final PrefMonitor<Integer> KMAP9_COLOR =
      create(new PrefMonitorInt("KMAPColor9", 0xD2F53C));
  public static final PrefMonitor<Integer> KMAP10_COLOR =
      create(new PrefMonitorInt("KMAPColor10", 0x000080));
  public static final PrefMonitor<Integer> KMAP11_COLOR =
      create(new PrefMonitorInt("KMAPColor11", 0x911EB4));
  public static final PrefMonitor<Integer> KMAP12_COLOR =
      create(new PrefMonitorInt("KMAPColor12", 0x3CB4AF));
  public static final PrefMonitor<Integer> KMAP13_COLOR =
      create(new PrefMonitorInt("KMAPColor13", 0x0082CB));
  public static final PrefMonitor<Integer> KMAP14_COLOR =
      create(new PrefMonitorInt("KMAPColor14", 0xE6BEFF));
  public static final PrefMonitor<Integer> KMAP15_COLOR =
      create(new PrefMonitorInt("KMAPColor15", 0xAAFFC3));
  public static final PrefMonitor<Integer> KMAP16_COLOR =
      create(new PrefMonitorInt("KMAPColor16", 0xF032E6));

  // FPGA commander colors
  public static final PrefMonitor<Integer> FPGA_DEFINE_COLOR =
      create(new PrefMonitorInt("FPGADefineColor", 0xFF0000));
  public static final PrefMonitor<Integer> FPGA_DEFINE_HIGHLIGHT_COLOR =
      create(new PrefMonitorInt("FPGADefineHighlightColor", 0x00FF00));
  public static final PrefMonitor<Integer> FPGA_DEFINE_RESIZE_COLOR =
      create(new PrefMonitorInt("FPGADefineResizeColor", 0x00FFFF));
  public static final PrefMonitor<Integer> FPGA_DEFINE_MOVE_COLOR =
      create(new PrefMonitorInt("FPGADefineMoveColor", 0xFF00FF));
  public static final PrefMonitor<Integer> FPGA_MAPPED_COLOR =
      create(new PrefMonitorInt("FPGAMappedColor", 0x005000));
  public static final PrefMonitor<Integer> FPGA_SELECTED_MAPPED_COLOR =
      create(new PrefMonitorInt("FPGASelectedMappedColor", 0xFF0000));
  public static final PrefMonitor<Integer> FPGA_SELECTABLE_MAPPED_COLOR =
      create(new PrefMonitorInt("FPGASelectableMappedColor", 0x00A000));
  public static final PrefMonitor<Integer> FPGA_SELECT_COLOR =
      create(new PrefMonitorInt("FPGASelectColor", 0x0000FF));

  // Experimental preferences
  public static final String ACCEL_DEFAULT = "default";

  public static final String ACCEL_NONE = "none";

  public static final String ACCEL_OPENGL = "opengl";

  public static final String ACCEL_D3D = "d3d";

  public static final PrefMonitor<String> GRAPHICS_ACCELERATION =
      create(
          new PrefMonitorStringOpts(
              "graphicsAcceleration",
              new String[] {ACCEL_DEFAULT, ACCEL_NONE, ACCEL_OPENGL, ACCEL_D3D},
              ACCEL_DEFAULT));
  public static final PrefMonitor<Boolean> AntiAliassing =
      create(new PrefMonitorBoolean("AntiAliassing", true));

  // Third party softwares preferences
  public static final PrefMonitor<String> QUESTA_PATH =
      create(new PrefMonitorString("questaPath", ""));

  public static final PrefMonitor<Boolean> QUESTA_VALIDATION =
      create(new PrefMonitorBoolean("questaValidation", false));
  public static final PrefMonitor<String> QuartusToolPath =
      create(new PrefMonitorString("QuartusToolPath", ""));
  public static final PrefMonitor<String> ISEToolPath =
      create(new PrefMonitorString("ISEToolPath", ""));
  public static final PrefMonitor<String> VivadoToolPath =
      create(new PrefMonitorString("VivadoToolPath", ""));
  public static final PrefMonitor<String> OpenFpgaToolPath =
      create(new PrefMonitorString("OpenFpgaToolPath", ""));

  // hidden window preferences - not part of the preferences dialog, changes
  // to preference does not affect current windows, and the values are not
  // saved until the application is closed
  public static final String RECENT_PROJECTS = "recentProjects";

  private static final RecentProjects recentProjects = new RecentProjects();

  public static final PrefMonitor<Double> TICK_FREQUENCY =
      create(new PrefMonitorDouble("tickFrequency", 1.0));

  public static final PrefMonitor<Boolean> LAYOUT_SHOW_GRID =
      create(new PrefMonitorBoolean("layoutGrid", true));

  public static final PrefMonitor<Double> LAYOUT_ZOOM =
      create(new PrefMonitorDouble("layoutZoom", 1.0));

  public static final PrefMonitor<Boolean> APPEARANCE_SHOW_GRID =
      create(new PrefMonitorBoolean("appearanceGrid", true));

  public static final PrefMonitor<Double> APPEARANCE_ZOOM =
      create(new PrefMonitorDouble("appearanceZoom", 1.0));

  public static final PrefMonitor<Integer> WINDOW_STATE =
      create(new PrefMonitorInt("windowState", JFrame.NORMAL));

  public static final PrefMonitor<Integer> WINDOW_WIDTH =
      create(
          new PrefMonitorInt(
              "windowWidth",
              ((!GraphicsEnvironment.isHeadless())
                  ? Toolkit.getDefaultToolkit().getScreenSize().width
                  : 0)
                  / 2));

  public static final PrefMonitor<Integer> WINDOW_HEIGHT =
      create(
          new PrefMonitorInt(
              "windowHeight",
              ((!GraphicsEnvironment.isHeadless())
                  ? Toolkit.getDefaultToolkit().getScreenSize().height
                  : 0)));

  public static void resetWindow() {
    CANVAS_PLACEMENT.set(Direction.EAST.toString());
    WINDOW_MAIN_SPLIT.set(0.251);
    WINDOW_LEFT_SPLIT.set(0.51);
    WINDOW_RIGHT_SPLIT.set(0.751);
  }

  public static final PrefMonitor<String> WINDOW_LOCATION =
      create(new PrefMonitorString("windowLocation", "0,0"));

  public static final PrefMonitor<Double> WINDOW_MAIN_SPLIT =
      create(new PrefMonitorDouble("windowMainSplit", 0.25));

  public static final PrefMonitor<Double> WINDOW_LEFT_SPLIT =
      create(new PrefMonitorDouble("windowLeftSplit", 0.5));

  public static final PrefMonitor<Double> WINDOW_RIGHT_SPLIT =
      create(new PrefMonitorDouble("windowRightSplit", 0.75));

  public static final PrefMonitor<String> DIALOG_DIRECTORY =
      create(new PrefMonitorString("dialogDirectory", ""));

  /* Hotkey Settings */
  /* Watch whether in headless mode */
  public static final int hotkeyMenuMask =
      GraphicsEnvironment.isHeadless()
          ? InputEvent.ALT_DOWN_MASK : new JMenu().getToolkit().getMenuShortcutKeyMaskEx();
  public static final PrefMonitor<KeyStroke> HOTKEY_SIM_AUTO_PROPAGATE =
      create(new PrefMonitorKeyStroke("hotkeySimAutoPropagate", KeyEvent.VK_E, hotkeyMenuMask,
          true, true));

  public static final PrefMonitor<KeyStroke> HOTKEY_SIM_RESET =
      create(new PrefMonitorKeyStroke("hotkeySimReset", KeyEvent.VK_R, hotkeyMenuMask,
          true, true));

  public static final PrefMonitor<KeyStroke> HOTKEY_SIM_STEP =
      create(new PrefMonitorKeyStroke("hotkeySimStep", KeyEvent.VK_I, hotkeyMenuMask,
          true, true));

  public static final PrefMonitor<KeyStroke> HOTKEY_SIM_TICK_HALF =
      create(new PrefMonitorKeyStroke("hotkeySimTickHalf", KeyEvent.VK_T, hotkeyMenuMask,
          true, true));

  public static final PrefMonitor<KeyStroke> HOTKEY_SIM_TICK_FULL =
      create(new PrefMonitorKeyStroke("hotkeySimTickFull", KeyEvent.VK_F9, hotkeyMenuMask,
          true, true));

  public static final PrefMonitor<KeyStroke> HOTKEY_SIM_TICK_ENABLED =
      create(new PrefMonitorKeyStroke("hotkeySimTickEnabled", KeyEvent.VK_K, hotkeyMenuMask,
          true, true));

  public static final PrefMonitor<KeyStroke> HOTKEY_EDIT_UNDO =
      create(new PrefMonitorKeyStroke("hotkeyEditUndo", KeyEvent.VK_Z, hotkeyMenuMask,
          true, true));

  public static final PrefMonitor<KeyStroke> HOTKEY_EDIT_REDO =
      create(new PrefMonitorKeyStroke("hotkeyEditRedo",
          KeyEvent.VK_Z, InputEvent.SHIFT_DOWN_MASK | hotkeyMenuMask,
          true, true));

  public static final PrefMonitor<KeyStroke> HOTKEY_WINDOW_CLOSE =
      create(new PrefMonitorKeyStroke("hotkeyWindowClose",
          KeyEvent.VK_W, hotkeyMenuMask | InputEvent.SHIFT_DOWN_MASK,
          true, true));

  public static final PrefMonitor<KeyStroke> HOTKEY_WINDOW_MINIMIZE =
      create(new PrefMonitorKeyStroke("hotkeyWindowMinimize",
          KeyEvent.VK_M, hotkeyMenuMask | InputEvent.SHIFT_DOWN_MASK,
          true, true));

  public static final PrefMonitor<KeyStroke> HOTKEY_FILE_EXPORT =
      create(new PrefMonitorKeyStroke("hotkeyFileExport",
          KeyEvent.VK_E, InputEvent.SHIFT_DOWN_MASK | hotkeyMenuMask,
          true, true));

  public static final PrefMonitor<KeyStroke> HOTKEY_FILE_PRINT =
      create(new PrefMonitorKeyStroke("hotkeyFilePrint", KeyEvent.VK_P, hotkeyMenuMask,
          true, true));

  public static final PrefMonitor<KeyStroke> HOTKEY_FILE_QUIT =
      create(new PrefMonitorKeyStroke("hotkeyFileQuit", KeyEvent.VK_Q, hotkeyMenuMask,
          true, true));

  public static final PrefMonitor<KeyStroke> HOTKEY_DIR_NORTH =
      create(new PrefMonitorKeyStroke("hotkeyDirNorth", KeyEvent.VK_UP, 0));

  public static final PrefMonitor<KeyStroke> HOTKEY_DIR_SOUTH =
      create(new PrefMonitorKeyStroke("hotkeyDirSouth", KeyEvent.VK_DOWN, 0));

  public static final PrefMonitor<KeyStroke> HOTKEY_DIR_EAST =
      create(new PrefMonitorKeyStroke("hotkeyDirEast", KeyEvent.VK_RIGHT, 0));

  public static final PrefMonitor<KeyStroke> HOTKEY_DIR_WEST =
      create(new PrefMonitorKeyStroke("hotkeyDirWest", KeyEvent.VK_LEFT, 0));

  public static final PrefMonitor<KeyStroke> HOTKEY_EDIT_MENU_DUPLICATE =
      create(new PrefMonitorKeyStroke("hotkeyEditMenuDuplicate", KeyEvent.VK_D, hotkeyMenuMask,
          true, true));

  public static final PrefMonitor<KeyStroke> HOTKEY_EDIT_TOOL_DUPLICATE =
      create(new PrefMonitorKeyStroke("hotkeyEditToolDuplicate", KeyEvent.VK_INSERT, 0));

  public static final PrefMonitor<KeyStroke> HOTKEY_PROJ_MOVE_UP =
      create(new PrefMonitorKeyStroke("hotkeyProjMoveUp",
          KeyEvent.VK_U, InputEvent.SHIFT_DOWN_MASK | hotkeyMenuMask,
          true, true));

  public static final PrefMonitor<KeyStroke> HOTKEY_PROJ_MOVE_DOWN =
      create(new PrefMonitorKeyStroke("hotkeyProjMoveDown",
          KeyEvent.VK_D, InputEvent.SHIFT_DOWN_MASK | hotkeyMenuMask,
          true, true));

  public static final PrefMonitor<KeyStroke> HOTKEY_AUTO_LABEL_OPEN =
      create(new PrefMonitorKeyStroke("hotkeyAutoLabelOpen", KeyEvent.VK_L, 0));

  public static final PrefMonitor<KeyStroke> HOTKEY_AUTO_LABEL_TOGGLE =
      create(new PrefMonitorKeyStroke("hotkeyAutoLabelToggle", KeyEvent.VK_T, 0));

  public static final PrefMonitor<KeyStroke> HOTKEY_AUTO_LABEL_VIEW =
      create(new PrefMonitorKeyStroke("hotkeyAutoLabelView", KeyEvent.VK_V, 0));

  public static final PrefMonitor<KeyStroke> HOTKEY_AUTO_LABEL_HIDE =
      create(new PrefMonitorKeyStroke("hotkeyAutoLabelHide", KeyEvent.VK_H, 0));

  public static final PrefMonitor<KeyStroke> HOTKEY_AUTO_LABEL_SELF_NUMBERED_STOP =
      create(new PrefMonitorKeyStroke("hotkeyAutoLabelSelfNumberedStop", KeyEvent.VK_A, 0));

  public static final PrefMonitor<KeyStroke> HOTKEY_ADD_TOOL_ROTATE =
      create(new PrefMonitorKeyStroke("hotkeyAddToolRotate", KeyEvent.VK_R, 0));

  public static final PrefMonitor<KeyStroke> HOTKEY_GATE_MODIFIER_SIZE_SMALL =
      create(new PrefMonitorKeyStroke("hotkeyGateModifierSizeSmall", new KeyStroke[] {
          KeyStroke.getKeyStroke(KeyEvent.VK_S, 0),
          KeyStroke.getKeyStroke(KeyEvent.VK_N, 0),
      }));

  public static final PrefMonitor<KeyStroke> HOTKEY_GATE_MODIFIER_SIZE_MEDIUM =
      create(new PrefMonitorKeyStroke("hotkeyGateModifierSizeMedium", KeyEvent.VK_M, 0));

  public static final PrefMonitor<KeyStroke> HOTKEY_GATE_MODIFIER_SIZE_WIDE =
      create(new PrefMonitorKeyStroke("hotkeyGateModifierSizeWide", KeyEvent.VK_W, 0));

  public static final PrefMonitor<KeyStroke> HOTKEY_GATE_MODIFIER_INPUT_ADD =
      create(new PrefMonitorKeyStroke("hotkeyGateModifierInputAdd", new KeyStroke[] {
          KeyStroke.getKeyStroke(KeyEvent.VK_EQUALS, 0),
          KeyStroke.getKeyStroke(KeyEvent.VK_PLUS, 0),
          KeyStroke.getKeyStroke(KeyEvent.VK_ADD, 0),
      }));

  public static final PrefMonitor<KeyStroke> HOTKEY_GATE_MODIFIER_INPUT_SUB =
      create(new PrefMonitorKeyStroke("hotkeyGateModifierInputSub", new KeyStroke[] {
          KeyStroke.getKeyStroke(KeyEvent.VK_MINUS, 0),
          KeyStroke.getKeyStroke(KeyEvent.VK_SUBTRACT, 0),
      }));

  public static void resetHotkeys() {
    try {
      int menuMask = hotkeyMenuMask;
      HOTKEY_SIM_AUTO_PROPAGATE.set(KeyStroke.getKeyStroke(KeyEvent.VK_E, menuMask));
      HOTKEY_SIM_RESET.set(KeyStroke.getKeyStroke(KeyEvent.VK_R, menuMask));
      HOTKEY_SIM_STEP.set(KeyStroke.getKeyStroke(KeyEvent.VK_I, menuMask));
      HOTKEY_SIM_TICK_HALF.set(KeyStroke.getKeyStroke(KeyEvent.VK_T, menuMask));
      HOTKEY_SIM_TICK_FULL.set(KeyStroke.getKeyStroke(KeyEvent.VK_F9, menuMask));
      HOTKEY_SIM_TICK_ENABLED.set(KeyStroke.getKeyStroke(KeyEvent.VK_K, menuMask));
      HOTKEY_EDIT_UNDO.set(KeyStroke.getKeyStroke(KeyEvent.VK_Z, menuMask));
      HOTKEY_EDIT_REDO.set(KeyStroke.getKeyStroke(KeyEvent.VK_Z,
          InputEvent.SHIFT_DOWN_MASK | menuMask));
      HOTKEY_WINDOW_CLOSE.set(KeyStroke.getKeyStroke(KeyEvent.VK_W, menuMask));
      HOTKEY_WINDOW_MINIMIZE.set(KeyStroke.getKeyStroke(KeyEvent.VK_M, menuMask));
      HOTKEY_FILE_EXPORT.set(KeyStroke.getKeyStroke(KeyEvent.VK_E,
          InputEvent.SHIFT_DOWN_MASK | menuMask));
      HOTKEY_FILE_PRINT.set(KeyStroke.getKeyStroke(KeyEvent.VK_P, menuMask));
      HOTKEY_FILE_QUIT.set(KeyStroke.getKeyStroke(KeyEvent.VK_Q, menuMask));
      HOTKEY_PROJ_MOVE_UP.set(KeyStroke.getKeyStroke(
          KeyEvent.VK_U, InputEvent.SHIFT_DOWN_MASK | hotkeyMenuMask));
      HOTKEY_PROJ_MOVE_DOWN.set(KeyStroke.getKeyStroke(
          KeyEvent.VK_D, InputEvent.SHIFT_DOWN_MASK | hotkeyMenuMask));
      HOTKEY_DIR_NORTH.set(KeyStroke.getKeyStroke(KeyEvent.VK_UP, 0));
      HOTKEY_DIR_SOUTH.set(KeyStroke.getKeyStroke(KeyEvent.VK_DOWN, 0));
      HOTKEY_DIR_EAST.set(KeyStroke.getKeyStroke(KeyEvent.VK_RIGHT, 0));
      HOTKEY_DIR_WEST.set(KeyStroke.getKeyStroke(KeyEvent.VK_LEFT, 0));
      HOTKEY_EDIT_MENU_DUPLICATE.set(KeyStroke.getKeyStroke(KeyEvent.VK_D, hotkeyMenuMask));
      HOTKEY_EDIT_TOOL_DUPLICATE.set(KeyStroke.getKeyStroke(KeyEvent.VK_INSERT, 0));
      HOTKEY_AUTO_LABEL_OPEN.set(KeyStroke.getKeyStroke(KeyEvent.VK_L, 0));
      HOTKEY_AUTO_LABEL_TOGGLE.set(KeyStroke.getKeyStroke(KeyEvent.VK_T, 0));
      HOTKEY_AUTO_LABEL_VIEW.set(KeyStroke.getKeyStroke(KeyEvent.VK_V, 0));
      HOTKEY_AUTO_LABEL_HIDE.set(KeyStroke.getKeyStroke(KeyEvent.VK_H, 0));
      HOTKEY_ADD_TOOL_ROTATE.set(KeyStroke.getKeyStroke(KeyEvent.VK_R, 0));
      ((PrefMonitorKeyStroke) HOTKEY_GATE_MODIFIER_SIZE_SMALL).set(new KeyStroke[] {
          KeyStroke.getKeyStroke(KeyEvent.VK_S, 0),
          KeyStroke.getKeyStroke(KeyEvent.VK_N, 0),
      });
      HOTKEY_GATE_MODIFIER_SIZE_MEDIUM.set(KeyStroke.getKeyStroke(KeyEvent.VK_M, 0));
      HOTKEY_GATE_MODIFIER_SIZE_WIDE.set(KeyStroke.getKeyStroke(KeyEvent.VK_W, 0));
      ((PrefMonitorKeyStroke) HOTKEY_GATE_MODIFIER_INPUT_ADD).set(new KeyStroke[] {
          KeyStroke.getKeyStroke(KeyEvent.VK_EQUALS, 0),
          KeyStroke.getKeyStroke(KeyEvent.VK_PLUS, 0),
          KeyStroke.getKeyStroke(KeyEvent.VK_ADD, 0),
      });
      ((PrefMonitorKeyStroke) HOTKEY_GATE_MODIFIER_INPUT_SUB).set(new KeyStroke[] {
          KeyStroke.getKeyStroke(KeyEvent.VK_MINUS, 0),
          KeyStroke.getKeyStroke(KeyEvent.VK_SUBTRACT, 0),
      });
      HOTKEY_AUTO_LABEL_SELF_NUMBERED_STOP.set(KeyStroke.getKeyStroke(KeyEvent.VK_A, 0));
      AppPreferences.getPrefs().flush();
    } catch (BackingStoreException e) {
      throw new RuntimeException(e);
    }
  }

  public static final List<Menu> gui_sync_objects = new ArrayList<>();

  public static void hotkeySync() {
    try {
      AppPreferences.getPrefs().flush();
      for (Menu m : gui_sync_objects) {
        m.hotkeyUpdate();
      }
    } catch (BackingStoreException e) {
      throw new RuntimeException(e);
    }
  }

  public static void hotkeyReflectError(Exception e) {
    e.printStackTrace();
  }

  public static String hotkeyCheckConflict(int keyCode, int modifier) {
    try {
      /* Check myself */
      Field[] fields = AppPreferences.class.getDeclaredFields();

      for (var f : fields) {
        String name = f.getName();
        if (name.contains("HOTKEY_")) {
          @SuppressWarnings("unchecked")
          PrefMonitor<KeyStroke> keyStroke = (PrefMonitor<KeyStroke>) f.get(AppPreferences.class);
          if (((PrefMonitorKeyStroke) keyStroke).compare(keyCode, modifier)) {
            return S.get("hotkeyErrConflict")
                + S.get(((PrefMonitorKeyStroke) keyStroke).getName());
          }
        }
      }

      /* Check all the menu items */
      for (var m : gui_sync_objects) {
        Field[] menuFields = m.getClass().getDeclaredFields();
        for (var f : menuFields) {
          f.setAccessible(true);
          if (f.getType().toString().contains("com.cburch.logisim.gui.menu.MenuItemImpl")) {
            MenuItemImpl item = (MenuItemImpl) f.get(m);
            KeyStroke itemStroke = item.getAccelerator();
            if (itemStroke == null) {
              continue;
            }
            String compareString = InputEvent.getModifiersExText(itemStroke.getModifiers()) + "+"
                + KeyEvent.getKeyText(itemStroke.getKeyCode());
            String expectedKey = InputEvent.getModifiersExText(modifier) + "+"
                + KeyEvent.getKeyText(keyCode);
            if (expectedKey.equals(compareString)) {
              return S.get("hotkeyErrConflict")
                  + item.getText();
            }
          }
        }
      }
    } catch (Exception e) {
      hotkeyReflectError(e);
    }
    return "";
  }
}
