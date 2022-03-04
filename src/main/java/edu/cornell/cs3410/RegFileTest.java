package edu.cornell.cs3410;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.List;
import java.util.Queue;
import java.util.LinkedList;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.cburch.logisim.circuit.Circuit;
import com.cburch.logisim.circuit.CircuitState;
import com.cburch.logisim.circuit.Propagator;
import com.cburch.logisim.circuit.Resetter;
import com.cburch.logisim.circuit.SubcircuitFactory;
import com.cburch.logisim.comp.Component;
import com.cburch.logisim.data.BitWidth;
import com.cburch.logisim.data.Value;
import com.cburch.logisim.file.LoadFailedException;
import com.cburch.logisim.instance.InstanceState;
import com.cburch.logisim.proj.Project;
import com.cburch.logisim.proj.ProjectActions;
import com.cburch.logisim.std.wiring.Clock;

import edu.cornell.cs3410.Program32;
import edu.cornell.cs3410.ProgramAssembler.Listing;
import edu.cornell.cs3410.ProgramState;
import edu.cornell.cs3410.RegisterFile32;
import edu.cornell.cs3410.RegisterData;
import static edu.cornell.cs3410.RegisterUtils.NUM_REGISTERS;

/**
 * RegFileTest, an automated tester for sequential circuits based on RF state.
 *
 * @author Peter Tseng
 * (edited later by Favian Contreras)
 * (edited Jan 17, 2019 by Pablo Fiori)
 * (edited Jan 16, 2020 by Daniel Weber)
 * (edited Jan 25, 2021 by Randy Zhou)
 */
public class RegFileTest {

    private static final BitWidth REG_WIDTH = BitWidth.create(32);
    // In general, this looks like:
    // ## desc = a cool description
    // ## expect[10] = 0x1337
    private static final Pattern CMD_PATTERN = Pattern.compile(
        "^##\\s*(\\w+)(\\[([0-9]+)\\])?\\s*=\\s*(.*\\S)\\s*$"
    );

    public static int runTest(String test, TestCircuit circ, boolean print) throws LoadFailedException, IOException {
        // If there ever comes a day when these are not inited to all 0's,
        // these two lines will need to be changed.
        int[] starting = new int[32];
        int[] expected = new int[32];

        String description = null;
        int cycles = 0;

        // Read test file, parsing out special directives along the way.
        BufferedReader in = null;
        try {
            in = new BufferedReader(new FileReader(test));
        } catch(FileNotFoundException e) {
            System.out.println("No file: "+test);
            return -1;
        }
        String line;
        StringBuffer buf = new StringBuffer();
        while ((line = in.readLine()) != null) {
            // System.out.println(line);
            buf.append(line+"\n");
            Matcher matcher = CMD_PATTERN.matcher(line);
            if (matcher.find()) {
                String cmd = matcher.group(1).toLowerCase();
                String idx = matcher.group(3);
                String val = matcher.group(4);
                if (cmd.startsWith("desc")) {
                    description = val;
                }
                else if (cmd.startsWith("cycles")) {
                    cycles = Integer.parseInt(val);
                }
                else if (cmd.startsWith("expect")) {
                    expected[Integer.parseInt(idx)] = parse(val);
                }
                else if (cmd.startsWith("start")) {
                    starting[Integer.parseInt(idx)] = parse(val);
                }
                else if (cmd.startsWith("init")) {
                    starting[Integer.parseInt(idx)] = parse(val);
                }
                else {
                    String err = "Unrecognized directive " + cmd;
                    throw new IllegalArgumentException(err);
                }
            }
        }

        if (description == null) {
            throw new IllegalArgumentException("No description");
        }
        if (cycles <= 0) {
            throw new IllegalArgumentException("No cycle count");
        }

        int[] actual = null;
        try {
            actual = circ.runTest(buf.toString(), cycles, starting);
        }
        catch (Exception e) {
            // Why doesn't this exit normally?
            // e.printStackTrace();
            System.out.printf("[Exception] %s\n", description);
            System.out.printf("    Error message: " + e.getMessage() + "\n");
            return -1;
            //System.exit(-1);
        }

        int errorsHere;
        // Circuit testing
        errorsHere = countDiffs(expected, actual);

        if (print) {
            System.out.printf("[%s] %s\n", count(errorsHere), description);
            printDiffs(expected, actual);
        }

        return errorsHere;
    }

    /**
     * Runs tests.
     *
     * @param args First argument = .circ file, other arguments = test files.
     */
    public static void runRiscTest(String test, String file) throws LoadFailedException, IOException {

        Project proj = ProjectActions.doOpenNoWindow(null, new File(file));

        TestCircuit circ = null;
        try {
            circ = new TestCircuit(proj);
        }
        catch (Exception e) {
            // Why doesn't this exit normally?
            e.printStackTrace();
            System.exit(-1);
        }

        int errors = runTest(test, circ, true);
        int testsPassed = (errors == 0 ? 1 : 0);

        System.out.println("TOTAL: " + count(errors));
        System.out.printf("Tests with no errors: %d/%d\n", testsPassed, 1);
        System.exit(0);
    }

    /**
     * Holds info about a circuit to be tested.
     */
    private static class TestCircuit {
        private final CircuitState state;
        private final InstanceState registerFileState;
        private final InstanceState programRomState;

        /**
         * Constructs a TestCircuit.
         *
         * @param proj Project file of circuit
         * @throws IllegalArgumentException if there is not exactly one register file or program ROM
         */
        private TestCircuit(Project proj) {
            // We need to use their main processor's CircuitState.
            // Try in this order: RISCV32, RISCV, or main circuit.
            // Ask students to submit their processors with this naming!
            Circuit main;
            Circuit riscv = proj.getLogisimFile().getCircuit("RISCV");
            Circuit riscv32 = proj.getLogisimFile().getCircuit("RISCV32");
            Circuit defaultmain = proj.getLogisimFile().getCircuit("main");

            // What, why do they have both?
            if (riscv != null && riscv32 != null) {
                throw new IllegalArgumentException("Warning: Had both RISCV and RISCV32 subcircuits. Ensure that only one processor of either name has been implemented. You will be docked ~15 points if we have to manually fix this after grading.");
            }

            if (riscv32 != null) {
                state = proj.getCircuitState(riscv32);
                main = riscv32;
                proj.setCurrentCircuit(riscv32);
            }
            else if (riscv != null) {
                state = proj.getCircuitState(riscv);
                main = riscv;
                proj.setCurrentCircuit(riscv);
            }
            else if (defaultmain != null) {
                System.err.println("Warning: circuit is named main, not RISCV nor RISCV32. This is mostly fine, but make sure your \"main\" circuit is the processor you are submitting (you will be docked ~15 points if not and we have to fix it manually).");
                state = proj.getCircuitState(defaultmain);
                main = defaultmain;
                proj.setCurrentCircuit(defaultmain);
            }
            // this case only works on their currently active circuit. If this
            // active circuit is not the processor, then subsequent grading
            // will fail...
            else {
                System.err.println("Warning: no RISCV, RISCV32 or main subcircuit. If our tests fail, you will be docked ~15 points for us to fix this even if there is a valid processor in a different subcircuit.");
                state = proj.getCircuitState();
                main = proj.getCurrentCircuit();
            }


            Circuit registerCircuit = null;
            Component registerFile = null;
            Component programRom = null;
            boolean haveClock = false;

            for (Circuit c : proj.getLogisimFile().getCircuits()) {
                for (Component x : c.getNonWires()) {
                    if (x.getFactory() instanceof RegisterFile32) {
                        if (registerFile != null) {
                            throw new IllegalArgumentException("More than one register file");
                        }
                        registerCircuit = c;
                        registerFile = x;
                    }
                    else if (x.getFactory() instanceof Program32) {
                        if (programRom != null) {
                            throw new IllegalArgumentException("More than one program ROM");
                        }
                        programRom = x;
                    }
                    else if (x.getFactory() instanceof Clock) {
                        haveClock = true;
                    }
                }
            }

            if (registerFile == null) {
                throw new IllegalArgumentException("No register file");
            }
            if (programRom == null) {
                throw new IllegalArgumentException("No program ROM");
            }
            if (!haveClock) {
                throw new IllegalArgumentException("No clock");
            }

            // If their register file is tucked away in a subcircuit,
            // we need to do some legwork to get the right InstanceState for it.
            if (registerCircuit == main) {
                registerFileState = state.getInstanceState(registerFile);
            }
            else {
                // We're going to hope it's only nested one deep?
                StateLevel stateLevel = findRegisterFileLevel(state, registerCircuit);
                if (stateLevel != null) {
                    if (stateLevel.level > 1) {
                        System.err.println("Warning: Register file is " + stateLevel.level + " levels deep (more than 1)!");
                    }
                }
                else {
                    throw new IllegalArgumentException("Register file not actually in circuit?");
                }

                registerFileState = stateLevel.state.getInstanceState(registerFile);
            }

            programRomState = state.getInstanceState(programRom);
        }

        /**
         * Helper class to keep track of a circuit state and its relative level
         */
        private class StateLevel {
            CircuitState state;
            int level;
        }

        /**
         * Recursively descends into the components of the main circuit and looks for the circuit containing the register file
         *
         * @param main Top level circuit
         * @param registerCircuit Circuit containing the register file
         * @return StateLevel containing the circuit state of the register file and the level at which the register file resides in
         */
        private StateLevel findRegisterFileLevel(CircuitState mainState, Circuit registerCircuit) {
            Queue<StateLevel> circuitsToVisit = new LinkedList<StateLevel>();
            StateLevel mainLevel = new StateLevel();
            mainLevel.state = mainState;
            mainLevel.level = 0;
            circuitsToVisit.offer(mainLevel);
            while (!circuitsToVisit.isEmpty()) {
                StateLevel cur = circuitsToVisit.poll();
                Circuit curCircuit = cur.state.getCircuit();
                for (Component x : curCircuit.getNonWires()) {
                    if (x.getFactory().getName().equals(registerCircuit.getName())) {
                        StateLevel substateLevel = new StateLevel();
                        substateLevel.state = registerCircuit.getSubcircuitFactory().getSubstate(cur.state, x);
                        substateLevel.level = cur.level + 1; // include the level of the register circuit
                        return substateLevel;
                    }
                    else if (x.getFactory() instanceof SubcircuitFactory) {
                        SubcircuitFactory subcircuit = (SubcircuitFactory) x.getFactory();
                        StateLevel sl = new StateLevel();
                        sl.state = subcircuit.getSubstate(cur.state, x);
                        sl.level = cur.level + 1;
                        circuitsToVisit.offer(sl);
                    }
                }
            }
            return null; // registerCircuit is not a child of main
        }

        /**
         * Runs a test on this circuit.
         *
         * @param program Test code
         * @param cycles Number of cycles to run for
         * @param starting Array of starting values for register file
         * @return Array of values in the register file after running test
         */
        private int[] runTest(String program, int cycles, int[] starting) throws IOException {
            Resetter.reset(state);
            setCode(programRomState, program);

            if (starting != null) {
                setRegisters(registerFileState, starting);
            }

            Propagator prop = state.getPropagator();

            // Initial propagate deals with people who have their PC register
            // updating on the opposite edge.
            prop.propagate();

            // Two ticks make one cycle, so go to cycles * 2
            // Add one more half-cycle for falling edge people
            for (int i = 0; i < cycles * 2 + 1; ++i) {
                prop.toggleClocks();
                prop.propagate();
            }

            return getRegisters(registerFileState);
        }
    }

    /**
     * Checks that the array is of the right length.
     *
     * @throws IllegalArgumentException if the array is the wrong length.
     */
    private static void checkLength(int[] array, String name, int expected) {
        if (array.length == expected) {
            return;
        }
        String format = "%s is length %d, but it should be length %d";
        String err = String.format(format, name, array.length, expected);
        throw new IllegalArgumentException(err);
    }

    /**
     * @return the number of differences between the two arrays.
     */
    private static int countDiffs(int[] expected, int[] actual) {
        checkLength(expected, "Expected array", NUM_REGISTERS);
        checkLength(actual, "Actual array", NUM_REGISTERS);
        int errors = 0;
        for (int i = 0; i < NUM_REGISTERS; ++i) {
            if (expected[i] != actual[i]) {
                ++errors;
            }
        }
        return errors;
    }

    /**
     * Prints to standard output the differences between the two arrays.
     */
    private static void printDiffs(int[] expected, int[] actual) {
        checkLength(expected, "Expected array", NUM_REGISTERS);
        checkLength(actual, "Actual array", NUM_REGISTERS);
        for (int i = 0; i < NUM_REGISTERS; ++i) {
            if (expected[i] != actual[i]) {
                System.out.printf("    Error in register %d. Expected 0x%08x, but got 0x%08x.\n", i, expected[i], actual[i]);
            }
        }
    }

    /**
     * Prints to standard output the differences between the two arrays in the given registers
     */
    private static void printDiffRegisters(int[] expected, int[] actual, int[] registers) {
        checkLength(expected, "Expected array", NUM_REGISTERS);
        checkLength(actual, "Actual array", NUM_REGISTERS);
        for (int i = 0; i < registers.length; ++i) {
            if (expected[registers[i]] != actual[registers[i]]) {
                System.out.printf("    Error in register %d. Expected 0x%08x, but got 0x%08x.\n", registers[i], expected[registers[i]], actual[registers[i]]);
            }
        }
    }

    /**
     * Changes a Program ROM's state so that it contains the specified code.
     *
     * @param state Program ROM's InstanceState
     * @param program Code to load
     */
    private static void setCode(InstanceState state, String program) throws IOException {
        Listing code = state.getAttributeValue(Program32.CONTENTS_ATTR);
        code.setSource(program);
    }

    /**
     * Changes a Register File's state so that it contains the specified values.
     *
     * @param state Register File's InstanceState
     * @param vals Values to set
     */
    private static void setRegisters(InstanceState state, int[] vals) {
        checkLength(vals, "Values array", NUM_REGISTERS);

        RegisterData data = RegisterData.get(state);
        for (int i = 1; i < NUM_REGISTERS; ++i) {
            data.regs[i] = Value.createKnown(REG_WIDTH, vals[i]);
        }
    }

    /**
     * @param state InstanceState of a register file.
     * @return Array of the values in the register file.
     */
    private static int[] getRegisters(InstanceState state) {
        RegisterData data = RegisterData.get(state);
        int[] regs = new int[NUM_REGISTERS];
        for (int i = 0; i < NUM_REGISTERS; ++i) {
            if (!data.regs[i].isFullyDefined()) {
                String err = String.format("Register %d undefined", i);
                throw new IllegalStateException(err);
            }
            regs[i] = data.regs[i].toIntValue();
        }
        return regs;
    }

    /**
     * Parses a string which could be a base 10 or base 16 number.
     * @return the number
     */
    private static int parse(String x) {
        if (x.toLowerCase().startsWith("0x")) {
            // 0x and 8 hexadecimal digits is the max. If longer, fail!
            if (x.length() > 10) {
                throw new IllegalArgumentException(x + " is out of range");
            }

            // Need to use a long here, otherwise 0x8000000 - 0xffffffff fail!
            long l = Long.parseLong(x.substring(2), 16);
            if (l > Integer.MAX_VALUE) {
                l -= (1L << 32);
            }
            return (int) l;
        }
        return Integer.parseInt(x);
    }

    /**
     * @return "1 error" if there is 1 error, otherwise "x errors" for x errors
     */
    private static String count(int errors) {
        if (errors == 1) {
            return " 1 error ";
        }
        return String.format(" %d errors ", errors);
    }

    /**
     * @return Aesthetically pleasing score with at most 1 floating point.
     */
    private static String niceScore(double score, double max_score) {
        StringBuilder str = new StringBuilder();
        if (score % 1 == 0) {
            str.append(String.format("%.0f/", score));
        }
        else {
            str.append(String.format("%.1f/", score));
        }
        if (max_score % 1 == 0) {
            str.append(String.format("%.0f\n", max_score));
            return str.toString();
        }
        else {
            str.append(String.format("%.1f\n", max_score));
            return str.toString();
        }
    }
}