/**
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

package com.cburch.logisim.soc.data;

import java.awt.event.WindowListener;
import java.util.LinkedList;

import javax.swing.JPanel;

import com.cburch.logisim.soc.util.AssemblerInterface;

public interface SocUpStateInterface {
  public int getLastRegisterWritten();
  public String getRegisterValueHex(int index);
  public String getRegisterAbiName(int index);
  public String getRegisterNormalName(int index);
  public int getProgramCounter();
  public LinkedList<TraceInfo> getTraces();
  public void SimButtonPressed();
  public SocUpSimulationState getSimState();
  public boolean programLoaded();
  public WindowListener getWindowListener();
  public JPanel getAsmWindow();
  public JPanel getStatePanel();
  public AssemblerInterface getAssembler();
  public SocProcessorInterface getProcessorInterface();
  public String getProcessorType();
  public int getElfType();
  public void repaint();
}
