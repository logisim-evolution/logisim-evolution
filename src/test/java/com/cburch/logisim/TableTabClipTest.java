package com.cburch.logisim;

import java.awt.datatransfer.DataFlavor;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class TableTabClipTest {

  @Test
  public void testIsDataFlavorSupported() throws Exception {

    // Prepare dummy data for constructor
    String[] headers = {"A"};
    String[][] contents = {{"1"}};

    // Access inner class: TableTabClip$Data
    Class<?> dataClass =
        Class.forName("com.cburch.logisim.analyze.gui.TableTabClip$Data");

    var constructor =
        dataClass.getDeclaredConstructor(String[].class, String[][].class);

    constructor.setAccessible(true);

    Object dataObject = constructor.newInstance(headers, contents);

    var method =
        dataClass.getDeclaredMethod("isDataFlavorSupported", DataFlavor.class);

    method.setAccessible(true);

    // Supported flavor
    boolean resultTrue =
        (boolean) method.invoke(dataObject, DataFlavor.stringFlavor);

    // Unsupported flavor
    boolean resultFalse =
        (boolean) method.invoke(dataObject, DataFlavor.imageFlavor);

    assertTrue(resultTrue);
    assertFalse(resultFalse);
  }
}
