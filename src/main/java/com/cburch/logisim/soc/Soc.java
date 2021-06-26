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

package com.cburch.logisim.soc;

import static com.cburch.logisim.soc.Strings.S;

import com.cburch.logisim.soc.bus.SocBus;
import com.cburch.logisim.soc.jtaguart.JtagUart;
import com.cburch.logisim.soc.memory.SocMemory;
import com.cburch.logisim.soc.nios2.Nios2;
import com.cburch.logisim.soc.pio.SocPio;
import com.cburch.logisim.soc.rv32im.Rv32im_riscv;
import com.cburch.logisim.soc.vga.SocVga;
import com.cburch.logisim.tools.FactoryDescription;
import com.cburch.logisim.tools.Library;
import com.cburch.logisim.tools.Tool;
import java.util.List;

public class Soc  extends Library {

  /**
   * Unique identifier of the library, used as reference in project files.
   * Do NOT change as it will prevent project files from loading.
   *
   * Identifier value must MUST be unique string among all libraries.
   */
  public static final String _ID = "Soc";

  private static final FactoryDescription[] DESCRIPTIONS = {
    new FactoryDescription(Rv32im_riscv.class, S.getter("Rv32imComponent"), "Rv32im.gif"),
    new FactoryDescription(Nios2.class, S.getter("Nios2Component"), "Nios2.gif"),
    new FactoryDescription(SocBus.class, S.getter("SocBusComponent")),
    new FactoryDescription(SocMemory.class, S.getter("SocMemoryComponent")),
    new FactoryDescription(SocPio.class, S.getter("SocPioComponent")),
    new FactoryDescription(SocVga.class, S.getter("SocVgaComponent")),
    new FactoryDescription(JtagUart.class, S.getter("SocJtagUartComponent")),
  };

  private List<Tool> tools = null;

  @Override
  public String getDisplayName() {
    return S.get("socLibrary");
  }

  @Override
  public List<Tool> getTools() {
    if (tools == null) {
      tools = FactoryDescription.getTools(Soc.class, DESCRIPTIONS);
    }
    return tools;
  }

  public boolean removeLibrary(String Name) {
    return false;
  }
}
