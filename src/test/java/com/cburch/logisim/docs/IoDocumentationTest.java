/*
 * Logisim-evolution - digital logic design tool and simulator
 * Copyright by the Logisim-evolution developers
 *
 * https://github.com/logisim-evolution/
 *
 * This is free software released under GNU GPLv3 license.
 */

package com.cburch.logisim.docs;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

class IoDocumentationTest {
  private static final Path DOC_ROOT = Path.of("src", "main", "resources", "doc");

  @ParameterizedTest
  @ValueSource(strings = {"ledbar", "realtimeclock", "matrixkeypad"})
  void referencePageExists(String page) {
    final var path = DOC_ROOT.resolve("en/html/libs/io").resolve(page + ".html");

    assertTrue(Files.isRegularFile(path), () -> "Missing I/O reference page " + path);
  }

  @ParameterizedTest
  @ValueSource(strings = {"ledbar", "realtimeclock", "matrixkeypad"})
  void englishIndexLinksReferencePage(String page) throws Exception {
    final var index =
        Files.readString(DOC_ROOT.resolve("en/html/libs/io/index.html"), StandardCharsets.UTF_8);

    assertTrue(
        index.contains("href=\"./" + page + ".html\""),
        () -> "English I/O index does not link " + page + ".html");
  }

  @ParameterizedTest
  @ValueSource(strings = {"de", "pt", "ru", "zh"})
  void packagedLocaleIndexFallsBackToEnglish(String language) throws Exception {
    final var indexPath = DOC_ROOT.resolve(language + "/html/libs/io/index.html");
    final var index = Files.readString(indexPath, StandardCharsets.UTF_8);

    for (final var page : new String[] {"ledbar", "realtimeclock", "matrixkeypad"}) {
      final var expected = "href=\"../../../../en/html/libs/io/" + page + ".html\"";
      assertTrue(
          index.contains(expected),
          () -> language + " I/O index does not fall back to English " + page + ".html");
    }
  }
}
