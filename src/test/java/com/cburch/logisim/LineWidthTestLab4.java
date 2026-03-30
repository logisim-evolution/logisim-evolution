package com.cburch.logisim;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

import com.cburch.logisim.circuit.Wire;

public class LineWidthTestLab4 {

    @Test
    public void testWireWidth() {
        // You changed this from 3 to 4 in Wire.java
        assertEquals(4, Wire.WIDTH);
    }
}
