package com.cburch.logisim.analyze.model;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.stream.Stream;

import javax.swing.JTextArea;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

public class ImplicantTest {

  private static Stream<Arguments> expressionProvider() {
    return Stream.of(
        // List of variables, expression, amount of primes in the result, expression format
        // Expressions to be minimized as DNF
        Arguments.of("a,b,c,d", "b c'd+a'b'd'+a'b'c+a'c d'+b'c'd'+a b'd+a c'+b c d'",
            5, AnalyzerModel.FORMAT_SUM_OF_PRODUCTS),
        Arguments.of("a,b,c,d", "b'c+a b'+a'b c'+a'b d'+b c'd+c d'+a d",
            5, AnalyzerModel.FORMAT_SUM_OF_PRODUCTS),
        Arguments.of("a,b", "a b + a b'", 1, AnalyzerModel.FORMAT_SUM_OF_PRODUCTS),
        Arguments.of("a,b", "a+a'", 1, AnalyzerModel.FORMAT_SUM_OF_PRODUCTS),
        Arguments.of("a,b", "a a'", 0, AnalyzerModel.FORMAT_SUM_OF_PRODUCTS),
        Arguments.of("a,b,c,d", "a'b'c'd'+a'b'c'd+a'b c'd+a b c'd'+a b c'd +a b'c'd'",
            3, AnalyzerModel.FORMAT_SUM_OF_PRODUCTS),
        Arguments.of("a,b,c,d,e,f", "a'b'd'+b'e'+b d e'+a b d'+b d'e", 4,
            AnalyzerModel.FORMAT_SUM_OF_PRODUCTS),

        // Expressions to be minimized as CNF
        Arguments.of("a,b,c,d", "b c'd+a 'b'd'+a'b'c+a'c d'+b'c'd'+a b'd+a c'+b c d'",
            4, AnalyzerModel.FORMAT_PRODUCT_OF_SUMS),
        Arguments.of("a,b,c,d", "b'c+a b'+a'b c'+a'b d'+b c'd+c d'+a d",
            3, AnalyzerModel.FORMAT_PRODUCT_OF_SUMS),
        Arguments.of("a,b", "a+a'", 0, AnalyzerModel.FORMAT_PRODUCT_OF_SUMS),
        Arguments.of("a,b", "a a'", 1, AnalyzerModel.FORMAT_PRODUCT_OF_SUMS),
        Arguments.of("a,b,c,d", "(a+b+c+d)(a+b+c+d')(a+b'+c+d')(a'+b'+c+d)(a'+b'+c+d')"
            + "(a'+b+c+d)", 3, AnalyzerModel.FORMAT_PRODUCT_OF_SUMS)
      );
  }

  /** Test method for
   * {@link com.cburch.logisim.analyze.model.Implicant#computeMinimal(int,
   * AnalyzerModel, String, javax.swing.JTextArea)}.
   *
   * @param vars Names of the variables in Expression, separated by commas
   * @param expr String representation of the Expression, must be parsable by
   *     {@link com.cburch.logisim.analyze.model.Parser#parse(String, AnalyzerModel)}
   * @param cost Amount of expected Implicants in the minimized Expression
   * @param format Format to be used for minimization. Formats declared in
   *     {@link com.cburch.logisim.analyze.model.AnalyzerModel}
   */
  @ParameterizedTest
  @MethodSource("expressionProvider")
  public void testComputeMinimal(String vars, String expr, int cost, int format) {
    // Create analyzer model
    final AnalyzerModel model = new AnalyzerModel();

    // Get variables from String and add them to the analyzer model
    final String[] varr = vars.split(",");
    final var inputs = model.getInputs();
    for (final var variable : varr) {
      assertDoesNotThrow(() -> inputs.add(Var.parse(variable)),
          "Parsing input variable '" + variable + "' failed!");
    }

    // Register output variables to the analyzer model
    assertDoesNotThrow(() -> model.getOutputs().add(Var.parse("x")),
        "Failed to parse output variable");
    assertDoesNotThrow(() -> model.getOutputs().add(Var.parse("y")),
        "Failed to parse output variable");

    // Add original expression to the analyzer model
    assertDoesNotThrow(
        () -> model.getOutputExpressions().setExpression("x", Parser.parse(expr, model)),
        "Failed to parse expression '" + expr + "'!");

    // Minimize Expression (use outputArea to allow for larger expressions)
    final var res = Implicant.computeMinimal(format, model, "x", new JTextArea());

    // Add new Expression to the analyzer model (needed to verify Expression is equivalent)
    model.getOutputExpressions().setExpression("y", Implicant.toExpression(format, model, res));

    // Compare outputs of old and minimized Expression to verify they are the same
    assertArrayEquals(model.getTruthTable().getOutputColumn(0),
        model.getTruthTable().getOutputColumn(1), "The truth table changed during minimization");

    // Check if the minimized Expression has the expected amount of primes
    assertEquals(cost, res.size(),
        "The amount of primes in the result does not match the expected value.");
  }
}