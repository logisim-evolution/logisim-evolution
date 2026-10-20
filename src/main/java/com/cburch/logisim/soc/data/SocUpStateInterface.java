/*
 * Logisim-evolution - digital logic design tool and simulator
 * Copyright by the Logisim-evolution developers
 *
 * https://github.com/logisim-evolution/
 *
 * This is free software released under GNU GPLv3 license
 */

package com.cburch.logisim.soc.data;

import com.cburch.logisim.soc.util.AssemblerInterface;
import java.awt.event.WindowListener;
import java.util.LinkedList;
import javax.swing.JPanel;

public interface SocUpStateInterface {
  int getLastRegisterWritten();

  String getRegisterValueHex(int index);

  String getRegisterAbiName(int index);

  String getRegisterNormalName(int index);

  int getProgramCounter();

  LinkedList<TraceInfo> getTraces();

  void simButtonPressed();

  SocUpSimulationState getSimState();

  boolean programLoaded();

  WindowListener getWindowListener();

  JPanel getAsmWindow();

  JPanel getStatePanel();

  AssemblerInterface getAssembler();

  SocProcessorInterface getProcessorInterface();

  String getProcessorType();

  int getElfType();

  void repaint();
}
