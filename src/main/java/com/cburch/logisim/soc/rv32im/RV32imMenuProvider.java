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

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.HashMap;

import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;

import com.cburch.logisim.circuit.CircuitState;
import com.cburch.logisim.data.Location;
import com.cburch.logisim.gui.main.Frame;
import com.cburch.logisim.instance.Instance;
import com.cburch.logisim.instance.StdAttr;
import com.cburch.logisim.proj.Project;
import com.cburch.logisim.soc.file.ElfHeader;
import com.cburch.logisim.soc.file.ProcessorReadElf;
import com.cburch.logisim.soc.gui.ListeningFrame;
import com.cburch.logisim.tools.CircuitStateHolder;
import com.cburch.logisim.tools.MenuExtender;

public class RV32imMenuProvider implements ActionListener {

  private static final int LOAD_ELF_FUNCTION = 1;
  private static final int SHOW_STATE_FUNCTION = 2;
  private static final int SHOW_PROGRAM = 3;

  private class InstanceMenuItem extends JMenuItem {
    private static final long serialVersionUID = 1L;
    private Instance instance;
    private int function;
    private RV32im_state.ProcessorState data;
    private CircuitState circuitState;
    private CircuitStateHolder.HierarchyInfo csh;
    
    public InstanceMenuItem(Instance inst, String label, int function, Object data, CircuitStateHolder.HierarchyInfo csh) {
      super(label);
      instance = inst;
      this.function = function;
      this.data = (RV32im_state.ProcessorState)data;
      circuitState = null;
      this.csh = csh;
    }
      
    public InstanceMenuItem(Instance inst, String label, int function, Object data, CircuitState state, CircuitStateHolder.HierarchyInfo csh) {
      super(label);
      instance = inst;
      this.function = function;
      this.data = (RV32im_state.ProcessorState)data;
      circuitState = state;
      this.csh = csh;
    }
        
    public InstanceMenuItem(Instance inst, String label, int function, CircuitState state) {
      super(label);
      instance = inst;
      this.function = function;
      data = null;
      circuitState = state;
      csh = null;
    }
      
    public Instance getInstance() { return instance; }
    public int getFunction() { return function; }
    public RV32im_state.ProcessorState getState() { return data; }
    public CircuitState getCircuitState() { return circuitState; }
    public CircuitStateHolder.HierarchyInfo getHierarchyInfo() { return csh; }
  }
  
  
  
  private class MenuProvider implements MenuExtender, CircuitStateHolder {

    private Instance instance;
    private RV32imMenuProvider parrent;
    RV32im_state.ProcessorState data;
    CircuitState circuitState;
    HierarchyInfo hierarchy;

    public MenuProvider(Instance inst, RV32imMenuProvider parrent) {
      this.instance = inst;
      this.parrent = parrent;
      circuitState = null;
      data = null;
      hierarchy = null;
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
      CircuitState state = circuitState == null ? proj.getCircuitState() : circuitState;
      InstanceMenuItem ReadElf = new InstanceMenuItem(instance,name,LOAD_ELF_FUNCTION,state);
      ReadElf.addActionListener(parrent);
      ReadElf.setEnabled(true);
      menu.addSeparator();
      menu.add(ReadElf);
      if (circuitState != null) {
        InstanceMenuItem showState = new InstanceMenuItem(instance,instName+" : "+S.get("Rv32imShowState"),
        		SHOW_STATE_FUNCTION,data,hierarchy);
        showState.addActionListener(parrent);
        showState.setEnabled(true);
        menu.add(showState);
      }
      name = circuitState != null ? instName+" : "+S.get("Rv32imShowProgram") : S.get("Rv32imShowProgram");
      if (state != null)
        if (((RV32im_state.ProcessorState)instance.getData(state)).programLoaded()) {
          HierarchyInfo hinfo;
          if (circuitState == null) {
            hinfo = new HierarchyInfo(proj.getCurrentCircuit());
            hinfo.addComponent(instance.getComponent());
          } else hinfo = hierarchy;
          InstanceMenuItem showProg = new InstanceMenuItem(instance,name,SHOW_PROGRAM,
                  (RV32im_state.ProcessorState)instance.getData(state),state,hinfo);
          showProg.addActionListener(parrent);
          showProg.setEnabled(true);
          menu.add(showProg);
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
    public void setHierarchyName(HierarchyInfo csh) {
      hierarchy = csh;
      hierarchy.addComponent(instance.getComponent());
    }
  }
  
  private class InstanceInformation {
    private Frame parrentFrame;
    private HashMap<RV32im_state.ProcessorState,ListeningFrame> myStates;
    private HashMap<RV32im_state.ProcessorState,ListeningFrame> myPrograms;
    
    public InstanceInformation(Instance inst, RV32imMenuProvider parrent) {
      parrentFrame = null;
      myStates = new HashMap<RV32im_state.ProcessorState,ListeningFrame>();
      myPrograms = new HashMap<RV32im_state.ProcessorState,ListeningFrame>();
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
    
    public void registerCpuState(RV32im_state.ProcessorState data) {
      if (!myStates.containsKey(data))
        myStates.put(data, null);
      if (!myPrograms.containsKey(data))
        myPrograms.put(data, null);
    }
    
    public void destroyCpuState(RV32im_state.ProcessorState data) {
      if (myStates.containsKey(data)) {
        if (myStates.get(data) != null) {
          myStates.get(data).setVisible(false);
          myStates.get(data).dispose();
        }
        myStates.remove(data);
      }
      if (myPrograms.containsKey(data)) {
        if (myPrograms.get(data) != null) {
          myPrograms.get(data).setVisible(false);
          myPrograms.get(data).dispose();
        }
        myPrograms.remove(data);
      }
    }
    
    public void showState(RV32im_state.ProcessorState data, CircuitStateHolder.HierarchyInfo csh) {
      if (parrentFrame == null || data == null)
        return;
      if (myStates.containsKey(data))
        if (myStates.get(data)!= null) {
          JFrame frame = myStates.get(data); 
          frame.setVisible(true);
          int state = frame.getExtendedState();
          state &= ~Frame.ICONIFIED;
          frame.setExtendedState(state);
          return;
        }
      ListeningFrame frame = new ListeningFrame(S.getter("RV32imMenuCpuStateWindowTitle"),csh);
      frame.setSize(data.getSize());
      frame.setResizable(false);
      parrentFrame.addWindowListener(frame);
      frame.setVisible(true);
      frame.getContentPane().add(data);
      frame.addWindowListener(data);
      myStates.put(data, frame);
    }

    public void showProgram(RV32im_state.ProcessorState data, CircuitStateHolder.HierarchyInfo csh, CircuitState state) {
      if (parrentFrame == null || data == null)
        return;
      if (myPrograms.containsKey(data))
        if (myPrograms.get(data)!= null) {
          JFrame frame = myPrograms.get(data); 
          frame.setVisible(true);
          int fstate = frame.getExtendedState();
          fstate &= ~Frame.ICONIFIED;
          frame.setExtendedState(fstate);
          return;
        }
      ListeningFrame frame = new ListeningFrame(S.getter("RV32imMenuCpuProgramWindowTitle"),csh);
      parrentFrame.addWindowListener(frame);
      JPanel pan = data.getAsmWindow(); 
      frame.add(pan);
      frame.setVisible(true);
      frame.pack();
      frame.addWindowListener(data);
      myPrograms.put(data, frame);
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
          case SHOW_STATE_FUNCTION : myInfo.get(inst).showState(info.getState(),info.getHierarchyInfo());
                                     return;
          case SHOW_PROGRAM        : myInfo.get(inst).showProgram(info.getState(), info.getHierarchyInfo(),info.getCircuitState());
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
    myInfo.get(inst).registerCpuState(data);
  }
  
  public void deregisterCpuState(RV32im_state.ProcessorState data, Instance inst) {
    if (myInfo.containsKey(inst))
      myInfo.get(inst).destroyCpuState(data);
  }
  
}
