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

import com.cburch.logisim.circuit.CircuitMutation;
import com.cburch.logisim.data.Location;
import com.cburch.logisim.prefs.AppPreferences;
import com.cburch.logisim.std.base.BaseLibrary;
import com.cburch.logisim.std.base.Text;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class TextPersistenceTest {

  @TempDir Path tempDir;

  @Test
  void darkThemeSavesAndReloadsOnlyTheLightColor() throws Exception {
    final var originalTheme = AppPreferences.LookAndFeel.get();
    try {
      AppPreferences.LookAndFeel.set("TestDark");
      final var loader = new Loader(null);
      final var file = LogisimFile.createNew(loader, null);
      file.addLibrary(loader.getBuiltin().getLibrary(BaseLibrary._ID));
      final var attrs = Text.FACTORY.createAttributeSet();
      final var storedColor = Text.ATTR_COLOR.parse("#0000ff78");
      attrs.setValue(Text.ATTR_TEXT, "color");
      attrs.setValue(Text.ATTR_COLOR, storedColor);
      final var mutation = new CircuitMutation(file.getMainCircuit());
      mutation.add(Text.FACTORY.createComponent(Location.create(100, 100, false), attrs));
      mutation.execute();
      final var path = tempDir.resolve("text-color.circ").toFile();

      assertTrue(loader.save(file, path));
      final var reloaded = new Loader(null).openLogisimFile(path);
      final var reloadedAttrs = reloaded.getMainCircuit().getNonWires().iterator().next().getAttributeSet();
      assertEquals(storedColor, reloadedAttrs.getValue(Text.ATTR_COLOR));
      assertEquals("#dadaff78", Text.ATTR_COLOR.toDisplayString(reloadedAttrs.getValue(Text.ATTR_COLOR)));
      assertTrue(Files.readString(path.toPath()).contains("#0000ff78"));
    } finally {
      AppPreferences.LookAndFeel.set(originalTheme);
    }
  }

  @Test
  void multilineTextSurvivesSaveAndReload() throws Exception {
    final var text = "first line\nsecond line";
    final var loader = new Loader(null);
    final var file = LogisimFile.createNew(loader, null);
    file.addLibrary(loader.getBuiltin().getLibrary(BaseLibrary._ID));
    final var attrs = Text.FACTORY.createAttributeSet();
    attrs.setValue(Text.ATTR_TEXT, text);
    final var component =
        Text.FACTORY.createComponent(Location.create(100, 100, false), attrs);
    final var mutation = new CircuitMutation(file.getMainCircuit());
    mutation.add(component);
    mutation.execute();
    final var path = tempDir.resolve("multiline-text.circ").toFile();

    assertTrue(loader.save(file, path));
    final var reloaded = new Loader(null).openLogisimFile(path);
    final var reloadedComponent = reloaded.getMainCircuit().getNonWires().iterator().next();

    assertEquals(
        text,
        reloadedComponent.getAttributeSet().getValue(Text.ATTR_TEXT),
        Files.readString(path.toPath()));
  }
}
