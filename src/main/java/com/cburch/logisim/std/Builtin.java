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
 * Original code by Carl Burch (http://www.cburch.com), 2011.
 * Subsequent modifications by:
 *   + College of the Holy Cross
 *     http://www.holycross.edu
 *   + Haute École Spécialisée Bernoise/Berner Fachhochschule
 *     http://www.bfh.ch
 *   + Haute École du paysage, d'ingénierie et d'architecture de Genève
 *     http://hepia.hesge.ch/
 *   + Haute École d'Ingénierie et de Gestion du Canton de Vaud
 *     http://www.heig-vd.ch/
 */

package com.cburch.logisim.std;

import static com.cburch.logisim.std.Strings.S;

import com.cburch.logisim.fpga.library.BFHPraktika;
import com.cburch.logisim.soc.Soc;
import com.cburch.logisim.std.arith.Arithmetic;
import com.cburch.logisim.std.base.Base;
import com.cburch.logisim.std.gates.Gates;
import com.cburch.logisim.std.hdl.Hdl;
import com.cburch.logisim.std.io.Io;
import com.cburch.logisim.std.io.extra.ITA_IO;
import com.cburch.logisim.std.memory.Memory;
import com.cburch.logisim.std.plexers.Plexers;
import com.cburch.logisim.std.tcl.Tcl;
import com.cburch.logisim.std.ttl.TTL;
import com.cburch.logisim.std.wiring.Wiring;
import com.cburch.logisim.tools.Library;
import com.cburch.logisim.tools.Tool;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * Library of built-in component libraries
 */
public class Builtin extends Library {

  /**
   * Unique identifier of the library, used as reference in project files.
   * Do NOT change as it will prevent project files from loading.
   *
   * Identifier value must MUST be unique string among all libraries.
   */
  public static final String _ID = "Builtin";

  private List<Library> libraries = null;

  @Override
  public String getDisplayName() {
    return S.get("builtinLibrary");
  }

  @Override
  public List<Library> getLibraries() {
    if (libraries == null) {
      libraries = Arrays.asList(
        new Base(),
        new Gates(),
        new Wiring(),
        new Plexers(),
        new Arithmetic(),
        new Memory(),
        new Io(),
        new TTL(),
        new Hdl(),
        new Tcl(),
        new BFHPraktika(),
        new ITA_IO(),
        new Soc());
    }
    return libraries;
  }

  @Override
  public List<Tool> getTools() {
    return Collections.emptyList();
  }
}
