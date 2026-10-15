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
import java.awt.Font;
import java.awt.Graphics2D;
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
    assertFalse(FormulaText.containsFormula("$\\overline{\\text{A}$"));
    assertFalse(FormulaText.containsFormula("вход $\\overline{\\text{A}}$"));
    assertFalse(FormulaText.containsFormula("$$"));
    assertFalse(FormulaText.containsFormula("Y=B+C"));
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

  private static Graphics2D graphics() {
    final var graphics =
        new BufferedImage(1, 1, BufferedImage.TYPE_INT_ARGB).createGraphics();
    graphics.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 14));
    return graphics;
  }
}
