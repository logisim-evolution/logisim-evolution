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

package com.cburch.logisim.soc.bus;

import static com.cburch.logisim.soc.Strings.S;

import java.awt.Frame;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.JMenuItem;
import javax.swing.JPopupMenu;

import com.cburch.logisim.instance.Instance;
import com.cburch.logisim.proj.Project;
import com.cburch.logisim.soc.data.SocBusInfo;
import com.cburch.logisim.soc.data.SocBusStateInfo;
import com.cburch.logisim.soc.gui.BusTransactionInsertionGui;
import com.cburch.logisim.tools.MenuExtender;

public class SocBusMenu implements ActionListener, MenuExtender {

  private Instance instance;
  private Frame frame;
  private JMenuItem memMap;
  private JMenuItem insertTrans;
  
  SocBusMenu(Instance inst) {
    instance = inst;
  }

  @Override
  public void configureMenu(JPopupMenu menu, Project proj) {
    this.frame = proj.getFrame();
    memMap = createItem(true,S.get("SocBusMemMap"));
    insertTrans = createItem(true,S.get("insertTrans"));
    menu.addSeparator();
    menu.add(memMap);
    menu.add(insertTrans);
  }

  @Override
  public void actionPerformed(ActionEvent e) {
    Object src = e.getSource();
    if (src == memMap)
      showMemoryMap();
    if (src == insertTrans)
      insertTransaction();
  }

  private JMenuItem createItem(boolean enabled, String label) {
    JMenuItem ret = new JMenuItem(label);
    ret.setEnabled(enabled);
    ret.addActionListener(this);
    return ret;
  }

  private void showMemoryMap() {
    SocBusInfo info = instance.getAttributeValue(SocBusAttributes.SOC_BUS_ID);
    SocBusStateInfo state = info.getSocSimulationManager().getSocBusState(info.getBusId());
    frame.addWindowListener(state);
    state.setVisible(true);
  }
  
  private void insertTransaction() {
    SocBusInfo info = instance.getAttributeValue(SocBusAttributes.SOC_BUS_ID);
    SocBusStateInfo state = info.getSocSimulationManager().getSocBusState(info.getBusId());
    BusTransactionInsertionGui gui = state.getTransactionInsertionGui();
    frame.addWindowListener(gui);
    gui.setVisible(true);
  }
}
