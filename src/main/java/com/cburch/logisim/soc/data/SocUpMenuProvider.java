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

import static com.cburch.logisim.soc.Strings.S;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.HashMap;

import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JMenuItem;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;

import com.cburch.logisim.circuit.CircuitState;
import com.cburch.logisim.data.Location;
import com.cburch.logisim.gui.generic.OptionPane;
import com.cburch.logisim.gui.main.Frame;
import com.cburch.logisim.instance.Instance;
import com.cburch.logisim.instance.StdAttr;
import com.cburch.logisim.proj.Project;
import com.cburch.logisim.soc.file.ProcessorReadElf;
import com.cburch.logisim.soc.gui.AssemblerPanel;
import com.cburch.logisim.soc.gui.ListeningFrame;
import com.cburch.logisim.soc.util.AssemblerInterface;
import com.cburch.logisim.tools.CircuitStateHolder;
import com.cburch.logisim.tools.MenuExtender;

public class SocUpMenuProvider  implements ActionListener {
	
  public static final SocUpMenuProvider SOCUPMENUPROVIDER = new SocUpMenuProvider();

  private static final int LOAD_ELF_FUNCTION = 1;
  private static final int SHOW_STATE_FUNCTION = 2;
  private static final int SHOW_PROGRAM = 3;
  private static final int SHOW_ASM = 4;

  private class InstanceMenuItem extends JMenuItem {
    private static final long serialVersionUID = 1L;
    private Instance instance;
    private int function;
    private SocUpStateInterface data;
    private CircuitState circuitState;
    private CircuitStateHolder.HierarchyInfo csh;
    
    public InstanceMenuItem(Instance inst, String label, int function, Object data, CircuitStateHolder.HierarchyInfo csh) {
      super(label);
      instance = inst;
      this.function = function;
      this.data = (SocUpStateInterface)data;
      circuitState = null;
      this.csh = csh;
    }
      
    public InstanceMenuItem(Instance inst, String label, int function, Object data, CircuitState state, CircuitStateHolder.HierarchyInfo csh) {
      super(label);
      instance = inst;
      this.function = function;
      this.data = (SocUpStateInterface)data;
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
    public SocUpStateInterface getState() { return data; }
    public CircuitState getCircuitState() { return circuitState; }
    public CircuitStateHolder.HierarchyInfo getHierarchyInfo() { return csh; }
  }
  
  
  
  private class MenuProvider implements MenuExtender, CircuitStateHolder {

    private Instance instance;
    private SocUpMenuProvider parrent;
    SocUpStateInterface data;
    CircuitState circuitState;
    HierarchyInfo hierarchy;

    public MenuProvider(Instance inst, SocUpMenuProvider parrent) {
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
      String name = circuitState != null ? instName+" : "+S.get("SocUpMenuAsmWindow") : S.get("SocUpMenuAsmWindow");
      CircuitState state = circuitState == null ? proj.getCircuitState() : circuitState;
      menu.addSeparator();
      HierarchyInfo hinfo;
      if (circuitState == null) {
        hinfo = new HierarchyInfo(proj.getCurrentCircuit());
        hinfo.addComponent(instance.getComponent());
      } else hinfo = hierarchy;
      InstanceMenuItem Asm = new InstanceMenuItem(instance,name,SHOW_ASM,(SocUpStateInterface)instance.getData(state),state,hinfo);
      Asm.addActionListener(parrent);
      Asm.setEnabled(true);
      menu.add(Asm);
      name = circuitState != null ? instName+" : "+S.get("SocUpMenuReadElf") : S.get("SocUpMenuReadElf");
      InstanceMenuItem ReadElf = new InstanceMenuItem(instance,name,LOAD_ELF_FUNCTION,state);
      ReadElf.addActionListener(parrent);
      ReadElf.setEnabled(true);
      menu.add(ReadElf);
      if (circuitState != null) {
        InstanceMenuItem showState = new InstanceMenuItem(instance,instName+" : "+S.get("SocUpMenuShowState"),
            SHOW_STATE_FUNCTION,data,hierarchy);
        showState.addActionListener(parrent);
        showState.setEnabled(true);
        menu.add(showState);
      }
      name = circuitState != null ? instName+" : "+S.get("SocUpMenuShowProgram") : S.get("SocUpMenuShowProgram");
      if (state != null)
        if (((SocUpStateInterface)instance.getData(state)).programLoaded()) {
          InstanceMenuItem showProg = new InstanceMenuItem(instance,name,SHOW_PROGRAM,
                  (SocUpStateInterface)instance.getData(state),state,hinfo);
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
      data = (SocUpStateInterface)state.getData(instance.getComponent());
    }

    @Override
    public void setHierarchyName(HierarchyInfo csh) {
      hierarchy = csh;
      hierarchy.addComponent(instance.getComponent());
    }
  }
  
  private class InstanceInformation {
    private Frame parrentFrame;
    private HashMap<SocUpStateInterface,ListeningFrame> myStates;
    private HashMap<SocUpStateInterface,ListeningFrame> myPrograms;
    private HashMap<SocUpStateInterface,ListeningFrame> myAsmWindows;
    
    public InstanceInformation(Instance inst, SocUpMenuProvider parrent) {
      parrentFrame = null;
      myStates = new HashMap<SocUpStateInterface,ListeningFrame>();
      myPrograms = new HashMap<SocUpStateInterface,ListeningFrame>();
      myAsmWindows = new HashMap<SocUpStateInterface,ListeningFrame>();
    }
    
    public void readElf(Instance instance, CircuitState circuitState) {
      JFileChooser fc = new JFileChooser();
      fc.setDialogTitle(S.get("SocUpMenuSelectElfFile"));
      int retVal = fc.showOpenDialog(parrentFrame);
      if (retVal != JFileChooser.APPROVE_OPTION)
        return;
      SocUpStateInterface data = (SocUpStateInterface) circuitState.getData(instance.getComponent());
      ProcessorReadElf reader = new ProcessorReadElf(fc.getSelectedFile(),instance,data.getElfType(),true);
      if (!reader.canExecute()||!reader.execute(circuitState)) {
        OptionPane.showMessageDialog(parrentFrame, reader.getErrorMessage(), S.get("SocUpMenuErrorReadingElfTitle"), OptionPane.ERROR_MESSAGE);
        return;
      }
      OptionPane.showMessageDialog(parrentFrame, S.get("ProcReadElfLoadedAndEntrySet"));
    }
    
    public void registerCpuState(SocUpStateInterface data) {
      if (!myStates.containsKey(data))
        myStates.put(data, null);
      if (!myPrograms.containsKey(data))
        myPrograms.put(data, null);
      if (!myAsmWindows.containsKey(data))
        myAsmWindows.put(data, null);
    }
    
    public void destroyCpuState(SocUpStateInterface data) {
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
      if (myAsmWindows.containsKey(data)) {
        if (myAsmWindows.get(data) != null) {
          myAsmWindows.get(data).setVisible(false);
          myAsmWindows.get(data).dispose();
        }
      }
    }
    
    public void showState(SocUpStateInterface data, CircuitStateHolder.HierarchyInfo csh) {
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
      ListeningFrame frame = new ListeningFrame(data.getProcessorType(),S.getter("SocUpMenuCpuStateWindowTitle"),csh);
      JPanel statePanel = data.getStatePanel();
      frame.setSize(statePanel.getSize());
      frame.setResizable(false);
      parrentFrame.addWindowListener(frame);
      frame.setVisible(true);
      frame.getContentPane().add(statePanel);
      frame.addWindowListener(data.getWindowListener());
      myStates.put(data, frame);
    }

    public void showProgram(SocUpStateInterface data, CircuitStateHolder.HierarchyInfo csh, CircuitState state) {
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
      ListeningFrame frame = new ListeningFrame(data.getProcessorType(),S.getter("SocUpMenuCpuProgramWindowTitle"),csh);
      parrentFrame.addWindowListener(frame);
      JPanel pan = data.getAsmWindow();
      frame.add(pan);
      frame.setVisible(true);
      frame.pack();
      frame.addWindowListener(data.getWindowListener());
      myPrograms.put(data, frame);
    }
    
    public void showAsmWindow(Instance inst, SocUpStateInterface data, CircuitStateHolder.HierarchyInfo csh, CircuitState state) {
      if (parrentFrame == null || data == null)
        return;
      if (myAsmWindows.containsKey(data))
        if (myAsmWindows.get(data)!= null) {
          JFrame frame = myAsmWindows.get(data);
          frame.setVisible(true);
          int fstate = frame.getExtendedState();
          fstate &= ~Frame.ICONIFIED;
          frame.setExtendedState(fstate);
          return;
        }
      ListeningFrame frame = new ListeningFrame(data.getProcessorType(),S.getter("SocUpMenuCpuAsmWindowTitle"),csh);
      parrentFrame.addWindowListener(frame);
      AssemblerInterface assembler = data.getAssembler();
      JPanel pan = new AssemblerPanel(frame, assembler.getHighlightStringIdentifier(), assembler,
        data.getProcessorInterface(),state); 
      frame.add(pan);
      frame.setVisible(true);
      frame.pack();
      frame.addWindowListener(data.getWindowListener());
      myAsmWindows.put(data, frame);
    }
    
    public void setParrentFrame(Frame frame) {
      parrentFrame = frame;
    }
    
    public void repaintStates() {
      for (SocUpStateInterface data : myStates.keySet()) data.repaint();
    }
    
  }
  
  private HashMap<Instance,InstanceInformation> myInfo;
  
  public SocUpMenuProvider() {
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
          case SHOW_ASM            : myInfo.get(inst).showAsmWindow(inst,info.getState(), info.getHierarchyInfo(),info.getCircuitState());
                                     return;
        }
      }
    }
  }
  
  private void setParrentFrame(Instance inst, Frame frame) {
    if (myInfo.containsKey(inst))
      myInfo.get(inst).setParrentFrame(frame);
  }
  
  public void registerCpuState(SocUpStateInterface data, Instance inst) {
    if (!myInfo.containsKey(inst)) 
      myInfo.put(inst, new InstanceInformation(inst,this));
    myInfo.get(inst).registerCpuState(data);
  }
  
  public void deregisterCpuState(SocUpStateInterface data, Instance inst) {
    if (myInfo.containsKey(inst))
      myInfo.get(inst).destroyCpuState(data);
  }
  
  public void repaintStates(Instance inst) {
    if (myInfo.containsKey(inst)) myInfo.get(inst).repaintStates();
  }
}
