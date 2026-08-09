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
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.fail;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import javax.xml.XMLConstants;
import javax.xml.parsers.DocumentBuilderFactory;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

class DocumentationGeneratorTest {

  private static final Path DOC_ROOT = Path.of("src", "main", "resources", "doc");
  private static final Path MANIFEST = Path.of("src", "main", "doc", "guide-memory.xml");
  private static final Path GERMAN_OVERLAY =
      Path.of("src", "main", "doc", "locales", "de-guide-memory.xml");

  @TempDir Path temporaryDirectory;

  @Test
  void generatesLegacyCompatibleEnglishAndGermanMemoryTrees() throws Exception {
    DocumentationGenerator.generate(
        MANIFEST, DOC_ROOT, temporaryDirectory, List.of(GERMAN_OVERLAY));

    assertGeneratedMapMatchesLegacy("en");
    assertGeneratedMapMatchesLegacy("de");
    assertGeneratedTocMatchesLegacy("en");
    assertGeneratedTocMatchesLegacy("de");

    final var germanMap = mapEntries(parseXml(temporaryDirectory.resolve("map_de.jhm")));
    assertEquals(
        4, germanMap.values().stream().filter(path -> path.startsWith("de/")).count());
    assertEquals(
        6, germanMap.values().stream().filter(path -> path.startsWith("en/")).count());
  }

  @Test
  void sparseOverlayFallsBackToEnglishFields() throws Exception {
    final var sparseOverlay = temporaryDirectory.resolve("sparse-de.xml");
    Files.writeString(
        sparseOverlay,
        """
        <?xml version="1.0" encoding="UTF-8"?>
        <locale language="de">
          <topic
              id="mem_guide"
              title="Speicherbauelemente"
              path="de/html/guide/mem/index.html" />
        </locale>
        """,
        StandardCharsets.UTF_8);
    final var output = temporaryDirectory.resolve("sparse-output");

    DocumentationGenerator.generate(MANIFEST, DOC_ROOT, output, List.of(sparseOverlay));

    final var germanMap = mapEntries(parseXml(output.resolve("map_de.jhm")));
    assertEquals("de/html/guide/mem/index.html", germanMap.get("mem_guide"));
    assertEquals("en/html/guide/mem/mem-poke.html", germanMap.get("mem_poke"));

    final var germanToc = parseXml(output.resolve("de").resolve("contents.xml"));
    assertEquals("Speicherbauelemente", findTocItem(germanToc, "mem_guide").getAttribute("text"));
    assertEquals("Memory poke", findTocItem(germanToc, "mem_poke").getAttribute("text"));
  }

  @Test
  void rejectsExplicitLocalizedPathThatDoesNotExist() throws Exception {
    final var invalidOverlay = temporaryDirectory.resolve("invalid-de.xml");
    Files.writeString(
        invalidOverlay,
        """
        <?xml version="1.0" encoding="UTF-8"?>
        <locale language="de">
          <topic
              id="mem_guide"
              title="Speicherbauelemente"
              path="de/html/guide/mem/missing.html" />
        </locale>
        """,
        StandardCharsets.UTF_8);

    assertThrows(
        IllegalArgumentException.class,
        () ->
            DocumentationGenerator.generate(
                MANIFEST,
                DOC_ROOT,
                temporaryDirectory.resolve("invalid-output"),
                List.of(invalidOverlay)));
  }

  private void assertGeneratedMapMatchesLegacy(String language) throws Exception {
    final var generated =
        mapEntries(parseXml(temporaryDirectory.resolve("map_" + language + ".jhm")));
    final var legacy = mapEntries(parseXml(DOC_ROOT.resolve("map_" + language + ".jhm")));
    final var legacySubset = new LinkedHashMap<String, String>();
    for (final var target : generated.keySet()) {
      legacySubset.put(target, legacy.get(target));
    }
    assertEquals(generated, legacySubset);
  }

  private void assertGeneratedTocMatchesLegacy(String language) throws Exception {
    final var generated =
        parseXml(temporaryDirectory.resolve(language).resolve("contents.xml"));
    final var legacy = parseXml(DOC_ROOT.resolve(language).resolve("contents.xml"));
    final var generatedRoot = findTocItem(generated, "mem_guide");
    final var legacyRoot = findTocItem(legacy, "mem_guide");
    assertEquals(tocEntries(generatedRoot), tocEntries(legacyRoot));
  }

  private static Document parseXml(Path path) throws Exception {
    final var factory = DocumentBuilderFactory.newInstance();
    factory.setFeature("http://xml.org/sax/features/external-general-entities", false);
    factory.setFeature("http://xml.org/sax/features/external-parameter-entities", false);
    factory.setFeature("http://apache.org/xml/features/nonvalidating/load-external-dtd", false);
    factory.setAttribute(XMLConstants.ACCESS_EXTERNAL_DTD, "");
    factory.setAttribute(XMLConstants.ACCESS_EXTERNAL_SCHEMA, "");
    factory.setXIncludeAware(false);
    factory.setExpandEntityReferences(false);
    return factory.newDocumentBuilder().parse(path.toFile());
  }

  private static Map<String, String> mapEntries(Document document) {
    final var entries = new LinkedHashMap<String, String>();
    final var mapIds = document.getElementsByTagName("mapID");
    for (var i = 0; i < mapIds.getLength(); i++) {
      final var element = (Element) mapIds.item(i);
      entries.putIfAbsent(element.getAttribute("target"), element.getAttribute("url"));
    }
    return entries;
  }

  private static Element findTocItem(Document document, String target) {
    final var items = document.getElementsByTagName("tocitem");
    for (var i = 0; i < items.getLength(); i++) {
      final var element = (Element) items.item(i);
      if (target.equals(element.getAttribute("target"))) {
        return element;
      }
    }
    return fail("Missing TOC target " + target);
  }

  private static List<String> tocEntries(Element root) {
    final var entries = new ArrayList<String>();
    collectTocEntries(root, 0, entries);
    return entries;
  }

  private static void collectTocEntries(Element item, int depth, List<String> entries) {
    entries.add(
        depth + "|" + item.getAttribute("target") + "|" + item.getAttribute("text"));
    for (var child = item.getFirstChild(); child != null; child = child.getNextSibling()) {
      if (child instanceof Element element && "tocitem".equals(element.getTagName())) {
        collectTocEntries(element, depth + 1, entries);
      }
    }
  }
}
