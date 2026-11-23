/*
 * Logisim-evolution - digital logic design tool and simulator
 * Copyright by the Logisim-evolution developers
 *
 * https://github.com/logisim-evolution/
 *
 * This is free software released under GNU GPLv3 license
 */

package com.cburch.logisim.gui.generic;

import static com.cburch.logisim.std.Strings.S;

import com.bric.colorpicker.ColorPicker;
import com.cburch.logisim.data.Attribute;
import com.cburch.logisim.data.Attributes;
import com.cburch.logisim.instance.StdAttr;
import com.cburch.logisim.prefs.AppPreferences;
import com.cburch.logisim.util.ColorUtil;
import com.cburch.logisim.util.JInputComponent;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import javax.swing.BorderFactory;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingConstants;

public class TextColorChooser extends JPanel implements JInputComponent {
  private static final long serialVersionUID = 1L;
  private static final int PREVIEW_WIDTH = 360;
  private static final int PREVIEW_HEIGHT = 48;
  private static final int PREVIEW_PADDING = 8;
  private static final Attribute<Color> COLOR_FORMAT = Attributes.forColor("color");

  private final ColorPicker picker = new ColorPicker();
  private final JLabel preview = new JLabel("AaBb 0123", SwingConstants.CENTER);
  private final JLabel previewValue = new JLabel();
  private final boolean darkTheme = AppPreferences.isDarkTheme(AppPreferences.LookAndFeel.get());
  private Color originalColor;
  private Color initialSelection;

  public TextColorChooser(Color storedColor, boolean showOpacity) {
    super(new BorderLayout());
    picker.setOpacityVisible(showOpacity);
    add(picker, BorderLayout.CENTER);

    final var previewPanel = new JPanel(new BorderLayout());
    previewPanel.setPreferredSize(
        new Dimension(
            AppPreferences.getScaled(PREVIEW_WIDTH), AppPreferences.getScaled(PREVIEW_HEIGHT)));
    final var padding = AppPreferences.getScaled(PREVIEW_PADDING);
    previewPanel.setBorder(BorderFactory.createEmptyBorder(padding, padding, padding, padding));
    preview.setOpaque(true);
    preview.setBackground(new Color(AppPreferences.CANVAS_BG_COLOR.get()));
    preview.setFont(StdAttr.DEFAULT_LABEL_FONT);
    previewPanel.add(preview, BorderLayout.CENTER);
    previewPanel.add(previewValue, BorderLayout.EAST);
    add(previewPanel, BorderLayout.SOUTH);

    // ColorPicker's initial mode setup suppresses its next slider update.
    picker.setColor(picker.getColor());
    setValue(storedColor);
    picker.addColorListener(color -> updatePreview());
  }

  public static Color showDialog(Component parent, Color storedColor, boolean showOpacity) {
    final var chooser = new TextColorChooser(storedColor, showOpacity);
    final var result =
        OptionPane.showConfirmDialog(
            parent, chooser, S.get("textColorAttr"), OptionPane.OK_CANCEL_OPTION, OptionPane.PLAIN_MESSAGE);
    return result == OptionPane.OK_OPTION ? chooser.getValue() : null;
  }

  @Override
  public Color getValue() {
    // AttrTable snapshots this value when opening the dialog, so null also makes Cancel a no-op.
    if (picker.getColor().equals(initialSelection)) return null;
    final var stored = getStoredColor();
    return stored.equals(originalColor) ? null : stored;
  }

  @Override
  public void setValue(Object value) {
    originalColor = (Color) value;
    final var initialColor = originalColor == null ? Color.WHITE : originalColor;
    picker.setColor(displayColor(initialColor));
    initialSelection = picker.getColor();
    updatePreview();
  }

  private Color getStoredColor() {
    final var selected = picker.getColor();
    if (originalColor != null
        && selected.getRed() == initialSelection.getRed()
        && selected.getGreen() == initialSelection.getGreen()
        && selected.getBlue() == initialSelection.getBlue()) {
      // Preserve clipped source RGB when only opacity changes.
      return new Color(
          originalColor.getRed(),
          originalColor.getGreen(),
          originalColor.getBlue(),
          selected.getAlpha());
    }
    // Luma inversion gives a light-color candidate, not an exact inverse after clipping.
    return darkTheme ? ColorUtil.getLuminanceInvertedColor(selected) : selected;
  }

  private Color displayColor(Color stored) {
    return darkTheme ? ColorUtil.getLuminanceInvertedColor(stored) : stored;
  }

  private void updatePreview() {
    final var displayed = displayColor(getStoredColor());
    preview.setForeground(displayed);
    previewValue.setText("  " + COLOR_FORMAT.toStandardString(displayed));
  }
}
