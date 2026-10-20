/*
 * Logisim-evolution - digital logic design tool and simulator
 * Copyright by the Logisim-evolution developers
 *
 * https://github.com/logisim-evolution/
 *
 * This is free software released under GNU GPLv3 license
 */

package com.cburch.logisim.soc.data;

import static com.cburch.logisim.soc.Strings.S;

import com.cburch.logisim.circuit.CircuitState;
import com.cburch.logisim.gui.generic.OptionPane;
import com.cburch.logisim.gui.main.Frame;
import com.cburch.logisim.instance.Instance;
import com.cburch.logisim.instance.StdAttr;
import com.cburch.logisim.proj.Project;
import com.cburch.logisim.soc.file.ProcessorReadElf;
import com.cburch.logisim.soc.gui.AssemblerPanel;
import com.cburch.logisim.soc.gui.ListeningFrame;
import com.cburch.logisim.tools.CircuitStateHolder;
import com.cburch.logisim.tools.MenuExtender;
import com.cburch.logisim.util.StringUtil;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.HashMap;
import javax.swing.JFileChooser;
import javax.swing.JMenuItem;
import javax.swing.JPopupMenu;

public class SocUpMenuProvider implements ActionListener {
  public static final SocUpMenuProvider SOCUPMENUPROVIDER = new SocUpMenuProvider();

  private static final int LOAD_ELF_FUNCTION = 1;
  private static final int SHOW_STATE_FUNCTION = 2;
  private static final int SHOW_PROGRAM = 3;
  private static final int SHOW_ASM = 4;

  private static class InstanceMenuItem extends JMenuItem {
    private static final long serialVersionUID = 1L;
    private final Instance instance;
    private final int function;
    private final SocUpStateInterface data;
    private final CircuitState circuitState;
    private final CircuitStateHolder.HierarchyInfo csh;

    public InstanceMenuItem(Instance inst, String label, int function, Object data, CircuitStateHolder.HierarchyInfo csh) {
      super(label);
      instance = inst;
      this.function = function;
      this.data = (SocUpStateInterface) data;
      circuitState = null;
      this.csh = csh;
    }

    public InstanceMenuItem(Instance inst, String label, int function, Object data, CircuitState state, CircuitStateHolder.HierarchyInfo csh) {
      super(label);
      instance = inst;
      this.function = function;
      this.data = (SocUpStateInterface) data;
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

    public Instance getInstance() {
      return instance;
    }

    public int getFunction() {
      return function;
    }

    public SocUpStateInterface getState() {
      return data;
    }

    public CircuitState getCircuitState() {
      return circuitState;
    }

    public CircuitStateHolder.HierarchyInfo getHierarchyInfo() {
      return csh;
    }
  }



  private class MenuProvider implements MenuExtender, CircuitStateHolder {

    private final Instance instance;
    private final SocUpMenuProvider parent;
    SocUpStateInterface data;
    CircuitState circuitState;
    HierarchyInfo hierarchy;

    public MenuProvider(Instance inst, SocUpMenuProvider parent) {
      this.instance = inst;
      this.parent = parent;
      circuitState = null;
      data = null;
      hierarchy = null;
    }

    @Override
    public void configureMenu(JPopupMenu menu, Project proj) {
      setParentFrame(instance, proj.getFrame());
      var instName = instance.getAttributeValue(StdAttr.LABEL);
      if (StringUtil.isNullOrEmpty(instName)) {
        final var loc = instance.getLocation();
        instName = instance.getFactory().getHDLName(instance.getAttributeSet()) + "@" + loc.getX() + "," + loc.getY();
      }
      var name = circuitState != null ? instName + " : " + S.get("SocUpMenuAsmWindow") : S.get("SocUpMenuAsmWindow");
      final var state = circuitState == null ? proj.getCircuitState() : circuitState;
      menu.addSeparator();
      HierarchyInfo hinfo;
      if (circuitState == null) {
        hinfo = new HierarchyInfo(proj.getCurrentCircuit());
        hinfo.addComponent(instance.getComponent());
      } else {
        hinfo = hierarchy;
      }
      final var asm = new InstanceMenuItem(instance, name, SHOW_ASM, instance.getData(state), state, hinfo);
      asm.addActionListener(parent);
      asm.setEnabled(true);
      menu.add(asm);
      name = circuitState != null ? instName + " : " + S.get("SocUpMenuReadElf") : S.get("SocUpMenuReadElf");
      final var readElf = new InstanceMenuItem(instance, name, LOAD_ELF_FUNCTION, state);
      readElf.addActionListener(parent);
      readElf.setEnabled(true);
      menu.add(readElf);
      if (circuitState != null) {
        final var showState = new InstanceMenuItem(instance,
            instName + " : " + S.get("SocUpMenuShowState"),
            SHOW_STATE_FUNCTION, data, hierarchy);
        showState.addActionListener(parent);
        showState.setEnabled(true);
        menu.add(showState);
      }
      name = circuitState != null ? instName + " : " + S.get("SocUpMenuShowProgram") : S.get("SocUpMenuShowProgram");
      if (state != null)
        if (((SocUpStateInterface) instance.getData(state)).programLoaded()) {
          final var showProg = new InstanceMenuItem(instance, name, SHOW_PROGRAM, instance.getData(state), state, hinfo);
          showProg.addActionListener(parent);
          showProg.setEnabled(true);
          menu.add(showProg);
        }
    }

    @Override
    public void setCircuitState(CircuitState state) {
      if (state == null) return;
      circuitState = state;
      data = (SocUpStateInterface) state.getData(instance.getComponent());
    }

    @Override
    public void setHierarchyName(HierarchyInfo csh) {
      hierarchy = csh;
      hierarchy.addComponent(instance.getComponent());
    }
  }

  private static class InstanceInformation {
    private Frame parentFrame;
    private final HashMap<SocUpStateInterface, ListeningFrame> myStates;
    private final HashMap<SocUpStateInterface, ListeningFrame> myPrograms;
    private final HashMap<SocUpStateInterface, ListeningFrame> myAsmWindows;

    public InstanceInformation(Instance inst, SocUpMenuProvider parent) {
      parentFrame = null;
      myStates = new HashMap<>();
      myPrograms = new HashMap<>();
      myAsmWindows = new HashMap<>();
    }

    public void readElf(Instance instance, CircuitState circuitState) {
      final var fc = new JFileChooser();
      fc.setDialogTitle(S.get("SocUpMenuSelectElfFile"));
      int retVal = fc.showOpenDialog(parentFrame);
      if (retVal != JFileChooser.APPROVE_OPTION) return;
      final var data = (SocUpStateInterface) circuitState.getData(instance.getComponent());
      final var reader = new ProcessorReadElf(fc.getSelectedFile(), instance, data.getElfType(), true);
      if (!reader.canExecute() || !reader.execute(circuitState)) {
        OptionPane.showMessageDialog(parentFrame, reader.getErrorMessage(), S.get("SocUpMenuErrorReadingElfTitle"), OptionPane.ERROR_MESSAGE);
        return;
      }
      OptionPane.showMessageDialog(parentFrame, S.get("ProcReadElfLoadedAndEntrySet"));
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
      if (parentFrame == null || data == null) return;
      if (myStates.containsKey(data))
        if (myStates.get(data) != null) {
          final var frame = myStates.get(data);
          frame.setVisible(true);
          var state = frame.getExtendedState();
          state &= ~Frame.ICONIFIED;
          frame.setExtendedState(state);
          return;
        }
      final var frame = new ListeningFrame(data.getProcessorType(), S.getter("SocUpMenuCpuStateWindowTitle"), csh);
      final var statePanel = data.getStatePanel();
      frame.setSize(statePanel.getSize());
      frame.setResizable(false);
      parentFrame.addWindowListener(frame);
      frame.setVisible(true);
      frame.getContentPane().add(statePanel);
      frame.addWindowListener(data.getWindowListener());
      myStates.put(data, frame);
    }

    public void showProgram(SocUpStateInterface data, CircuitStateHolder.HierarchyInfo csh, CircuitState state) {
      if (parentFrame == null || data == null) return;
      if (myPrograms.containsKey(data))
        if (myPrograms.get(data) != null) {
          final var frame = myPrograms.get(data);
          frame.setVisible(true);
          var frameState = frame.getExtendedState();
          frameState &= ~Frame.ICONIFIED;
          frame.setExtendedState(frameState);
          return;
        }
      final var frame = new ListeningFrame(data.getProcessorType(), S.getter("SocUpMenuCpuProgramWindowTitle"), csh);
      parentFrame.addWindowListener(frame);
      final var pan = data.getAsmWindow();
      frame.add(pan);
      frame.setVisible(true);
      frame.pack();
      frame.addWindowListener(data.getWindowListener());
      myPrograms.put(data, frame);
    }

    public void showAsmWindow(Instance inst, SocUpStateInterface data, CircuitStateHolder.HierarchyInfo csh, CircuitState state) {
      if (parentFrame == null || data == null) return;
      if (myAsmWindows.containsKey(data))
        if (myAsmWindows.get(data) != null) {
          final var frame = myAsmWindows.get(data);
          frame.setVisible(true);
          var fstate = frame.getExtendedState();
          fstate &= ~Frame.ICONIFIED;
          frame.setExtendedState(fstate);
          return;
        }
      final var frame = new ListeningFrame(data.getProcessorType(), S.getter("SocUpMenuCpuAsmWindowTitle"), csh);
      parentFrame.addWindowListener(frame);
      final var assembler = data.getAssembler();
      final var pan = new AssemblerPanel(frame, assembler.getHighlightStringIdentifier(), assembler, data.getProcessorInterface(), state);
      frame.add(pan);
      frame.setVisible(true);
      frame.pack();
      frame.addWindowListener(data.getWindowListener());
      myAsmWindows.put(data, frame);
    }

    public void setParentFrame(Frame frame) {
      parentFrame = frame;
    }

    public void repaintStates() {
      for (SocUpStateInterface data : myStates.keySet()) data.repaint();
    }

  }

  private final HashMap<Instance, InstanceInformation> myInfo;

  public SocUpMenuProvider() {
    myInfo = new HashMap<>();
  }

  public MenuExtender getMenu(Instance inst) {
    if (!myInfo.containsKey(inst)) {
      final var instInfo = new InstanceInformation(inst, this);
      myInfo.put(inst, instInfo);
    }
    return new MenuProvider(inst, this);
  }

  @Override
  public void actionPerformed(ActionEvent e) {
    final var source = e.getSource();
    if (source instanceof InstanceMenuItem info) {
      final var inst = info.getInstance();
      if (myInfo.containsKey(inst)) {
        switch (info.getFunction()) {
          case LOAD_ELF_FUNCTION -> {
            myInfo.get(inst).readElf(inst, info.getCircuitState());
            return;
          }
          case SHOW_STATE_FUNCTION -> {
            myInfo.get(inst).showState(info.getState(), info.getHierarchyInfo());
            return;
          }
          case SHOW_PROGRAM -> {
            myInfo.get(inst)
                .showProgram(info.getState(), info.getHierarchyInfo(), info.getCircuitState());
            return;
          }
          case SHOW_ASM -> {
            myInfo.get(inst).showAsmWindow(inst, info.getState(), info.getHierarchyInfo(),
                info.getCircuitState());
            return;
          }
        }
      }
    }
  }

  private void setParentFrame(Instance inst, Frame frame) {
    if (myInfo.containsKey(inst)) myInfo.get(inst).setParentFrame(frame);
  }

  public void registerCpuState(SocUpStateInterface data, Instance inst) {
    if (!myInfo.containsKey(inst)) myInfo.put(inst, new InstanceInformation(inst, this));
    myInfo.get(inst).registerCpuState(data);
  }

  public void deregisterCpuState(SocUpStateInterface data, Instance inst) {
    if (myInfo.containsKey(inst)) myInfo.get(inst).destroyCpuState(data);
  }

  public void repaintStates(Instance inst) {
    if (myInfo.containsKey(inst)) myInfo.get(inst).repaintStates();
  }
}
