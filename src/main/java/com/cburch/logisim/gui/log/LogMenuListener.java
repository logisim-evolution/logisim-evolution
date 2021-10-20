/*
 * Logisim-evolution - digital logic design tool and simulator
 * Copyright by the Logisim-evolution developers
 *
 * https://github.com/logisim-evolution/
 *
 * This is free software released under GNU GPLv3 license
 */

package com.cburch.logisim.gui.log;

import com.cburch.logisim.gui.menu.LogisimMenuBar;
import com.cburch.logisim.gui.menu.MenuListener;
import com.cburch.logisim.gui.menu.PrintHandler;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

public class LogMenuListener extends MenuListener {

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
        menuBar.addActionListener(LogisimMenuBar.EXPORT_IMAGE, this);
        menuBar.addActionListener(LogisimMenuBar.PRINT, this);
      } else {
        menuBar.removeActionListener(LogisimMenuBar.EXPORT_IMAGE, this);
        menuBar.removeActionListener(LogisimMenuBar.PRINT, this);
      }
    }
  }

  private final FileListener fileListener = new FileListener();
  private PrintHandler printer;

  public LogMenuListener(LogisimMenuBar menubar) {
    super(menubar);
    fileListener.register(false);
    editListener.register();
  }

  public void setPrintHandler(PrintHandler printer) {
    this.printer = printer;
    fileListener.register(printer != null);
  }
}
