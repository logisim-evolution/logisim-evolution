/*
 * Logisim-evolution - digital logic design tool and simulator
 * Copyright by the Logisim-evolution developers
 *
 * https://github.com/logisim-evolution/
 *
 * This is free software released under GNU GPLv3 license
 */

package com.cburch.logisim.gui.menu;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.cburch.logisim.gui.main.ExportImage;
import com.cburch.logisim.gui.main.ExportImage.ImageFileFilter;
import com.cburch.logisim.util.StringGetter;
import java.awt.Dimension;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.awt.print.PageFormat;
import java.awt.print.Printable;
import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import javax.swing.filechooser.FileFilter;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class PrintHandlerTest {

  @TempDir Path tempDir;

  @Test
  void selectedExportFilterWinsOverExistingFileExtension() {
    final var filters = imageFilters();
    final var selectedFilter = filters[3];
    final var chosenFilter =
        PrintHandler.chooseExportFilter(new File("timing.svg"), selectedFilter, filters);

    assertEquals(ExportImage.FORMAT_TIKZ, chosenFilter.getType());
  }

  @Test
  void fileExtensionSelectsFormatWhenSelectedFilterIsNotAnImageFilter() {
    final var filters = imageFilters();
    final var selectedFilter =
        new FileFilter() {
          @Override
          public boolean accept(File f) {
            return false;
          }

          @Override
          public String getDescription() {
            return "not an image filter";
          }
        };
    final var chosenFilter =
        PrintHandler.chooseExportFilter(new File("timing.svg"), selectedFilter, filters);

    assertEquals(ExportImage.FORMAT_SVG, chosenFilter.getType());
  }

  @Test
  void selectedFilterExtensionIsAppendedWhenFileNameHasDifferentExtension() {
    final var filters = imageFilters();
    final var dest = PrintHandler.ensureFileExtension(new File("timing.svg"), filters[3]);

    assertEquals(new File("timing.svg.tex"), dest);
  }

  @Test
  void directTikzExportWritesTikzContent() throws Exception {
    final var dest = tempDir.resolve("timing.tex").toFile();
    new TestPrintHandler().exportImage(dest, ExportImage.FORMAT_TIKZ);

    final var content = Files.readString(dest.toPath());
    assertTrue(content.contains("\\begin{tikzpicture}"));
    assertTrue(content.contains("\\draw"));
    assertFalse(content.contains("<svg"));
  }

  @Test
  void directSvgExportWritesSvgContent() throws Exception {
    final var dest = tempDir.resolve("timing.svg").toFile();
    new TestPrintHandler().exportImage(dest, ExportImage.FORMAT_SVG);

    final var content = Files.readString(dest.toPath());
    assertTrue(content.contains("<svg"));
    assertTrue(content.contains("<path"));
    assertFalse(content.contains("\\begin{tikzpicture}"));
  }

  private static ImageFileFilter[] imageFilters() {
    return new ImageFileFilter[] {
      new ImageFileFilter(ExportImage.FORMAT_PNG, getter("PNG"), new String[] {"png"}),
      new ImageFileFilter(ExportImage.FORMAT_GIF, getter("GIF"), new String[] {"gif"}),
      new ImageFileFilter(ExportImage.FORMAT_JPG, getter("JPEG"), new String[] {"jpg"}),
      new ImageFileFilter(ExportImage.FORMAT_TIKZ, getter("TikZ"), new String[] {"tex"}),
      new ImageFileFilter(ExportImage.FORMAT_SVG, getter("SVG"), new String[] {"svg"}),
      new ImageFileFilter(ExportImage.FORMAT_WAVEDROM, getter("WaveDrom"), new String[] {"json"})
    };
  }

  private static StringGetter getter(String value) {
    return new StringGetter() {
      @Override
      public String toString() {
        return value;
      }
    };
  }

  private static class TestPrintHandler extends PrintHandler {
    @Override
    public int print(Graphics2D g, PageFormat pf, int pageNum, double w, double h) {
      return Printable.PAGE_EXISTS;
    }

    @Override
    public Dimension getExportImageSize() {
      return new Dimension(20, 20);
    }

    @Override
    public void paintExportImage(BufferedImage img, Graphics2D g) {
      g.drawLine(1, 2, 18, 2);
    }
  }
}
