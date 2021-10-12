/*
 * Logisim-evolution - digital logic design tool and simulator
 * Copyright by the Logisim-evolution developers
 *
 * https://github.com/logisim-evolution/
 *
 * This is free software released under GNU GPLv3 license
 */

package com.cburch.logisim.std.tcl;

import static com.cburch.logisim.std.Strings.S;

import com.cburch.logisim.data.AbstractAttributeSet;
import com.cburch.logisim.data.Attribute;
import com.cburch.logisim.file.Loader;
import com.cburch.logisim.instance.StdAttr;
import com.cburch.logisim.util.JInputComponent;
import java.awt.Font;
import java.awt.Window;
import java.io.File;
import java.util.Arrays;
import java.util.List;
import javax.swing.JFileChooser;

public class TclComponentAttributes extends AbstractAttributeSet {

  private static class ContentFileAttribute extends Attribute<File> {

    ContentFileCell chooser;

    public ContentFileAttribute() {
      super("filePath", S.getter("tclConsoleContentFile"));
    }

    @Override
    public java.awt.Component getCellEditor(Window source, File file) {
      if (chooser == null) chooser = new ContentFileCell(file);
      chooser.setFileFilter(Loader.TCL_FILTER);
      chooser.setFileSelectionMode(JFileChooser.FILES_ONLY);
      return chooser;
    }

    @Override
    public File parse(String path) {
      return new File(path);
    }

    @Override
    public String toDisplayString(File file) {
      if (file.isDirectory()) return "...";
      else return file.getName();
    }

    @Override
    public String toStandardString(File file) {
      return file.getPath();
    }
  }

  private static class ContentFileCell extends JFileChooser implements JInputComponent {
    private static final long serialVersionUID = 1L;

    ContentFileCell(File initial) {
      super(initial);
    }

    @Override
    public Object getValue() {
      return getSelectedFile();
    }

    @Override
    public void setValue(Object value) {
      setSelectedFile((File) value);
    }
  }

  public static final Attribute<File> CONTENT_FILE_ATTR = new ContentFileAttribute();

  private static final List<Attribute<?>> attributes =
      Arrays.asList(new Attribute<?>[] {CONTENT_FILE_ATTR, StdAttr.LABEL, StdAttr.LABEL_FONT});

  private File contentFile;
  private String label = "";
  private Font labelFont = StdAttr.DEFAULT_LABEL_FONT;

  TclComponentAttributes() {
    contentFile = new File(System.getProperty("user.home"));
  }

  @Override
  protected void copyInto(AbstractAttributeSet dest) {
    final var attr = (TclComponentAttributes) dest;
    attr.labelFont = labelFont;
    attr.contentFile = new File(contentFile.getAbsolutePath());
  }

  @Override
  public List<Attribute<?>> getAttributes() {
    return attributes;
  }

  @SuppressWarnings("unchecked")
  @Override
  public <V> V getValue(Attribute<V> attr) {
    if (attr == CONTENT_FILE_ATTR) {
      return (V) contentFile;
    }
    if (attr == StdAttr.LABEL) {
      return (V) label;
    }
    if (attr == StdAttr.LABEL_FONT) {
      return (V) labelFont;
    }
    return null;
  }

  @Override
  public <V> void setValue(Attribute<V> attr, V value) {
    if (attr == CONTENT_FILE_ATTR) {
      final var newFile = (File) value;
      if (!contentFile.equals(newFile)) contentFile = newFile;
      fireAttributeValueChanged(attr, value, null);
    }
    if (attr == StdAttr.LABEL) {
      final var newLabel = (String) value;
      if (label.equals(newLabel)) return;
      @SuppressWarnings("unchecked")
      final V oldLabel = (V) label;
      label = newLabel;
      fireAttributeValueChanged(attr, value, oldLabel);
    }
    if (attr == StdAttr.LABEL_FONT) {
      final var newFont = (Font) value;
      if (labelFont.equals(newFont)) return;
      labelFont = newFont;
      fireAttributeValueChanged(attr, value, null);
    }
  }
}
