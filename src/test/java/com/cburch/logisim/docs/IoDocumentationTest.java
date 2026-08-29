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
import java.util.regex.Pattern;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

class IoDocumentationTest {
  private static final Path DOC_ROOT = Path.of("src", "main", "resources", "doc");
  private static final String[] REFERENCE_PAGES = {"ledbar", "realtimeclock", "matrixkeypad"};

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
  @ValueSource(strings = {"de", "pt", "zh"})
  void packagedLocaleIndexFallsBackToEnglish(String language) throws Exception {
    final var indexPath = DOC_ROOT.resolve(language + "/html/libs/io/index.html");
    final var index = Files.readString(indexPath, StandardCharsets.UTF_8);

    for (final var page : REFERENCE_PAGES) {
      final var expected = "href=\"../../../../en/html/libs/io/" + page + ".html\"";
      assertTrue(
          index.contains(expected),
          () -> language + " I/O index does not fall back to English " + page + ".html");
    }
  }

  @ParameterizedTest
  @ValueSource(strings = {"de", "fr", "ru", "zh"})
  void localizedHelpTreeIncludesReferencePages(String language) throws Exception {
    final var contents =
        Files.readString(DOC_ROOT.resolve(language + "/contents.xml"), StandardCharsets.UTF_8);

    for (final var page : REFERENCE_PAGES) {
      final var target = "target=\"io_" + page + "\"";
      assertTrue(
          contents.contains(target),
          () -> language + " JavaHelp tree does not include target io_" + page);
    }
  }

  @ParameterizedTest
  @ValueSource(strings = {"de", "fr", "zh"})
  void localizedHelpMapFallsBackToEnglish(String language) throws Exception {
    final var map =
        Files.readString(DOC_ROOT.resolve("map_" + language + ".jhm"), StandardCharsets.UTF_8);

    for (final var page : REFERENCE_PAGES) {
      final var entry =
          Pattern.compile(
              "<mapID\\s+target=\"io_"
                  + page
                  + "\"\\s+url=\"en/html/libs/io/"
                  + page
                  + "\\.html\"\\s*/>");
      assertTrue(
          entry.matcher(map).find(),
          () -> language + " JavaHelp map does not fall back to English " + page + ".html");
    }
  }
}
