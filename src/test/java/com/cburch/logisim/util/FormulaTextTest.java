/*
 * Logisim-evolution - digital logic design tool and simulator
 * Copyright by the Logisim-evolution developers
 *
 * https://github.com/logisim-evolution/
 *
 * This is free software released under GNU GPLv3 license
 */

package com.cburch.logisim.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.cburch.logisim.comp.TextField;
import com.cburch.logisim.gui.generic.TikZWriter;
import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.image.BufferedImage;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class FormulaTextTest {

  private static final String ISSUE_EXAMPLE =
      "$\\text{Y}=\\text{B}\\cdot\\overline{\\text{C}}+\\text{A}\\cdot\\text{B}$";
  private static final String OVERLINE = "$\\overline{\\text{A}}$";

  @TempDir Path tempDir;

  @Test
  void issueExampleIsNarrowerThanItsSource() {
    final var graphics = graphics();
    final var sourceWidth = graphics.getFontMetrics().stringWidth(ISSUE_EXAMPLE);
    final var formula =
        FormulaText.getBounds(
            graphics, ISSUE_EXAMPLE, 0, 0, GraphicsUtil.H_LEFT, GraphicsUtil.V_TOP);

    assertTrue(formula.width > 0);
    assertTrue(formula.width < sourceWidth);
    graphics.dispose();
  }

  @Test
  void nestedOverlineAndSubscriptHaveSize() {
    final var graphics = graphics();
    final var plain =
        FormulaText.getBounds(
            graphics, "$\\text{A}$", 0, 0, GraphicsUtil.H_LEFT, GraphicsUtil.V_TOP);
    final var nested =
        FormulaText.getBounds(
            graphics,
            "$\\overline{\\overline{\\text{A}}}$",
            0,
            0,
            GraphicsUtil.H_LEFT,
            GraphicsUtil.V_TOP);
    final var subscript =
        FormulaText.getBounds(
            graphics, "$\\text{A}_{1}$", 0, 0, GraphicsUtil.H_LEFT, GraphicsUtil.V_TOP);

    assertTrue(nested.width > 0);
    assertTrue(nested.height > plain.height);
    assertTrue(subscript.width > plain.width);
    graphics.dispose();
  }

  @Test
  void unknownOrPartialMathStaysOrdinaryText() {
    assertFalse(FormulaText.containsFormula("$\\frac{1}{2}$"));
    assertFalse(FormulaText.containsFormula("$\\sqrt{A}$"));
    assertFalse(FormulaText.containsFormula("$\\overline{\\text{A}$"));
    assertFalse(FormulaText.containsFormula("вход $\\overline{\\text{A}}$"));
    assertFalse(FormulaText.containsFormula("$$"));
    assertFalse(FormulaText.containsFormula("Y=B+C"));
    assertFalse(FormulaText.containsFormula("$A_$"));
    assertFalse(FormulaText.containsFormula("$A^$"));
    assertFalse(FormulaText.containsFormula("$A_{}$"));
    assertFalse(FormulaText.containsFormula("$A^{}$"));
    assertFalse(FormulaText.containsFormula("$A_^2$"));
  }

  @Test
  void comparisonSignsAndModulusAreNarrowerThanTheSource() {
    final var graphics = graphics();
    final var sources =
        new String[] {
          "$|\\text{A}|$",
          "$A<B$",
          "$A>B$",
          "$\\text{A}\\ne\\text{B}$",
          "$\\text{A} \\neq \\text{B}$",
          "$A\\le B$",
          "$A\\leq B$",
          "$A\\geq B$",
          "$A\\ge B$",
          "$\\ne$",
          "$\\overline{A\\geq B}$"
        };
    try {
      for (final var source : sources) {
        assertTrue(FormulaText.containsFormula(source), source);
        final var bounds = bounds(graphics, source);
        assertTrue(bounds.width > 0, source);
        assertTrue(bounds.height > 0, source);
        assertTrue(bounds.width < graphics.getFontMetrics().stringWidth(source), source);
      }
    } finally {
      graphics.dispose();
    }
  }

  @Test
  void bareSubscriptAndSuperscriptChangeTheDrawnSize() {
    final var graphics = graphics();
    try {
      final var plain = bounds(graphics, "$\\text{A}$");
      final var letter = bounds(graphics, "$A$");
      final var bare = bounds(graphics, "$A_s$");
      final var braced = bounds(graphics, "$A_{s}$");
      final var following = bounds(graphics, "$A_sx$");
      final var grouped = bounds(graphics, "$A_{sx}$");
      final var superscript = bounds(graphics, "$A^2$");
      final var both = bounds(graphics, "$A_s^2$");
      final var commandScript = bounds(graphics, "$A^\\ne$");
      final var textScript = bounds(graphics, "$A_\\text{out}$");
      final var overlinedScript = bounds(graphics, "$A_\\overline{B}$");
      final var plainScript = bounds(graphics, "$A_B$");

      assertTrue(bare.width > letter.width);
      assertEquals(bare.width, braced.width);
      assertEquals(bare.height, braced.height);
      assertTrue(following.width > bare.width);
      assertTrue(grouped.width < following.width);
      assertTrue(superscript.height > plain.height);
      assertTrue(both.width > bare.width);
      assertTrue(both.height > bare.height);
      assertTrue(commandScript.width > letter.width);
      assertTrue(textScript.width > letter.width);
      assertEquals(plainScript.width, overlinedScript.width);
      assertTrue(drawnInk("$A_\\overline{B}$") > drawnInk("$A_B$"));
    } finally {
      graphics.dispose();
    }
  }

  @Test
  void relationFormulaPaintsInkAndExportsSource() throws Exception {
    final var source = "$\\text{A}\\ne\\text{B}$";
    final var image = new BufferedImage(180, 80, BufferedImage.TYPE_INT_ARGB);
    final var canvas = image.createGraphics();
    canvas.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 18));
    canvas.setColor(Color.BLACK);
    FormulaText.draw(canvas, source, 12, 40, GraphicsUtil.H_LEFT, GraphicsUtil.V_BASELINE);
    canvas.dispose();
    assertTrue(inkCount(image) > 10);

    final var font = new Font(Font.SANS_SERIF, Font.PLAIN, 14);
    final var field = new TextField(10, 20, TextField.H_LEFT, TextField.V_BASELINE, font, false);
    field.setText(source);
    final var tikzGraphics = new TikZWriter();
    field.draw(tikzGraphics);
    final var tikz = tempDir.resolve("relation.tex").toFile();
    tikzGraphics.writeFile(tikz);
    final var tikzContent = Files.readString(tikz.toPath());
    assertTrue(tikzContent.contains(source));
    assertFalse(tikzContent.contains("\\draw"));

    final var svgGraphics = new TikZWriter();
    svgGraphics.setSvgMode(true);
    field.draw(svgGraphics);
    final var svg = tempDir.resolve("relation.svg").toFile();
    svgGraphics.writeSvg(200, 80, svg);
    final var svgContent = Files.readString(svg.toPath());
    assertFalse(svgContent.contains("\\ne"));
    assertTrue(svgContent.contains("≠"));
    assertTrue(svgContent.contains(">A<") || svgContent.matches("(?s).*<text[^>]*>\\s*A\\s*</text>.*"));
  }

  @Test
  void multilineFormulaKeepsPlainLinesAndAlignment() {
    final var graphics = graphics();
    try {
      final var mixed = "input\n$A\\ge B$";
      assertTrue(FormulaText.containsFormula(mixed));
      final var mixedBounds =
          FormulaText.getBounds(graphics, mixed, 40, 30, GraphicsUtil.H_CENTER, GraphicsUtil.V_TOP);
      final var oneLine = bounds(graphics, "$A\\ge B$");
      assertTrue(mixedBounds.height > oneLine.height);
      assertTrue(mixedBounds.x < 40);
      assertEquals(30, mixedBounds.y);

      final var field =
          new TextField(0, 0, TextField.H_RIGHT, TextField.V_BASELINE, graphics.getFont(), false);
      field.setText("$|A|<B$");
      assertTrue(field.getBounds(graphics).getWidth() < graphics.getFontMetrics().stringWidth("$|A|<B$"));
      assertTrue(field.getBounds(graphics).getX() < 0);
    } finally {
      graphics.dispose();
    }
  }

  @Test
  void textFieldBoundsUseTheDrawnFormula() {
    final var graphics = graphics();
    final var font = graphics.getFont();
    final var formula = new TextField(0, 0, TextField.H_LEFT, TextField.V_BASELINE, font, false);
    formula.setText(ISSUE_EXAMPLE);
    final var sourceWidth = graphics.getFontMetrics().stringWidth(ISSUE_EXAMPLE);

    assertTrue(formula.getBounds(graphics).getWidth() < sourceWidth);

    final var raw = "$\\foo{A}$";
    formula.setText(raw);
    assertEquals(graphics.getFontMetrics().stringWidth(raw), formula.getBounds(graphics).getWidth());
    graphics.dispose();
  }

  @Test
  void tikzExportKeepsTheSourceAndSvgDrawsTheFormula() throws Exception {
    final var font = new Font(Font.SANS_SERIF, Font.PLAIN, 14);
    final var field = new TextField(10, 20, TextField.H_LEFT, TextField.V_BASELINE, font, false);
    field.setText(OVERLINE);

    final var tikzGraphics = new TikZWriter();
    field.draw(tikzGraphics);
    final var tikz = tempDir.resolve("formula.tex").toFile();
    tikzGraphics.writeFile(tikz);
    final var tikzContent = Files.readString(tikz.toPath());
    assertTrue(tikzContent.contains(OVERLINE));
    assertFalse(tikzContent.contains("\\draw"));

    final var svgGraphics = new TikZWriter();
    svgGraphics.setSvgMode(true);
    field.draw(svgGraphics);
    final var svg = tempDir.resolve("formula.svg").toFile();
    svgGraphics.writeSvg(200, 80, svg);
    final var svgContent = Files.readString(svg.toPath());
    assertTrue(svgContent.matches("(?s).*<text[^>]*>\\s*A\\s*</text>.*"));
    assertFalse(svgContent.contains("overline"));
    assertTrue(svgContent.contains("<path"));
  }

  private static Rectangle bounds(Graphics2D graphics, String source) {
    return FormulaText.getBounds(
        graphics, source, 0, 0, GraphicsUtil.H_LEFT, GraphicsUtil.V_TOP);
  }

  private static int drawnInk(String source) {
    final var image = new BufferedImage(180, 90, BufferedImage.TYPE_INT_ARGB);
    final var canvas = image.createGraphics();
    canvas.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 28));
    canvas.setColor(Color.BLACK);
    FormulaText.draw(canvas, source, 8, 55, GraphicsUtil.H_LEFT, GraphicsUtil.V_BASELINE);
    canvas.dispose();
    return inkCount(image);
  }

  private static int inkCount(BufferedImage image) {
    var ink = 0;
    for (var y = 0; y < image.getHeight(); y++) {
      for (var x = 0; x < image.getWidth(); x++) {
        if ((image.getRGB(x, y) >>> 24) != 0) ink++;
      }
    }
    return ink;
  }

  private static Graphics2D graphics() {
    final var graphics =
        new BufferedImage(1, 1, BufferedImage.TYPE_INT_ARGB).createGraphics();
    graphics.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 14));
    return graphics;
  }
}
