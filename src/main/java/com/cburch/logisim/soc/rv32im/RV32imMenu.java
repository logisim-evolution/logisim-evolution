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

package com.cburch.logisim.soc.rv32im;

import static com.cburch.logisim.soc.Strings.S;

import java.awt.Frame;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPopupMenu;

import com.cburch.logisim.circuit.CircuitState;
import com.cburch.logisim.data.Location;
import com.cburch.logisim.instance.Instance;
import com.cburch.logisim.instance.StdAttr;
import com.cburch.logisim.proj.Project;
import com.cburch.logisim.soc.file.ElfHeader;
import com.cburch.logisim.soc.file.ProcessorReadElf;
import com.cburch.logisim.tools.CircuitStateHolder;
import com.cburch.logisim.tools.MenuExtender;

public class RV32imMenu implements ActionListener, MenuExtender, CircuitStateHolder {

  private Instance instance;
  private Frame frame;
  private JMenuItem ReadElf;
  private CircuitState cState;
  private JMenuItem showState;
  
  public RV32imMenu( Instance instance ) {
    this.instance = instance;
    cState = null;
  }
  
  @Override
  public void configureMenu(JPopupMenu menu, Project proj) {
    frame = proj.getFrame();
    String instName = instance.getAttributeValue(StdAttr.LABEL);
    if (instName == null || instName.isBlank()) {
      Location loc = instance.getLocation();
      instName = instance.getFactory().getHDLName(instance.getAttributeSet())+"@"+loc.getX()+","+loc.getY();
    }
    String name = cState == null ? S.get("Rv32imReadElf") : instName+" : "+S.get("Rv32imReadElf");
    ReadElf = createItem(true,name);
    showState = createItem(true,instName+" : "+S.get("Rv32imShowState"));
    menu.addSeparator();
    menu.add(ReadElf);
    if (cState != null)
      menu.add(showState);
  }

  @Override
  public void actionPerformed(ActionEvent e) {
    Object src = e.getSource();
    if (src == ReadElf) readElf();
    if (src == showState) {
      JFrame frame = (JFrame)instance.getData(cState);
      frame.setTitle(S.get("RV32imCpuStateWindow"));
      frame.setVisible(true);
    }
  }

  private JMenuItem createItem(boolean enabled, String label) {
    JMenuItem ret = new JMenuItem(label);
    ret.setEnabled(enabled);
    ret.addActionListener(this);
    return ret;
  }
  
  private void readElf() {
    JFileChooser fc = new JFileChooser();
    fc.setDialogTitle(S.get("Rv32imSelectElfFile"));
    int retVal = fc.showOpenDialog(frame);
    if (retVal != JFileChooser.APPROVE_OPTION)
      return;
    ProcessorReadElf reader = new ProcessorReadElf(fc.getSelectedFile(),instance,ElfHeader.EM_RISCV,true);
    if (!reader.canExecute()||!reader.execute(cState)) {
      JOptionPane.showMessageDialog(frame, reader.getErrorMessage(), S.get("Rv32imErrorReadingElfTitle"), JOptionPane.ERROR_MESSAGE);
      return;
    }
    JOptionPane.showMessageDialog(frame, S.get("ProcReadElfLoadedAndEntrySet"));
  }

  @Override
  public void setCircuitState(CircuitState state) {
    cState = state;
  }

}
