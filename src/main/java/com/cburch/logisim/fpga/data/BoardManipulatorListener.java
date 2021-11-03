/*
 * Logisim-evolution - digital logic design tool and simulator
 * © 2001 Logisim-evolution contributors
 *
 * https://github.com/logisim-evolution/
 *
 * This is free software released under GNU GPLv3 license
 */

package com.cburch.logisim.fpga.data;

public interface BoardManipulatorListener {

  void boardNameChanged(String newBoardName);

  void componentsChanged(IoComponentsInformation ioComps);
}
