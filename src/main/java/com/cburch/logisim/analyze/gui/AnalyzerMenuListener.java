/*
 * Logisim-evolution - digital logic design tool and simulator
 * Copyright by the Logisim-evolution developers
 *
 * https://github.com/logisim-evolution/
 *
 * This is free software released under GNU GPLv3 license
 */

package com.cburch.logisim.analyze.gui;

import com.cburch.logisim.gui.menu.LogisimMenuBar;
import com.cburch.logisim.gui.menu.MenuListener;
import com.cburch.logisim.gui.menu.PrintHandler;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

public class AnalyzerMenuListener extends MenuListener {

  protected class FileListener implements ActionListener {
    @Override
    public void actionPerformed(ActionEvent event) {
      if (printer != null) printer.actionPerformed(event);
    }

    boolean registered;

    public void register(boolean en) {
      if (registered == en) return;
      registered = en;
      if (en) {
        menubar.addActionListener(LogisimMenuBar.EXPORT_IMAGE, this);
        menubar.addActionListener(LogisimMenuBar.PRINT, this);
      } else {
        menubar.removeActionListener(LogisimMenuBar.EXPORT_IMAGE, this);
        menubar.removeActionListener(LogisimMenuBar.PRINT, this);
      }
    }
  }

  private final FileListener fileListener = new FileListener();
  private PrintHandler printer;

  public AnalyzerMenuListener(LogisimMenuBar menubar) {
    super(menubar);
    fileListener.register(false);
    editListener.register();
  }

  public void setPrintHandler(PrintHandler printer) {
    this.printer = printer;
    fileListener.register(printer != null);
  }
}
