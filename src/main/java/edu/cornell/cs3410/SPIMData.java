package edu.cornell.cs3410;

import com.cburch.logisim.data.Value;
import com.cburch.logisim.instance.InstanceData;
import com.cburch.logisim.instance.InstanceState;
import com.cburch.logisim.instance.StdAttr;

import com.cburch.logisim.data.BitWidth;
import com.cburch.logisim.data.Value;

/**
 * This class handles internal state for SPIM 
 * regs[0 - 31] are in register file
 * regs[32] is PC
 * regs[33] is EPC 
 * regs[34] is CP0, reg[8]
 * regs[35] is CP0, reg[12]
 * regs[36] is CP0, reg[13]
 * regs[37] is CP0, reg[14]
 */
public class SPIMData implements InstanceData, Cloneable {
    /** Retrieves the state associated with this register in the circuit state,
     * generating the state if necessary.
     */
    private static final int N_REGS = 38;
    private static final int NUM_BITS = 5;
    private static final BitWidth WIDTH = BitWidth.create(32);
    private static final BitWidth DEPTH = BitWidth.create(NUM_BITS);

    private static final Value zero = Value.createKnown(WIDTH, 0);
    private static final Value xxxx = Value.createError(WIDTH);
    private static final Value zzzz = Value.createUnknown(WIDTH);
    
    public static SPIMData get(InstanceState state) {
        SPIMData ret = (SPIMData) state.getData();
        if (ret == null) {
            // If it doesn't yet exist, then we'll set it up with our default
            // values and put it into the circuit state so it can be retrieved
            // in future propagations.
            ret = new SPIMData(null, new Value[N_REGS]); 
            state.setData(ret);
        }
        return ret;
    }

    private Value lastClock;
    Value[] regs;

    private SPIMData(Value lastClock, Value[] regs) {
        this.lastClock = lastClock;
        this.regs = regs;
        this.regs[0] = zero;
        reset(zero);
    }

    public void reset(Value val) {
        for (int i = 1; i < N_REGS; i++) {
            regs[i] = val;
        }
    }

    @Override
    public SPIMData clone() {
        try {
            // Not sure this works if registers is an array...
            // But KWalsh did it and it seemed to work okay.
            return (SPIMData) super.clone();
        }
        catch(CloneNotSupportedException e) {
            return null;
        }
    }

    public boolean updateClock(Value newClock, Object trigger) {
        Value oldClock = lastClock;
        lastClock = newClock;
        if (trigger == null || trigger == StdAttr.TRIG_RISING) {
            return oldClock == Value.FALSE && newClock == Value.TRUE;
        }
        else if (trigger == StdAttr.TRIG_FALLING) {
            return oldClock == Value.TRUE && newClock == Value.FALSE;
        }
        else if (trigger == StdAttr.TRIG_HIGH) {
            return newClock == Value.TRUE;
        }
        else if (trigger == StdAttr.TRIG_LOW) {
            return newClock == Value.FALSE;
        }
        else {
            return oldClock == Value.FALSE && newClock == Value.TRUE;
        }
    }
}
