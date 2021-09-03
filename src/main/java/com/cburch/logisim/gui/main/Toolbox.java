/*
 * Logisim-evolution - digital logic design tool and simulator
 * Copyright by the Logisim-evolution developers
 *
 * https://github.com/logisim-evolution/
 *
 * This is free software released under GNU GPLv3 license
 */

package com.cburch.logisim.gui.main;

import com.cburch.draw.toolbar.Toolbar;
import com.cburch.logisim.gui.generic.ProjectExplorer;
import com.cburch.logisim.gui.menu.MenuListener;
import com.cburch.logisim.proj.Project;
import com.cburch.logisim.tools.Tool;
import java.awt.BorderLayout;
import javax.swing.JPanel;
import javax.swing.JScrollPane;

class Toolbox extends JPanel {
  private static final long serialVersionUID = 1L;
  private final ProjectExplorer toolbox;

  Toolbox(Project proj, Frame frame, MenuListener menu) {
    super(new BorderLayout());

    ToolboxToolbarModel toolbarModel = new ToolboxToolbarModel(frame, menu);
    Toolbar toolbar = new Toolbar(toolbarModel);
    add(toolbar, BorderLayout.NORTH);

    toolbox = new ProjectExplorer(proj, false);
    toolbox.setListener(new ToolboxManip(proj, toolbox));
    add(new JScrollPane(toolbox), BorderLayout.CENTER);

    toolbarModel.menuEnableChanged(menu);
  }

  void setHaloedTool(Tool value) {
    toolbox.setHaloedTool(value);
  }

  public void updateStructure() {
    toolbox.updateStructure();
  }
}
