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
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.event.WindowEvent;
import java.awt.event.WindowListener;
import java.util.LinkedList;

import javax.swing.JOptionPane;
import javax.swing.JPanel;

import com.cburch.logisim.circuit.CircuitState;
import com.cburch.logisim.circuit.ComponentDataGuiProvider;
import com.cburch.logisim.comp.Component;
import com.cburch.logisim.data.BitWidth;
import com.cburch.logisim.data.Bounds;
import com.cburch.logisim.data.Location;
import com.cburch.logisim.data.Value;
import com.cburch.logisim.instance.Instance;
import com.cburch.logisim.instance.InstanceComponent;
import com.cburch.logisim.instance.InstanceData;
import com.cburch.logisim.instance.InstanceState;
import com.cburch.logisim.instance.InstanceStateImpl;
import com.cburch.logisim.prefs.AppPreferences;
import com.cburch.logisim.soc.data.SocBusInfo;
import com.cburch.logisim.soc.data.SocBusTransaction;
import com.cburch.logisim.soc.data.SocProcessorInterface;
import com.cburch.logisim.soc.data.SocSupport;
import com.cburch.logisim.soc.data.SocUpSimulationState;
import com.cburch.logisim.soc.data.SocUpSimulationStateListener;
import com.cburch.logisim.util.GraphicsUtil;

public class RV32im_state implements SocUpSimulationStateListener,SocProcessorInterface {

  public class ProcessorState extends JPanel implements InstanceData,Cloneable,ComponentDataGuiProvider,
                                                        WindowListener {
	private static final long serialVersionUID = 1L;
	private int[] registers;
    private Boolean[] registers_valid;
    private int pc;
    private int lastRegisterWritten = -1;
    private LinkedList<TraceInfo> instrTrace;
    private Value lastClock;
    private SocUpSimulationState simState;
    private Instance myInstance;
    private boolean visible;
    
    public ProcessorState(Instance inst) {
      registers = new int[32];
      registers_valid = new Boolean[32];
      instrTrace = new LinkedList<TraceInfo>();
      lastClock = Value.createUnknown(BitWidth.ONE);
      simState = new SocUpSimulationState();
      myInstance = inst;
      this.setSize(AppPreferences.getScaled(Rv32im_riscv.upStateBounds.getWidth()), 
              AppPreferences.getScaled(Rv32im_riscv.upStateBounds.getHeight()));
      Rv32im_riscv.MENU_PROVIDER.registerCpuState(this, inst);
      visible = false;
      reset();
    }
    
    @Override
    public void paint(Graphics g) {
      draw( (Graphics2D) g, true);
    }
    public void reset() {
      pc = resetVector;
      for (int i = 1 ; i < 31 ; i++)
        registers_valid[i] = false;
      registers_valid[0] = true;
      lastRegisterWritten = -1;
      instrTrace.clear();
    }
    
    public void setClock(Value clock, CircuitState cState) {
      if (lastClock == Value.FALSE && clock == Value.TRUE)
        execute(cState);
      lastClock = clock;
    }

    public int getProgramCounter() {
      return pc;
    }
    
    public SocUpSimulationState getSimState() {
      return simState;
    }
    
    public void SimButtonPressed() {
      simState.buttonPressed();
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
      
    public void interrupt() {
      pc = exceptionVector;
    }
    
    public Component getMasterComponent() {
      return attachedBus.getComponent();
    }
    
    public void execute(CircuitState cState) {
      /* check the simulation state */
      if (!simState.canExecute())
        return;
      /* TODO: check interrupts */
      /* fetch an instruction */
      SocBusTransaction trans = new SocBusTransaction(SocBusTransaction.READTransaction,
      		pc,0,SocBusTransaction.WordAccess,attachedBus.getComponent());
      attachedBus.getSocSimulationManager().initializeTransaction(trans, attachedBus.getBusId(),cState);
      if (trans.hasError()) {
        JOptionPane.showMessageDialog(null,trans.getErrorMessage(),
              SocSupport.getMasterName(cState,RV32im_state.this.getName())+S.get("RV32imFetchTransaction"),JOptionPane.ERROR_MESSAGE);
        simState.errorInExecution();
        return;
      }
      /* decode instruction */
      int instruction = trans.getReadData(); 
      DECODER.decode(instruction);
      /* execute instruction */
      RV32imExecutionUnitInterface exe = DECODER.getExeUnit();
      lastRegisterWritten = -1;
      while (instrTrace.size() >= NrOfTraces)
        instrTrace.removeLast();
      if (exe == null) {
        JOptionPane.showMessageDialog(null,S.get("RV32imFetchInvalidInstruction"),
              SocSupport.getMasterName(cState,RV32im_state.this.getName())+S.get("RV32imFetchTransaction"),JOptionPane.ERROR_MESSAGE);
        simState.errorInExecution();
        instrTrace.addFirst(new TraceInfo(pc,instruction,S.get("RV32imFetchInvInstrAsm"),true));
        pc = pc + 4;
        if (visible) repaint();
        return;
      }
      TraceInfo trace = new TraceInfo(pc,instruction,exe.getAsmInstruction(),false);
      if (!exe.execute(this,cState)) {
        StringBuffer s = new StringBuffer();
        s.append(S.get("RV32imFetchExecutionError"));
        if (exe.getErrorMessage() != null)
          s.append("\n"+exe.getErrorMessage());
        JOptionPane.showMessageDialog(null,s.toString(),
              SocSupport.getMasterName(cState,RV32im_state.this.getName())+S.get("RV32imFetchTransaction"),JOptionPane.ERROR_MESSAGE);
        simState.errorInExecution();
        trace.setError();
        instrTrace.addFirst(trace);
        if (visible) repaint();
        return;
      }
      instrTrace.addFirst(trace);
      /* all done increment pc */
      if (!exe.performedJump())
        pc = pc+4;
      if (visible) repaint();
    }
      
    public ProcessorState clone() {
      try {
        return (ProcessorState) super.clone();
      } catch (CloneNotSupportedException e) {
        return null;
      }
    }
    
    public void insertTransaction(SocBusTransaction trans, boolean hidden, CircuitState cState) {
      if (hidden) trans.setAsHiddenTransaction();
        attachedBus.getSocSimulationManager().initializeTransaction(trans, attachedBus.getBusId(),cState);
    }
    
    public void drawRegisters(Graphics2D g, int x , int y, boolean scale) {
      Graphics2D g2 = (Graphics2D) g.create();
      Bounds bds;
      if (scale)
        g2.setFont(AppPreferences.getScaledFont(g.getFont()));
      g2.translate(x, y);
      int blockWidth = getBlockWidth(g2,scale);
      int blockX = ((scale ? AppPreferences.getScaled(160):160)-blockWidth)/2;
      if (scale) {
        blockWidth = AppPreferences.getDownScaled(blockWidth);
        blockX = AppPreferences.getDownScaled(blockX);
      }
      g2.setColor(Color.YELLOW);
      bds = RV32im_state.getBounds(0,0,160,495,scale);
      g2.fillRect(bds.getX(), bds.getY(), bds.getWidth(), bds.getHeight());
      g2.setColor(Color.BLUE);
      bds = RV32im_state.getBounds(0,0,160,15,scale);
      g2.fillRect(bds.getX(), bds.getY(), bds.getWidth(), bds.getHeight());
      g2.setColor(Color.YELLOW);
      bds = RV32im_state.getBounds(80,6,0,0,scale);
      GraphicsUtil.drawCenteredText(g2, S.get("Rv32imRegisterFile"), bds.getX(), bds.getY());
      g2.setColor(Color.BLACK);
      bds = RV32im_state.getBounds(0,0,160,495,scale);
      g2.drawRect(bds.getX(), bds.getY(), bds.getWidth(), bds.getHeight());
      for (int i = 0 ; i < 32 ; i++) {
    	bds = RV32im_state.getBounds(20,21+i*15,0,0,scale);
        GraphicsUtil.drawCenteredText(g2, "x"+i, bds.getX(), bds.getY());
        g2.setColor(i==lastRegisterWritten ? Color.BLUE : Color.WHITE);
        bds = RV32im_state.getBounds(blockX, 16+i*15, blockWidth, 13,scale);
        g2.fillRect(bds.getX(), bds.getY(), bds.getWidth(), bds.getHeight());
        g2.setColor(Color.BLACK);
        g2.drawRect(bds.getX(), bds.getY(), bds.getWidth(), bds.getHeight());
        g2.setColor(i==lastRegisterWritten ? Color.WHITE : Color.BLUE);
        bds = RV32im_state.getBounds(blockX+blockWidth/2, 21+i*15,0,0,scale);
        GraphicsUtil.drawCenteredText(g2, getRegisterValueHex(i), bds.getX(), bds.getY());
        g2.setColor(Color.darkGray);
        bds = RV32im_state.getBounds(140, 21+i*15,0,0,scale);
        GraphicsUtil.drawCenteredText(g2, registerABINames[i] , bds.getX(), bds.getY());
        g2.setColor(Color.BLACK);
      }
      g2.dispose();
    }
      
    public void drawProgramCounter(Graphics2D g, int x , int y, boolean scale) {
      Graphics2D g2 = (Graphics2D) g.create();
      Bounds bds;
      if (scale)
        g2.setFont(AppPreferences.getScaledFont(g.getFont()));
      bds = RV32im_state.getBounds(x,y,0,0,scale);
      g2.translate(bds.getX(), bds.getY());
      int blockWidth = getBlockWidth(g2,scale);
      if (scale)
        blockWidth = AppPreferences.getDownScaled(blockWidth);
      g2.setColor(Color.YELLOW);
      bds = RV32im_state.getBounds(0,0,blockWidth,30,scale);
      g2.fillRect(bds.getX(), bds.getY(), bds.getWidth(), bds.getHeight());
      g2.setColor(Color.BLUE);
      bds = RV32im_state.getBounds(0,0,blockWidth,15,scale);
      g2.fillRect(bds.getX(), bds.getY(), bds.getWidth(), bds.getHeight());
      g2.setColor(Color.YELLOW);
      bds = RV32im_state.getBounds(blockWidth/2,6,0,0,scale);
      GraphicsUtil.drawCenteredText(g2, S.get("Rv32imProgramCounter"), bds.getX(), bds.getY());
      g2.setColor(Color.BLACK);
      bds = RV32im_state.getBounds(0,0,blockWidth,30,scale);
      g2.drawRect(bds.getX(), bds.getY(), bds.getWidth(), bds.getHeight());
      g2.setColor(Color.WHITE);
      bds = RV32im_state.getBounds(1,16,blockWidth-2,13,scale);
      g2.fillRect(bds.getX(), bds.getY(), bds.getWidth(), bds.getHeight());
      g2.setColor(Color.BLACK);
      g2.drawRect(bds.getX(), bds.getY(), bds.getWidth(), bds.getHeight());
      g2.setColor(Color.RED);
      bds = RV32im_state.getBounds(blockWidth/2,21,0,0,scale);
      GraphicsUtil.drawCenteredText(g2, String.format("0x%08X", pc), bds.getX(), bds.getY());
      g2.dispose();
    }
      
    public void drawTrace(Graphics2D g, int x , int y, boolean scale) {
      Graphics2D g2 = (Graphics2D) g.create();
      Bounds bds;
      if (scale)
        g2.setFont(AppPreferences.getScaledFont(g.getFont()));
      int blockWidth = getBlockWidth(g2,scale);
      if (scale)
        blockWidth = AppPreferences.getDownScaled(blockWidth);
      bds = RV32im_state.getBounds(x,y,0,0,scale);
      g2.translate(bds.getX(), bds.getY());
      g2.setColor(Color.YELLOW);
      bds = RV32im_state.getBounds(0,0,415,455,scale);
      g2.fillRect(bds.getX(), bds.getY(), bds.getWidth(), bds.getHeight());
      g2.setColor (Color.BLUE);
      bds = RV32im_state.getBounds(0,0,415,15,scale);
      g2.fillRect(bds.getX(), bds.getY(), bds.getWidth(), bds.getHeight());
      g2.setColor(Color.YELLOW);
      bds = RV32im_state.getBounds(207,6,0,0,scale);
      GraphicsUtil.drawCenteredText(g2, S.get("Rv32imExecutionTrace"), bds.getX(), bds.getY());
      g2.setColor(Color.BLACK);
      bds = RV32im_state.getBounds(0,0,415,455,scale);
      g2.drawRect(bds.getX(), bds.getY(), bds.getWidth(), bds.getHeight());
      g2.setColor(Color.WHITE);
      bds = RV32im_state.getBounds(5,15,blockWidth,15,scale);
      g2.fillRect(bds.getX(), bds.getY(), bds.getWidth(), bds.getHeight());
      bds = RV32im_state.getBounds(10+blockWidth,15,blockWidth,15,scale);
      g2.fillRect(bds.getX(), bds.getY(), bds.getWidth(), bds.getHeight());
      bds = RV32im_state.getBounds(15+2*blockWidth,15,395-2*blockWidth,15,scale);
      g2.fillRect(bds.getX(), bds.getY(), bds.getWidth(), bds.getHeight());
      g2.setColor(Color.BLACK);
      bds = RV32im_state.getBounds(5+blockWidth/2,21,0,0,scale);
      GraphicsUtil.drawCenteredText(g2, S.get("Rv32imProgramCounter"), bds.getX(), bds.getY());
      bds = RV32im_state.getBounds(10+blockWidth+blockWidth/2,21,0,0,scale);
      GraphicsUtil.drawCenteredText(g2, S.get("Rv32imBinInstruction"), bds.getX(), bds.getY());
      bds = RV32im_state.getBounds(215+blockWidth,21,0,0,scale);
      GraphicsUtil.drawCenteredText(g2, S.get("Rv32imAsmInstruction"), bds.getX(), bds.getY());
      if (instrTrace.isEmpty()) {
        bds = RV32im_state.getBounds(207,250,0,0,scale);
        GraphicsUtil.drawCenteredText(g2, S.get("Rv32imEmptyTrace"), bds.getX(), bds.getY());
      } else {
        int yOff = 30;
        for (TraceInfo t : instrTrace) {
          t.paint(g2, yOff,scale);
          yOff += TRACEHEIGHT;
        }
      }
      g2.dispose();
    }
    
    public void draw(Graphics2D g, boolean scale) {
      drawRegisters(g,0,0,scale);
      drawProgramCounter(g,170,0,scale);
      drawTrace(g,170,40,scale);
    }

    @Override
    public void destroy() {
      Rv32im_riscv.MENU_PROVIDER.deregisterCpuState(this, myInstance);
    }

	@Override
	public void windowOpened(WindowEvent e) {repaint(); visible = true;}

	@Override
	public void windowClosing(WindowEvent e) {visible = false;}

	@Override
	public void windowClosed(WindowEvent e) {}

	@Override
	public void windowIconified(WindowEvent e) {visible = false;}

	@Override
	public void windowDeiconified(WindowEvent e) {repaint(); visible = true;}

	@Override
	public void windowActivated(WindowEvent e) {visible = true;}

	@Override
	public void windowDeactivated(WindowEvent e) {}
      
  }

  private int resetVector;
  private int exceptionVector;
  private int nrOfIrqs;
  private String label;
  private SocBusInfo attachedBus;
  
  private static final RV32imDecoder DECODER = new RV32imDecoder(); 
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
    
    public void paint(Graphics2D g , int yOffset , boolean scale) {
      int blockWidth = getBlockWidth(g,scale);
      if (scale)
        blockWidth = AppPreferences.getDownScaled(blockWidth);
      int xOff = 5;
      paintBox(g,xOff,yOffset,pc, scale, blockWidth);
      xOff += blockWidth+5;
      paintBox(g,xOff,yOffset,instruction, scale, blockWidth);
      xOff += blockWidth+5;
      g.setColor(error ? Color.RED : Color.BLACK);
      Font f = g.getFont();
      Font myFont = scale ? AppPreferences.getScaledFont(new Font( "Monospaced", Font.PLAIN, 12 ).deriveFont(Font.BOLD)) :
                            new Font( "Monospaced", Font.PLAIN, 12 ).deriveFont(Font.BOLD);
      g.setFont(myFont);
      Bounds bds = RV32im_state.getBounds(xOff,yOffset+15,0,0,scale);
      g.drawString(asm, bds.getX(), bds.getY());
      g.setFont(f);
    }
    
    private void paintBox(Graphics2D g, int x , int y , int value , boolean scale , int blockWidth) {
      g.setColor(Color.WHITE);
      Bounds bds;
      bds = getBounds(x, y+1, blockWidth, TRACEHEIGHT-2,scale);
      g.fillRect(bds.getX(), bds.getY(), bds.getWidth(), bds.getHeight());
      g.setColor(Color.BLACK);
      g.drawRect(bds.getX(), bds.getY(), bds.getWidth(), bds.getHeight());
      g.setColor(error ? Color.RED : Color.DARK_GRAY);
      bds = RV32im_state.getBounds(x+blockWidth/2, y+TRACEHEIGHT/2,0,0,scale);
      GraphicsUtil.drawCenteredText(g, String.format("0x%08X", value), bds.getX(), bds.getY());
      g.setColor(Color.BLACK);
    }
  }
  
  public RV32im_state() {
    resetVector = 0;
    exceptionVector = 0x14;
    nrOfIrqs = 0;
    label = "";
    attachedBus = new SocBusInfo("");
  }

  public void copyInto(RV32im_state dest) {
    dest.resetVector = resetVector;
    dest.exceptionVector = exceptionVector;
    dest.nrOfIrqs = nrOfIrqs;
    dest.label = label;
    dest.attachedBus.setBusId(attachedBus.getBusId());
  }

  public String getName() {
    String name = label;
    if (name == null || name.isEmpty()) {
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
  
  public ProcessorState getNewState(Instance inst) {
    return new ProcessorState(inst);
  }
  
  public ProcessorState getRegState() {
    InstanceComponent comp = (InstanceComponent) attachedBus.getComponent();
    if (comp == null)
      return null;
    InstanceState state = comp.getInstanceStateImpl();
    if (state == null)
      return null;
    return (ProcessorState) state.getData();
  }
	  
  public void paint(int x , int y , Graphics2D g2, Instance inst, boolean visible, InstanceData pstate) {
    Graphics2D g = (Graphics2D) g2.create();
    g.translate(x+Rv32im_riscv.upStateBounds.getX(), y+Rv32im_riscv.upStateBounds.getY());
    ProcessorState state = (ProcessorState) pstate;
    if (visible&&state != null) {
      state.draw(g,false);
    } else {
      g.setColor(Color.LIGHT_GRAY);
      g.fillRect(0, 0, Rv32im_riscv.upStateBounds.getWidth(), Rv32im_riscv.upStateBounds.getHeight());
      g.setColor(Color.BLACK);
      GraphicsUtil.drawCenteredText(g, S.get("SocHiddenForFasterSimulation"), Rv32im_riscv.upStateBounds.getWidth()/2, Rv32im_riscv.upStateBounds.getHeight()/2);
    }
    g.dispose();
    if (state != null) state.simState.paint(g2, x, y, Rv32im_riscv.simStateBounds);
  }

  private static Bounds getBounds(int x , int y , int width , int height, boolean scale) {
    if (scale)
      return Bounds.create(AppPreferences.getScaled(x), AppPreferences.getScaled(y), 
              AppPreferences.getScaled(width), AppPreferences.getScaled(height));
    return Bounds.create(x, y, width, height);
  }

  private static int getBlockWidth(Graphics2D g2,boolean scale) {
    FontMetrics f =g2.getFontMetrics();
    int StrWidth = f.stringWidth("0x00000000")+(scale ? AppPreferences.getScaled(2) : 2);
    int blkPrefWidth = scale ? AppPreferences.getScaled(80) : 80;
    int blockWidth = StrWidth < blkPrefWidth ? blkPrefWidth : StrWidth;
    return blockWidth;
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
      if (getRegState() != null)
        getRegState().reset();
      comp.getInstance().fireInvalidated();
    }
  }

  @Override
  public void insertTransaction(SocBusTransaction trans, boolean hidden, CircuitState cState) {
	if (hidden) trans.setAsHiddenTransaction();
    if (cState == null) {
      InstanceComponent comp = (InstanceComponent) attachedBus.getComponent();
      if (comp == null)
        return;
      InstanceStateImpl state = comp.getInstanceStateImpl();
      if (state == null)
        return;
      cState = state.getProject().getCircuitState();
    }
	attachedBus.getSocSimulationManager().initializeTransaction(trans, attachedBus.getBusId(),cState);
  }

}
