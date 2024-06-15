/*
 * Logisim-evolution - digital logic design tool and simulator
 * Copyright by the Logisim-evolution developers
 *
 * https://github.com/logisim-evolution/
 *
 * This is free software released under GNU GPLv3 license
 */

package com.cburch.logisim.riscv;

import com.cburch.logisim.data.BitWidth;
import com.cburch.logisim.data.Value;
import com.cburch.logisim.instance.InstanceData;
import com.cburch.logisim.instance.InstanceState;

/** Represents the state of a cpu. */
class rv32imData implements InstanceData, Cloneable {
  /** The last input values observed. */
  private Value lastClock;
  private long lastDataIn;

  /** Output values */
  private static final Value HiZ32 = Value.createUnknown(BitWidth.create(32));

  private Value address;          // value to be placed on the address bus;
  private Value outputData;       // value to be placed on data bus
  private int outputDataWidth;    // width of the data to be written in bytes (1,2,or 4)
  private Value memRead;
  private Value memWrite;

  /** Registers */
  private boolean fetching;

  private final ProgramCounter pc;
  private final InstructionRegister ir;
  private final IntegerRegisters x;

  // More To Do

  /** Constructs a state with the given values. */
  public rv32imData(Value lastClock, long resetAddress) {

    // initial values for registers
    this.lastClock = lastClock;
    this.pc = new ProgramCounter(resetAddress);
    this.ir = new InstructionRegister(0x13); // Initial value 0x13 is opcode for addi x0,x0,0 (nop)
    this.x = new IntegerRegisters();

    // In the first clock cycle we are fetching the first instruction
    fetchNextInstruction();

  }

  /**
   * Set up outputs to fetch next instruction
   */
   private void fetchNextInstruction()
   {
     fetching = true;

     // Values for outputs fetching instruction
     address = Value.createKnown(32,pc.get());
     outputData = HiZ32;     // The output data bus is in High Z
     outputDataWidth = 4;    // all 4 bytes of the output
     memRead = Value.TRUE;  //  MemRead active
     memWrite = Value.FALSE; // MemWrite not active
   }

  /**
   * Retrieves the state associated with this counter in the circuit state, generating the state if
   * necessary.
   */
  public static rv32imData get(InstanceState state) {
    rv32imData ret = (rv32imData) state.getData();
    if (ret == null) {
      // If it doesn't yet exist, then we'll set it up with our default
      // values and put it into the circuit state so it can be retrieved
      // in future propagations.
      ret = new rv32imData(null, state.getAttributeValue(rv32im.ATTR_RESET_ADDR));
      state.setData(ret);
    }
    return ret;
  }

  /** Returns a copy of this object. */
  @Override
  public Object clone() {
    // We can just use what super.clone() returns: The only instance
    // variables are
    // Value objects, which are immutable, so we don't care that both the
    // copy
    // and the copied refer to the same Value objects. If we had mutable
    // instance
    // variables, then of course we would need to clone them.
    try {
      return super.clone();
    } catch (CloneNotSupportedException e) {
      return null;
    }
  }

  /** Updates the last clock observed, returning true if triggered. */
  public boolean updateClock(Value value) {
    Value old = lastClock;
    lastClock = value;
    return old == Value.FALSE && value == Value.TRUE;
  }

  /** reset CPU state to initial values */
  public void reset(long pcInit) {
     pc.set(pcInit);
  }

  /** update CPU state (execute) */
  public void update(long dataIn) {

    lastDataIn = dataIn;

    if(fetching) { ir.set(dataIn); }

    switch(ir.opcode()) {
      case 0x13:  // I-type arithmetic instruction
        ArithmeticInstruction.executeImmediate(this);
        pc.increment();
        fetchNextInstruction();
        break;
      case 0x33:  // R-type arithmetic instruction
      case 0x03:  // load instruction (I-type)
      case 0x23:  // store instruction (S-type)
      case 0x63:  // branch instruction (B-type)
      case 0x6F:  // Jump And Link (J-Type)
      case 0x67:  // Jump And Link Reg (I-Type)
      case 0x37:  // Load Upper Immediate (U-type)
      case 0x17:  // Add Upper Immediate to PC (U-type)
      case 0x73:  // System instructions
      default: // Unknown instruction: should cause an exception
        pc.increment();
        fetchNextInstruction();
    }

  }

  public Value getAddress() { return address; }
  public Value getOutputData() { return outputData; }
  public long getOutputDataWidth() { return outputDataWidth; }
  public Value getMemRead() { return memRead; }
  public Value getMemWrite() { return memWrite;  }
  /** get last value of dataIn */
  public long getLastDataIn() { return lastDataIn; }
  public ProgramCounter getPC() { return pc; }
  public InstructionRegister getIR() { return ir; }
  public long getX(int index) { return x.get(index); }
  public void setX(int index, long value) { x.set(index,value); }
}
