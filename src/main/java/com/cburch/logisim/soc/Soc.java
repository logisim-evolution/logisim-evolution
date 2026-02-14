/*
 * Logisim-evolution - digital logic design tool and simulator
 * Copyright by the Logisim-evolution developers
 *
 * https://github.com/logisim-evolution/
 *
 * This is free software released under GNU GPLv3 license
 */

package com.cburch.logisim.soc;

import static com.cburch.logisim.soc.Strings.S;

import com.cburch.logisim.soc.bus.SocBus;
import com.cburch.logisim.soc.dma.SocDma;
import com.cburch.logisim.soc.jtaguart.JtagUart;
import com.cburch.logisim.soc.memory.SocMemory;
import com.cburch.logisim.soc.nios2.Nios2;
import com.cburch.logisim.soc.pio.SocPio;
import com.cburch.logisim.soc.rv32im.Rv32imRiscV;
import com.cburch.logisim.soc.vga.SocVga;
import com.cburch.logisim.tools.FactoryDescription;
import com.cburch.logisim.tools.Library;
import com.cburch.logisim.tools.Tool;
import java.util.List;

public class Soc extends Library {

  /**
   * Unique identifier of the library, used as reference in project files. Do NOT change as it will
   * prevent project files from loading.
   *
   * <p>Identifier value must MUST be unique string among all libraries.
   */
  public static final String _ID = "Soc";

  private static final FactoryDescription[] DESCRIPTIONS = {
    new FactoryDescription(Rv32imRiscV.class, S.getter("Rv32imComponent"), "Rv32im.gif"),
    new FactoryDescription(Nios2.class, S.getter("Nios2Component"), "Nios2.gif"),
    new FactoryDescription(SocBus.class, S.getter("SocBusComponent")),
    new FactoryDescription(SocMemory.class, S.getter("SocMemoryComponent")),
    new FactoryDescription(SocPio.class, S.getter("SocPioComponent")),
    new FactoryDescription(SocVga.class, S.getter("SocVgaComponent")),
    new FactoryDescription(SocDma.class, S.getter("SocDmaComponent")),
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
}
