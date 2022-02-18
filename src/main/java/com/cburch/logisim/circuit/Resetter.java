package com.cburch.logisim.circuit;

/**
 * Resetter; a class to reset a CircuitState.
 *
 * I'm taking advantage of the fact that CircuitState is package-private.
 * Hacktastic, but all I want to do is reset the circuit; not have to deal with
 * going through the Simulator class and all that.
 *
 * @author Peter Tseng
 */
public class Resetter {
    private Resetter() {
        // Private; can't construct this.
    }

    public static void reset(CircuitState cs) {
        cs.reset();
    }
}