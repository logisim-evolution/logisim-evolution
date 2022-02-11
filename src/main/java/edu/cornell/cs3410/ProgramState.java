package edu.cornell.cs3410;

import com.cburch.logisim.data.BitWidth;
import com.cburch.logisim.data.Value;
import com.cburch.logisim.instance.Instance;
import com.cburch.logisim.instance.InstanceData;
import com.cburch.logisim.instance.InstanceState;
import com.cburch.logisim.proj.Project;

import edu.cornell.cs3410.ProgramAssembler.Listing;

public class ProgramState implements InstanceData, Cloneable {
    Listing code;
    int pc;
    private Project proj;
    private Instance instance;
    private static final int PC_UNDEFINED = -1;
    private static final int PC_ERROR = -2;
	private static final BitWidth OP_WIDTH = BitWidth.create(32);

    public static ProgramState get(InstanceState state, Listing code) {
        ProgramState ret = (ProgramState) state.getData();
        Instance instance = state.getInstance();
        if (ret == null) {
            ret = new ProgramState(code);
            state.setData(ret);
        }
        if (ret.code != code) {
            ret.code = code;
            code.setListener(ret);
        }
        if (ret.instance != instance) {
            ret.instance = instance;
        }
        return ret;
    }

    private ProgramState(Listing code) {
        this.code = code;
        if (code != null) {
            code.setListener(this);
        }
        pc = PC_UNDEFINED;
    }

    public Project getProject() { return proj; }
    public void setProject(Project p) { proj = p; }

    public void codeChanged() {
        if (instance != null) {
            instance.fireInvalidated();
        }
    }

    String decode(int i) {
        return ProgramAssembler.disassemble(code.instr(i), 4*i);
    }

    Value instr() {
        if (code == null) {
            return Value.createKnown(OP_WIDTH, 0);
        }
        else if (isValidPC()) {
            return Value.createKnown(OP_WIDTH, code.instr(pc));
        }
        return Value.createError(OP_WIDTH);
    }

    boolean haveCodeFor(int i) {
        return code.segmentOf(i) != null;
    }
    boolean isValidPC() {
        return pc >= 0;
    }
    boolean isUndefinedPC() {
        return pc == PC_UNDEFINED;
    }
    boolean isErrorPC() {
        return pc == PC_ERROR;
    }

    @Override
    public ProgramState clone() {
        try {
            return (ProgramState) super.clone();
        }
        catch (CloneNotSupportedException e) {
            return null;
        }
    }

    public void update(Value pc_in) {
        if (pc_in.isErrorValue()) {
            pc = PC_ERROR;
        }
        else if (!pc_in.isFullyDefined()) {
            pc = PC_UNDEFINED;
        }
        else if ((pc_in.toIntValue() & 3) != 0) {
            pc = PC_ERROR;
        }
        else {
            pc = pc_in.toIntValue() >>> 2;
        }
    }
}
