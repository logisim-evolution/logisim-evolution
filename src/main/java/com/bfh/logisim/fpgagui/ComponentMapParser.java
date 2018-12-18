package com.bfh.logisim.fpgagui;

import java.io.File;
import java.io.IOException;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.w3c.dom.Document;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import com.bfh.logisim.fpgaboardeditor.BoardInformation;
import com.bfh.logisim.fpgaboardeditor.BoardRectangle;
import com.bfh.logisim.fpgaboardeditor.FPGAIOInformationContainer;

public class ComponentMapParser {

	private File fileToPase = null;
	private MappableResourcesContainer MappableComponents  = null;
	private BoardInformation BoardInfo = null;
	private String[] MapSectionStrings = { "Key", "LocationX", "LocationY",
			"Width", "Height" };

	public ComponentMapParser(File file, MappableResourcesContainer mapResContainer,
			BoardInformation brdInfo) {

		fileToPase = file;
		MappableComponents = mapResContainer;
		BoardInfo = brdInfo;

	}

	private void UnMapAll() {
		MappableComponents.UnmapAll();
		MappableComponents.rebuildMappedLists();
	}

	@SuppressWarnings("finally")
	public int parseFile() {
		NodeList Elements  = null;
		String AbsoluteFileName = fileToPase.getPath();

		// Create instance of DocumentBuilderFactory
		DocumentBuilderFactory factory = DocumentBuilderFactory
				.newInstance();
		// Get the DocumentBuilder
		DocumentBuilder parser = null;

		try {
			parser = factory.newDocumentBuilder();
		} catch (ParserConfigurationException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		// Create blank DOM Document
		File xml = new File(AbsoluteFileName);
		Document MapDoc = null;
		try {
			MapDoc = parser.parse(xml);
		} catch (SAXException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		Elements = MapDoc
				.getElementsByTagName("LogisimGoesFPGABoardMapInformation");
		Node CircuitInfo = Elements.item(0);
		NodeList CircuitInfoDetails = CircuitInfo.getChildNodes();

		for (int i = 0; i < CircuitInfoDetails.getLength(); i++) {
			if (CircuitInfoDetails.item(i).getNodeName()
					.equals("GlobalMapInformation")) {
				NamedNodeMap Attrs = CircuitInfoDetails.item(i)
						.getAttributes();
				for (int j = 0; j < Attrs.getLength(); j++) {
					if (Attrs.item(j).getNodeName().equals("BoardName")) {
						if (!BoardInfo.getBoardName().equals(
								Attrs.item(j).getNodeValue())) {
							/* TODO ERROR MESSAGE */
							return -1;
						}
					} else if (Attrs.item(j).getNodeName()
							.equals("ToplevelCircuitName")) {
						if (!MappableComponents.GetToplevelName()
								.equals(Attrs.item(j).getNodeValue())) {
							/* TODO ERROR MESSAGE */
							return -1;
						}
					}
				}
				break;
			}
		}

		/* cleanup the current map */
		UnMapAll();
		for (int i = 0; i < CircuitInfoDetails.getLength(); i++) {
			if (CircuitInfoDetails.item(i).getNodeName()
					.startsWith("MAPPEDCOMPONENT")) {
				int x = -1, y = -1, width = -1, height = -1;
				String key = "";
				NamedNodeMap Attrs = CircuitInfoDetails.item(i)
						.getAttributes();
				for (int j = 0; j < Attrs.getLength(); j++) {
					if (Attrs.item(j).getNodeName()
							.equals(MapSectionStrings[0])) {
						key = Attrs.item(j).getNodeValue();
					}
					if (Attrs.item(j).getNodeName()
							.equals(MapSectionStrings[1])) {
						x = Integer.parseInt(Attrs.item(j)
								.getNodeValue());
					}
					if (Attrs.item(j).getNodeName()
							.equals(MapSectionStrings[2])) {
						y = Integer.parseInt(Attrs.item(j)
								.getNodeValue());
					}
					if (Attrs.item(j).getNodeName()
							.equals(MapSectionStrings[3])) {
						width = Integer.parseInt(Attrs.item(j)
								.getNodeValue());
					}
					if (Attrs.item(j).getNodeName()
							.equals(MapSectionStrings[4])) {
						height = Integer.parseInt(Attrs.item(j)
								.getNodeValue());
					}
				}
				if (!key.isEmpty() && (x > 0) && (y > 0) && (width > 0)
						&& (height > 0)) {
					BoardRectangle rect = null;
					for (FPGAIOInformationContainer comp : BoardInfo
							.GetAllComponents()) {
						if ((comp.GetRectangle().getXpos() == x)
								&& (comp.GetRectangle().getYpos() == y)
								&& (comp.GetRectangle().getWidth() == width)
								&& (comp.GetRectangle().getHeight() == height)) {
							rect = comp.GetRectangle();
							break;
						}
					}
					if (rect != null) {
						MappableComponents.TryMap(key, rect,
								BoardInfo.GetComponentType(rect));
					}
				}
			}
		}
		return 0;
	}
}
