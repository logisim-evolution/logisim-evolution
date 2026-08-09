/*
 * Logisim-evolution - digital logic design tool and simulator
 * Copyright by the Logisim-evolution developers
 *
 * https://github.com/logisim-evolution/
 *
 * This is free software released under GNU GPLv3 license
 */

package com.cburch.logisim.gui.generic;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.cburch.logisim.util.XmlUtil;
import java.awt.Color;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Base64;
import javax.imageio.ImageIO;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.w3c.dom.Element;
import org.w3c.dom.Node;

class TikZWriterTest {

  private static final String PNG_DATA_URI_PREFIX = "data:image/png;base64,";
  private static final String XLINK_NAMESPACE = "http://www.w3.org/1999/xlink";

  @TempDir Path tempDir;

  @Test
  void svgEmbedsImagesWithTheCurrentTransformAndDrawingOrder() throws Exception {
    final var source = new BufferedImage(2, 2, BufferedImage.TYPE_INT_ARGB);
    source.setRGB(0, 0, new Color(255, 0, 0, 128).getRGB());
    source.setRGB(1, 0, Color.GREEN.getRGB());
    source.setRGB(0, 1, Color.BLUE.getRGB());
    source.setRGB(1, 1, Color.WHITE.getRGB());

    final var writer = new TikZWriter();
    writer.fillRect(0, 0, 1, 1);
    writer.translate(10, 20);
    writer.scale(2, 3);
    assertTrue(writer.drawImage(source, 1, 2, null));
    writer.fillRect(3, 4, 1, 1);

    final var output = tempDir.resolve("embedded-image.svg");
    writer.writeSvg(40, 50, output.toFile());

    final var document = parseSvg(output);
    final var children = document.getDocumentElement().getChildNodes();
    final var elementNames = new ArrayList<String>();
    for (var index = 0; index < children.getLength(); index++) {
      final var child = children.item(index);
      if (child.getNodeType() == Node.ELEMENT_NODE) elementNames.add(child.getNodeName());
    }
    assertEquals(java.util.List.of("rect", "image", "rect"), elementNames);

    final var image = (Element) document.getElementsByTagName("image").item(0);
    assertEquals("0", image.getAttribute("x"));
    assertEquals("0", image.getAttribute("y"));
    assertEquals("2", image.getAttribute("width"));
    assertEquals("2", image.getAttribute("height"));
    assertEquals("matrix(2 0 0 3 12 26)", image.getAttribute("transform"));

    final var decoded = decodeEmbeddedPng(image);
    assertEquals(2, decoded.getWidth());
    assertEquals(2, decoded.getHeight());
    assertEquals(source.getRGB(0, 0), decoded.getRGB(0, 0));
    assertEquals(source.getRGB(1, 0), decoded.getRGB(1, 0));
    assertEquals(source.getRGB(0, 1), decoded.getRGB(0, 1));
    assertEquals(source.getRGB(1, 1), decoded.getRGB(1, 1));
  }

  @Test
  void svgCropsAndScalesTheSourceRectangle() throws Exception {
    final var source = new BufferedImage(3, 2, BufferedImage.TYPE_INT_ARGB);
    source.setRGB(0, 0, Color.RED.getRGB());
    source.setRGB(1, 0, Color.GREEN.getRGB());
    source.setRGB(2, 0, Color.BLUE.getRGB());
    source.setRGB(0, 1, Color.CYAN.getRGB());
    source.setRGB(1, 1, Color.MAGENTA.getRGB());
    source.setRGB(2, 1, Color.YELLOW.getRGB());

    final var writer = new TikZWriter();
    assertTrue(writer.drawImage(source, 10, 20, 14, 22, 1, 0, 3, 2, null));

    final var output = tempDir.resolve("cropped-image.svg");
    writer.writeSvg(30, 40, output.toFile());

    final var document = parseSvg(output);
    final var image = (Element) document.getElementsByTagName("image").item(0);
    assertEquals("2", image.getAttribute("width"));
    assertEquals("2", image.getAttribute("height"));
    assertEquals("matrix(2 0 0 1 10 20)", image.getAttribute("transform"));

    final var decoded = decodeEmbeddedPng(image);
    assertEquals(source.getRGB(1, 0), decoded.getRGB(0, 0));
    assertEquals(source.getRGB(2, 0), decoded.getRGB(1, 0));
    assertEquals(source.getRGB(1, 1), decoded.getRGB(0, 1));
    assertEquals(source.getRGB(2, 1), decoded.getRGB(1, 1));
  }

  @Test
  void tikzOutputMarksRasterImagesAsUnsupported() throws Exception {
    final var source = new BufferedImage(1, 1, BufferedImage.TYPE_INT_ARGB);
    source.setRGB(0, 0, Color.RED.getRGB());

    final var writer = new TikZWriter();
    assertTrue(writer.drawImage(source, 0, 0, null));

    final var output = tempDir.resolve("image.tex");
    writer.writeFile(output.toFile());

    final var content = Files.readString(output);
    assertTrue(content.contains("% Raster image omitted: TikZ image export is not supported."));
    assertFalse(content.contains(PNG_DATA_URI_PREFIX));
  }

  private static org.w3c.dom.Document parseSvg(Path path) throws Exception {
    final var factory = XmlUtil.getHardenedBuilderFactory();
    factory.setNamespaceAware(true);
    return factory.newDocumentBuilder().parse(path.toFile());
  }

  private static BufferedImage decodeEmbeddedPng(Element image) throws Exception {
    final var dataUri = image.getAttributeNS(XLINK_NAMESPACE, "href");
    assertTrue(dataUri.startsWith(PNG_DATA_URI_PREFIX));
    final var png = Base64.getDecoder().decode(dataUri.substring(PNG_DATA_URI_PREFIX.length()));
    final var decoded = ImageIO.read(new ByteArrayInputStream(png));
    assertNotNull(decoded);
    return decoded;
  }
}
