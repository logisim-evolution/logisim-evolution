/*
 * Logisim-evolution - digital logic design tool and simulator
 * Copyright by the Logisim-evolution developers
 *
 * https://github.com/logisim-evolution/
 *
 * This is free software released under GNU GPLv3 license
 */

package com.cburch.logisim.std.base;

import static com.cburch.logisim.std.Strings.S;

import com.cburch.logisim.data.Attribute;
import com.cburch.logisim.util.ImageUtil;
import com.cburch.logisim.util.JFileChoosers;
import com.cburch.logisim.util.JInputDialog;
import java.awt.Component;
import java.awt.Window;
import java.io.File;
import javax.swing.JFileChooser;
import javax.swing.filechooser.FileFilter;

public class ImageSourceAttribute extends Attribute<String> {

  public ImageSourceAttribute(String name) {
    super(name, S.getter("imageSourceAttr"));
  }

  @Override
  public Component getCellEditor(Window source, String value) {
    return new ImageFileChooserDialog(source, value);
  }

  @Override
  public String parse(String value) {
    return value == null ? "" : value;
  }

  @Override
  public String toDisplayString(String value) {
    if (value == null || value.isBlank()) {
      return S.get("imageSourceNone");
    }
    if (value.startsWith("data:image/")) {
      final var semicolon = value.indexOf(';');
      final var type = semicolon > 11 ? value.substring(11, semicolon) : "image";
      return "[" + type.toUpperCase() + " Data]";
    }
    return new File(value).getName();
  }

  @Override
  public String toStandardString(String value) {
    return value == null ? "" : value.replaceAll("[\\r\\n]+", "");
  }

  private static class ImageFileChooserDialog extends JFileChooser implements JInputDialog {
    private final Window parent;
    private final String currentValue;
    private String resultValue;

    public ImageFileChooserDialog(Window parent, String currentValue) {
      this.parent = parent;
      this.currentValue = currentValue;
      this.resultValue = currentValue;
    }

    @Override
    public void setVisible(boolean b) {
      if (!b) return;
      final var chooser = JFileChoosers.create();
      chooser.setDialogTitle(S.get("imageChooseDialogTitle"));
      chooser.setFileFilter(new FileFilter() {
        @Override
        public boolean accept(File f) {
          if (f.isDirectory()) return true;
          final var name = f.getName().toLowerCase();
          return name.endsWith(".png")
              || name.endsWith(".jpg")
              || name.endsWith(".jpeg")
              || name.endsWith(".gif");
          // SVG and WebP intentionally excluded — Java standard ImageIO cannot render SVG or WebP without extra plugins.
        }

        @Override
        public String getDescription() {
          return S.get("imageFileFilterDescription");
        }
      });

      final var returnVal = chooser.showOpenDialog(parent);
      if (returnVal == JFileChooser.APPROVE_OPTION) {
        final var selected = chooser.getSelectedFile();
        try {
          resultValue = ImageUtil.fileToBase64(selected);
        } catch (Exception e) {
          resultValue = selected.getAbsolutePath();
        }
      } else {
        resultValue = currentValue;
      }
    }

    @Override
    public Object getValue() {
      return resultValue;
    }

    @Override
    public void setValue(Object value) {
      this.resultValue = (String) value;
    }
  }
}
