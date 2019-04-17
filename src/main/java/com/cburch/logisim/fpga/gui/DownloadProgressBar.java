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

import com.cburch.logisim.proj.Projects;
import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Rectangle;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JProgressBar;

public class DownloadProgressBar extends JFrame {

  /** */
  private static final long serialVersionUID = 1L;

  private JLabel LocText;
  private JProgressBar Progress;

  private int ProgresSteps = 5;
  private static Dimension LoctextSize = new Dimension(600, 30);

  public DownloadProgressBar(String title) {
    super(title);
    SetupGui();
  }

  public DownloadProgressBar(String title, int NrOfProgressSteps) {
    super(title);
    ProgresSteps = NrOfProgressSteps;
    SetupGui();
  }

  private void SetupGui() {
    GridBagLayout layout = new GridBagLayout();
    GridBagConstraints gbc = new GridBagConstraints();
    setResizable(false);
    setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
    setLayout(layout);
    LocText = new JLabel("...");
    LocText.setMaximumSize(LoctextSize);
    LocText.setPreferredSize(LoctextSize);
    gbc.gridx = 0;
    gbc.gridy = 0;
    gbc.fill = GridBagConstraints.HORIZONTAL;
    add(LocText, gbc);
    Progress = new JProgressBar(0, ProgresSteps);
    Progress.setValue(0);
    Progress.setStringPainted(true);
    gbc.gridy = 1;
    add(Progress, gbc);
    pack();
    setLocation(Projects.getCenteredLoc(getWidth(), getHeight() * 4));
    setVisible(true);
  }

  public void SetStatus(String msg) {
    LocText.setText(msg);
    Rectangle bounds = LocText.getBounds();
    bounds.x = 0;
    bounds.y = 0;
    LocText.repaint(bounds);
  }

  public void SetProgress(int val) {
    Progress.setValue(val);
    Rectangle bounds = Progress.getBounds();
    bounds.x = 0;
    bounds.y = 0;
    Progress.repaint(bounds);
  }
}
