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

package com.cburch.logisim.soc.gui;

import static com.cburch.logisim.soc.Strings.S;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowEvent;
import java.awt.event.WindowListener;
import java.util.HashMap;

import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPopupMenu;

import com.cburch.logisim.circuit.CircuitState;
import com.cburch.logisim.data.Location;
import com.cburch.logisim.gui.main.Frame;
import com.cburch.logisim.instance.Instance;
import com.cburch.logisim.instance.StdAttr;
import com.cburch.logisim.proj.Project;
import com.cburch.logisim.soc.file.ElfHeader;
import com.cburch.logisim.soc.file.ProcessorReadElf;
import com.cburch.logisim.soc.rv32im.RV32im_state;
import com.cburch.logisim.tools.CircuitStateHolder;
import com.cburch.logisim.tools.MenuExtender;

public class RV32imMenuProvider implements ActionListener {

  private static final int LOAD_ELF_FUNCTION = 1;
  private static final int SHOW_STATE_FUNCTION = 2;

  private class InstanceMenuItem extends JMenuItem {
    private static final long serialVersionUID = 1L;
    private Instance instance;
    private int function;
    private RV32im_state.ProcessorState data;
    private CircuitState circuitState;
    private String name;
    
    public InstanceMenuItem(Instance inst, String label, int function, Object data, String name) {
      super(label);
      instance = inst;
      this.function = function;
      this.data = (RV32im_state.ProcessorState)data;
      circuitState = null;
      this.name = name;
    }
      
    public InstanceMenuItem(Instance inst, String label, int function, CircuitState state) {
      super(label);
      instance = inst;
      this.function = function;
      data = null;
      circuitState = state;
      name = null;
    }
      
    public Instance getInstance() { return instance; }
    public int getFunction() { return function; }
    public RV32im_state.ProcessorState getState() { return data; }
    public CircuitState getCircuitState() { return circuitState; }
    public String getName() { return name; }
  }
  
  
  
  private class MenuProvider implements MenuExtender, CircuitStateHolder {

    private Instance instance;
    private RV32imMenuProvider parrent;
    RV32im_state.ProcessorState data;
    CircuitState circuitState;
    StringBuffer hierName;

    public MenuProvider(Instance inst, RV32imMenuProvider parrent) {
      this.instance = inst;
      this.parrent = parrent;
      circuitState = null;
      data = null;
      hierName = new StringBuffer();
    }

    @Override
    public void configureMenu(JPopupMenu menu, Project proj) {
      setParrentFrame(instance,proj.getFrame());
      String instName = instance.getAttributeValue(StdAttr.LABEL);
      if (instName == null || instName.isEmpty()) {
        Location loc = instance.getLocation();
        instName = instance.getFactory().getHDLName(instance.getAttributeSet())+"@"+loc.getX()+","+loc.getY();
      }
      String name = circuitState != null ? instName+" : "+S.get("Rv32imReadElf") : S.get("Rv32imReadElf");
      InstanceMenuItem ReadElf = new InstanceMenuItem(instance,name,LOAD_ELF_FUNCTION,circuitState);
      ReadElf.addActionListener(parrent);
      ReadElf.setEnabled(true);
      menu.addSeparator();
      menu.add(ReadElf);
      if (circuitState != null) {
        InstanceMenuItem showState = new InstanceMenuItem(instance,instName+" : "+S.get("Rv32imShowState"),
        		SHOW_STATE_FUNCTION,data,hierName.toString());
        showState.addActionListener(parrent);
        showState.setEnabled(true);
        menu.add(showState);
      }
    }
    
    @Override
    public void setCircuitState(CircuitState state) {
      if (state == null)
        return;
      circuitState = state;
      data = (RV32im_state.ProcessorState)state.getData(instance.getComponent());
    }

    @Override
    public void addHierarchyName(String name) {
      if (hierName.length() != 0)
        hierName.append(":");
      hierName.append(name);
    }
  }
  
  private class ListeningFrame extends JFrame implements WindowListener {

    private static final long serialVersionUID = 1L;

	@Override
    public void windowOpened(WindowEvent e) {}

    @Override
    public void windowClosing(WindowEvent e) { setVisible(false); }

    @Override
    public void windowClosed(WindowEvent e) {}

    @Override
    public void windowIconified(WindowEvent e) {}

    @Override
    public void windowDeiconified(WindowEvent e) {}

    @Override
    public void windowActivated(WindowEvent e) {}

    @Override
    public void windowDeactivated(WindowEvent e) {}
      
  }
  
  private class InstanceInformation {
    private Frame parrentFrame;
    private HashMap<RV32im_state.ProcessorState,ListeningFrame> myFrames;
    
    public InstanceInformation(Instance inst, RV32imMenuProvider parrent) {
      parrentFrame = null;
      myFrames = new HashMap<RV32im_state.ProcessorState,ListeningFrame>();
    }
    
    public void readElf(Instance instance, CircuitState circuitState) {
      JFileChooser fc = new JFileChooser();
      fc.setDialogTitle(S.get("Rv32imSelectElfFile"));
      int retVal = fc.showOpenDialog(parrentFrame);
      if (retVal != JFileChooser.APPROVE_OPTION)
        return;
      ProcessorReadElf reader = new ProcessorReadElf(fc.getSelectedFile(),instance,ElfHeader.EM_RISCV,true);
      if (!reader.canExecute()||!reader.execute(circuitState)) {
        JOptionPane.showMessageDialog(parrentFrame, reader.getErrorMessage(), S.get("Rv32imErrorReadingElfTitle"), JOptionPane.ERROR_MESSAGE);
        return;
      }
      JOptionPane.showMessageDialog(parrentFrame, S.get("ProcReadElfLoadedAndEntrySet"));
    }
    
    public void registerCpuState(RV32im_state.ProcessorState data, Instance inst) {
      if (myFrames.containsKey(data))
        return;
      if (parrentFrame == null)
        myFrames.put(data, null);
      else {
        ListeningFrame frame = new ListeningFrame();
        frame.setSize(data.getSize());
        frame.setResizable(false);
        frame.getContentPane().add(data);
        frame.addWindowListener(data);
        parrentFrame.addWindowListener(frame);
        myFrames.put(data, frame);
      }
    }
    
    public void destroyCpuState(RV32im_state.ProcessorState data, Instance inst) {
      if (!myFrames.containsKey(data))
        return;
      if (myFrames.get(data) != null)
        myFrames.get(data).setVisible(false);
      myFrames.remove(data);
    }
    
    public void showState(RV32im_state.ProcessorState data, String name) {
      if (parrentFrame == null || data == null)
        return;
      String title = S.get("RV32imMenuCpuStateWindowTitle")+name;
      if (myFrames.containsKey(data))
        if (myFrames.get(data)!= null) {
          if (!myFrames.get(data).getTitle().equals(title))
            myFrames.get(data).setTitle(title);
          myFrames.get(data).setVisible(true);
          return;
        }
      ListeningFrame frame = new ListeningFrame();
      frame.setSize(data.getSize());
      frame.setResizable(false);
      frame.setTitle(title);
      parrentFrame.addWindowListener(frame);
      frame.setVisible(true);
      frame.getContentPane().add(data);
      frame.addWindowListener(data);
      myFrames.put(data, frame);
    }
    
    public void setParrentFrame(Frame frame) {
      parrentFrame = frame;
    }
    
  }
  
  private HashMap<Instance,InstanceInformation> myInfo;
  
  public RV32imMenuProvider() {
    myInfo = new HashMap<Instance,InstanceInformation>();
  }
  
  public MenuExtender getMenu(Instance inst) {
    if (!myInfo.containsKey(inst)) {
      InstanceInformation instInfo = new InstanceInformation(inst,this);
      myInfo.put(inst, instInfo);
    }
    return new MenuProvider(inst,this);
  }

  @Override
  public void actionPerformed(ActionEvent e) {
    Object source = e.getSource();
    if (source instanceof InstanceMenuItem) {
      InstanceMenuItem info = (InstanceMenuItem) source;
      Instance inst = info.getInstance();
      if (myInfo.containsKey(inst)) {
        switch (info.getFunction()) {
          case LOAD_ELF_FUNCTION   : myInfo.get(inst).readElf(inst,info.getCircuitState());
                                     return;
          case SHOW_STATE_FUNCTION : myInfo.get(inst).showState(info.getState(),info.getName());
                                     return;
        }
      }
    }
  }
  
  private void setParrentFrame(Instance inst, Frame frame) {
    if (myInfo.containsKey(inst))
      myInfo.get(inst).setParrentFrame(frame);
  }
  
  public void registerCpuState(RV32im_state.ProcessorState data, Instance inst) {
    if (!myInfo.containsKey(inst)) 
      myInfo.put(inst, new InstanceInformation(inst,this));
    myInfo.get(inst).registerCpuState(data, inst);
  }
  
  public void deregisterCpuState(RV32im_state.ProcessorState data, Instance inst) {
    if (myInfo.containsKey(inst))
      myInfo.get(inst).destroyCpuState(data, inst);
  }
  
}
