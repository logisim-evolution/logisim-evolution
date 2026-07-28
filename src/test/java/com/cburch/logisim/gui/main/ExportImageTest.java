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

import com.cburch.logisim.gui.generic.TikZWriter;
import java.awt.Color;
import java.awt.image.BufferedImage;
import java.nio.file.Files;
import java.nio.file.Path;
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
}
