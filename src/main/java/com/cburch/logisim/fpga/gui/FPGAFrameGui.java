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

import java.awt.LayoutManager;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JProgressBar;

public class FPGAFrameGui extends JFrame implements IFPGAFrame {
  /** For the need of class serialization */
  private static final long serialVersionUID = 1L;

  public FPGAFrameGui(String title) {
    super(title);
    // TODO Auto-generated constructor stub
  }

  @Override
  public void add(IFPGALabel label, IFPGAGrid grid) {
    // TODO Auto-generated method stub
    super.add((JLabel) label, grid);
  }

  @Override
  public void add(IFPGAProgressBar progress, IFPGAGrid grid) {
    // TODO Auto-generated method stub
    super.add((JProgressBar) progress, grid);
  }

  @Override
  public void setLayout(IFPGAGridLayout layout) {
    // TODO Auto-generated method stub
    super.setLayout((LayoutManager) layout);
  }
}
