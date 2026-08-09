/*
 * Logisim-evolution - digital logic design tool and simulator
 * Copyright by the Logisim-evolution developers
 *
 * https://github.com/logisim-evolution/
 *
 * This is free software released under GNU GPLv3 license
 */

package com.cburch.logisim.gui.main;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.cburch.logisim.circuit.CircuitMutation;
import com.cburch.logisim.circuit.CircuitState;
import com.cburch.logisim.comp.ComponentDrawContext;
import com.cburch.logisim.data.Location;
import com.cburch.logisim.file.Loader;
import com.cburch.logisim.file.LogisimFile;
import com.cburch.logisim.gui.generic.TikZWriter;
import com.cburch.logisim.instance.StdAttr;
import com.cburch.logisim.prefs.AppPreferences;
import com.cburch.logisim.prefs.PrefMonitor;
import com.cburch.logisim.proj.Project;
import com.cburch.logisim.std.gates.GatesLibrary;
import com.cburch.logisim.std.io.IoLibrary;
import com.cburch.logisim.tools.AddTool;
import java.awt.Color;
import java.awt.image.BufferedImage;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.prefs.PreferenceChangeEvent;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class ExportImageTest {

  private static final Color DARK_CANVAS = new Color(0x2B2B2B);

  @TempDir Path tempDir;

  @Test
  void nonPrinterRasterUsesCanvasBackground() {
    final var image = new BufferedImage(3, 2, BufferedImage.TYPE_INT_RGB);
    final var graphics = image.createGraphics();

    ExportImage.paintExportBackground(
        graphics, image.getWidth(), image.getHeight(), ExportImage.FORMAT_PNG, false, DARK_CANVAS);
    graphics.dispose();

    assertEquals(DARK_CANVAS.getRGB(), image.getRGB(0, 0));
    assertEquals(DARK_CANVAS.getRGB(), image.getRGB(2, 1));
  }

  @Test
  void nonPrinterVectorUsesCanvasBackground() throws Exception {
    final var tikzGraphics = new TikZWriter();
    ExportImage.paintExportBackground(
        tikzGraphics, 3, 2, ExportImage.FORMAT_TIKZ, false, DARK_CANVAS);
    final var tikz = tempDir.resolve("circuit.tex").toFile();
    tikzGraphics.writeFile(tikz);

    final var svgGraphics = new TikZWriter();
    ExportImage.paintExportBackground(
        svgGraphics, 3, 2, ExportImage.FORMAT_SVG, false, DARK_CANVAS);
    final var svg = tempDir.resolve("circuit.svg").toFile();
    svgGraphics.writeSvg(3, 2, svg);

    final var tikzContent = Files.readString(tikz.toPath());
    final var svgContent = Files.readString(svg.toPath());
    assertTrue(tikzContent.contains("\\definecolor{custcol_2B2B2B}{HTML}{2B2B2B}"));
    assertTrue(tikzContent.contains("\\fill"));
    assertTrue(svgContent.contains("fill=\"#2B2B2B\""));
    assertTrue(svgContent.contains("<rect"));
  }

  @Test
  void printerViewKeepsExistingRasterAndVectorBackgrounds() throws Exception {
    final var image = new BufferedImage(3, 2, BufferedImage.TYPE_INT_RGB);
    final var rasterGraphics = image.createGraphics();
    ExportImage.paintExportBackground(
        rasterGraphics, image.getWidth(), image.getHeight(), ExportImage.FORMAT_PNG, true, DARK_CANVAS);
    rasterGraphics.dispose();

    final var tikzGraphics = new TikZWriter();
    ExportImage.paintExportBackground(
        tikzGraphics, 3, 2, ExportImage.FORMAT_TIKZ, true, DARK_CANVAS);
    final var tikz = tempDir.resolve("printer-view.tex").toFile();
    tikzGraphics.writeFile(tikz);

    final var svgGraphics = new TikZWriter();
    ExportImage.paintExportBackground(
        svgGraphics, 3, 2, ExportImage.FORMAT_SVG, true, DARK_CANVAS);
    final var svg = tempDir.resolve("printer-view.svg").toFile();
    svgGraphics.writeSvg(3, 2, svg);

    assertEquals(Color.WHITE.getRGB(), image.getRGB(0, 0));
    assertEquals(Color.WHITE.getRGB(), image.getRGB(2, 1));
    assertFalse(Files.readString(tikz.toPath()).contains("\\fill"));
    assertFalse(Files.readString(svg.toPath()).contains("<rect"));
  }

  @Test
  void darkThemePrinterViewDrawsComponentsOnWhiteBackground() {
    final var originalComponentColor = AppPreferences.COMPONENT_COLOR.get();
    setPreference(AppPreferences.COMPONENT_COLOR, AppPreferences.DARK_COMPONENT_COLOR);
    try {
      final var file = LogisimFile.createNew(new Loader(null), null);
      final var circuit = file.getMainCircuit();
      final var andFactory = ((AddTool) new GatesLibrary().getTool("AND Gate")).getFactory();
      final var attributes = andFactory.createAttributeSet();
      attributes.setValue(StdAttr.LABEL, "auto_test");
      final var gate = andFactory.createComponent(Location.create(60, 50, false), attributes);
      final var mutation = new CircuitMutation(circuit);
      mutation.add(gate);
      mutation.execute();

      final var image = new BufferedImage(120, 100, BufferedImage.TYPE_INT_RGB);
      final var graphics = image.createGraphics();
      ExportImage.paintExportBackground(
          graphics,
          image.getWidth(),
          image.getHeight(),
          ExportImage.FORMAT_PNG,
          true,
          DARK_CANVAS);
      final var context =
          new ComponentDrawContext(null, circuit, null, graphics, graphics, true) {
            @Override
            public Object getGateShape() {
              return AppPreferences.SHAPE_SHAPED;
            }
          };

      circuit.draw(context, null);
      graphics.dispose();

      assertTrue(hasNonWhitePixel(image));
      assertEquals(AppPreferences.DARK_COMPONENT_COLOR, AppPreferences.COMPONENT_COLOR.get());
    } finally {
      setPreference(AppPreferences.COMPONENT_COLOR, originalComponentColor);
    }
  }

  @Test
  void rgbVideoCircuitEmbedsItsDisplayInSvg() throws Exception {
    final var file = LogisimFile.createNew(new Loader(null), null);
    final var project = new Project(file);
    final var circuit = file.getMainCircuit();
    circuit.setProject(project);
    final var circuitState = CircuitState.createRootState(project, circuit);
    final var videoFactory = ((AddTool) new IoLibrary().getTool("RGB Video")).getFactory();
    final var video =
        videoFactory.createComponent(
            Location.create(100, 300, false), videoFactory.createAttributeSet());
    final var mutation = new CircuitMutation(circuit);
    mutation.add(video);
    mutation.execute();

    final var graphics = new TikZWriter();
    final var context =
        new ComponentDrawContext(null, circuit, circuitState, graphics, graphics, false);
    circuit.draw(context, null);
    final var svg = tempDir.resolve("rgb-video.svg").toFile();
    graphics.writeSvg(350, 350, svg);

    final var content = Files.readString(svg.toPath());
    assertTrue(content.contains("<image"));
    assertTrue(content.contains("width=\"128\""));
    assertTrue(content.contains("height=\"128\""));
    assertTrue(content.contains("transform=\"matrix(2 0 0 2 77 37)\""));
    assertTrue(content.contains("data:image/png;base64,"));
  }

  private static boolean hasNonWhitePixel(BufferedImage image) {
    for (var y = 0; y < image.getHeight(); y++) {
      for (var x = 0; x < image.getWidth(); x++) {
        if (image.getRGB(x, y) != Color.WHITE.getRGB()) return true;
      }
    }
    return false;
  }

  private static void setPreference(PrefMonitor<Integer> monitor, int value) {
    final var preferences = AppPreferences.getPrefs();
    preferences.putInt(monitor.getIdentifier(), value);
    monitor.preferenceChange(
        new PreferenceChangeEvent(
            preferences, monitor.getIdentifier(), Integer.toString(value)));
  }
}
