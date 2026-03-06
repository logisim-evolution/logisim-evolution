package com.cburch.logisim.gui.icons;

import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import java.awt.Color;
import java.awt.Graphics2D;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.*;

class JoystickIconTest {

    @Test
    void testBlueAndGrayColorsAreUsed() {

        JoystickIcon icon = new JoystickIcon(); //Icon Instance

        Graphics2D g2 = mock(Graphics2D.class); // Makes a copy of the joystock graphics object

        icon.paintIcon(g2);// call the paint icon

        ArgumentCaptor<Color> colorCaptor = ArgumentCaptor.forClass(Color.class); //gets the paint arguments that were
        // called

        verify(g2, atLeastOnce()).setColor(colorCaptor.capture()); //makes sure the set color arguemnts were called.

        List<Color> usedColors = colorCaptor.getAllValues(); //gets a list of all of the colors that were set

        assertTrue(usedColors.contains(Color.BLUE)); //makes sure there is a blue color out of the objects that was made

        assertTrue(usedColors.contains(Color.GRAY)); //makes sure there was a gray color out of the
        // objects that were made.

    }
}