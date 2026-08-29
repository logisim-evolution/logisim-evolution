/*
 * Logisim-evolution - digital logic design tool and simulator
 * Copyright by the Logisim-evolution developers
 *
 * https://github.com/logisim-evolution/
 *
 * This is free software released under GNU GPLv3 license.
 */

package com.cburch.logisim.docs;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.regex.Pattern;
import javax.xml.XMLConstants;
import javax.xml.parsers.DocumentBuilderFactory;
import org.junit.jupiter.api.Test;
import org.w3c.dom.Element;

class ExtraIoDocumentationTest {
  private static final Path DOC_ROOT = Path.of("src", "main", "resources", "doc");
  private static final Path EXTRA_IO_ROOT = DOC_ROOT.resolve("en/html/libs/ioextra");
  private static final Path LIBRARY_SOURCE =
      Path.of(
          "src",
          "main",
          "java",
          "com",
          "cburch",
          "logisim",
          "std",
          "io",
          "extra",
          "ExtraIoLibrary.java");
  private static final Map<String, String> COMPONENT_PAGES =
      Map.of(
          "Switch", "switch",
          "Buzzer", "buzzer",
          "Slider", "slider",
          "DigitalOscilloscope", "digitalscope",
          "PlaRom", "pla",
          "TwoWaySwitch", "twowayswitch",
          "TwoPinLed", "twopinled");
  private static final Map<String, String> TARGET_PAGES =
      Map.of(
          "ioex", "index",
          "ioex_switch", "switch",
          "ioex_buzzer", "buzzer",
          "ioex_slider", "slider",
          "ioex_digitalscope", "digitalscope",
          "ioex_plarom", "pla",
          "ioex_twowayswitch", "twowayswitch",
          "ioex_twopinled", "twopinled");
  private static final Pattern FACTORY_DESCRIPTION =
      Pattern.compile("new\\s+FactoryDescription\\(\\s*(\\w+)\\.class", Pattern.DOTALL);

  @Test
  void everyRegisteredComponentHasAnEnglishReferencePage() throws Exception {
    final var source = Files.readString(LIBRARY_SOURCE, StandardCharsets.UTF_8);
    final var matcher = FACTORY_DESCRIPTION.matcher(source);
    final var registeredComponents = new TreeSet<String>();
    while (matcher.find()) {
      registeredComponents.add(matcher.group(1));
    }

    assertEquals(
        new TreeSet<>(COMPONENT_PAGES.keySet()),
        registeredComponents,
        "Update the Extra I/O reference when the registered component list changes");
    for (final var page : COMPONENT_PAGES.values()) {
      final var path = EXTRA_IO_ROOT.resolve(page + ".html");
      assertTrue(Files.isRegularFile(path), () -> "Missing Extra I/O reference page " + path);
    }
  }

  @Test
  void englishIndexesLinkEveryReferencePage() throws Exception {
    final var localIndex =
        Files.readString(EXTRA_IO_ROOT.resolve("index.html"), StandardCharsets.UTF_8);
    final var globalIndex =
        Files.readString(DOC_ROOT.resolve("en/html/libs/index.html"), StandardCharsets.UTF_8);

    for (final var page : COMPONENT_PAGES.values()) {
      assertTrue(
          localIndex.contains("href=\"" + page + ".html\""),
          () -> "English Extra I/O index does not link " + page + ".html");
      assertTrue(
          globalIndex.contains("href=\"ioextra/" + page + ".html\""),
          () -> "English library index does not link Extra I/O page " + page + ".html");
    }
  }

  @Test
  void exposedLocalizedIndexesFallBackToEnglish() throws Exception {
    final var chineseIndex =
        Files.readString(
            DOC_ROOT.resolve("zh/html/libs/ioextra/index.html"), StandardCharsets.UTF_8);

    for (final var page : COMPONENT_PAGES.values()) {
      final var localFallback =
          "href=\"../../../../en/html/libs/ioextra/" + page + ".html\"";
      assertTrue(
          chineseIndex.contains(localFallback),
          () -> "Chinese Extra I/O index does not fall back to English " + page + ".html");
      for (final var language : new String[] {"fr", "zh"}) {
        final var globalIndex =
            Files.readString(
                DOC_ROOT.resolve(language + "/html/libs/index.html"), StandardCharsets.UTF_8);
        final var globalFallback =
            "href=\"../../../en/html/libs/ioextra/" + page + ".html\"";
        assertTrue(
            globalIndex.contains(globalFallback),
            () -> language + " library index does not fall back to " + page + ".html");
      }
    }
  }

  @Test
  void packagedHelpTreesExposeEveryReferencePage() throws Exception {
    for (final var language : new String[] {"en", "de", "fr", "ru", "zh"}) {
      final var contents = parseXml(DOC_ROOT.resolve(language + "/contents.xml"));
      final var actualTargets = new TreeSet<String>();
      final var items = contents.getElementsByTagName("tocitem");
      for (var i = 0; i < items.getLength(); i++) {
        actualTargets.add(((Element) items.item(i)).getAttribute("target"));
      }

      assertTrue(
          actualTargets.containsAll(TARGET_PAGES.keySet()),
          () -> language + " JavaHelp tree does not expose all Extra I/O pages");
    }
  }

  @Test
  void packagedHelpMapsFallBackToEnglish() throws Exception {
    for (final var language : new String[] {"en", "de", "fr", "pt", "ru", "zh"}) {
      final var map = parseXml(DOC_ROOT.resolve("map_" + language + ".jhm"));
      final var actualEntries = new TreeMap<String, String>();
      final var entries = map.getElementsByTagName("mapID");
      for (var i = 0; i < entries.getLength(); i++) {
        final var entry = (Element) entries.item(i);
        actualEntries.put(entry.getAttribute("target"), entry.getAttribute("url"));
      }

      for (final var target : TARGET_PAGES.entrySet()) {
        assertEquals(
            "en/html/libs/ioextra/" + target.getValue() + ".html",
            actualEntries.get(target.getKey()),
            () -> language + " JavaHelp map has an incorrect Extra I/O fallback");
      }
    }
  }

  private static org.w3c.dom.Document parseXml(Path path) throws Exception {
    final var factory = DocumentBuilderFactory.newInstance();
    factory.setFeature("http://xml.org/sax/features/external-general-entities", false);
    factory.setFeature("http://xml.org/sax/features/external-parameter-entities", false);
    factory.setFeature(
        "http://apache.org/xml/features/nonvalidating/load-external-dtd", false);
    factory.setAttribute(XMLConstants.ACCESS_EXTERNAL_DTD, "");
    factory.setAttribute(XMLConstants.ACCESS_EXTERNAL_SCHEMA, "");
    factory.setXIncludeAware(false);
    factory.setExpandEntityReferences(false);
    return factory.newDocumentBuilder().parse(path.toFile());
  }
}
