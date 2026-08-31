/*
 * Logisim-evolution - digital logic design tool and simulator
 * Copyright by the Logisim-evolution developers
 *
 * https://github.com/logisim-evolution/
 *
 * This is free software released under GNU GPLv3 license
 */

package com.cburch.logisim.docs;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.cburch.logisim.Main;
import com.cburch.logisim.gui.start.Startup;
import com.cburch.logisim.util.LocaleManager;
import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Locale;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.TreeSet;
import java.util.regex.Pattern;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class CommandLineDocumentationTest {

  private static final Path DOC_ROOT = Path.of("build", "resources", "main", "doc");
  private static final Path SETTINGS =
      Path.of("build", "resources", "main", "resources", "logisim", "settings.properties");
  private static final Pattern HELP_OPTION =
      Pattern.compile("(?m)^\\s*(?:-[a-z],\\s*)?--([a-z][a-z0-9-]*)(?:\\s|$)");
  private static final Pattern DOCUMENTED_OPTION =
      Pattern.compile("--([a-z][a-z0-9-]*)");
  private static final Pattern DOCUMENTED_LOCALE =
      Pattern.compile("<tr>\\s*<td>\\s*<tt>([a-z]{2})</tt>", Pattern.CASE_INSENSITIVE);
  private static final Pattern COMMAND_LINE_MAP =
      Pattern.compile(
          "<mapID\\s+target=[\"']prefs_cmdline[\"']\\s+url=[\"']([^\"']+)[\"']");

  private boolean originalHeadless;
  private Locale originalLocale;

  @BeforeEach
  void recordGlobalState() {
    originalHeadless = Main.headless;
    originalLocale = LocaleManager.getLocale();
  }

  @AfterEach
  void restoreGlobalState() {
    Main.headless = originalHeadless;
    LocaleManager.setLocale(originalLocale);
  }

  @Test
  void documentsEveryCurrentLongOptionInEnglishAndChinese() throws Exception {
    final var currentOptions = currentLongOptions();

    assertFalse(currentOptions.isEmpty(), "No options were parsed from --help output");
    assertEquals(currentOptions, documentedOptions("en"));
    assertEquals(currentOptions, documentedOptions("zh"));
  }

  @Test
  void documentsEverySupportedLocaleInEnglishAndChinese() throws Exception {
    final var settings = new Properties();
    try (var input = Files.newInputStream(SETTINGS)) {
      settings.load(input);
    }
    final var supported =
        new TreeSet<>(Arrays.asList(settings.getProperty("locales", "").trim().split("\\s+")));

    assertFalse(supported.isEmpty(), "No locales were found in settings.properties");
    assertEquals(supported, documentedLocales("en"));
    assertEquals(supported, documentedLocales("zh"));
  }

  @Test
  void usesCurrentCommandLinePagesOrEnglishFallbacks() throws Exception {
    final var expected =
        Map.of(
            "de", "en/html/guide/prefs/pref-cmdline.html",
            "en", "en/html/guide/prefs/pref-cmdline.html",
            "fr", "en/html/guide/prefs/pref-cmdline.html",
            "pt", "en/html/guide/prefs/pref-cmdline.html",
            "ru", "en/html/guide/prefs/pref-cmdline.html",
            "zh", "zh/html/guide/prefs/pref-cmdline.html");

    for (final var entry : expected.entrySet()) {
      final var map =
          Files.readString(DOC_ROOT.resolve("map_" + entry.getKey() + ".jhm"), StandardCharsets.UTF_8);
      final var matcher = COMMAND_LINE_MAP.matcher(map);
      assertTrue(matcher.find(), () -> "Missing prefs_cmdline in map_" + entry.getKey() + ".jhm");
      assertEquals(entry.getValue(), matcher.group(1), entry.getKey());
    }
  }

  private static Set<String> currentLongOptions() {
    final var originalOut = System.out;
    final var output = new ByteArrayOutputStream();
    try {
      System.setOut(new PrintStream(output, true, StandardCharsets.UTF_8));
      assertNotNull(
          Startup.parseArgs(new String[] {"--tty", "table", "--locale", "en", "--help"}));
    } finally {
      System.setOut(originalOut);
    }
    return matches(HELP_OPTION, output.toString(StandardCharsets.UTF_8));
  }

  private static Set<String> documentedOptions(String language) throws Exception {
    return matches(DOCUMENTED_OPTION, commandLinePage(language));
  }

  private static Set<String> documentedLocales(String language) throws Exception {
    return matches(DOCUMENTED_LOCALE, commandLinePage(language));
  }

  private static String commandLinePage(String language) throws Exception {
    return Files.readString(
        DOC_ROOT.resolve(language + "/html/guide/prefs/pref-cmdline.html"),
        StandardCharsets.UTF_8);
  }

  private static Set<String> matches(Pattern pattern, String text) {
    final var result = new TreeSet<String>();
    final var matcher = pattern.matcher(text);
    while (matcher.find()) {
      result.add(matcher.group(1));
    }
    return result;
  }
}
