/*
 * This file is part of logisim-evolution.
 *
 * Logisim-evolution is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by the
 * Free Software Foundation, either version 3 of the License, or (at your
 * option) any later version.
 *
 * Logisim-evolution is distributed in the hope that it will be useful, but
 * WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 * or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
 * for more details.
 *
 * You should have received a copy of the GNU General Public License along
 * with logisim-evolution. If not, see <http://www.gnu.org/licenses/>.
 *
 * Original code by Marcin Orlowski (http://MarcinOrlowski.com), 2021.
 */

package com.cburch.logisim.gui.start;

import org.apache.batik.swing.JSVGCanvas;

public class AppLogo extends JSVGCanvas {
  private static final String LOGO_IMG = "resources/logisim/img/logisim-evolution-logo.svg";

  public AppLogo() {
    // disabling events and selectable texts
    super(null, false, false);

    final var logoUri = getClass().getClassLoader().getResource(LOGO_IMG).toString();
    if (logoUri != null) {
      setURI(logoUri);
    }

    setProgressivePaint(true);
    setRecenterOnResize(true);
  }
}
