/*
 * Logisim-evolution - digital logic design tool and simulator
 * Copyright by the Logisim-evolution developers
 *
 * https://github.com/logisim-evolution/
 *
 * This is free software released under GNU GPLv3 license
 */

package com.cburch.logisim.file;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.Test;
import org.xml.sax.SAXException;

class OptionsTest {
  @Test
  void hdlCompatibleNamesAreRequiredByDefault() {
    final var options = new Options();

    assertTrue(options.requiresHdlCompatibleNames());
  }

  @Test
  void hdlCompatibleNamesCanBeDisabledPerProject() {
    final var options = new Options();

    options.getAttributeSet().setValue(Options.ATTR_HDL_COMPATIBLE_NAMES, false);

    assertFalse(options.requiresHdlCompatibleNames());
  }

  @Test
  void hdlCompatibleNamesAreWrittenToCircuitFiles() {
    final var file = LogisimFile.createNew(new Loader(null), null);
    file.getOptions().getAttributeSet().setValue(Options.ATTR_HDL_COMPATIBLE_NAMES, false);

    final var xml = write(file);

    assertTrue(xml.contains("<a name=\"hdlCompatibleNames\" val=\"false\"/>"));
  }

  @Test
  void hdlCompatibleNamesAreReadFromCircuitFiles() throws IOException, SAXException {
    final var file = LogisimFile.createNew(new Loader(null), null);
    file.getOptions().getAttributeSet().setValue(Options.ATTR_HDL_COMPATIBLE_NAMES, false);

    final var restored =
        LogisimFile.load(
            new ByteArrayInputStream(write(file).getBytes(StandardCharsets.UTF_8)),
            new Loader(null));

    assertFalse(restored.getOptions().requiresHdlCompatibleNames());
  }

  private static String write(LogisimFile file) {
    final var out = new ByteArrayOutputStream();
    file.write(out, new Loader(null));
    return out.toString(StandardCharsets.UTF_8);
  }
}
