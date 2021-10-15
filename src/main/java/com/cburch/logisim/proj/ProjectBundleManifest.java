/*
 * Logisim-evolution - digital logic design tool and simulator
 * Copyright by the Logisim-evolution developers
 *
 * https://github.com/logisim-evolution/
 *
 * This is free software released under GNU GPLv3 license
 */

package com.cburch.logisim.proj;

import static com.cburch.logisim.proj.Strings.S;

import java.io.IOException;
import java.io.StringWriter;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import java.util.zip.ZipOutputStream;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import org.xml.sax.SAXException;

import com.cburch.logisim.gui.generic.OptionPane;
import com.cburch.logisim.gui.main.Frame;
import com.cburch.logisim.util.XmlUtil;

public class ProjectBundleManifest {
  
  private static final String VERSION_STRING = "Version"; 
  private static final String VERSION = "0.9";
  private static final String PROJECTINFO = "ProjectInformation";
  public static final String VERSION_PREAMBLE = "Created_with";
  public static final String MAIN_FILE_PREAMBLE = "Main_file";

  public static class infofileInformation {
    private final String logisimVersion;
    private final String mainCircuitFile;

    public infofileInformation(String logisimVersion, String mainCircuitFile) {
      this.logisimVersion = logisimVersion;
      this.mainCircuitFile = mainCircuitFile;
    }

    public String getBundleLogisimVersion() {
      return logisimVersion;
    }

    public String getMainLogisimFilename() {
      return mainCircuitFile;
    }
  }

  public static infofileInformation getInfoContainer(String logisimVersion, String mainCircuitFile) {
    return new infofileInformation(logisimVersion, mainCircuitFile);
  }

  public static void writeManifest(ZipOutputStream zipfile, infofileInformation info) throws IOException {
    if (zipfile == null) return; 
    try {
      // Create instance of DocumentBuilderFactory
      final var factory = XmlUtil.getHardenedBuilderFactory();
      // Get the DocumentBuilder
      final var parser = factory.newDocumentBuilder();
      // Create blank DOM Document
      final var boardInfo = parser.newDocument();
      final var manifest = boardInfo.createElement("element");
      boardInfo.appendChild(manifest);
      manifest.setAttribute("name", "Manifest");
      manifest.setAttribute("type", "ds:ManifestType");
      final var version = boardInfo.createElement(VERSION_STRING);
      version.setAttribute("name", VERSION);
      manifest.appendChild(version);
      final var progInfo = boardInfo.createElement(PROJECTINFO);
      manifest.appendChild(progInfo);
      progInfo.setAttribute(VERSION_PREAMBLE, info.logisimVersion.replace(" ", "_"));
      progInfo.setAttribute(MAIN_FILE_PREAMBLE, info.mainCircuitFile);
      final var tranFactory = TransformerFactory.newInstance();
      final var aTransformer = tranFactory.newTransformer();
      aTransformer.setOutputProperty(OutputKeys.INDENT, "yes");
      final var src = new DOMSource(boardInfo);
      final var dest = new StreamResult(new StringWriter());
      aTransformer.transform(src, dest);
      zipfile.putNextEntry(new ZipEntry("manifest.xml"));
      zipfile.write(dest.getWriter().toString().getBytes());
    } catch (ParserConfigurationException e) {
      //FIXME: handle exception
      System.err.println(e.getMessage());
    } catch (TransformerConfigurationException e) {
      //FIXME: handle exception
      System.err.println(e.getMessage());
    } catch (TransformerException e) {
      //FIXME: handle exception
      System.err.println(e.getMessage());
    }
  }
  
  public static infofileInformation getManifestInfo(ZipFile zipFile, Frame frame) throws IOException {
    try {
      // Create instance of DocumentBuilderFactory
      final var factory = XmlUtil.getHardenedBuilderFactory();
      // Get the DocumentBuilder
      final var parser = factory.newDocumentBuilder();
      // Create blank DOM Document
      final var projInfoEntry = zipFile.getEntry("manifest.xml");
      if (projInfoEntry == null) {
        OptionPane.showMessageDialog(frame, S.fmt("projBundleReadError", S.get("projBundleNoInfo")));
        return null;
      }
      final var projInfoStream = zipFile.getInputStream(projInfoEntry);
      final var docInfo = parser.parse(projInfoStream);
      final var manifestNodes = docInfo.getElementsByTagName("element");
      if (manifestNodes.getLength() != 1) {
        OptionPane.showMessageDialog(frame, S.fmt("projBundleReadError", S.get("projBundleMisformatted")));
        return null;
      }
      final var manifestNode = manifestNodes.item(0); 
      final var manifestInfo = manifestNode.getChildNodes();
      // first we find the version
      var versionFound = false;
      for (var nodeId = 0; nodeId < manifestInfo.getLength(); nodeId++) {
        final var node = manifestInfo.item(nodeId);
        if (VERSION_STRING.equals(node.getNodeName())) {
          versionFound = true;
          final var nodeAttr = node.getAttributes();
          if (nodeAttr.getLength() != 1) {
            OptionPane.showMessageDialog(frame, S.fmt("projBundleReadError", S.get("projBundleMisformatted")));
            return null;
          }
          final var attr = nodeAttr.item(0);
          if (!"name".equals(attr.getNodeName()) || !VERSION.equals(attr.getNodeValue())) {
            OptionPane.showMessageDialog(frame, S.fmt("projBundleReadError", S.get("projBundleMisformatted")));
            return null;
          }
        }
      }
      if (!versionFound) {
        OptionPane.showMessageDialog(frame, S.fmt("projBundleReadError", S.get("projBundleMisformatted")));
        return null;
      }
      // now we find the info
      var main = "";
      var creator = "";
      for (var nodeId = 0; nodeId < manifestInfo.getLength(); nodeId++) {
        final var node = manifestInfo.item(nodeId);
        if (PROJECTINFO.equals(node.getNodeName())) {
          final var attrs = node.getAttributes();
          for (var attrId = 0; attrId < attrs.getLength(); attrId++) {
            final var attr = attrs.item(attrId);
            if (VERSION_PREAMBLE.equals(attr.getNodeName())) creator = attr.getNodeValue();
            else if (MAIN_FILE_PREAMBLE.equals(attr.getNodeName())) main = attr.getNodeValue(); 
          }
        }
        if (!main.isEmpty() && !creator.isEmpty()) {
          return new infofileInformation(creator, main);
        }
      }      
    } catch (ParserConfigurationException e) {
      //FIXME: handle exception
      System.err.println(e.getMessage());
    } catch (SAXException e) {
      //FIXME: handle exception
      System.err.println(e.getMessage());
    }
    return null;
  }
}
