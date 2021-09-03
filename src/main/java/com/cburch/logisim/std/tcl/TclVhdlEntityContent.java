/*
 * Logisim-evolution - digital logic design tool and simulator
 * Copyright by the Logisim-evolution developers
 *
 * https://github.com/logisim-evolution/
 *
 * This is free software released under GNU GPLv3 license
 */

package com.cburch.logisim.std.tcl;

import com.cburch.logisim.std.hdl.VhdlContentComponent;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * This is the same as the parent, just the template has to change. Code duplication is due to
 * VhdlContent strange structure. Please optimize if you got the time, sorry for this debt.
 *
 * @author christian.mueller@heig-vd.ch
 */
public class TclVhdlEntityContent extends VhdlContentComponent {

  public static TclVhdlEntityContent create() {
    return new TclVhdlEntityContent();
  }

  // TODO: remove code duplication with parent class
  private static String loadTemplate() {
    final var input = VhdlContentComponent.class.getResourceAsStream(RESOURCE);
    final var in = new BufferedReader(new InputStreamReader(input));
    final var tmp = new StringBuilder();
    String line;

    try {
      while ((line = in.readLine()) != null) {
        tmp.append(line);
        tmp.append(System.getProperty("line.separator"));
      }
    } catch (IOException ex) {
      return "";
    } finally {
      try {
        if (input != null) input.close();
      } catch (IOException ex) {
        Logger.getLogger(VhdlContentComponent.class.getName()).log(Level.SEVERE, null, ex);
      }
    }

    return tmp.toString();
  }

  private static final String RESOURCE = "/resources/logisim/tcl/entity.templ";

  private static final String TEMPLATE = loadTemplate();

  protected TclVhdlEntityContent() {
    super();
    super.setContent(TEMPLATE);
  }
}
