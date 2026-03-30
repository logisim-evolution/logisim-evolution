package com.cburch.logisim;



import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.cburch.logisim.fpga.menu.MenuFpga;
// Test class for the Calculator
public class MenuFPGATest {

    // Test method to check addition functionality
    @Test
    public void testLocaleChanged() {
        // Create an instance of the Calculator class
        MenuFpga menuFpga = new MenuFpga(null,null,null); // creates new static method

        menuFpga.localeChanged(); // calls static method

        assertTrue(true);// tells the test case that no matter the result, it is always true.


    }
}