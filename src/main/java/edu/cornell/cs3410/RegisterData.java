package edu.cornell.cs3410;

import com.cburch.logisim.data.Value;
import com.cburch.logisim.instance.InstanceData;
import com.cburch.logisim.instance.InstanceState;
import com.cburch.logisim.instance.StdAttr;

import static edu.cornell.cs3410.RegisterUtils.*;

/**
 * This class handles internal state for RegisterFile32.
 */
public class RegisterData implements InstanceData, Cloneable {
    /** Retrieves the state associated with this register in the circuit state,
     * generating the state if necessary.
     */
    public static RegisterData get(InstanceState state) {
        RegisterData ret = (RegisterData) state.getData();
        if (ret == null) {
            // If it doesn't yet exist, then we'll set it up with our default
            // values and put it into the circuit state so it can be retrieved
            // in future propagations.
            ret = new RegisterData(null, new Value[NUM_REGISTERS]);
            state.setData(ret);
        }
        return ret;
    }

    private Value lastClock;
    Value[] regs;

    private RegisterData(Value lastClock, Value[] regs) {
        this.lastClock = lastClock;
        this.regs = regs;
        this.regs[0] = zero;
        reset(zero);
    }

    public void reset(Value val) {
        for (int i = 1; i < NUM_REGISTERS; i++) {
            regs[i] = val;
        }
    }

    @Override
    public RegisterData clone() {
        try {
            // Not sure this works if registers is an array...
            // But KWalsh did it and it seemed to work okay.
            return (RegisterData) super.clone();
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
