/*
 * Logisim-evolution - digital logic design tool and simulator
 * Copyright by the Logisim-evolution developers
 *
 * https://github.com/logisim-evolution/
 *
 * This is free software released under GNU GPLv3 license
 */

package com.cburch.logisim.file;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.cburch.logisim.instance.StdAttr;
import com.cburch.logisim.vhdl.base.VhdlContent;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.Test;

class VhdlAppearanceXmlTest {
  @Test
  void vhdlAppearanceRoundTripsOnVhdlElement() throws Exception {
    final var loader = new Loader(null);
    final var file = LogisimFile.createNew(loader, null);
    final var vhdl = VhdlContent.create("MyVhdl", file);

    vhdl.setAppearance(StdAttr.APPEAR_CLASSIC);
    file.addVhdlContent(vhdl);

    final var output = new ByteArrayOutputStream();
    file.write(output, loader);

    final var xml = new String(output.toByteArray(), StandardCharsets.UTF_8);
    assertTrue(xml.contains("appearance=\"classic\""));

    final var loaded =
        LogisimFile.load(new ByteArrayInputStream(output.toByteArray()), new Loader(null));
    final var loadedVhdl = loaded.getVhdlContent("MyVhdl");

    assertEquals(StdAttr.APPEAR_CLASSIC, loadedVhdl.getAppearance());
    assertEquals(
        StdAttr.APPEAR_CLASSIC,
        loadedVhdl.getStaticAttributes().getValue(StdAttr.APPEARANCE));
  }
}
