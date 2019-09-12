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

import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;
import java.util.LinkedList;

import javax.swing.JOptionPane;

import com.cburch.logisim.data.BitWidth;
import com.cburch.logisim.data.Location;
import com.cburch.logisim.data.Value;
import com.cburch.logisim.instance.Instance;
import com.cburch.logisim.instance.InstanceComponent;
import com.cburch.logisim.soc.data.SocBusInfo;
import com.cburch.logisim.soc.data.SocBusTransaction;
import com.cburch.logisim.soc.data.SocProcessorInterface;
import com.cburch.logisim.soc.data.SocUpSimulationState;
import com.cburch.logisim.soc.data.SocUpSimulationStateListener;
import com.cburch.logisim.util.GraphicsUtil;

public class RV32im_state implements SocUpSimulationStateListener,SocProcessorInterface {

  private int[] registers;
  private Boolean[] registers_valid;
  private int pc;
  private int lastRegisterWritten = -1;
  private Value lastClock;
  private int resetVector;
  private int exceptionVector;
  private int nrOfIrqs;
  private RV32imDecoder decoder;
  private String label;
  private SocBusInfo attachedBus;
  private SocUpSimulationState simState;
  private LinkedList<TraceInfo> instrTrace;
  
  public static String[] registerABINames = {"zero","ra","sp","gp","tp","t0","t1","t2",
		                                      "s0","s1","a0","a1","a2","a3","a4","a5",
		                                      "a6","a7","s2","s3","s4","s5","s6","s7","s8","s9",
		                                      "s10","s11","t3","t4","t5","t6"};
  private static final int NrOfTraces = 21;
  private static final int TRACEHEIGHT = 20;
  
  private class TraceInfo {
    private int pc;
    private int instruction;
    private String asm;
    private boolean error;
    
    public TraceInfo(int pc , int instruction, String asm, boolean error) {
      this.pc = pc;
      this.instruction = instruction;
      this.asm = asm;
      this.error = error;
    }
    
    public void setError() {
      error = true;
    }
    
    public void paint(Graphics2D g , int yOffset ) {
      int xOff = 5;
      paintBox(g,xOff,yOffset,pc);
      xOff += 85;
      paintBox(g,xOff,yOffset,instruction);
      xOff += 85;
      g.setColor(error ? Color.RED : Color.BLACK);
      Font f = g.getFont();
      g.setFont(new Font( "Monospaced", Font.PLAIN, 12 ).deriveFont(Font.BOLD));
      g.drawString(asm, xOff, yOffset+15);
      g.setFont(f);
    }
    
    private void paintBox(Graphics2D g, int x , int y , int value ) {
      g.setColor(Color.WHITE);
      g.fillRect(x, y+1, 80, TRACEHEIGHT-2);
      g.setColor(Color.BLACK);
      g.drawRect(x, y+1, 80, TRACEHEIGHT-2);
      g.setColor(error ? Color.RED : Color.DARK_GRAY);
      GraphicsUtil.drawCenteredText(g, String.format("0x%08X", value), x+40, y+TRACEHEIGHT/2);
      g.setColor(Color.BLACK);
    }
  }
  
  public RV32im_state() {
    lastClock = Value.createUnknown(BitWidth.ONE);
    resetVector = 0;
    exceptionVector = 0x14;
    nrOfIrqs = 0;
    decoder = new RV32imDecoder();
    label = "";
    attachedBus = new SocBusInfo("");
    simState = new SocUpSimulationState();
    simState.registerListener(this);
    instrTrace = new LinkedList<TraceInfo>();
    reset();
  }
  
  public String getMasterName() {
    String name = label;
    if (name == null || name.isBlank()) {
      Location loc = attachedBus.getComponent().getLocation();
      name = attachedBus.getComponent().getFactory().getDisplayName()+"@"+loc.getX()+","+loc.getY();
    }
    return name;
  }
  
  public boolean setResetVector(int value) {
    if (resetVector == value)
      return false;
    resetVector = value;
    return true;
  }
  
  public Integer getResetVector() {
    return resetVector;
  }
  
  public boolean setExceptionVector(int value) {
    if (exceptionVector == value)
      return false;
    exceptionVector = value;
    return true;
  }
  
  public Integer getExceptionVector() {
    return exceptionVector;
  }
  
  public boolean setNrOfIrqs(int value) {
    if (nrOfIrqs == value)
      return false;
    nrOfIrqs = value;
    return true;
  }
  
  public Integer getNrOfIrqs() {
    return nrOfIrqs;
  }
  
  public boolean setLabel(String value) {
    if (label.equals(value))
      return false;
    label = value;
    return true;
  }
  
  public String getLabel() {
    return label;
  }
  
  public boolean setAttachedBus( SocBusInfo value ) {
    if (attachedBus.getBusId().equals(value.getBusId()))
      return false;
    attachedBus.setBusId(value.getBusId());
    return true;
  }
  
  public SocBusInfo getAttachedBus() {
    return attachedBus;
  }
  
  private void execute() {
    /* check the simulation state */
    if (!simState.canExecute())
      return;
    /* TODO: check interrupts */
    /* fetch an instruction */
    SocBusTransaction trans = new SocBusTransaction(SocBusTransaction.READTransaction,
    		pc,0,SocBusTransaction.WordAccess,getMasterName());
    SocBusTransaction result = attachedBus.getSocSimulationManager().initializeTransaction(trans, attachedBus.getBusId());
    if (result.hasError()) {
      JOptionPane.showMessageDialog(null,result.getErrorMessage(),
    		  getMasterName()+S.get("RV32imFetchTransaction"),JOptionPane.ERROR_MESSAGE);
      simState.errorInExecution();
      return;
    }
    /* decode instruction */
    int instruction = result.getData(); 
    decoder.decode(instruction);
    /* execute instruction */
    lastRegisterWritten = -1;
    while (instrTrace.size() >= NrOfTraces)
      instrTrace.removeLast();
    RV32imExecutionUnitInterface exe = decoder.getExeUnit();
    if (exe == null) {
      JOptionPane.showMessageDialog(null,S.get("RV32imFetchInvalidInstruction"),
            getMasterName()+S.get("RV32imFetchTransaction"),JOptionPane.ERROR_MESSAGE);
      simState.errorInExecution();
      instrTrace.addFirst(new TraceInfo(pc,instruction,S.get("RV32imFetchInvInstrAsm"),true));
      pc = pc + 4;
      return;
    }
    TraceInfo trace = new TraceInfo(pc,instruction,exe.getAsmInstruction(),false);
    if (!exe.execute(this)) {
      StringBuffer s = new StringBuffer();
      s.append(S.get("RV32imFetchExecutionError"));
      if (exe.getErrorMessage() != null)
        s.append("\n"+exe.getErrorMessage());
      JOptionPane.showMessageDialog(null,s.toString(),
         getMasterName()+S.get("RV32imFetchTransaction"),JOptionPane.ERROR_MESSAGE);
      simState.errorInExecution();
      trace.setError();
      instrTrace.addFirst(trace);
      return;
    }
    instrTrace.addFirst(trace);
    /* all done increment pc */
    if (!exe.performedJump())
      pc = pc+4;
  }
  
  public void updateClock(Value clock , Value reset) {
    if (lastClock.equals(Value.FALSE)&&clock.equals(Value.TRUE)) {
      if (reset.equals(Value.TRUE))
        reset();
      else {
        execute();
      }
    }
    lastClock = clock;
  }
  
  public void reset() {
    pc = resetVector;
    if (registers == null)
      registers = new int[31];
    if (registers_valid == null)
      registers_valid = new Boolean[31];
    for (int i = 0 ; i < 31 ; i++)
      registers_valid[i] = false;
    lastRegisterWritten = -1;
    instrTrace.clear();
  }
  
  public void interrupt() {
    pc = exceptionVector;
  }
  
  public int getProgramCounter() {
    return pc;
  }
  
  public void setProgramCounter(int value) {
    /* TODO: check for misalligned exception */
    pc = value;
  }
  
  public int getRegisterValue(int index) {
    if (index == 0 || index > 31)
      return 0;
    return registers[index-1];
  }
  
  public String getRegisterValueHex(int index) {
    if (RegisterIsValid(index))
      return String.format("0x%08X", getRegisterValue(index));
    return "??????????";
  }
  
  public Boolean RegisterIsValid(int index) {
    if (index == 0)
      return true;
    if (index > 31)
      return false;
    return registers_valid[index-1];
  }
  
  public void writeRegister(int index , int value) {
    lastRegisterWritten = -1;
    if (index == 0 || index > 31)
      return;
    registers_valid[index-1] = true;
    registers[index-1] = value;
    lastRegisterWritten = index;
  }
  
  public void drawRegisters(Graphics2D g2) {
    g2.setColor(Color.YELLOW);
    g2.fillRect(0, 0, 160, 495);
    g2.setColor(Color.BLUE);
    g2.fillRect(0, 0, 160, 15);
    g2.setColor(Color.YELLOW);
    GraphicsUtil.drawCenteredText(g2, S.get("Rv32imRegisterFile"), 80, 6);
    g2.setColor(Color.BLACK);
    g2.drawRect(0, 0, 160, 495);
    for (int i = 0 ; i < 32 ; i++) {
      GraphicsUtil.drawCenteredText(g2, "x"+i, 20, 21+i*15);
      g2.setColor(i==lastRegisterWritten ? Color.BLUE : Color.WHITE);
      g2.fillRect(40, 16+i*15, 78, 13);
      g2.setColor(Color.BLACK);
      g2.drawRect(40, 16+i*15, 78, 13);
      g2.setColor(i==lastRegisterWritten ? Color.WHITE : Color.BLUE);
      GraphicsUtil.drawCenteredText(g2, getRegisterValueHex(i), 79, 21+i*15);
      g2.setColor(Color.darkGray);
      GraphicsUtil.drawCenteredText(g2, registerABINames[i] , 140, 21+i*15);
      g2.setColor(Color.BLACK);
    }
  }
  
  public void drawProgramCounter(Graphics2D g2) {
    g2.setColor(Color.YELLOW);
    g2.fillRect(0, 0, 80, 30);
    g2.setColor(Color.BLUE);
    g2.fillRect(0, 0, 80, 15);
    g2.setColor(Color.YELLOW);
    GraphicsUtil.drawCenteredText(g2, S.get("Rv32imProgramCounter"), 40, 6);
    g2.setColor(Color.BLACK);
    g2.drawRect(0, 0, 80, 30);
    g2.setColor(Color.WHITE);
    g2.fillRect(1, 16, 78, 13);
    g2.setColor(Color.BLACK);
    g2.drawRect(1, 16, 78, 13);
    g2.setColor(Color.RED);
    GraphicsUtil.drawCenteredText(g2, String.format("0x%08X", pc), 40, 21);
  }
  
  public void drawTrace(Graphics2D g2) {
    g2.setColor(Color.YELLOW);
    g2.fillRect(0, 0, 415, 455);
    g2.setColor (Color.BLUE);
    g2.fillRect(0, 0, 415, 15);
    g2.setColor(Color.YELLOW);
    GraphicsUtil.drawCenteredText(g2, S.get("Rv32imExecutionTrace"), 207, 6);
    g2.setColor(Color.BLACK);
    g2.drawRect(0, 0, 415, 455);
    g2.setColor(Color.WHITE);
    g2.fillRect(5, 15, 80, 15);
    g2.fillRect(90, 15, 80, 15);
    g2.fillRect(175, 15, 235, 15);
    g2.setColor(Color.BLACK);
    GraphicsUtil.drawCenteredText(g2, S.get("Rv32imProgramCounter"), 45, 21);
    GraphicsUtil.drawCenteredText(g2, S.get("Rv32imBinInstruction"), 130, 21);
    GraphicsUtil.drawCenteredText(g2, S.get("Rv32imAsmInstruction"), 287, 21);
    if (instrTrace.isEmpty())
      GraphicsUtil.drawCenteredText(g2, S.get("Rv32imEmptyTrace"), 207, 250);
    else {
      int yOff = 30;
      for (TraceInfo t : instrTrace) {
        t.paint(g2, yOff);
        yOff += TRACEHEIGHT;
      }
    }
  }
  
  public void simButtonPressed() {
    simState.buttonPressed();
  }
  
  public void paint(int x , int y , Graphics2D g2, Instance inst, boolean visible) {
    Graphics2D g = (Graphics2D) g2.create();
    g.translate(x+Rv32im_riscv.upStateBounds.getX(), y+Rv32im_riscv.upStateBounds.getY());
    if (visible) {
      drawRegisters(g);
      g.translate(170, 0);
      drawProgramCounter(g);
      g.translate(0, 40);
      drawTrace(g);
    } else {
      g.setColor(Color.LIGHT_GRAY);
      g.fillRect(0, 0, Rv32im_riscv.upStateBounds.getWidth(), Rv32im_riscv.upStateBounds.getHeight());
      g.setColor(Color.BLACK);
      GraphicsUtil.drawCenteredText(g, S.get("SocHiddenForFasterSimulation"), Rv32im_riscv.upStateBounds.getWidth()/2, Rv32im_riscv.upStateBounds.getHeight()/2);
    }
    g.dispose();
    simState.paint(g2, x, y, Rv32im_riscv.simStateBounds);
  }

  @Override
  public void SimulationStateChanged() {
    if (attachedBus != null && attachedBus.getComponent() != null)
      ((InstanceComponent)attachedBus.getComponent()).getInstance().fireInvalidated();
  }

  @Override
  public void setEntryPointandReset(long entryPoint) {
    int entry = (int) entryPoint;
    if (attachedBus != null && attachedBus.getComponent() != null) {
      InstanceComponent comp = (InstanceComponent)attachedBus.getComponent();
      comp.getAttributeSet().setValue(RV32imAttributes.RESET_VECTOR, entry);
      reset();
      comp.getInstance().fireInvalidated();
    }
  }

  @Override
  public SocBusTransaction insertTransaction(SocBusTransaction trans, boolean hidden) {
	if (hidden) trans.setAsHiddenTransaction();
	return attachedBus.getSocSimulationManager().initializeTransaction(trans, attachedBus.getBusId());
  }

}
