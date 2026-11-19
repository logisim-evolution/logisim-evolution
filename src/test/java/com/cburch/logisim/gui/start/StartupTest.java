/*
 * Logisim-evolution - digital logic design tool
 * Copyright by the logisim-evolution developers
 *
 * https://github.com/logisim-evolution/
 *
 * This is free software released under GNU GPLv3 license
 */

package com.cburch.logisim.gui.start;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.cburch.logisim.Main;
import com.cburch.logisim.util.LocaleManager;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Locale;
import java.util.jar.Attributes;
import java.util.jar.JarOutputStream;
import java.util.jar.Manifest;
import java.util.stream.Stream;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;

class StartupTest {
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
  void localeOptionAppliesToHelpText() {
    final var originalOut = System.out;
    final var output = new ByteArrayOutputStream();
    try {
      System.setOut(new PrintStream(output, true, StandardCharsets.UTF_8));

      Startup.parseArgs(new String[] {"--tty", "table", "--locale", "zh", "--help"});

      final var helpText = output.toString(StandardCharsets.UTF_8);
      assertTrue(helpText.contains("显示本参数摘要帮助页。"));
      assertFalse(helpText.contains("Displays this argument summary help page."));
    } finally {
      System.setOut(originalOut);
    }
  }

  @ParameterizedTest
  @ValueSource(strings = {"--help", "--version"})
  void informationalOptionsRequestSuccessfulTermination(String option) {
    final var startup = parseWithoutOutput("--tty", "table", option);

    assertNotNull(startup);
    assertTrue(startup.shallQuit());
  }

  @ParameterizedTest(name = "{0}")
  @MethodSource("invalidOptionArguments")
  void invalidOptionValuesFailParsing(String description, String[] args) {
    assertNull(Startup.parseArgs(args), description);
  }

  @Test
  void parsingResultBelongsToItsStartupInstance() {
    final var regularStartup = Startup.parseArgs(new String[] {"--tty", "table", "test.circ"});
    final var helpStartup = parseWithoutOutput("--tty", "table", "--help");

    assertNotNull(regularStartup);
    assertFalse(regularStartup.shallQuit());
    assertNotNull(helpStartup);
    assertTrue(helpStartup.shallQuit());
    assertFalse(regularStartup.shallQuit());
  }

  @Test
  void fpgaCablePreservesExactQuartusNameForFpgaDownload() {
    final var cableName = "USB-Blaster II [3-2]";

    final var startup =
        Startup.parseArgs(
            new String[] {
              "--fpga-cable", cableName,
              "--test-fpga", "test.circ", "main", "board"
            });

    assertNotNull(startup);
    assertEquals(cableName, startup.getFpgaCableName());

    final var cableAfterFpgaOption =
        Startup.parseArgs(
            new String[] {
              "--test-fpga", "test.circ", "main", "board",
              "--fpga-cable", cableName
            });
    assertNotNull(cableAfterFpgaOption);
    assertEquals(cableName, cableAfterFpgaOption.getFpgaCableName());
  }

  @Test
  void fpgaCableRequiresFpgaDownload() {
    assertNull(
        Startup.parseArgs(
            new String[] {"--tty", "table", "--fpga-cable", "USB-Blaster [1-1]", "test.circ"}));
  }

  @Test
  void loadLibOptionCollectsEveryLibraryInOrder(@TempDir Path tempDir) throws IOException {
    final var jarLib = Files.createFile(tempDir.resolve("components.jar")).toFile();
    final var circLib = Files.createFile(tempDir.resolve("helpers.circ")).toFile();

    final var startup =
        parseWithoutOutput(
            "--load-lib", jarLib.getPath(), "--load-lib", circLib.getPath(), "main.circ");

    assertNotNull(startup);
    assertEquals(List.of(jarLib, circLib), startup.getLibrariesToLoad());
    assertEquals(List.of(new File("main.circ")), startup.getFilesToOpen());
  }

  @Test
  void loadLibOptionIsEmptyByDefault() {
    final var startup = parseWithoutOutput("--tty", "table", "test.circ");

    assertNotNull(startup);
    assertTrue(startup.getLibrariesToLoad().isEmpty());
  }

  @Test
  void loadLibOptionRejectsUnsupportedFileType(@TempDir Path tempDir) throws IOException {
    final var textFile = Files.createFile(tempDir.resolve("notes.txt")).toFile();

    assertNull(parseWithoutOutput("--load-lib", textFile.getPath()));
  }

  @Test
  void loadLibOptionCannotBeCombinedWithTty(@TempDir Path tempDir) throws IOException {
    final var circLib = Files.createFile(tempDir.resolve("helpers.circ")).toFile();

    assertNull(
        parseWithoutOutput("--tty", "table", "--load-lib", circLib.getPath(), "test.circ"));
  }

  @Test
  void jarLibraryClassNameIsReadFromManifest(@TempDir Path tempDir) throws IOException {
    final var manifest = new Manifest();
    manifest.getMainAttributes().put(Attributes.Name.MANIFEST_VERSION, "1.0");
    manifest.getMainAttributes().putValue("Library-Class", "com.example.MyLibrary");
    final var jarWithClass = tempDir.resolve("with-class.jar").toFile();
    // An empty JAR containing only the manifest is enough for this test.
    new JarOutputStream(new FileOutputStream(jarWithClass), manifest).close();

    assertEquals("com.example.MyLibrary", Startup.getJarLibraryClassName(jarWithClass));
  }

  @Test
  void jarLibraryClassNameIsNullWhenUnavailable(@TempDir Path tempDir) throws IOException {
    final var jarWithoutManifest = tempDir.resolve("no-manifest.jar").toFile();
    // No manifest, therefore no "Library-Class" attribute.
    new JarOutputStream(new FileOutputStream(jarWithoutManifest)).close();
    final var notAJar = Files.writeString(tempDir.resolve("broken.jar"), "not a jar").toFile();

    assertNull(Startup.getJarLibraryClassName(jarWithoutManifest));
    assertNull(Startup.getJarLibraryClassName(notAJar));
  }

  private static Stream<Arguments> invalidOptionArguments() {
    return Stream.of(
        Arguments.of(
            "invalid locale", new String[] {"--tty", "table", "--locale", "not-a-locale"}),
        Arguments.of("invalid TTY format", new String[] {"--tty", "invalid"}),
        Arguments.of(
            "duplicate substitution",
            new String[] {
              "--tty", "table", "--substitute", "old.circ", "new.circ", "--substitute",
              "old.circ", "other.circ"
            }),
        Arguments.of(
            "invalid load arity",
            new String[] {"--tty", "table", "--load", "memory.hex", "label", "extra"}),
        Arguments.of(
            "duplicate load label",
            new String[] {
              "test.circ", "--tty", "table", "--load", "first.hex", "label", "--load",
              "second.hex", "label"
            }),
        Arguments.of(
            "invalid gate style", new String[] {"--tty", "table", "--gates", "invalid"}),
        Arguments.of(
            "invalid geometry", new String[] {"--tty", "table", "--geometry", "invalid"}),
        Arguments.of(
            "invalid template", new String[] {"--tty", "table", "--user-template", "missing-template.circ"}),
        Arguments.of(
            "missing library file", new String[] {"--load-lib", "missing-library.jar"}),
        Arguments.of(
            "missing library argument", new String[] {"--load-lib"}),
        Arguments.of(
            "invalid FPGA flag",
            new String[] {"--test-fpga", "test.circ", "main", "board", "invalid"}));
  }

  private static Startup parseWithoutOutput(String... args) {
    final var originalOut = System.out;
    try {
      System.setOut(
          new PrintStream(new ByteArrayOutputStream(), true, StandardCharsets.UTF_8));
      return Startup.parseArgs(args);
    } finally {
      System.setOut(originalOut);
    }
  }
}
