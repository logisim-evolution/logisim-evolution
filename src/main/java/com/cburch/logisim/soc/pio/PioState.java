/*
 * Logisim-evolution - digital logic design tool and simulator
 * Copyright by the Logisim-evolution developers
 *
 * https://github.com/logisim-evolution/
 *
 * This is free software released under GNU GPLv3 license
 */

package com.cburch.logisim.soc.pio;

import com.cburch.logisim.data.AttributeOption;
import com.cburch.logisim.data.BitWidth;
import com.cburch.logisim.data.Value;
import com.cburch.logisim.instance.InstanceComponent;
import com.cburch.logisim.instance.InstanceData;
import com.cburch.logisim.instance.InstanceState;
import com.cburch.logisim.soc.data.SocBusInfo;
import com.cburch.logisim.soc.data.SocBusSlaveInterface;
import com.cburch.logisim.soc.data.SocBusSlaveListener;
import com.cburch.logisim.soc.data.SocBusTransaction;
import com.cburch.logisim.soc.data.SocSupport;
import com.cburch.logisim.util.StringUtil;
import java.util.ArrayList;

public class PioState implements SocBusSlaveInterface {

  public class PioRegState implements InstanceData, Cloneable {
    public int outputRegister;
    public int captureRegister;
    public int directionRegister;
    public int interruptMask;
    public boolean oldIrq;
    public boolean oldIrqValid;
    private int oldInputs;
    private boolean oldInputsValid;

    public PioRegState() {
      reset();
    }

    @Override
    public PioRegState clone() {
      try {
        return (PioRegState) super.clone();
      } catch (CloneNotSupportedException e) {
        return null;
      }
    }

    public void updateCaptureRegister(int newInputs) {
      if (oldInputsValid && newInputs == oldInputs)
        return;
      if (oldInputsValid && inputIsCapturedSynchronisely()) {
        for (int i = 0; i < nrOfIOs.getWidth(); i++) {
          int oldi = (oldInputs >> i) & 1;
          int newi = (newInputs >> i) & 1;
          if (edgeCaptureType == PioAttributes.CAPT_ANY) {
            if (oldi != newi)
              captureRegister |= 1 << i;
          } else if (edgeCaptureType == PioAttributes.CAPT_RISING) {
            if (oldi == 0 && newi == 1)
              captureRegister |= 1 << i;
          } else if (oldi == 1 && newi == 0)
            captureRegister |= 1 << i;
        }
      }
      oldInputsValid = true;
      oldInputs = newInputs;
    }

    public void reset() {
      outputRegister = outputResetValue;
      captureRegister = 0;
      directionRegister = 0;
      oldInputs = 0;
      oldInputsValid = false;
      interruptMask = 0;
      oldIrq = false;
      oldIrqValid = false;
    }
  }

  private static final int DATA_REG_INDEX  = 0x0;
  private static final int DIR_REG_INDEX   = 0x4;
  private static final int IRQ_MASK_INDEX  = 0x8;
  private static final int EDGE_CAPT_INDEX = 0xC;
  private static final int OUT_SET_INDEX   = 0x10;
  private static final int OUT_CLEAR_INDEX = 0x14;

  private BitWidth nrOfIOs = BitWidth.create(1);
  private String label = "";
  private final SocBusInfo attachedBus = new SocBusInfo("");
  private Integer startAddress = 0;
  private final ArrayList<SocBusSlaveListener> listeners = new ArrayList<>();
  private AttributeOption direction = PioAttributes.PORT_INPUT;
  private Integer outputResetValue = 0;
  private Boolean outputEnableBitManipulations = false;
  private Boolean inputSynchronousCapture = false;
  private AttributeOption edgeCaptureType = PioAttributes.CAPT_RISING;
  private Boolean inputCaptBitClearing = false;
  private Boolean inputGeneratesIrq = false;
  private AttributeOption irqType = PioAttributes.IRQ_LEVEL;


  public void copyInto(PioState d) {
    d.nrOfIOs = nrOfIOs;
    d.label = label;
    d.attachedBus.setBusId(attachedBus.getBusId());
    d.startAddress = startAddress;
    d.direction = direction;
    d.outputResetValue = outputResetValue;
    d.outputEnableBitManipulations = outputEnableBitManipulations;
    d.inputSynchronousCapture = inputSynchronousCapture;
    d.edgeCaptureType = edgeCaptureType;
    d.inputCaptBitClearing = inputCaptBitClearing;
    d.inputGeneratesIrq = inputGeneratesIrq;
    d.irqType = irqType;
  }

  public BitWidth getNrOfIOs() {
    return nrOfIOs;
  }

  public String getLabel() {
    return label;
  }

  public SocBusInfo getAttachedBus() {
    return attachedBus;
  }

  public AttributeOption getPortDirection() {
    return direction;
  }

  public Integer getOutputResetValue() {
    return outputResetValue;
  }

  public Boolean outputSupportsBitManipulations() {
    return outputEnableBitManipulations;
  }

  public Boolean inputIsCapturedSynchronisely() {
    return inputSynchronousCapture;
  }

  public AttributeOption getInputCaptureEdge() {
    return edgeCaptureType;
  }

  public Boolean inputCaptureSupportsBitClearing() {
    return inputCaptBitClearing;
  }

  public Boolean inputGeneratesIrq() {
    return inputGeneratesIrq && direction != PioAttributes.PORT_OUTPUT;
  }

  public AttributeOption getIrqType() {
    return (inputGeneratesIrq() && inputIsCapturedSynchronisely())
        ? irqType
        : PioAttributes.IRQ_LEVEL;
  }

  public boolean setNrOfIOs(BitWidth nr) {
    if (nr.getWidth() == nrOfIOs.getWidth())
      return false;
    nrOfIOs = nr;
    return true;
  }

  public boolean setLabel(String l) {
    if (label.equals(l))
      return false;
    label = l;
    fireNameChanged();
    return true;
  }

  public boolean setAttachedBus(SocBusInfo i) {
    if (attachedBus.getBusId().equals(i.getBusId()))
      return false;
    attachedBus.setBusId(i.getBusId());
    return true;
  }

  public boolean setStartAddress(Integer addr) {
    if (startAddress.equals(addr)) return false;
    startAddress = addr;
    firememMapChanged();
    return true;
  }

  public boolean setPortDirection(AttributeOption dir) {
    if (direction == dir)
      return false;
    direction = dir;
    return true;
  }

  public boolean setOutputResetValue(Integer val) {
    if (outputResetValue.equals(val)) return false;
    outputResetValue = val;
    return true;
  }

  public boolean setOutputBitManupulations(Boolean b) {
    if (outputEnableBitManipulations.equals(b)) return false;
    outputEnableBitManipulations = b;
    return true;
  }

  public boolean setInputSynchronousCapture(Boolean b) {
    if (inputSynchronousCapture.equals(b)) return false;
    inputSynchronousCapture = b;
    return true;
  }

  public boolean setInputCaptureEdge(AttributeOption val) {
    if (edgeCaptureType == val)
      return false;
    edgeCaptureType = val;
    return true;
  }

  public boolean setInputCaptureBitClearing(Boolean b) {
    if (inputCaptBitClearing.equals(b)) return false;
    inputCaptBitClearing = b;
    return true;
  }

  public boolean setIrqGeneration(Boolean b) {
    if (inputGeneratesIrq.equals(b)) return false;
    inputGeneratesIrq = b;
    return true;
  }

  public boolean setIrqType(AttributeOption val) {
    if (irqType == val)
      return false;
    irqType = val;
    return true;
  }

  public int handleOperations(InstanceState state, boolean captureOnly) {
    PioState.PioRegState regs = (PioState.PioRegState) state.getData();
    if (regs == null) {
      regs = new PioRegState();
      state.setData(regs);
    }
    if (state.getPortValue(SocPio.RESET_INDEX) == Value.TRUE)
      regs.reset();
    int index = inputGeneratesIrq() ? 2 : 1;
    int nrOfBits = nrOfIOs.getWidth();
    int outputStart = index + (direction == PioAttributes.PORT_INOUT ? nrOfIOs.getWidth() : 0);
    int inputs = 0;
    for (int i = 0; i < nrOfBits; i++) {
      if (state.getPortValue(index + i) == Value.TRUE)
        inputs |= 1 << i;
    }
    if (inputGeneratesIrq()) {
      int irqSource = irqType == PioAttributes.IRQ_LEVEL ? inputs : regs.captureRegister;
      int irqs = irqSource & regs.interruptMask;
      boolean isIrq = irqs != 0;
      if (!regs.oldIrqValid || isIrq != regs.oldIrq)
        state.setPort(SocPio.IRQ_INDEX, isIrq ? Value.TRUE : Value.FALSE, 10);
      regs.oldIrqValid = true;
      regs.oldIrq = isIrq;
    }
    if (captureOnly) return inputs;
    regs.updateCaptureRegister(inputs);
    if (direction != PioAttributes.PORT_INPUT) {
      for (int i = 0; i < nrOfBits; i++) {
        Value val = (regs.outputRegister & (1 << i)) != 0 ? Value.TRUE : Value.FALSE;
        boolean isOutput =
            direction != PioAttributes.PORT_BIDIR || ((regs.directionRegister >> i) & 1) != 0;
        if (isOutput)
          state.setPort(outputStart + i, val, 10);
      }
    }
    return 0;
  }

  private PioRegState getRegPropagateState() {
    return (PioRegState) attachedBus.getSocSimulationManager().getdata(attachedBus.getComponent());
  }

  private InstanceState getPropagateState() {
    return attachedBus.getSocSimulationManager().getState(attachedBus.getComponent());
  }

  private void handleOutputWriteTransaction(SocBusTransaction trans) {
    if (direction == PioAttributes.PORT_INPUT)
      trans.setError(SocBusTransaction.READ_ONLY_ACCESS_ERROR);
    else {
      PioRegState pdata = getRegPropagateState();
      pdata.outputRegister = trans.getWriteData();
      handleOperations(getPropagateState(), false);
    }
  }

  private void handleInputReadTransaction(SocBusTransaction trans) {
    if (direction == PioAttributes.PORT_OUTPUT)
      trans.setError(SocBusTransaction.WRITE_ONLY_ACCESS_ERROR);
    else
      trans.setReadData(handleOperations(getPropagateState(), true));
  }

  private void handleDirectionRegister(SocBusTransaction trans) {
    if (direction != PioAttributes.PORT_BIDIR) {
      trans.setError(SocBusTransaction.REGISTER_DOES_NOT_EXIST_ERROR);
      return;
    }
    PioRegState s = getRegPropagateState();
    if (trans.isReadTransaction())
      trans.setReadData(s.directionRegister);
    if (trans.isWriteTransaction())
      s.directionRegister = trans.getWriteData();
  }

  private void handleIrqMaskRegister(SocBusTransaction trans) {
    if (!inputGeneratesIrq()) {
      trans.setError(SocBusTransaction.REGISTER_DOES_NOT_EXIST_ERROR);
      return;
    }
    PioRegState s = getRegPropagateState();
    if (trans.isReadTransaction())
      trans.setReadData(s.interruptMask);
    else
      s.interruptMask = trans.getWriteData();
  }

  private void handleCaptureRegister(SocBusTransaction trans) {
    if (!inputIsCapturedSynchronisely()) {
      trans.setError(SocBusTransaction.REGISTER_DOES_NOT_EXIST_ERROR);
      return;
    }
    PioRegState s = getRegPropagateState();
    if (trans.isReadTransaction())
      trans.setReadData(s.captureRegister);
    if (trans.isWriteTransaction()) {
      if (inputCaptureSupportsBitClearing()) {
        int mask = ~trans.getWriteData();
        s.captureRegister &= mask;
      } else
        s.captureRegister = 0;
    }
  }

  private void handleOutputBitOperation(SocBusTransaction trans, boolean clear) {
    if (!outputSupportsBitManipulations()) {
      trans.setError(SocBusTransaction.REGISTER_DOES_NOT_EXIST_ERROR);
      return;
    }
    if (trans.isReadTransaction()) {
      trans.setError(SocBusTransaction.WRITE_ONLY_ACCESS_ERROR);
    }
    PioRegState s = getRegPropagateState();
    int mask = trans.getWriteData();
    if (clear) {
      mask ^= -1;
      s.outputRegister &= mask;
    } else {
      s.outputRegister |= mask;
    }
    handleOperations(getPropagateState(), false);
  }

  /* Here the SocBusSlave interface handles are defined */
  @Override
  public boolean canHandleTransaction(SocBusTransaction trans) {
    long addr = SocSupport.convUnsignedInt(trans.getAddress());
    long start = SocSupport.convUnsignedInt(startAddress);
    long end = start + 24L;
    return (addr >= start && addr < end);
  }

  @Override
  public void handleTransaction(SocBusTransaction trans) {
    if (!canHandleTransaction(trans))
      return;
    trans.setTransactionResponder(attachedBus.getComponent());
    long addr = SocSupport.convUnsignedInt(trans.getAddress());
    long start = SocSupport.convUnsignedInt(startAddress);
    int index = (int) (addr - start);
    if (trans.getAccessType() != SocBusTransaction.WORD_ACCESS) {
      trans.setError(SocBusTransaction.ACCESS_TYPE_NOT_SUPPORTED_ERROR);
      return;
    }
    switch (index) {
      case DATA_REG_INDEX -> {
        if (trans.isWriteTransaction())
          handleOutputWriteTransaction(trans);
        if (trans.isReadTransaction())
          handleInputReadTransaction(trans);
      }
      case DIR_REG_INDEX -> handleDirectionRegister(trans);
      case IRQ_MASK_INDEX -> handleIrqMaskRegister(trans);
      case EDGE_CAPT_INDEX -> handleCaptureRegister(trans);
      case OUT_SET_INDEX -> handleOutputBitOperation(trans, false);
      case OUT_CLEAR_INDEX -> handleOutputBitOperation(trans, true);
      default -> trans.setError(SocBusTransaction.MISALIGNED_ADDRESS_ERROR);
    }
  }

  @Override
  public Integer getStartAddress() {
    return startAddress;
  }

  @Override
  public Integer getMemorySize() {
    return 24;
  }

  @Override
  public String getName() {
    var name = "BUG: Unknown";
    if (attachedBus != null && attachedBus.getComponent() != null) {
      name = label;
      if (StringUtil.isNullOrEmpty(name)) {
        final var loc = attachedBus.getComponent().getLocation();
        name = String.format("%s@%d,%d", attachedBus.getComponent().getFactory().getDisplayName(), loc.getX(), loc.getY());
      }
    }
    return name;
  }

  @Override
  public void registerListener(SocBusSlaveListener l) {
    if (!listeners.contains(l))
      listeners.add(l);
  }

  @Override
  public void removeListener(SocBusSlaveListener l) {
    listeners.remove(l);
  }


  @Override
  public InstanceComponent getComponent() {
    if (attachedBus == null || attachedBus.getComponent() == null)
      return null;
    return (InstanceComponent) attachedBus.getComponent();
  }

  private void fireNameChanged() {
    for (SocBusSlaveListener l : listeners)
      l.labelChanged();
  }

  private void firememMapChanged() {
    for (SocBusSlaveListener l : listeners)
      l.memoryMapChanged();
  }

}
