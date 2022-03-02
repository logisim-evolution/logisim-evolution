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

import org.w3c.dom.Text;
import org.xml.sax.SAXException;

import com.cburch.logisim.gui.generic.OptionPane;
import com.cburch.logisim.gui.main.Frame;
import com.cburch.logisim.util.XmlUtil;

public class ProjectBundleManifest {
  
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

  /**
   * This function writes the manifest file to a given zip-file 
   *
   * @param zipfile Zipfile to write to
   * @param info Logisim version with which this manifest was created and main-circuit-file
   * @throws IOException
   */
  public static void writeManifest(ZipOutputStream zipfile, infofileInformation info) throws IOException {
    if (zipfile == null) return; 
    try {
      final var factory = XmlUtil.getHardenedBuilderFactory();
      final var parser = factory.newDocumentBuilder();
      final var boardInfo = parser.newDocument();
      final var manifest = boardInfo.createElement("logisim");
      boardInfo.appendChild(manifest);
      manifest.setAttribute("type", "bundle");
      manifest.setAttribute("version", "1");
      final var meta = boardInfo.createElement("meta");
      manifest.appendChild(meta);
      final var progInfo = boardInfo.createElement("tool");
      meta.appendChild(progInfo);
      final var parts = info.logisimVersion.split(" ");
      progInfo.setAttribute("name", parts[0]);
      if (parts.length > 1) {
        progInfo.setAttribute("version", parts[1]);
      }
      final var project = boardInfo.createElement("project");
      manifest.appendChild(project);
      final var files = boardInfo.createElement("files");
      project.appendChild(files);
      final var mainFile = boardInfo.createElement("file");
      files.appendChild(mainFile);
      mainFile.setAttribute("main", "true");
      final var fileName = boardInfo.createTextNode(info.mainCircuitFile);
      mainFile.appendChild(fileName);
      final var tranFactory = TransformerFactory.newInstance();
      final var aTransformer = tranFactory.newTransformer();
      aTransformer.setOutputProperty(OutputKeys.INDENT, "yes");
      final var src = new DOMSource(boardInfo);
      final var dest = new StreamResult(new StringWriter());
      aTransformer.transform(src, dest);
      zipfile.putNextEntry(new ZipEntry("manifest.xml"));
      zipfile.write(dest.getWriter().toString().getBytes());
    } catch (ParserConfigurationException e) {
      System.err.println(e.getMessage());
    } catch (TransformerConfigurationException e) {
      System.err.println(e.getMessage());
    } catch (TransformerException e) {
      System.err.println(e.getMessage());
    }
  }
  
  /**
   * This function reads the contents of the manifest-file from a given zip-file 
   *
   * @param zipFile zipfile to read from
   * @param frame parrent frame of the caller
   * @return Information contained in the manifest-file
   * @throws IOException
   */
  public static infofileInformation getManifestInfo(ZipFile zipFile, Frame frame) throws IOException {
    try {
      final var factory = XmlUtil.getHardenedBuilderFactory();
      final var parser = factory.newDocumentBuilder();
      final var projInfoEntry = zipFile.getEntry("manifest.xml");
      if (projInfoEntry == null) {
        OptionPane.showMessageDialog(frame, S.fmt("projBundleReadError", S.get("projBundleNoInfo")));
        return null;
      }
      final var projInfoStream = zipFile.getInputStream(projInfoEntry);
      final var docInfo = parser.parse(projInfoStream);
      final var manifestNodes = docInfo.getElementsByTagName("logisim");
      if (manifestNodes.getLength() != 1) {
        OptionPane.showMessageDialog(frame, S.fmt("projBundleReadError", S.get("projBundleMisformatted")));
        return null;
      }
      final var manifestNode = manifestNodes.item(0); 
      final var manifestInfo = manifestNode.getChildNodes();
      // first we find the version of the manifest to check if we can process
      final var nodeAttr = manifestNode.getAttributes();
      if (nodeAttr.getLength() != 2) {
        OptionPane.showMessageDialog(frame, S.fmt("projBundleReadError", S.get("projBundleMisformatted")));
        return null;
      }
      final var attr0 = nodeAttr.item(0);
      final var attr1 = nodeAttr.item(1);
      if (!"type".equals(attr0.getNodeName()) || !"bundle".equals(attr0.getNodeValue()) 
          || !"version".equals(attr1.getNodeName()) || !"1".equals(attr1.getNodeValue())) {
        OptionPane.showMessageDialog(frame, S.fmt("projBundleReadError", S.get("projBundleMisformatted")));
        return null;
      }
      // now we find the info of the main file
      var main = "";
      var creator = "";
      for (var nodeId = 0; nodeId < manifestInfo.getLength(); nodeId++) {
        final var node = manifestInfo.item(nodeId);
        if ("project".equals(node.getNodeName())) {
          final var projectChilds = node.getChildNodes();
          for (var childId = 0; childId < projectChilds.getLength(); childId++) {
            final var childNode = projectChilds.item(childId);
            if ("files".equals(childNode.getNodeName())) {
              final var fileNodes = childNode.getChildNodes();
              for (var fileId = 0; fileId < fileNodes.getLength(); fileId++) {
                final var fileNode = fileNodes.item(fileId);
                if ("file".equals(fileNode.getNodeName())) {
                  final var fileAttrs = fileNode.getAttributes();
                  if (fileAttrs.getLength() == 1 && "main".equals(fileAttrs.item(0).getNodeName()) 
                      && "true".equals(fileAttrs.item(0).getNodeValue())) {
                    final var mainNodes = fileNode.getChildNodes();
                    if ((mainNodes.getLength() == 1) && (mainNodes.item(0) instanceof Text filename)) {
                      main = filename.getNodeValue();
                    }
                  }
                }
              }
            }
          }
        } else if ("meta".equals(node.getNodeName())) {
          final var metaChilds = node.getChildNodes();
          for (var metaId = 0; metaId < metaChilds.getLength(); metaId++) {
            final var metaNode = metaChilds.item(metaId);
            if ("tool".equals(metaNode.getNodeName())) {
              final var metaAttrs = metaNode.getAttributes();
              if (metaAttrs.getLength() == 2) {
                final var metaAttr1 = metaAttrs.item(0);
                final var metaAttr2 = metaAttrs.item(1);
                if ("name".equals(metaAttr1.getNodeName()) && "version".equals(metaAttr2.getNodeName())) {
                  creator = String.format("%s %s", metaAttr1.getNodeValue(), metaAttr2.getNodeValue());
                }
              }
            }
          }
        }
      }      
      if (!main.isEmpty() && !creator.isEmpty()) {
        return new infofileInformation(creator, main);
      }
    } catch (ParserConfigurationException e) {
      System.err.println(e.getMessage());
    } catch (SAXException e) {
      System.err.println(e.getMessage());
    }
    return null;
  }
}
