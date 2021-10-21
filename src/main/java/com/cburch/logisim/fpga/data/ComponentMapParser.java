/*
 * Logisim-evolution - digital logic design tool and simulator
 * Copyright by the Logisim-evolution developers
 *
 * https://github.com/logisim-evolution/
 *
 * This is free software released under GNU GPLv3 license
 */

package com.cburch.logisim.fpga.data;

import static com.cburch.logisim.fpga.Strings.S;

import com.cburch.logisim.util.XmlUtil;
import java.io.File;
import java.io.IOException;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.ParserConfigurationException;
import org.w3c.dom.Document;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

public class ComponentMapParser {

  private File fileToPase = null;
  private MappableResourcesContainer mappableComponents = null;
  private BoardInformation boardInfo = null;
  private final String[] MapSectionStrings = {"Key", "LocationX", "LocationY", "Width", "Height"};
  private static final int WrongCircuit = -1;
  private static final int WrongBoard = -2;
  private static final int ErrorCreatingDocument = -3;
  private static final int ErrorParsingFile = -4;

  public ComponentMapParser(File file, MappableResourcesContainer mapResContainer, BoardInformation brdInfo) {
    fileToPase = file;
    mappableComponents = mapResContainer;
    boardInfo = brdInfo;
  }

  private void unMapAll() {
    mappableComponents.unMapAll();
    mappableComponents.updateMapableComponents();
  }

  public String getError(int error) {
    return switch (error) {
      case WrongCircuit -> S.get("BoardMapWrongCircuit");
      case WrongBoard -> S.get("BoardMapWrongBoard");
      case ErrorCreatingDocument -> S.get("BoardMapErrorCD");
      case ErrorParsingFile -> S.get("BoardMapErrorPF");
      default -> S.get("BoardMapUnknown");
    };
  }

  public int parseFile() {
    NodeList nodeList = null;
    String absoluteFileName = fileToPase.getPath();

    // Create instance of DocumentBuilderFactory
    final var factory = XmlUtil.getHardenedBuilderFactory();
    // Get the DocumentBuilder
    DocumentBuilder parser = null;

    try {
      parser = factory.newDocumentBuilder();
    } catch (ParserConfigurationException e) {
      return ErrorCreatingDocument;
    }

    // Create blank DOM Document
    final var xml = new File(absoluteFileName);
    Document mapDoc = null;
    try {
      mapDoc = parser.parse(xml);
    } catch (SAXException | IOException e) {
      return ErrorParsingFile;
    }

    nodeList = mapDoc.getElementsByTagName("LogisimGoesFPGABoardMapInformation");
    final var circuitInfo = nodeList.item(0);
    final var circuitInfoDetails = circuitInfo.getChildNodes();

    for (var i = 0; i < circuitInfoDetails.getLength(); i++) {
      if (circuitInfoDetails.item(i).getNodeName().equals("GlobalMapInformation")) {
        final var attrs = circuitInfoDetails.item(i).getAttributes();
        for (var j = 0; j < attrs.getLength(); j++) {
          if (attrs.item(j).getNodeName().equals("BoardName")) {
            if (!boardInfo.getBoardName().equals(attrs.item(j).getNodeValue())) {
              return WrongBoard;
            }
          } else if (attrs.item(j).getNodeName().equals("ToplevelCircuitName")) {
            if (!mappableComponents.getToplevelName().equals(attrs.item(j).getNodeValue())) {
              return WrongCircuit;
            }
          }
        }
        break;
      }
    }

    /* cleanup the current map */
    unMapAll();
    for (var i = 0; i < circuitInfoDetails.getLength(); i++) {
      if (circuitInfoDetails.item(i).getNodeName().startsWith("MAPPEDCOMPONENT")) {
        var x = -1;
        var y = -1;
        var width = -1;
        var height = -1;
        var key = "";
        final var attrs = circuitInfoDetails.item(i).getAttributes();
        for (var j = 0; j < attrs.getLength(); j++) {
          if (attrs.item(j).getNodeName().equals(MapSectionStrings[0])) {
            key = attrs.item(j).getNodeValue();
          }
          if (attrs.item(j).getNodeName().equals(MapSectionStrings[1])) {
            x = Integer.parseInt(attrs.item(j).getNodeValue());
          }
          if (attrs.item(j).getNodeName().equals(MapSectionStrings[2])) {
            y = Integer.parseInt(attrs.item(j).getNodeValue());
          }
          if (attrs.item(j).getNodeName().equals(MapSectionStrings[3])) {
            width = Integer.parseInt(attrs.item(j).getNodeValue());
          }
          if (attrs.item(j).getNodeName().equals(MapSectionStrings[4])) {
            height = Integer.parseInt(attrs.item(j).getNodeValue());
          }
        }
        if (!key.isEmpty() && (x > 0) && (y > 0) && (width > 0) && (height > 0)) {
          BoardRectangle rect = null;
          for (final var comp : boardInfo.getAllComponents()) {
            if ((comp.getRectangle().getXpos() == x)
                && (comp.getRectangle().getYpos() == y)
                && (comp.getRectangle().getWidth() == width)
                && (comp.getRectangle().getHeight() == height)) {
              rect = comp.getRectangle();
              break;
            }
          }
          if (rect != null) {
            mappableComponents.tryMap(key, rect);
          }
        }
      }
    }
    return 0;
  }
}
