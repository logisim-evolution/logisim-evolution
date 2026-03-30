
package com.cburch.logisim.icons;

import static org.junit.jupiter.api.Assertions.assertEquals;
import java.awt.Color;

import com.cburch.logisim.gui.icons.AppearEditIcon;
import org.junit.jupiter.api.Test;

class AppearEditIconTest {
    @Test
    void tipColorIsGreen() {
        assertEquals(Color.GREEN, AppearEditIcon.TIP_COLOR);
    }
}
