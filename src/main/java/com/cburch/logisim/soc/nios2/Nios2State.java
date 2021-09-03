/*
 * Logisim-evolution - digital logic design tool and simulator
 * Copyright by the Logisim-evolution developers
 * 
 * https://github.com/logisim-evolution/
 * 
 * This is free software released under GNU GPLv3 license
 */

package com.cburch.logisim.soc.nios2;

import static com.cburch.logisim.soc.Strings.S;

import com.cburch.contracts.BaseWindowListenerContract;
import com.cburch.logisim.circuit.CircuitState;
import com.cburch.logisim.circuit.ComponentDataGuiProvider;
import com.cburch.logisim.comp.Component;
import com.cburch.logisim.data.BitWidth;
import com.cburch.logisim.data.Value;
import com.cburch.logisim.gui.generic.OptionPane;
import com.cburch.logisim.instance.Instance;
import com.cburch.logisim.instance.InstanceComponent;
import com.cburch.logisim.instance.InstanceData;
import com.cburch.logisim.prefs.AppPreferences;
import com.cburch.logisim.soc.data.SocBusInfo;
import com.cburch.logisim.soc.data.SocBusTransaction;
import com.cburch.logisim.soc.data.SocProcessorInterface;
import com.cburch.logisim.soc.data.SocSupport;
import com.cburch.logisim.soc.data.SocUpMenuProvider;
import com.cburch.logisim.soc.data.SocUpSimulationState;
import com.cburch.logisim.soc.data.SocUpSimulationStateListener;
import com.cburch.logisim.soc.data.SocUpStateInterface;
import com.cburch.logisim.soc.data.TraceInfo;
import com.cburch.logisim.soc.file.ElfHeader;
import com.cburch.logisim.soc.file.ElfProgramHeader;
import com.cburch.logisim.soc.file.ElfSectionHeader;
import com.cburch.logisim.soc.gui.BreakpointPanel;
import com.cburch.logisim.soc.gui.CpuDrawSupport;
import com.cburch.logisim.soc.util.AssemblerInterface;
import com.cburch.logisim.util.GraphicsUtil;
import java.awt.Color;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.event.WindowEvent;
import java.awt.event.WindowListener;
import java.util.HashMap;
import java.util.LinkedList;
import javax.swing.JPanel;
import org.fife.ui.rsyntaxtextarea.AbstractTokenMakerFactory;
import org.fife.ui.rsyntaxtextarea.TokenMakerFactory;

public class Nios2State implements SocUpSimulationStateListener, SocProcessorInterface {

  public class ProcessorState extends JPanel implements InstanceData, Cloneable, ComponentDataGuiProvider, BaseWindowListenerContract, SocUpStateInterface {
    private static final int STATUS_RSIE = 1 << 23;
    private static final int STATUS_PIE = 1;
    private static final long serialVersionUID = 1L;
    private final int[] registers;
    private final Boolean[] registers_valid;
    private int pc;
    private int status;
    private int estatus;
    private int bstatus;
    private int ienable;
    private int ipending;
    private int lastRegisterWritten = -1;
    private final LinkedList<TraceInfo> instrTrace;
    private Value lastClock;
    private final SocUpSimulationState simState;
    private final Instance myInstance;
    private boolean visible;
    private Integer entryPoint;
    private boolean programLoaded;
    private final BreakpointPanel bPanel;

    public ProcessorState(Instance inst) {
      registers = new int[32];
      registers_valid = new Boolean[32];
      instrTrace = new LinkedList<>();
      lastClock = Value.createUnknown(BitWidth.ONE);
      simState = new SocUpSimulationState();
      myInstance = inst;
      this.setSize(
          AppPreferences.getScaled(CpuDrawSupport.upStateBounds.getWidth()),
          AppPreferences.getScaled(CpuDrawSupport.upStateBounds.getHeight()));
      SocUpMenuProvider.SOCUPMENUPROVIDER.registerCpuState(this, inst);
      visible = false;
      entryPoint = null;
      programLoaded = false;
      AbstractTokenMakerFactory atmf = (AbstractTokenMakerFactory) TokenMakerFactory.getDefaultInstance();
      atmf.putMapping(ASSEMBLER.getHighlightStringIdentifier(), "com.cburch.logisim.soc.nios2.Nios2SyntaxHighlighter");
      bPanel = new BreakpointPanel(ASSEMBLER.getHighlightStringIdentifier());
      reset();
    }

    @Override
    public void paint(Graphics g) {
      draw((Graphics2D) g, true);
    }

    public void reset() {
      reset(null, null, null, null);
    }

    public Instance getInstance() {
      return myInstance;
    }

    public void reset(CircuitState state, Integer entry, ElfProgramHeader progInfo, ElfSectionHeader sectInfo) {
      if (entry != null) entryPoint = entry;
      if (progInfo != null || sectInfo != null) {
        programLoaded = true;
        bPanel.loadProgram(state, getProcessorInterface(), progInfo, sectInfo, ASSEMBLER);
      }
      pc = entryPoint != null ? entryPoint : resetVector;
      for (var i = 0; i < 31; i++) registers_valid[i] = false;
      lastRegisterWritten = -1;
      status = STATUS_RSIE;
      estatus = 0;
      bstatus = 0;
      ienable = 0;
      ipending = 0;
      instrTrace.clear();
      if (visible) repaint();
      simState.reset();
    }

    public int getEntryPoint() {
      return entryPoint;
    }

    @Override
    public boolean programLoaded() {
      return programLoaded;
    }

    @Override
    public JPanel getAsmWindow() {
      return bPanel;
    }

    public int getStatus() {
      return status;
    }

    public void setStatus(int value) {
      status = value & STATUS_PIE;
      status |= STATUS_RSIE;
    }

    public int getIenable() {
      return ienable;
    }

    public void setIenable(int value) {
      if (ienable != value) {
        ienable = value;
        repaint();
      }
    }

    public int getIpending() {
      return ipending;
    }

    public void setIpending(int value) {
      if (ipending != value) {
        ipending = value;
        repaint();
      }
    }

    public int getControlRegister(int index) {
      return switch (index) {
        case 0 -> status;
        case 1 -> estatus;
        case 2 -> bstatus;
        case 3 -> ienable;
        case 4 -> ipending;
        default -> 0;
      };
    }

    public void setControlRegister(int index, int value) {
      switch (index) {
        case 0:
          setStatus(value);
          break;
        case 1:
          estatus = value;
          break;
        case 2:
          bstatus = value;
          break;
        case 3:
          ienable = value;
          break;
        default:
          throw new IllegalStateException("Unsupported index value: " + index);
      }
    }

    public void setClock(Value clock, CircuitState cState) {
      if (lastClock == Value.FALSE && clock == Value.TRUE) execute(cState);
      lastClock = clock;
    }

    @Override
    public int getProgramCounter() {
      return pc;
    }

    @Override
    public SocUpSimulationState getSimState() {
      return simState;
    }

    @Override
    public void SimButtonPressed() {
      simState.buttonPressed();
    }

    public void setProgramCounter(int value) {
      /* TODO: check for misaligned exception */
      pc = value;
    }

    public int getRegisterValue(int index) {
      if (index == 0 || index > 31) return 0;
      /* TODO: handle correctly undefined registers instead of returning 0 */
      return registers[index - 1];
    }

    @Override
    public String getRegisterValueHex(int index) {
      return RegisterIsValid(index) ? String.format("0x%08X", getRegisterValue(index)) : "??????????";
    }

    public Boolean RegisterIsValid(int index) {
      if (index == 0) return true;
      if (index > 31) return false;
      return registers_valid[index - 1];
    }

    public void writeRegister(int index, int value) {
      lastRegisterWritten = -1;
      if (!(index == 0 || index > 31)) {
        registers_valid[index - 1] = true;
        registers[index - 1] = value;
        lastRegisterWritten = index;
      }
    }

    public void interrupt() {
      estatus = status;
      status &= (~STATUS_PIE);
      pc = exceptionVector;
      repaint();
    }

    public void endofInterrupt() {
      status = estatus;
      pc = getRegisterValue(29);
      repaint();
    }

    public void breakReq() {
      bstatus = status;
      status &= (~STATUS_PIE);
      final var nextPc = SocSupport.convUnsignedInt(pc) + 4L;
      writeRegister(30, SocSupport.convUnsignedLong(nextPc));
      pc = breakVector;
      repaint();
    }

    public void breakRet() {
      status = bstatus;
      pc = getRegisterValue(30);
      repaint();
    }

    public Component getMasterComponent() {
      return attachedBus.getComponent();
    }

    public void execute(CircuitState cState) {
      /* check the simulation state */
      if (!simState.canExecute()) return;
      /* here we handle the custom instructions */
      if (ASSEMBLER.getExeUnit() != null && ASSEMBLER.getExeUnit() instanceof Nios2CustomInstructions) {
        Nios2CustomInstructions cust = (Nios2CustomInstructions) ASSEMBLER.getExeUnit();
        if (cust.isValid() && cust.waitingOnReady(this, cState)) return;
      }
      HashMap<Integer, Integer> breakPoints = bPanel.getBreakPoints();
      if (breakPoints.containsKey(pc)) {
        if (simState.breakPointReached()) {
          bPanel.gotoLine(breakPoints.get(pc) - 1);
          OptionPane.showMessageDialog(
              null,
              S.get("RV32imBreakPointReached"),
              SocSupport.getMasterName(cState, Nios2State.this.getName()),
              OptionPane.INFORMATION_MESSAGE);
          return;
        }
      }
      /* check interrupts */
      if ((status & STATUS_PIE) != 0) {
        int maskedIrqs = ienable & ipending;
        if (maskedIrqs != 0) {
          writeRegister(29, pc);
          interrupt();
          repaint();
        }
      }
      /* fetch an instruction */
      SocBusTransaction trans =
          new SocBusTransaction(SocBusTransaction.READ_TRANSACTION, pc, 0, SocBusTransaction.WORD_ACCESS, attachedBus.getComponent());
      attachedBus
          .getSocSimulationManager()
          .initializeTransaction(trans, attachedBus.getBusId(), cState);
      if (trans.hasError()) {
        OptionPane.showMessageDialog(
            null,
            trans.getErrorMessage(),
            SocSupport.getMasterName(cState, Nios2State.this.getName()) + S.get("RV32imFetchTransaction"),
            OptionPane.ERROR_MESSAGE);
        simState.errorInExecution();
        return;
      }
      /* decode instruction */
      final var instruction = trans.getReadData();
      ASSEMBLER.decode(instruction);
      /* execute instruction */
      final var exe = ASSEMBLER.getExeUnit();
      lastRegisterWritten = -1;
      while (instrTrace.size() >= CpuDrawSupport.NR_OF_TRACES)
        instrTrace.removeLast();
      if (exe == null) {
        OptionPane.showMessageDialog(
            null,
            S.get("RV32imFetchInvalidInstruction"),
            SocSupport.getMasterName(cState, Nios2State.this.getName()) + S.get("RV32imFetchTransaction"),
            OptionPane.ERROR_MESSAGE);
        simState.errorInExecution();
        instrTrace.addFirst(new TraceInfo(pc, instruction, S.get("RV32imFetchInvInstrAsm"), true));
        pc = pc + 4;
        if (visible) repaint();
        return;
      }
      final var trace = new TraceInfo(pc, instruction, exe.getAsmInstruction(), false);
      if (!exe.execute(this, cState)) {
        final var s = new StringBuilder();
        s.append(S.get("RV32imFetchExecutionError"));
        if (exe.getErrorMessage() != null)
          s.append("\n").append(exe.getErrorMessage());
        OptionPane.showMessageDialog(
            null,
            s.toString(),
            SocSupport.getMasterName(cState, Nios2State.this.getName()) + S.get("RV32imFetchTransaction"),
            OptionPane.ERROR_MESSAGE);
        simState.errorInExecution();
        trace.setError();
        instrTrace.addFirst(trace);
        if (visible) repaint();
        return;
      }
      instrTrace.addFirst(trace);
      /* all done increment pc */
      if (!exe.performedJump()) pc = pc + 4;
      if (visible) repaint();
    }

    @Override
    public ProcessorState clone() {
      try {
        return (ProcessorState) super.clone();
      } catch (CloneNotSupportedException e) {
        return null;
      }
    }

    public void insertTransaction(SocBusTransaction trans, boolean hidden, CircuitState cState) {
      if (hidden) trans.setAsHiddenTransaction();
      attachedBus
          .getSocSimulationManager()
          .initializeTransaction(trans, attachedBus.getBusId(), cState);
    }

    public void draw(Graphics2D g, boolean scale) {
      CpuDrawSupport.drawRegisters(g, 0, 0, scale, this);
      CpuDrawSupport.drawHexReg(g, 170, 0, scale, pc, S.get("Rv32imProgramCounter"), true);
      CpuDrawSupport.drawHexReg(g, 275, 0, scale, status, S.get("Nios2Status"), true);
      CpuDrawSupport.drawHexReg(g, 380, 0, scale, estatus, S.get("Nios2Estatus"), true);
      CpuDrawSupport.drawHexReg(g, 485, 0, scale, bstatus, S.get("Nios2Bstatus"), true);
      CpuDrawSupport.drawTrace(g, 170, 40, scale, this);
      if (nrOfIrqs > 0) {
        CpuDrawSupport.drawIRQs(g, 0, 500, scale, nrOfIrqs, ipending, ienable);
      }
    }

    @Override
    public void destroy() {
      SocUpMenuProvider.SOCUPMENUPROVIDER.deregisterCpuState(this, myInstance);
    }

    @Override
    public void windowOpened(WindowEvent e) {
      repaint();
      visible = true;
    }

    @Override
    public void windowClosing(WindowEvent e) {
      visible = false;
    }

    @Override
    public void windowIconified(WindowEvent e) {
      visible = false;
    }

    @Override
    public void windowDeiconified(WindowEvent e) {
      repaint();
      visible = true;
    }

    @Override
    public void windowActivated(WindowEvent e) {
      visible = true;
    }

    @Override
    public int getLastRegisterWritten() {
      return lastRegisterWritten;
    }

    @Override
    public String getRegisterAbiName(int index) {
      return registerABINames[index];
    }

    @Override
    public String getRegisterNormalName(int index) {
      return "r" + index;
    }

    @Override
    public LinkedList<TraceInfo> getTraces() {
      return instrTrace;
    }

    @Override
    public WindowListener getWindowListener() {
      return this;
    }

    @Override
    public JPanel getStatePanel() {
      return this;
    }

    @Override
    public String getProcessorType() {
      return "Nios2s";
    }

    @Override
    public AssemblerInterface getAssembler() {
      return ASSEMBLER;
    }

    @Override
    public SocProcessorInterface getProcessorInterface() {
      return myInstance.getAttributeValue(Nios2Attributes.NIOS2_STATE);
    }

    @Override
    public int getElfType() {
      return ElfHeader.EM_INTEL_NIOS2;
    }
  }

  private int resetVector;
  private int exceptionVector;
  private int breakVector;
  private int nrOfIrqs;
  private String label;
  private final SocBusInfo attachedBus;

  public static final AssemblerInterface ASSEMBLER = new Nios2Assembler();
  public static final String[] registerABINames = {
      "zero", "at", "r2", "r3", "r4", "r5", "r6", "r7",
      "r8", "r9", "r10", "r11", "r12", "r13", "r14", "r15",
      "r16", "r17", "r18", "r19", "r20", "r21", "r22", "r23", "et", "bt",
      "gp", "sp", "fp", "ea", "sstat", "ra"};

  public static int getRegisterIndex(String name) {
    final var regName = name.toLowerCase();
    for (var i = 0; i < registerABINames.length; i++) {
      if (registerABINames[i].equals(regName)) return i;
    }
    if (regName.startsWith("r") && regName.length() < 4) {
      int index;
      try {
        index = Integer.parseUnsignedInt(regName.substring(1));
      } catch (NumberFormatException e) {
        index = -1;
      }
      return index;
    } else if (regName.startsWith("ctl") && regName.length() < 6) {
      int index;
      try {
        index = Integer.parseUnsignedInt(regName.substring(3));
      } catch (NumberFormatException e) {
        index = -1;
      }
      return index;
    } else if (regName.startsWith("c") && regName.length() < 4) {
      int index;
      try {
        index = Integer.parseUnsignedInt(regName.substring(1));
      } catch (NumberFormatException e) {
        index = -1;
      }
      return index;
    }
    return -1;
  }

  public static boolean isCustomRegister(String name) {
    final var regName = name.toLowerCase();
    return regName.startsWith("c") && regName.length() < 4 && !regName.startsWith("ctl");
  }

  public static boolean isControlRegister(String name) {
    String regName = name.toLowerCase();
    return regName.startsWith("ctl") && regName.length() < 6;
  }

  public Nios2State() {
    resetVector = 0;
    exceptionVector = 0x14;
    breakVector = 0x30;
    nrOfIrqs = 0;
    label = "";
    attachedBus = new SocBusInfo("");
  }

  public void copyInto(Nios2State dest) {
    dest.resetVector = resetVector;
    dest.exceptionVector = exceptionVector;
    dest.breakVector = breakVector;
    dest.nrOfIrqs = nrOfIrqs;
    dest.label = label;
    dest.attachedBus.setBusId(attachedBus.getBusId());
  }

  public String getName() {
    var name = label;
    if (name == null || name.isEmpty()) {
      final var loc = attachedBus.getComponent().getLocation();
      name = String.format("%s@%d,%d", attachedBus.getComponent().getFactory().getDisplayName(), loc.getX(), loc.getY());
    }
    return name;
  }

  public boolean setResetVector(int value) {
    if (resetVector == value) return false;
    resetVector = value;
    return true;
  }

  public Integer getResetVector() {
    return resetVector;
  }

  public boolean setExceptionVector(int value) {
    if (exceptionVector == value) return false;
    exceptionVector = value;
    return true;
  }

  public Integer getExceptionVector() {
    return exceptionVector;
  }

  public boolean setBreakVector(int value) {
    if (breakVector == value) return false;
    breakVector = value;
    return true;
  }

  public Integer getBreakVector() {
    return breakVector;
  }

  public boolean setNrOfIrqs(int value) {
    if (nrOfIrqs == value) return false;
    nrOfIrqs = value;
    return true;
  }

  public Integer getNrOfIrqs() {
    return nrOfIrqs;
  }

  public boolean setLabel(String value) {
    if (label.equals(value)) return false;
    label = value;
    return true;
  }

  public String getLabel() {
    return label;
  }

  public boolean setAttachedBus(SocBusInfo value) {
    if (attachedBus.getBusId().equals(value.getBusId())) return false;
    attachedBus.setBusId(value.getBusId());
    return true;
  }

  public SocBusInfo getAttachedBus() {
    return attachedBus;
  }

  public ProcessorState getNewState(Instance inst) {
    return new ProcessorState(inst);
  }

  public void paint(int x, int y, Graphics2D g2, Instance inst, boolean visible, InstanceData pstate) {
    final var gfx = (Graphics2D) g2.create();
    gfx.translate(x + CpuDrawSupport.upStateBounds.getX(), y + CpuDrawSupport.upStateBounds.getY());
    final var state = (ProcessorState) pstate;
    if (visible && state != null) {
      state.draw(gfx, false);
    } else {
      gfx.setColor(Color.LIGHT_GRAY);
      gfx.fillRect(
          0, 0, CpuDrawSupport.upStateBounds.getWidth(), CpuDrawSupport.upStateBounds.getHeight());
      gfx.setColor(Color.BLACK);
      GraphicsUtil.drawCenteredText(
          gfx,
          S.get("SocHiddenForFasterSimulation"),
          CpuDrawSupport.upStateBounds.getWidth() / 2,
          CpuDrawSupport.upStateBounds.getHeight() / 2);
    }
    gfx.dispose();
    if (state != null) state.simState.paint(g2, x, y, CpuDrawSupport.simStateBounds);
  }

  @Override
  public void SimulationStateChanged() {
    if (attachedBus != null && attachedBus.getComponent() != null)
      ((InstanceComponent) attachedBus.getComponent()).getInstance().fireInvalidated();
  }

  @Override
  public void setEntryPointandReset(CircuitState state, long entryPoint, ElfProgramHeader progInfo, ElfSectionHeader sectInfo) {
    int entry = (int) entryPoint;
    if (attachedBus != null && attachedBus.getComponent() != null) {
      final var comp = (InstanceComponent) attachedBus.getComponent();
      if (comp.getInstance() != null) {
        final var pstate = (ProcessorState) comp.getInstance().getData(state);
        if (pstate != null) pstate.reset(state, entry, progInfo, sectInfo);
        comp.getInstance().fireInvalidated();
      }
    }
  }

  @Override
  public void insertTransaction(SocBusTransaction trans, boolean hidden, CircuitState cState) {
    if (hidden) trans.setAsHiddenTransaction();
    if (cState == null) {
      final var comp = (InstanceComponent) attachedBus.getComponent();
      if (comp == null) return;
      final var state = comp.getInstanceStateImpl();
      if (state == null) return;
      cState = state.getProject().getCircuitState();
    }
    attachedBus
        .getSocSimulationManager()
        .initializeTransaction(trans, attachedBus.getBusId(), cState);
  }

  @Override
  public int getEntryPoint(CircuitState cState) {
    if (cState != null) {
      final var comp = (InstanceComponent) attachedBus.getComponent();
      if (comp != null) {
        return ((ProcessorState) cState.getData(comp)).getEntryPoint();
      }
    }
    return 0;
  }

}
