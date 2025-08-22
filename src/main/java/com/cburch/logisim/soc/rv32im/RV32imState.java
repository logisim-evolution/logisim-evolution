/*
 * Logisim-evolution - digital logic design tool and simulator
 * Copyright by the Logisim-evolution developers
 *
 * https://github.com/logisim-evolution/
 *
 * This is free software released under GNU GPLv3 license
 */

package com.cburch.logisim.soc.rv32im;

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
import com.cburch.logisim.instance.InstanceStateImpl;
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
import com.cburch.logisim.util.StringUtil;
import java.awt.Color;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.event.WindowEvent;
import java.awt.event.WindowListener;
import java.util.LinkedList;
import javax.swing.JPanel;
import org.fife.ui.rsyntaxtextarea.AbstractTokenMakerFactory;
import org.fife.ui.rsyntaxtextarea.TokenMakerFactory;

public class RV32imState implements SocUpSimulationStateListener, SocProcessorInterface {

  private static final Integer[] implementedSprs = {
      0xF11, 0xF12, 0xF13, 0xF14, 0x300, 0x301, 0x304, 0x305,
      0x341, 0x342, 0x343, 0x344, 0x7B0, 0x7B1, 0x7A0, 0x7A1, 0x7A2, 0x7A4};
  public static final String[] implementedSprNames = {
      "MVENDORID", "MARCHID", "MIMPID", "MHARTID", "MSTATUS", "MISA", "MIE", "MTVEC",
      "MEPC", "MCAUSE", "MTVAL", "MIP", "DCSR", "DPC", "TSELECT", "TDATA1", "TDATA2",
      "TINFO" };

  public class ProcessorState extends JPanel
      implements InstanceData,
          Cloneable,
          ComponentDataGuiProvider,
          BaseWindowListenerContract,
          SocUpStateInterface {
    private static final long serialVersionUID = 1L;
    private final int[] registers;
    private final int[] csrs; // TODO: for the moment the csrs are just dummy to allow for nios 5 simulation
    private final Boolean[] registers_valid;
    private int pc;
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
      csrs = new int[17];
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
      final var atmf = (AbstractTokenMakerFactory) TokenMakerFactory.getDefaultInstance();
      atmf.putMapping(
          ASSEMBLER.getHighlightStringIdentifier(),
          "com.cburch.logisim.soc.rv32im.RV32imSyntaxHighlighter");
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

    public void reset(
        CircuitState state, Integer entry, ElfProgramHeader progInfo, ElfSectionHeader sectInfo) {
      if (entry != null) entryPoint = entry;
      if (progInfo != null || sectInfo != null) {
        programLoaded = true;
        bPanel.loadProgram(
            state,
            myInstance.getAttributeValue(RV32imAttributes.RV32IM_STATE),
            progInfo,
            sectInfo,
            ASSEMBLER);
      }
      pc = entryPoint != null ? entryPoint : resetVector;
      for (var i = 0; i < 31; i++) {
        registers_valid[i] = false;
      }
      for (var i = 0; i < 17; i++) {
        csrs[i] = 0;
      }
      lastRegisterWritten = -1;
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
    public void simButtonPressed() {
      simState.buttonPressed();
    }

    public void setProgramCounter(int value) {
      /* TODO: check for misaligned exception */
      pc = value;
    }

    public int getRegisterValue(int index) {
      return (index == 0 || index > 31)
          ? 0
          : registers[
              index - 1]; // TODO: handle correctly undefined registers instead of returning 0
    }

    @Override
    public String getRegisterValueHex(int index) {
      return isRegisterValid(index)
          ? String.format("0x%08X", getRegisterValue(index))
          : "??????????";
    }

    public Boolean isRegisterValid(int index) {
      if (index == 0) return true;
      if (index > 31) return false;
      return registers_valid[index - 1];
    }

    public void writeRegister(int index, int value) {
      lastRegisterWritten = -1;
      if (index == 0 || index > 31) return;
      registers_valid[index - 1] = true;
      registers[index - 1] = value;
      lastRegisterWritten = index;
    }
    
    public int getCsrValue(int sprIndex) {
      final var index = getSprArrayIndex(sprIndex); 
      return (index < 0)
          ? 0
          : csrs[index];
    }
    
    public void writeCsr(int sprIndex, int value) {
      final var index = getSprArrayIndex(sprIndex);
      if (index < 4 || index == 17) return; // these are RO CSR's
      csrs[index] = value;
    }

    public void interrupt() {
      pc = exceptionVector;
    }

    public Component getMasterComponent() {
      return attachedBus.getComponent();
    }

    public void execute(CircuitState cState) {
      /* check the simulation state */
      if (!simState.canExecute()) return;
      final var breakPoints = bPanel.getBreakPoints();
      if (breakPoints.containsKey(pc)) {
        if (simState.breakPointReached()) {
          bPanel.gotoLine(breakPoints.get(pc) - 1);
          OptionPane.showMessageDialog(
              null,
              S.get("RV32imBreakPointReached"),
              SocSupport.getMasterName(cState, RV32imState.this.getName()),
              OptionPane.INFORMATION_MESSAGE);
          return;
        }
      }
      /* TODO: check interrupts */
      /* fetch an instruction */
      final var trans =
          new SocBusTransaction(
              SocBusTransaction.READ_TRANSACTION,
              pc,
              0,
              SocBusTransaction.WORD_ACCESS,
              attachedBus.getComponent());
      attachedBus
          .getSocSimulationManager()
          .initializeTransaction(trans, attachedBus.getBusId(), cState);
      if (trans.hasError()) {
        OptionPane.showMessageDialog(
            null,
            trans.getErrorMessage(),
            SocSupport.getMasterName(cState, RV32imState.this.getName())
                + S.get("RV32imFetchTransaction"),
            OptionPane.ERROR_MESSAGE);
        simState.errorInExecution();
        return;
      }
      /* decode instruction */
      int instruction = trans.getReadData();
      ASSEMBLER.decode(instruction);
      /* execute instruction */
      final var exe = ASSEMBLER.getExeUnit();
      lastRegisterWritten = -1;
      while (instrTrace.size() >= CpuDrawSupport.NR_OF_TRACES) instrTrace.removeLast();
      if (exe == null) {
        OptionPane.showMessageDialog(
            null,
            S.get("RV32imFetchInvalidInstruction"),
            SocSupport.getMasterName(cState, RV32imState.this.getName())
                + S.get("RV32imFetchTransaction"),
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
        if (exe.getErrorMessage() != null) s.append("\n").append(exe.getErrorMessage());
        OptionPane.showMessageDialog(
            null,
            s.toString(),
            SocSupport.getMasterName(cState, RV32imState.this.getName())
                + S.get("RV32imFetchTransaction"),
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
      CpuDrawSupport.drawTrace(g, 170, 40, scale, this);
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
      return "x" + index;
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
    public AssemblerInterface getAssembler() {
      return ASSEMBLER;
    }

    @Override
    public SocProcessorInterface getProcessorInterface() {
      return myInstance.getAttributeValue(RV32imAttributes.RV32IM_STATE);
    }

    @Override
    public String getProcessorType() {
      return "RV32im (RISC V)";
    }

    @Override
    public int getElfType() {
      return ElfHeader.EM_RISCV;
    }
  }

  private int resetVector;
  private int exceptionVector;
  private int nrOfIrqs;
  private String label;
  private final SocBusInfo attachedBus;

  public static final AssemblerInterface ASSEMBLER = new RV32imAssembler();
  public static final String[] registerABINames = {
    "zero", "ra", "sp", "gp", "tp", "t0", "t1", "t2", "s0", "s1", "a0", "a1", "a2", "a3", "a4",
    "a5", "a6", "a7", "s2", "s3", "s4", "s5", "s6", "s7", "s8", "s9", "s10", "s11", "t3", "t4",
    "t5", "t6"
  };

  public static int getRegisterIndex(String name) {
    final var regName = name.toLowerCase();
    for (int i = 0; i < registerABINames.length; i++)
      if (registerABINames[i].equals(regName)) return i;
    if (regName.startsWith("x") && regName.length() < 4) {
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

  public RV32imState() {
    resetVector = 0;
    exceptionVector = 0x14;
    nrOfIrqs = 0;
    label = "";
    attachedBus = new SocBusInfo("");
  }

  public void copyInto(RV32imState dest) {
    dest.resetVector = resetVector;
    dest.exceptionVector = exceptionVector;
    dest.nrOfIrqs = nrOfIrqs;
    dest.label = label;
    dest.attachedBus.setBusId(attachedBus.getBusId());
  }

  public String getName() {
    var name = label;
    if (StringUtil.isNullOrEmpty(name)) {
      final var loc = attachedBus.getComponent().getLocation();
      name =
          String.format(
              "%s@%d,%d",
              attachedBus.getComponent().getFactory().getDisplayName(), loc.getX(), loc.getY());
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

  public void paint(
      int x, int y, Graphics2D g2, Instance inst, boolean visible, InstanceData pstate) {
    Graphics2D g = (Graphics2D) g2.create();
    g.translate(x + CpuDrawSupport.upStateBounds.getX(), y + CpuDrawSupport.upStateBounds.getY());
    ProcessorState state = (ProcessorState) pstate;
    if (visible && state != null) {
      state.draw(g, false);
    } else {
      g.setColor(Color.LIGHT_GRAY);
      g.fillRect(
          0, 0, CpuDrawSupport.upStateBounds.getWidth(), CpuDrawSupport.upStateBounds.getHeight());
      g.setColor(Color.BLACK);
      GraphicsUtil.drawCenteredText(
          g,
          S.get("SocHiddenForFasterSimulation"),
          CpuDrawSupport.upStateBounds.getWidth() / 2,
          CpuDrawSupport.upStateBounds.getHeight() / 2);
    }
    g.dispose();
    if (state != null) state.simState.paint(g2, x, y, CpuDrawSupport.simStateBounds);
  }

  @Override
  public void simulationStateChanged() {
    if (attachedBus != null && attachedBus.getComponent() != null)
      ((InstanceComponent) attachedBus.getComponent()).getInstance().fireInvalidated();
  }

  @Override
  public void setEntryPointandReset(
      CircuitState state, long entryPoint, ElfProgramHeader progInfo, ElfSectionHeader sectInfo) {
    int entry = (int) entryPoint;
    if (attachedBus != null && attachedBus.getComponent() != null) {
      InstanceComponent comp = (InstanceComponent) attachedBus.getComponent();
      if (comp.getInstance() != null) {
        ProcessorState pstate = (ProcessorState) comp.getInstance().getData(state);
        if (pstate != null) pstate.reset(state, entry, progInfo, sectInfo);
        comp.getInstance().fireInvalidated();
      }
    }
  }

  @Override
  public void insertTransaction(SocBusTransaction trans, boolean hidden, CircuitState cState) {
    if (hidden) trans.setAsHiddenTransaction();
    if (cState == null) {
      InstanceComponent comp = (InstanceComponent) attachedBus.getComponent();
      if (comp == null) return;
      InstanceStateImpl state = comp.getInstanceStateImpl();
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
      InstanceComponent comp = (InstanceComponent) attachedBus.getComponent();
      if (comp == null) return 0;
      return ((ProcessorState) cState.getData(comp)).getEntryPoint();
    }
    return 0;
  }
  
  public static boolean isSprImplemented(int index) {
    var contained = false;
    for (var i = 0; i < implementedSprs.length; i++) {
      if (implementedSprs[i].equals(index)) {
        contained = true;
      }
    }
    return contained;
  }
    
  public static int getSprArrayIndex(int index) {
    if (isSprImplemented(index)) {
      for (var i = 0; i < implementedSprs.length; i++) {
        if (implementedSprs[i].equals(index)) {
          return i;
        }
      }
    }
    return -1;
  }
    
  public static int getSprArrayIndex(String name) {
    var index = -1;
    for (var i = 0; i < implementedSprNames.length; i++) {
      if (implementedSprNames[i].equals(name.toUpperCase())) {
        index = i;
      }
    }
    return index;
  }
      
  public static String getSprName(int index) {
    return isSprImplemented(index)
          ? implementedSprNames[getSprArrayIndex(index)].toLowerCase()
          : String.format("0x%03X", index);
  }
    
  public static int getSprValue(int index) {
    return (index < 0 || index >= implementedSprs.length) 
          ? -1
          : implementedSprs[index];
  }
    
}
