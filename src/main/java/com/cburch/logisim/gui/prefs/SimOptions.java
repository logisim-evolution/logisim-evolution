/*
 * Logisim-evolution - digital logic design tool and simulator
 * Copyright by the Logisim-evolution developers
 *
 * https://github.com/logisim-evolution/
 *
 * This is free software released under GNU GPLv3 license
 */

package com.cburch.logisim.gui.prefs;

import static com.cburch.logisim.gui.Strings.S;

import com.cburch.logisim.data.Value;
import com.cburch.logisim.prefs.AppPreferences;
import com.cburch.logisim.prefs.PrefMonitor;
import com.cburch.logisim.proj.Projects;
import java.awt.Color;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.prefs.PreferenceChangeEvent;
import java.util.prefs.PreferenceChangeListener;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.SwingConstants;

public class SimOptions extends OptionsPanel {

  private static final long serialVersionUID = 1L;
  private final MyColorListener mcol = new MyColorListener();
  private final JLabel trueColorTitle = new JLabel();
  private final ColorChooserButton trueColor;
  private final JLabel trueCharTitle = new JLabel();
  private final SymbolChooser trueChar = new SymbolChooser(AppPreferences.TRUE_CHAR, "1T");
  private final JLabel falseColorTitle = new JLabel();
  private final ColorChooserButton falseColor;
  private final JLabel falseCharTitle = new JLabel();
  private final SymbolChooser falseChar = new SymbolChooser(AppPreferences.FALSE_CHAR, "0F");
  private final JLabel unknownColorTitle = new JLabel();
  private final ColorChooserButton unknownColor;
  private final JLabel unknownCharTitle = new JLabel();
  private final SymbolChooser unknownChar = new SymbolChooser(AppPreferences.UNKNOWN_CHAR, "U?Z");
  private final JLabel errorColorTitle = new JLabel();
  private final ColorChooserButton errorColor;
  private final JLabel errorCharTitle = new JLabel();
  private final SymbolChooser errorChar = new SymbolChooser(AppPreferences.ERROR_CHAR, "E!X");
  private final JLabel nilColorTitle = new JLabel();
  private final ColorChooserButton nilColor;
  private final JLabel dontCareCharTitle = new JLabel();
  private final SymbolChooser dontCareChar = new SymbolChooser(AppPreferences.DONTCARE_CHAR, "-X");
  private final JLabel busColorTitle = new JLabel();
  private final ColorChooserButton busColor;
  private final JLabel highlightColorTitle = new JLabel();
  private final ColorChooserButton highlightColor;
  private final JLabel widthErrorColorTitle = new JLabel();
  private final ColorChooserButton widthErrorColor;
  private final JLabel widthErrorCaptionColorTitle = new JLabel();
  private final ColorChooserButton widthErrorCaptionColor;
  private final JLabel widthErrorHighlightColorTitle = new JLabel();
  private final ColorChooserButton widthErrorHighlightColor;
  private final JLabel widthErrorBackgroundColorTitle = new JLabel();
  private final ColorChooserButton clockFrequencyColor;
  private final JLabel clockFrequencyColorTitle = new JLabel();
  private final ColorChooserButton widthErrorBackgroundColor;
  private final JButton defaultButton = new JButton();
  private final JButton colorBlindButton = new JButton();
  private final JLabel kmap1ColorTitle = new JLabel();
  private final JLabel kmap2ColorTitle = new JLabel();
  private final JLabel kmap3ColorTitle = new JLabel();
  private final JLabel kmap4ColorTitle = new JLabel();
  private final JLabel kmap5ColorTitle = new JLabel();
  private final JLabel kmap6ColorTitle = new JLabel();
  private final JLabel kmap7ColorTitle = new JLabel();
  private final JLabel kmap8ColorTitle = new JLabel();
  private final JLabel kmap9ColorTitle = new JLabel();
  private final JLabel kmap10ColorTitle = new JLabel();
  private final JLabel kmap11ColorTitle = new JLabel();
  private final JLabel kmap12ColorTitle = new JLabel();
  private final JLabel kmap13ColorTitle = new JLabel();
  private final JLabel kmap14ColorTitle = new JLabel();
  private final JLabel kmap15ColorTitle = new JLabel();
  private final JLabel kmap16ColorTitle = new JLabel();
  private final JLabel kmapColorsTitle = new JLabel("", SwingConstants.CENTER);
  private final ColorChooserButton kmap1Color;
  private final ColorChooserButton kmap2Color;
  private final ColorChooserButton kmap3Color;
  private final ColorChooserButton kmap4Color;
  private final ColorChooserButton kmap5Color;
  private final ColorChooserButton kmap6Color;
  private final ColorChooserButton kmap7Color;
  private final ColorChooserButton kmap8Color;
  private final ColorChooserButton kmap9Color;
  private final ColorChooserButton kmap10Color;
  private final ColorChooserButton kmap11Color;
  private final ColorChooserButton kmap12Color;
  private final ColorChooserButton kmap13Color;
  private final ColorChooserButton kmap14Color;
  private final ColorChooserButton kmap15Color;
  private final ColorChooserButton kmap16Color;

  public SimOptions(PreferencesFrame window) {
    super(window);
    AppPreferences.getPrefs().addPreferenceChangeListener(new MyListener());

    final var gbc = new GridBagConstraints();
    setLayout(new GridBagLayout());
    gbc.insets = new Insets(2, 4, 4, 2);
    gbc.anchor = GridBagConstraints.CENTER;
    gbc.gridx = 0;
    gbc.gridy = 0;
    gbc.fill = GridBagConstraints.HORIZONTAL;
    add(trueColorTitle, gbc);

    gbc.gridx++;
    trueColor = new ColorChooserButton(window, AppPreferences.TRUE_COLOR);
    add(trueColor, gbc);
    gbc.gridx++;
    add(trueCharTitle, gbc);
    gbc.gridx++;
    add(trueChar, gbc);

    gbc.gridx = 0;
    gbc.gridy++;
    add(falseColorTitle, gbc);
    gbc.gridx++;
    falseColor = new ColorChooserButton(window, AppPreferences.FALSE_COLOR);
    add(falseColor, gbc);
    gbc.gridx++;
    add(falseCharTitle, gbc);
    gbc.gridx++;
    add(falseChar, gbc);

    gbc.gridx = 0;
    gbc.gridy++;
    add(unknownColorTitle, gbc);
    gbc.gridx++;
    unknownColor = new ColorChooserButton(window, AppPreferences.UNKNOWN_COLOR);
    add(unknownColor, gbc);
    gbc.gridx++;
    add(unknownCharTitle, gbc);
    gbc.gridx++;
    add(unknownChar, gbc);

    gbc.gridx = 0;
    gbc.gridy++;
    add(errorColorTitle, gbc);
    gbc.gridx++;
    errorColor = new ColorChooserButton(window, AppPreferences.ERROR_COLOR);
    add(errorColor, gbc);
    gbc.gridx++;
    add(errorCharTitle, gbc);
    gbc.gridx++;
    add(errorChar, gbc);

    gbc.gridx = 0;
    gbc.gridy++;
    add(nilColorTitle, gbc);
    gbc.gridx++;
    nilColor = new ColorChooserButton(window, AppPreferences.NIL_COLOR);
    add(nilColor, gbc);
    gbc.gridx++;
    add(dontCareCharTitle, gbc);
    gbc.gridx++;
    add(dontCareChar, gbc);

    gbc.gridx = 0;
    gbc.gridy++;
    add(busColorTitle, gbc);
    gbc.gridx++;
    busColor = new ColorChooserButton(window, AppPreferences.BUS_COLOR);
    add(busColor, gbc);
    gbc.gridx++;
    add(highlightColorTitle, gbc);
    gbc.gridx++;
    highlightColor = new ColorChooserButton(window, AppPreferences.STROKE_COLOR);
    add(highlightColor, gbc);

    gbc.gridx = 0;
    gbc.gridy++;
    add(widthErrorColorTitle, gbc);
    gbc.gridx++;
    widthErrorColor = new ColorChooserButton(window, AppPreferences.WIDTH_ERROR_COLOR);
    add(widthErrorColor, gbc);
    gbc.gridx++;
    add(widthErrorCaptionColorTitle, gbc);
    gbc.gridx++;
    widthErrorCaptionColor =
        new ColorChooserButton(window, AppPreferences.WIDTH_ERROR_CAPTION_COLOR);
    add(widthErrorCaptionColor, gbc);

    gbc.gridx = 0;
    gbc.gridy++;
    add(widthErrorHighlightColorTitle, gbc);
    gbc.gridx++;
    widthErrorHighlightColor =
        new ColorChooserButton(window, AppPreferences.WIDTH_ERROR_HIGHLIGHT_COLOR);
    add(widthErrorHighlightColor, gbc);
    gbc.gridx++;
    add(widthErrorBackgroundColorTitle, gbc);
    gbc.gridx++;
    widthErrorBackgroundColor =
        new ColorChooserButton(window, AppPreferences.WIDTH_ERROR_BACKGROUND_COLOR);
    add(widthErrorBackgroundColor, gbc);

    gbc.gridx = 0;
    gbc.gridy++;
    add(clockFrequencyColorTitle, gbc);
    gbc.gridx++;
    clockFrequencyColor = new ColorChooserButton(window, AppPreferences.CLOCK_FREQUENCY_COLOR);
    add(clockFrequencyColor, gbc);

    gbc.gridx = 0;
    gbc.gridy++;
    gbc.gridwidth = 4;
    add(kmapColorsTitle, gbc);

    // FIXME: refactor this code
    gbc.gridy++;
    gbc.gridwidth = 1;
    add(kmap1ColorTitle, gbc);
    gbc.gridx++;
    kmap1Color = new ColorChooserButton(window, AppPreferences.KMAP1_COLOR);
    add(kmap1Color, gbc);
    gbc.gridx++;
    add(kmap2ColorTitle, gbc);
    gbc.gridx++;
    kmap2Color = new ColorChooserButton(window, AppPreferences.KMAP2_COLOR);
    add(kmap2Color, gbc);

    gbc.gridx = 0;
    gbc.gridy++;
    add(kmap3ColorTitle, gbc);
    gbc.gridx++;
    kmap3Color = new ColorChooserButton(window, AppPreferences.KMAP3_COLOR);
    add(kmap3Color, gbc);
    gbc.gridx++;
    add(kmap4ColorTitle, gbc);
    gbc.gridx++;
    kmap4Color = new ColorChooserButton(window, AppPreferences.KMAP4_COLOR);
    add(kmap4Color, gbc);

    gbc.gridx = 0;
    gbc.gridy++;
    add(kmap5ColorTitle, gbc);
    gbc.gridx++;
    kmap5Color = new ColorChooserButton(window, AppPreferences.KMAP5_COLOR);
    add(kmap5Color, gbc);
    gbc.gridx++;
    add(kmap6ColorTitle, gbc);
    gbc.gridx++;
    kmap6Color = new ColorChooserButton(window, AppPreferences.KMAP6_COLOR);
    add(kmap6Color, gbc);

    gbc.gridx = 0;
    gbc.gridy++;
    add(kmap7ColorTitle, gbc);
    gbc.gridx++;
    kmap7Color = new ColorChooserButton(window, AppPreferences.KMAP7_COLOR);
    add(kmap7Color, gbc);
    gbc.gridx++;
    add(kmap8ColorTitle, gbc);
    gbc.gridx++;
    kmap8Color = new ColorChooserButton(window, AppPreferences.KMAP8_COLOR);
    add(kmap8Color, gbc);

    gbc.gridx = 0;
    gbc.gridy++;
    add(kmap9ColorTitle, gbc);
    gbc.gridx++;
    kmap9Color = new ColorChooserButton(window, AppPreferences.KMAP9_COLOR);
    add(kmap9Color, gbc);
    gbc.gridx++;
    add(kmap10ColorTitle, gbc);
    gbc.gridx++;
    kmap10Color = new ColorChooserButton(window, AppPreferences.KMAP10_COLOR);
    add(kmap10Color, gbc);

    gbc.gridx = 0;
    gbc.gridy++;
    add(kmap11ColorTitle, gbc);
    gbc.gridx++;
    kmap11Color = new ColorChooserButton(window, AppPreferences.KMAP11_COLOR);
    add(kmap11Color, gbc);
    gbc.gridx++;
    add(kmap12ColorTitle, gbc);
    gbc.gridx++;
    kmap12Color = new ColorChooserButton(window, AppPreferences.KMAP12_COLOR);
    add(kmap12Color, gbc);

    gbc.gridx = 0;
    gbc.gridy++;
    add(kmap13ColorTitle, gbc);
    gbc.gridx++;
    kmap13Color = new ColorChooserButton(window, AppPreferences.KMAP13_COLOR);
    add(kmap13Color, gbc);
    gbc.gridx++;
    add(kmap14ColorTitle, gbc);
    gbc.gridx++;
    kmap14Color = new ColorChooserButton(window, AppPreferences.KMAP14_COLOR);
    add(kmap14Color, gbc);

    gbc.gridx = 0;
    gbc.gridy++;
    add(kmap15ColorTitle, gbc);
    gbc.gridx++;
    kmap15Color = new ColorChooserButton(window, AppPreferences.KMAP15_COLOR);
    add(kmap15Color, gbc);
    gbc.gridx++;
    add(kmap16ColorTitle, gbc);
    gbc.gridx++;
    kmap16Color = new ColorChooserButton(window, AppPreferences.KMAP16_COLOR);
    add(kmap16Color, gbc);

    gbc.gridx = 0;
    gbc.gridy++;
    gbc.gridwidth = 2;
    defaultButton.setActionCommand("default");
    defaultButton.addActionListener(mcol);
    add(defaultButton, gbc);
    gbc.gridx += 2;
    colorBlindButton.setActionCommand("colorblind");
    colorBlindButton.addActionListener(mcol);
    add(colorBlindButton, gbc);

    localeChanged();
  }

  @Override
  public String getHelpText() {
    return S.get("simHelp");
  }

  @Override
  public String getTitle() {
    return S.get("simTitle");
  }

  @Override
  public void localeChanged() {
    trueColorTitle.setText(S.get("simTrueColTitle"));
    trueCharTitle.setText(S.get("simTrueCharTitle"));
    falseColorTitle.setText(S.get("simFalseColTitle"));
    falseCharTitle.setText(S.get("simFalseCharTitle"));
    unknownColorTitle.setText(S.get("simUnknownColTitle"));
    unknownCharTitle.setText(S.get("simUnknownCharTitle"));
    errorColorTitle.setText(S.get("simErrorColTitle"));
    errorCharTitle.setText(S.get("simErrorCharTitle"));
    nilColorTitle.setText(S.get("simNilColTitle"));
    dontCareCharTitle.setText(S.get("simDontCareCharTitle"));
    busColorTitle.setText(S.get("simBusColTitle"));
    highlightColorTitle.setText(S.get("simStrokeColTitle"));
    widthErrorColorTitle.setText(S.get("simWidthErrorTitle"));
    widthErrorCaptionColorTitle.setText(S.get("simWidthErrorCaptionTitle"));
    widthErrorHighlightColorTitle.setText(S.get("simWidthErrorHighlightTitle"));
    widthErrorBackgroundColorTitle.setText(S.get("simWidthErrorBackgroundTitle"));
    clockFrequencyColorTitle.setText(S.get("simClockFrequencyTitle"));
    defaultButton.setText(S.get("simDefaultColors"));
    colorBlindButton.setText(S.get("simColorBlindColors"));
    kmap1ColorTitle.setText(S.get("simKmapColors", 1));
    kmap2ColorTitle.setText(S.get("simKmapColors", 2));
    kmap3ColorTitle.setText(S.get("simKmapColors", 3));
    kmap4ColorTitle.setText(S.get("simKmapColors", 4));
    kmap5ColorTitle.setText(S.get("simKmapColors", 5));
    kmap6ColorTitle.setText(S.get("simKmapColors", 6));
    kmap7ColorTitle.setText(S.get("simKmapColors", 7));
    kmap8ColorTitle.setText(S.get("simKmapColors", 8));
    kmap9ColorTitle.setText(S.get("simKmapColors", 9));
    kmap10ColorTitle.setText(S.get("simKmapColors", 10));
    kmap11ColorTitle.setText(S.get("simKmapColors", 11));
    kmap12ColorTitle.setText(S.get("simKmapColors", 12));
    kmap13ColorTitle.setText(S.get("simKmapColors", 13));
    kmap14ColorTitle.setText(S.get("simKmapColors", 14));
    kmap15ColorTitle.setText(S.get("simKmapColors", 15));
    kmap16ColorTitle.setText(S.get("simKmapColors", 16));
    kmapColorsTitle.setText(S.get("simKmapColorsTitle"));
  }

  private void setDefaults() {
    AppPreferences.TRUE_COLOR.set(0x0000D300);
    AppPreferences.FALSE_COLOR.set(0x00006500);
    AppPreferences.UNKNOWN_COLOR.set(0x002827FF);
    AppPreferences.ERROR_COLOR.set(0x00C10000);
    AppPreferences.NIL_COLOR.set(0x818181);
    AppPreferences.BUS_COLOR.set(1);
    AppPreferences.STROKE_COLOR.set(0xFE00FF);
    AppPreferences.WIDTH_ERROR_COLOR.set(0xFF7A00);
    AppPreferences.WIDTH_ERROR_CAPTION_COLOR.set(0x560000);
    AppPreferences.WIDTH_ERROR_HIGHLIGHT_COLOR.set(0xFFFE00);
    AppPreferences.WIDTH_ERROR_BACKGROUND_COLOR.set(0xFFE6D2);
    AppPreferences.CLOCK_FREQUENCY_COLOR.set(0xFF00B4);
    AppPreferences.KMAP1_COLOR.set(0x810000);
    AppPreferences.KMAP2_COLOR.set(0xE7194B);
    AppPreferences.KMAP3_COLOR.set(0xFABEBF);
    AppPreferences.KMAP4_COLOR.set(0xAA6E29);
    AppPreferences.KMAP5_COLOR.set(0xF58231);
    AppPreferences.KMAP6_COLOR.set(0xFFD7B5);
    AppPreferences.KMAP7_COLOR.set(0x818000);
    AppPreferences.KMAP8_COLOR.set(0xFFFF1A);
    AppPreferences.KMAP9_COLOR.set(0xD2F53D);
    AppPreferences.KMAP10_COLOR.set(0x000081);
    AppPreferences.KMAP11_COLOR.set(0x911EB5);
    AppPreferences.KMAP12_COLOR.set(0x3CB5AF);
    AppPreferences.KMAP13_COLOR.set(0x0082CC);
    AppPreferences.KMAP14_COLOR.set(0xE7BEFF);
    AppPreferences.KMAP15_COLOR.set(0xAAFFC4);
    AppPreferences.KMAP16_COLOR.set(0xF032E7);
    repaint();
  }

  private void setColorBlind() {
    AppPreferences.TRUE_COLOR.set(0xF4EB42);
    AppPreferences.FALSE_COLOR.set(0x203BE8);
    AppPreferences.UNKNOWN_COLOR.set(0x01BC9D);
    AppPreferences.ERROR_COLOR.set(0x00C10000);
    AppPreferences.NIL_COLOR.set(0x818181);
    AppPreferences.BUS_COLOR.set(1);
    AppPreferences.STROKE_COLOR.set(0xBBBBBB);
    AppPreferences.WIDTH_ERROR_COLOR.set(0xC413DB);
    AppPreferences.WIDTH_ERROR_CAPTION_COLOR.set(0x560000);
    AppPreferences.WIDTH_ERROR_HIGHLIGHT_COLOR.set(0xFFFE00);
    AppPreferences.WIDTH_ERROR_BACKGROUND_COLOR.set(0xFFE6D2);
    AppPreferences.CLOCK_FREQUENCY_COLOR.set(0xFF00B4); // FIXME: Calculate proper color!
    AppPreferences.KMAP1_COLOR.set(0x490092);
    AppPreferences.KMAP2_COLOR.set(0x920000);
    AppPreferences.KMAP3_COLOR.set(0x004949);
    AppPreferences.KMAP4_COLOR.set(0x006DDB);
    AppPreferences.KMAP5_COLOR.set(0x924900);
    AppPreferences.KMAP6_COLOR.set(0x009292);
    AppPreferences.KMAP7_COLOR.set(0xB66DFF);
    AppPreferences.KMAP8_COLOR.set(0xDBD100);
    AppPreferences.KMAP9_COLOR.set(0xFF6DB6);
    AppPreferences.KMAP10_COLOR.set(0x6DB6FF);
    AppPreferences.KMAP11_COLOR.set(0x24FF24);
    AppPreferences.KMAP12_COLOR.set(0xFFB677);
    AppPreferences.KMAP13_COLOR.set(0xB6DBFF);
    AppPreferences.KMAP14_COLOR.set(0xFFFF6D);
    AppPreferences.KMAP15_COLOR.set(0x009292);
    AppPreferences.KMAP16_COLOR.set(0xFFB677);
    repaint();
  }

  private static class MyListener implements PreferenceChangeListener {

    @Override
    public void preferenceChange(PreferenceChangeEvent evt) {
      var update = false;
      final var key = evt.getKey();
      if (key.equals(AppPreferences.TRUE_COLOR.getIdentifier())) {
        Value.trueColor = new Color(AppPreferences.TRUE_COLOR.get());
        update = true;
      } else if (key.equals(AppPreferences.TRUE_CHAR.getIdentifier())) {
        Value.TRUECHAR = AppPreferences.TRUE_CHAR.get().charAt(0);
        update = true;
      } else if (key.equals(AppPreferences.FALSE_COLOR.getIdentifier())) {
        Value.falseColor = new Color(AppPreferences.FALSE_COLOR.get());
        update = true;
      } else if (key.equals(AppPreferences.FALSE_CHAR.getIdentifier())) {
        Value.FALSECHAR = AppPreferences.FALSE_CHAR.get().charAt(0);
        update = true;
      } else if (key.equals(AppPreferences.UNKNOWN_COLOR.getIdentifier())) {
        Value.unknownColor = new Color(AppPreferences.UNKNOWN_COLOR.get());
        update = true;
      } else if (key.equals(AppPreferences.UNKNOWN_CHAR.getIdentifier())) {
        Value.UNKNOWNCHAR = AppPreferences.UNKNOWN_CHAR.get().charAt(0);
        update = true;
      } else if (key.equals(AppPreferences.ERROR_COLOR.getIdentifier())) {
        Value.errorColor = new Color(AppPreferences.ERROR_COLOR.get());
        update = true;
      } else if (key.equals(AppPreferences.ERROR_CHAR.getIdentifier())) {
        Value.ERRORCHAR = AppPreferences.ERROR_CHAR.get().charAt(0);
        update = true;
      } else if (key.equals(AppPreferences.NIL_COLOR.getIdentifier())) {
        Value.nilColor = new Color(AppPreferences.NIL_COLOR.get());
        update = true;
      } else if (key.equals(AppPreferences.DONTCARE_CHAR.getIdentifier())) {
        Value.DONTCARECHAR = AppPreferences.DONTCARE_CHAR.get().charAt(0);
        update = true;
      } else if (key.equals(AppPreferences.BUS_COLOR.getIdentifier())) {
        Value.multiColor = new Color(AppPreferences.BUS_COLOR.get());
        update = true;
      } else if (key.equals(AppPreferences.STROKE_COLOR.getIdentifier())) {
        Value.strokeColor = new Color(AppPreferences.STROKE_COLOR.get());
        update = true;
      } else if (key.equals(AppPreferences.WIDTH_ERROR_COLOR.getIdentifier())) {
        Value.widthErrorColor = new Color(AppPreferences.WIDTH_ERROR_COLOR.get());
        update = true;
      } else if (key.equals(AppPreferences.WIDTH_ERROR_CAPTION_COLOR.getIdentifier())) {
        Value.widthErrorCaptionColor = new Color(AppPreferences.WIDTH_ERROR_CAPTION_COLOR.get());
        update = true;
      } else if (key.equals(AppPreferences.WIDTH_ERROR_HIGHLIGHT_COLOR.getIdentifier())) {
        Value.widthErrorHighlightColor =
            new Color(AppPreferences.WIDTH_ERROR_HIGHLIGHT_COLOR.get());
        update = true;
      } else if (key.equals(AppPreferences.WIDTH_ERROR_BACKGROUND_COLOR.getIdentifier())) {
        Value.widthErrorCaptionBgcolor =
            new Color(AppPreferences.WIDTH_ERROR_BACKGROUND_COLOR.get());
        update = true;
      } else if (key.equals(AppPreferences.CLOCK_FREQUENCY_COLOR.getIdentifier())) {
        Value.clockFrequencyColor = new Color(AppPreferences.CLOCK_FREQUENCY_COLOR.get());
        update = true;
      }
      if (update) {
        for (final var proj : Projects.getOpenProjects()) proj.getFrame().repaint();
      }
    }
  }

  private class MyColorListener implements ActionListener {

    @Override
    public void actionPerformed(ActionEvent e) {
      if (e.getActionCommand().equals("default")) {
        setDefaults();
      } else if (e.getActionCommand().equals("colorblind")) {
        setColorBlind();
      }
    }
  }

  private static class SymbolChooser extends JComboBox<Character> {
    private static final long serialVersionUID = 1L;
    private final PrefMonitor<String> myPref;

    public SymbolChooser(PrefMonitor<String> pref, String choices) {
      super();
      myPref = pref;
      this.addActionListener(new MyactionListener());
      final Character def = pref.get().charAt(0);
      var seldef = -1;
      for (var i = 0; i < choices.length(); i++) {
        final Character sel = choices.charAt(i);
        if (sel.equals(def)) seldef = i;
        this.addItem(sel);
      }
      if (seldef >= 0) this.setSelectedIndex(seldef);
    }

    private class MyactionListener implements ActionListener {
      @Override
      public void actionPerformed(ActionEvent e) {
        @SuppressWarnings("unchecked")
        final var me = (JComboBox<Character>) e.getSource();
        final Character s = (Character) me.getSelectedItem();
        if (s != myPref.get().charAt(0)) {
          myPref.set(Character.toString(s));
        }
      }
    }
  }
}
