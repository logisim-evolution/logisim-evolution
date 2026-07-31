/*
 * Logisim-evolution - digital logic design tool and simulator
 * Copyright by the Logisim-evolution developers
 *
 * https://github.com/logisim-evolution/
 *
 * This is free software released under GNU GPLv3 license
 */

package com.cburch.logisim.docs;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;
import javax.xml.XMLConstants;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

public final class DocumentationGenerator {

  private static final String ENGLISH = "en";
  private static final Pattern LANGUAGE = Pattern.compile("[a-z]{2}");
  private static final String MAP_PUBLIC_ID =
      "-//Sun Microsystems Inc.//DTD JavaHelp Map Version 2.0//EN";
  private static final String MAP_SYSTEM_ID =
      "http://java.sun.com/products/javahelp/map_2_0.dtd";
  private static final String TOC_PUBLIC_ID =
      "-//Sun Microsystems Inc.//DTD JavaHelp TOC Version 2.0//EN";
  private static final String TOC_SYSTEM_ID =
      "http://java.sun.com/products/javahelp/toc_2_0.dtd";

  private DocumentationGenerator() {}

  public static void main(String[] args) throws Exception {
    if (args.length < 3) {
      throw new IllegalArgumentException(
          "Usage: DocumentationGenerator <manifest> <doc-root> <output-root> [overlay ...]");
    }

    final var overlays = new ArrayList<Path>();
    for (var i = 3; i < args.length; i++) {
      overlays.add(Path.of(args[i]));
    }
    generate(Path.of(args[0]), Path.of(args[1]), Path.of(args[2]), overlays);
  }

  public static void generate(
      Path manifestPath, Path docRoot, Path outputRoot, List<Path> overlayPaths)
      throws Exception {
    final var topics = readManifest(manifestPath, docRoot);
    writeLocale(outputRoot, ENGLISH, resolveTopics(topics, Map.of()));

    final var languages = new HashSet<String>();
    languages.add(ENGLISH);
    for (final var overlayPath : overlayPaths) {
      final var overlay = readOverlay(overlayPath, topics, docRoot);
      if (!languages.add(overlay.language())) {
        throw new IllegalArgumentException(
            "Duplicate locale overlay for " + overlay.language());
      }
      writeLocale(outputRoot, overlay.language(), resolveTopics(topics, overlay.entries()));
    }
  }

  private static List<Topic> readManifest(Path path, Path docRoot) throws Exception {
    final var document = parseXml(path);
    final var root = document.getDocumentElement();
    if (!"documentation".equals(root.getTagName())) {
      throw new IllegalArgumentException("Expected <documentation> in " + path);
    }
    if (!ENGLISH.equals(requiredAttribute(root, "language", path))) {
      throw new IllegalArgumentException("The canonical manifest language must be English");
    }

    final var ids = new HashSet<String>();
    final var topics = new ArrayList<Topic>();
    for (final var element : childElements(root, "topic")) {
      topics.add(readTopic(element, path, docRoot, ids));
    }
    if (topics.isEmpty()) {
      throw new IllegalArgumentException("The documentation manifest has no topics");
    }
    return List.copyOf(topics);
  }

  private static Topic readTopic(
      Element element, Path source, Path docRoot, Set<String> ids) throws IOException {
    final var id = requiredAttribute(element, "id", source);
    if (!ids.add(id)) {
      throw new IllegalArgumentException("Duplicate topic id '" + id + "' in " + source);
    }
    final var path = requiredAttribute(element, "path", source);
    validateDocumentPath(docRoot, path, source);

    final var children = new ArrayList<Topic>();
    for (final var child : childElements(element, "topic")) {
      children.add(readTopic(child, source, docRoot, ids));
    }
    return new Topic(
        id,
        requiredAttribute(element, "title", source),
        path,
        List.copyOf(children));
  }

  private static LocaleOverlay readOverlay(Path path, List<Topic> topics, Path docRoot)
      throws Exception {
    final var document = parseXml(path);
    final var root = document.getDocumentElement();
    if (!"locale".equals(root.getTagName())) {
      throw new IllegalArgumentException("Expected <locale> in " + path);
    }
    final var language = requiredAttribute(root, "language", path);
    if (!LANGUAGE.matcher(language).matches() || ENGLISH.equals(language)) {
      throw new IllegalArgumentException("Invalid overlay language '" + language + "'");
    }

    final var knownIds = new HashSet<String>();
    collectIds(topics, knownIds);
    final var entries = new HashMap<String, LocalizedTopic>();
    for (final var element : childElements(root, "topic")) {
      final var id = requiredAttribute(element, "id", path);
      if (!knownIds.contains(id)) {
        throw new IllegalArgumentException("Unknown topic id '" + id + "' in " + path);
      }
      final var localizedPath = optionalAttribute(element, "path");
      if (localizedPath != null) {
        validateDocumentPath(docRoot, localizedPath, path);
      }
      final var entry =
          new LocalizedTopic(requiredAttribute(element, "title", path), localizedPath);
      if (entries.putIfAbsent(id, entry) != null) {
        throw new IllegalArgumentException("Duplicate topic id '" + id + "' in " + path);
      }
    }
    return new LocaleOverlay(language, Map.copyOf(entries));
  }

  private static List<ResolvedTopic> resolveTopics(
      List<Topic> topics, Map<String, LocalizedTopic> overlay) {
    return topics.stream().map(topic -> resolveTopic(topic, overlay)).toList();
  }

  private static ResolvedTopic resolveTopic(
      Topic topic, Map<String, LocalizedTopic> overlay) {
    final var localized = overlay.get(topic.id());
    final var title = localized == null ? topic.title() : localized.title();
    final var path =
        localized == null || localized.path() == null ? topic.path() : localized.path();
    final var children =
        topic.children().stream().map(child -> resolveTopic(child, overlay)).toList();
    return new ResolvedTopic(topic.id(), title, path, children);
  }

  private static void writeLocale(Path outputRoot, String language, List<ResolvedTopic> topics)
      throws Exception {
    writeMap(outputRoot.resolve("map_" + language + ".jhm"), topics);
    writeToc(outputRoot.resolve(language).resolve("contents.xml"), topics);
  }

  private static void writeMap(Path output, List<ResolvedTopic> topics) throws Exception {
    final var document = newDocument();
    final var root = document.createElement("map");
    root.setAttribute("version", "2.0");
    document.appendChild(root);
    appendMapEntries(document, root, topics);
    writeXml(document, output, MAP_PUBLIC_ID, MAP_SYSTEM_ID);
  }

  private static void appendMapEntries(
      Document document, Element root, List<ResolvedTopic> topics) {
    for (final var topic : topics) {
      final var entry = document.createElement("mapID");
      entry.setAttribute("target", topic.id());
      entry.setAttribute("url", topic.path());
      root.appendChild(entry);
      appendMapEntries(document, root, topic.children());
    }
  }

  private static void writeToc(Path output, List<ResolvedTopic> topics) throws Exception {
    final var document = newDocument();
    final var root = document.createElement("toc");
    root.setAttribute("version", "2.0");
    document.appendChild(root);
    for (final var topic : topics) {
      root.appendChild(createTocItem(document, topic));
    }
    writeXml(document, output, TOC_PUBLIC_ID, TOC_SYSTEM_ID);
  }

  private static Element createTocItem(Document document, ResolvedTopic topic) {
    final var item = document.createElement("tocitem");
    item.setAttribute("target", topic.id());
    item.setAttribute("text", topic.title());
    for (final var child : topic.children()) {
      item.appendChild(createTocItem(document, child));
    }
    return item;
  }

  private static Document parseXml(Path path) throws Exception {
    final var factory = DocumentBuilderFactory.newInstance();
    factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
    factory.setFeature("http://xml.org/sax/features/external-general-entities", false);
    factory.setFeature("http://xml.org/sax/features/external-parameter-entities", false);
    factory.setFeature("http://apache.org/xml/features/nonvalidating/load-external-dtd", false);
    factory.setAttribute(XMLConstants.ACCESS_EXTERNAL_DTD, "");
    factory.setAttribute(XMLConstants.ACCESS_EXTERNAL_SCHEMA, "");
    factory.setXIncludeAware(false);
    factory.setExpandEntityReferences(false);
    return factory.newDocumentBuilder().parse(path.toFile());
  }

  private static Document newDocument() throws Exception {
    return DocumentBuilderFactory.newInstance().newDocumentBuilder().newDocument();
  }

  private static void writeXml(
      Document document, Path output, String publicId, String systemId) throws Exception {
    Files.createDirectories(output.getParent());
    final var factory = TransformerFactory.newInstance();
    factory.setAttribute(XMLConstants.ACCESS_EXTERNAL_DTD, "");
    factory.setAttribute(XMLConstants.ACCESS_EXTERNAL_STYLESHEET, "");
    final var transformer = factory.newTransformer();
    transformer.setOutputProperty(OutputKeys.ENCODING, "UTF-8");
    transformer.setOutputProperty(OutputKeys.INDENT, "yes");
    transformer.setOutputProperty(OutputKeys.DOCTYPE_PUBLIC, publicId);
    transformer.setOutputProperty(OutputKeys.DOCTYPE_SYSTEM, systemId);
    transformer.setOutputProperty("{http://xml.apache.org/xslt}indent-amount", "2");
    transformer.transform(new DOMSource(document), new StreamResult(output.toFile()));
  }

  private static List<Element> childElements(Element parent, String tagName) {
    final var elements = new ArrayList<Element>();
    for (var child = parent.getFirstChild(); child != null; child = child.getNextSibling()) {
      if (child instanceof Element element && tagName.equals(element.getTagName())) {
        elements.add(element);
      }
    }
    return elements;
  }

  private static String requiredAttribute(Element element, String name, Path source) {
    final var value = optionalAttribute(element, name);
    if (value == null) {
      throw new IllegalArgumentException(
          "Missing attribute '" + name + "' on <" + element.getTagName() + "> in " + source);
    }
    return value;
  }

  private static String optionalAttribute(Element element, String name) {
    final var value = element.getAttribute(name).trim();
    return value.isEmpty() ? null : value;
  }

  private static void validateDocumentPath(Path docRoot, String value, Path source)
      throws IOException {
    final var relative = Path.of(value).normalize();
    final var normalizedRoot = docRoot.toAbsolutePath().normalize();
    final var resolved = normalizedRoot.resolve(relative).normalize();
    if (relative.isAbsolute() || !resolved.startsWith(normalizedRoot)) {
      throw new IllegalArgumentException(
          "Documentation path points outside the documentation root in " + source + ": " + value);
    }
    if (!Files.isRegularFile(resolved)) {
      throw new IllegalArgumentException(
          "Documentation path does not exist in " + source + ": " + value);
    }
    if (!pathExistsWithExactCase(normalizedRoot, resolved)) {
      throw new IllegalArgumentException(
          "Documentation path has a case mismatch in " + source + ": " + value);
    }
  }

  private static boolean pathExistsWithExactCase(Path root, Path target) throws IOException {
    var current = root;
    for (final var part : root.relativize(target)) {
      try (var children = Files.list(current)) {
        if (children.noneMatch(child -> child.getFileName().equals(part))) {
          return false;
        }
      }
      current = current.resolve(part);
    }
    return true;
  }

  private static void collectIds(List<Topic> topics, Set<String> ids) {
    for (final var topic : topics) {
      ids.add(topic.id());
      collectIds(topic.children(), ids);
    }
  }

  private record Topic(String id, String title, String path, List<Topic> children) {}

  private record LocalizedTopic(String title, String path) {}

  private record LocaleOverlay(String language, Map<String, LocalizedTopic> entries) {}

  private record ResolvedTopic(
      String id, String title, String path, List<ResolvedTopic> children) {}
}
