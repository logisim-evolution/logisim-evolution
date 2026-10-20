/*
 * Logisim-evolution - digital logic design tool and simulator
 * Copyright by the Logisim-evolution developers
 *
 * https://github.com/logisim-evolution/
 *
 * This is free software released under GNU GPLv3 license
 */

package com.cburch.logisim.gui.search;

import com.cburch.logisim.proj.Project;
import java.awt.Window;
import javax.swing.JMenuBar;

/**
 * What the search dialog was opened from, handed to every provider so it can decide
 * what is worth offering.
 * <p>
 * The context matters because Logisim-evolution puts a different menu bar on each
 * kind of window- the main window, Analyze, Chronogram and the FPGA windows all
 * differ - and a provider should index the window the user is actually looking at
 * rather than some global notion of the application.
 *
 * @param owner   window the dialog belongs to; results act on this window
 * @param menuBar the owner's menu bar, or {@code null} when it has none
 * @param project project the owner belongs to, or {@code null} for standalone
 *                windows
 */
public record SearchContext(Window owner, JMenuBar menuBar, Project project) {
}
