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

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.regex.Pattern;
import javax.xml.XMLConstants;
import javax.xml.parsers.DocumentBuilderFactory;
import org.junit.jupiter.api.Test;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;
import org.xml.sax.helpers.DefaultHandler;

class DocumentationStructureTest {

  private static final String BASELINE_RESOURCE = "/documentation-known-issues.txt";
  private static final Path DOC_ROOT = Path.of("build", "resources", "main", "doc");
  private static final Set<String> REQUIRED_HELP_SETS =
      Set.of("doc_de.hs", "doc_en.hs", "doc_fr.hs", "doc_pt.hs", "doc_ru.hs", "doc_zh.hs");
  private static final Set<String> REQUIRED_TARGETS =
      Set.of("top", "guide", "tutorial", "libs");
  private static final Pattern HELP_SET_NAME = Pattern.compile("doc_([a-z]{2})\\.hs");
  private static final Pattern HELP_SET_MAP =
      Pattern.compile(
          "<mapref\\s+[^>]*location=[\"']([^\"']+)[\"']", Pattern.CASE_INSENSITIVE);
  private static final Pattern HELP_SET_VIEW =
      Pattern.compile("<view\\b[^>]*>(.*?)</view>", Pattern.CASE_INSENSITIVE | Pattern.DOTALL);
  private static final Pattern HTML_ANCHOR =
      Pattern.compile("\\b(?:id|name)=[\"']([^\"']+)[\"']", Pattern.CASE_INSENSITIVE);

  @Test
  void javaHelpStructureMatchesKnownIssueBaseline() throws Exception {
    final var expected = readKnownIssues();
    final var actual = validateJavaHelp(DOC_ROOT);

    assertEquals(
        expected,
        actual,
        () ->
            "JavaHelp structure changed. Fix new issues or intentionally update "
                + BASELINE_RESOURCE
                + ".\nCurrent issues:\n"
                + String.join("\n", actual));
  }

  private static List<String> readKnownIssues() throws Exception {
    try (var input = DocumentationStructureTest.class.getResourceAsStream(BASELINE_RESOURCE)) {
      if (input == null) {
        throw new IllegalStateException("Missing test resource " + BASELINE_RESOURCE);
      }
      try (var reader =
          new BufferedReader(new InputStreamReader(input, StandardCharsets.UTF_8))) {
        return reader
            .lines()
            .map(String::trim)
            .filter(line -> !line.isEmpty() && !line.startsWith("#"))
            .sorted()
            .toList();
      }
    }
  }

  private static List<String> validateJavaHelp(Path docRoot) throws Exception {
    final var issues = new ArrayList<String>();
    final var helpSetNames = new TreeSet<String>();
    try (var paths = Files.list(docRoot)) {
      for (final var helpSet : paths.filter(Files::isRegularFile).sorted().toList()) {
        final var matcher = HELP_SET_NAME.matcher(helpSet.getFileName().toString());
        if (matcher.matches()) {
          helpSetNames.add(helpSet.getFileName().toString());
          validateHelpSet(docRoot, helpSet, issues);
        }
      }
    }
    assertEquals(REQUIRED_HELP_SETS, helpSetNames, "Unexpected packaged HelpSet descriptors");
    issues.sort(String::compareTo);
    return issues;
  }

  private static void validateHelpSet(Path docRoot, Path helpSetPath, List<String> issues)
      throws Exception {
    final var helpSetLabel = relativeLabel(docRoot, helpSetPath);
    final var helpSetText = Files.readString(helpSetPath, StandardCharsets.UTF_8);
    try {
      parseXml(helpSetPath);
    } catch (Exception exception) {
      issues.add(helpSetLabel + ": XML is not well-formed");
    }
    final var mapReference = firstMatch(HELP_SET_MAP, helpSetText);
    final var contentsReference = viewData(helpSetText, "TOC");
    final var searchReference = viewData(helpSetText, "Search");

    if (mapReference == null) {
      issues.add(helpSetLabel + ": missing mapref");
      return;
    }
    if (contentsReference == null) {
      issues.add(helpSetLabel + ": missing TOC data");
      return;
    }

    final var mapPath = docRoot.resolve(mapReference).normalize();
    final var contentsPath = docRoot.resolve(contentsReference).normalize();
    validateReferencedPath(docRoot, helpSetLabel, "mapref", mapReference, mapPath, issues);
    validateReferencedPath(
        docRoot, helpSetLabel, "TOC data", contentsReference, contentsPath, issues);
    if (searchReference != null) {
      validateReferencedPath(
          docRoot,
          helpSetLabel,
          "Search data",
          searchReference,
          docRoot.resolve(searchReference).normalize(),
          issues);
    }
    if (!Files.isRegularFile(mapPath) || !Files.isRegularFile(contentsPath)) {
      return;
    }

    validateMapAndContents(docRoot, mapPath, contentsPath, issues);
  }

  private static void validateMapAndContents(
      Path docRoot, Path mapPath, Path contentsPath, List<String> issues) throws Exception {
    final var mapDocument = parseXml(mapPath);
    final var mapEntries = new TreeMap<String, String>();
    final var mapIds = mapDocument.getElementsByTagName("mapID");
    for (var i = 0; i < mapIds.getLength(); i++) {
      final var element = (Element) mapIds.item(i);
      final var target = element.getAttribute("target");
      final var url = element.getAttribute("url");
      if (target.isBlank() || url.isBlank()) {
        issues.add(relativeLabel(docRoot, mapPath) + ": mapID missing target or url");
      } else if (mapEntries.putIfAbsent(target, url) != null) {
        issues.add(relativeLabel(docRoot, mapPath) + ": duplicate mapID '" + target + "'");
      }
    }

    final var targets = new TreeSet<>(REQUIRED_TARGETS);
    final var imageTargets = new TreeSet<String>();
    final var contentsDocument = parseXml(contentsPath);
    final var tocItems = contentsDocument.getElementsByTagName("tocitem");
    for (var i = 0; i < tocItems.getLength(); i++) {
      final var element = (Element) tocItems.item(i);
      final var target = element.getAttribute("target");
      final var image = element.getAttribute("image");
      if (!target.isBlank()) {
        targets.add(target);
      }
      if (!image.isBlank()) {
        imageTargets.add(image);
      }
    }

    for (final var target : targets) {
      validateMappedTarget(
          docRoot, mapPath, contentsPath, mapEntries, "target", target, issues);
    }
    for (final var target : imageTargets) {
      validateMappedTarget(
          docRoot, mapPath, contentsPath, mapEntries, "image target", target, issues);
    }
  }

  private static void validateMappedTarget(
      Path docRoot,
      Path mapPath,
      Path contentsPath,
      Map<String, String> mapEntries,
      String kind,
      String target,
      List<String> issues)
      throws Exception {
    final var url = mapEntries.get(target);
    if (url == null) {
      issues.add(
          relativeLabel(docRoot, contentsPath)
              + ": "
              + kind
              + " '"
              + target
              + "' has no mapID in "
              + mapPath.getFileName());
      return;
    }

    final var hash = url.indexOf('#');
    final var pathPart = hash < 0 ? url : url.substring(0, hash);
    final var fragment = hash < 0 ? "" : url.substring(hash + 1);
    final var targetPath = docRoot.resolve(pathPart).normalize();
    final var mapLabel =
        relativeLabel(docRoot, mapPath) + ": mapID '" + target + "'";
    if (!targetPath.startsWith(docRoot.normalize())) {
      issues.add(mapLabel + " points outside the documentation root " + url);
    } else if (!Files.exists(targetPath)) {
      issues.add(mapLabel + " points to missing file " + url);
    } else if (!pathExistsWithExactCase(docRoot, targetPath)) {
      issues.add(mapLabel + " has path case mismatch " + url);
    } else if (!fragment.isEmpty()
        && (pathPart.endsWith(".html") || pathPart.endsWith(".htm"))
        && !htmlAnchors(targetPath).contains(fragment)) {
      issues.add(mapLabel + " points to missing anchor " + url);
    }
  }

  private static void validateReferencedPath(
      Path docRoot,
      String sourceLabel,
      String kind,
      String reference,
      Path target,
      List<String> issues) {
    if (!target.startsWith(docRoot.normalize())) {
      issues.add(sourceLabel + ": " + kind + " points outside documentation root " + reference);
    } else if (!Files.exists(target)) {
      issues.add(sourceLabel + ": " + kind + " points to missing path " + reference);
    } else if (!pathExistsWithExactCase(docRoot, target)) {
      issues.add(sourceLabel + ": " + kind + " has path case mismatch " + reference);
    }
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
    final var builder = factory.newDocumentBuilder();
    builder.setErrorHandler(
        new DefaultHandler() {
          @Override
          public void error(SAXParseException exception) throws SAXException {
            throw exception;
          }

          @Override
          public void fatalError(SAXParseException exception) throws SAXException {
            throw exception;
          }
        });
    return builder.parse(path.toFile());
  }

  private static String firstMatch(Pattern pattern, String input) {
    final var matcher = pattern.matcher(input);
    return matcher.find() ? matcher.group(1).trim() : null;
  }

  private static String viewData(String helpSet, String viewName) {
    final var views = HELP_SET_VIEW.matcher(helpSet);
    while (views.find()) {
      final var view = views.group(1);
      if (viewName.equals(elementText(view, "name"))) {
        return elementText(view, "data");
      }
    }
    return null;
  }

  private static String elementText(String input, String tag) {
    final var pattern =
        Pattern.compile(
            "<" + tag + "\\b[^>]*>(.*?)</" + tag + ">",
            Pattern.CASE_INSENSITIVE | Pattern.DOTALL);
    final var matcher = pattern.matcher(input);
    return matcher.find() ? matcher.group(1).trim() : null;
  }

  private static Set<String> htmlAnchors(Path path) throws Exception {
    final var anchors = new TreeSet<String>();
    final var matcher = HTML_ANCHOR.matcher(Files.readString(path, StandardCharsets.UTF_8));
    while (matcher.find()) {
      anchors.add(matcher.group(1));
    }
    return anchors;
  }

  private static boolean pathExistsWithExactCase(Path root, Path target) {
    if (!Files.exists(target)) {
      return false;
    }
    var current = root.normalize();
    for (final var part : root.normalize().relativize(target.normalize())) {
      try (var children = Files.list(current)) {
        if (children.noneMatch(child -> child.getFileName().equals(part))) {
          return false;
        }
      } catch (Exception exception) {
        return false;
      }
      current = current.resolve(part);
    }
    return true;
  }

  private static String relativeLabel(Path root, Path path) {
    return root.normalize().relativize(path.normalize()).toString().replace('\\', '/');
  }
}
