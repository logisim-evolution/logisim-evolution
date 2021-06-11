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

  private static final FactoryDescription[] DESCRIPTIONS = {
    new FactoryDescription(
        "Rv32im", S.getter("Rv32imComponent"), "Rv32im.gif", Rv32im_riscv.class),
    new FactoryDescription(
        "Nios2", S.getter("Nios2Component"), "Nios2.gif", Nios2.class),
    new FactoryDescription(
        "SocBus", S.getter("SocBusComponent"), "" , SocBus.class ),
    new FactoryDescription(
         "Socmem", S.getter("SocMemoryComponent"), "" , SocMemory.class ),
    new FactoryDescription(
            "SocPio", S.getter("SocPioComponent"), "" , SocPio.class),
    new FactoryDescription(
            "SocVga", S.getter("SocVgaComponent"), "" , SocVga.class),
    new FactoryDescription(
            "SocJtagUart", S.getter("SocJtagUartComponent"), "" , JtagUart.class),
  };

  private List<Tool> tools = null;

  @Override
  public String getDisplayName() {
    return S.get("socLibrary");
  }

  @Override
  public String getName() {
    return "Soc";
  }

  @Override
  public List<Tool> getTools() {
    if (tools == null) {
      tools = FactoryDescription.getTools(Soc.class, DESCRIPTIONS);
    }
    return tools;
  }
}
