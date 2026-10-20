/*
 * Logisim-evolution - digital logic design tool and simulator
 * Copyright by the Logisim-evolution developers
 *
 * https://github.com/logisim-evolution/
 *
 * This is free software released under GNU GPLv3 license
 */

package com.cburch.logisim.std;

import static com.cburch.logisim.std.Strings.S;

import com.cburch.logisim.soc.Soc;
import com.cburch.logisim.std.arith.ArithmeticLibrary;
import com.cburch.logisim.std.arith.floating.FPArithmeticLibrary;
import com.cburch.logisim.std.base.BaseLibrary;
import com.cburch.logisim.std.bfh.BfhLibrary;
import com.cburch.logisim.std.gates.GatesLibrary;
import com.cburch.logisim.std.hdl.HdlLibrary;
import com.cburch.logisim.std.io.IoLibrary;
import com.cburch.logisim.std.io.extra.ExtraIoLibrary;
import com.cburch.logisim.std.memory.MemoryLibrary;
import com.cburch.logisim.std.plexers.PlexersLibrary;
import com.cburch.logisim.std.tcl.TclLibrary;
import com.cburch.logisim.std.ttl.TtlLibrary;
import com.cburch.logisim.std.wiring.WiringLibrary;
import com.cburch.logisim.tools.Library;
import com.cburch.logisim.tools.Tool;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public class Builtin extends Library {
  /**
   * Unique identifier of the library, used as reference in project files. Do NOT change as it will
   * prevent project files from loading.
   *
   * <p>Identifier value must MUST be unique string among all libraries.
   */
  public static final String _ID = "Builtin";

  private List<Library> libraries = null;

  public Builtin() {
    libraries =
        Arrays.asList(
            new BaseLibrary(),
            new GatesLibrary(),
            new WiringLibrary(),
            new PlexersLibrary(),
            new ArithmeticLibrary(),
            new FPArithmeticLibrary(),
            new MemoryLibrary(),
            new IoLibrary(),
            new TtlLibrary(),
            new HdlLibrary(),
            new TclLibrary(),
            new BfhLibrary(),
            new ExtraIoLibrary(),
            new Soc());
  }

  @Override
  public String getDisplayName() {
    return S.get("builtinLibrary");
  }

  @Override
  public List<Library> getLibraries() {
    return libraries;
  }

  @Override
  public List<Tool> getTools() {
    return Collections.emptyList();
  }
}
