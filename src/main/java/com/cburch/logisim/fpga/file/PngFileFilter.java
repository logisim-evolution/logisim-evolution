/*
 * Logisim-evolution - digital logic design tool and simulator
 * Copyright by the Logisim-evolution developers
 *
 * https://github.com/logisim-evolution/
 *
 * This is free software released under GNU GPLv3 license
 */

package com.cburch.logisim.fpga.file;

import static com.cburch.logisim.fpga.Strings.S;

import java.io.File;
import javax.swing.filechooser.FileFilter;

public class PngFileFilter extends FileFilter {

  public static final String PNG_EXTENSION = ".png";
  public static final PngFileFilter PNG_FILTER = new PngFileFilter();

  @Override
  public boolean accept(File f) {
    return f.isDirectory() || f.getName().endsWith(PNG_EXTENSION);
  }

  @Override
  public String getDescription() {
    return S.get("FpgaFilePng");
  }
}
