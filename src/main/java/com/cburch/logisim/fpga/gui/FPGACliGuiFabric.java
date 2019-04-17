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

package com.cburch.logisim.fpga.gui;

import com.cburch.logisim.LogisimRuntimeSettings;

public class FPGACliGuiFabric {

  public static IFPGAFrame getFPGAFrame(String title) {
    if (LogisimRuntimeSettings.isRunTimeIsGui()) return new FPGAFrameGui(title);
    else return new FPGAFrameCli(title);
  }

  public static IFPGALabel getFPGALabel(String value) {
    if (LogisimRuntimeSettings.isRunTimeIsGui()) return new FPGALabelGui(value);
    else return new FPGALabelCli(value);
  }

  public static IFPGAProgressBar getFPGAProgressBar() {
    if (LogisimRuntimeSettings.isRunTimeIsGui()) return new FPGAProgressBarGui();
    else return new FPGAProgressBarCli();
  }

  public static IFPGAProgressBar getFPGAProgressBar(int min, int max) {
    if (LogisimRuntimeSettings.isRunTimeIsGui()) return new FPGAProgressBarGui(min, max);
    else return new FPGAProgressBarCli(min, max);
  }

  public static IFPGAGrid getFPGAGrid() {
    if (LogisimRuntimeSettings.isRunTimeIsGui()) return new FPGAGridGui();
    else return new FPGAGridCli();
  }

  public static IFPGAGridLayout getFPGAGridLayout() {
    if (LogisimRuntimeSettings.isRunTimeIsGui()) return new FPGAGridLayoutGui();
    else return new FPGAGridLayoutCli();
  }

  public static IFPGAOptionPanel getFPGAOptionPanel() {
    if (LogisimRuntimeSettings.isRunTimeIsGui()) return new FPGAOptionPanelGui();
    else return new FPGAOptionPanelCli();
  }
}
