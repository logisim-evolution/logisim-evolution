package com.cburch.draw.util;

import static com.cburch.logisim.std.Strings.S;

import com.cburch.logisim.data.Attribute;
import com.cburch.logisim.proj.Strings;
import com.cburch.logisim.util.JFileChoosers;
import com.cburch.logisim.util.JInputDialog;
import java.awt.Component;
import java.awt.Window;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.Base64;
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.filechooser.FileFilter;

/**
 * Image shape attribute.
 */
public class ImageAttribute extends Attribute<String> {

  public ImageAttribute() {
    super("", S.getter("attrImageData"));
  }

  @Override
  public String parse(String value) {
    return value;
  }

  @Override
  public Component getCellEditor(Window source, String value) {
    return new ImageCell(source);
  }

  @Override
  public String toDisplayString(String value) {
    return S.get("romContentsValue");
  }

  private static class ImageCell extends JLabel implements JInputDialog {
    final Window source;
    String imageData;

    ImageCell(Window source) {
      super(S.get("romContentsValue"));
      this.source = source;
    }

    @Override
    public void setVisible(boolean visible) {
      JFileChooser chooser = JFileChoosers.create();
      chooser.setFileFilter(new FileFilter() {
        @Override
        public boolean accept(File file) {
          var fileName = file.getName();
          return file.isDirectory()
                  || fileName.endsWith(".jpg")
                  || fileName.endsWith(".jpeg")
                  || fileName.endsWith(".png");
        }

        @Override
        public String getDescription() {
          return com.cburch.logisim.file.Strings.S.get("imageFileFilter");
        }
      });
      chooser.setDialogTitle(Strings.S.get("FileOpenImage"));

      final var returnVal = chooser.showOpenDialog(null);
      if (returnVal != JFileChooser.APPROVE_OPTION) {
        return;
      }
      final var selected = chooser.getSelectedFile();
      if (selected == null) {
        return;
      }
      try (var fr = new FileInputStream(selected)) {
        imageData = Base64.getEncoder().encodeToString(fr.readAllBytes());
      } catch (IOException ex) {
        System.err.println("Failed to load image file '" + selected.getAbsolutePath() + "'");
      }
    }

    @Override
    public Object getValue() {
      return imageData;
    }

    @Override
    public void setValue(Object value) {
      imageData = ((String) value);
    }
  }
}
