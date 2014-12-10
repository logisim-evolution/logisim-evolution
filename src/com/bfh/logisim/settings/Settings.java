/*******************************************************************************
 * This file is part of logisim-evolution.
 *
 *   logisim-evolution is free software: you can redistribute it and/or modify
 *   it under the terms of the GNU General Public License as published by
 *   the Free Software Foundation, either version 3 of the License, or
 *   (at your option) any later version.
 *
 *   logisim-evolution is distributed in the hope that it will be useful,
 *   but WITHOUT ANY WARRANTY; without even the implied warranty of
 *   MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *   GNU General Public License for more details.
 *
 *   You should have received a copy of the GNU General Public License
 *   along with logisim-evolution.  If not, see <http://www.gnu.org/licenses/>.
 *
 *   Original code by Carl Burch (http://www.cburch.com), 2011.
 *   Subsequent modifications by :
 *     + Haute École Spécialisée Bernoise
 *       http://www.bfh.ch
 *     + Haute École du paysage, d'ingénierie et d'architecture de Genève
 *       http://hepia.hesge.ch/
 *     + Haute École d'Ingénierie et de Gestion du Canton de Vaud
 *       http://www.heig-vd.ch/
 *   The project is currently maintained by :
 *     + REDS Institute - HEIG-VD
 *       Yverdon-les-Bains, Switzerland
 *       http://reds.heig-vd.ch
 *******************************************************************************/

package com.bfh.logisim.settings;

import java.io.File;
import java.util.Collection;

import javax.swing.JOptionPane;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Result;
import javax.xml.transform.Source;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import org.w3c.dom.Attr;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

public class Settings {
	private static String[] loadAltera() {
		String[] alteraProgs = { "quartus_sh", "quartus_pgm", "quartus_map" };
		String osname = System.getProperty("os.name");
		if (osname == null)
			throw new IllegalArgumentException("no os.name");
		else {
			if (osname.toLowerCase().indexOf("windows") != -1)
				for (int i = 0; i < alteraProgs.length; i++)
					alteraProgs[i] += ".exe";
		}
		return alteraProgs;
	}

	private static String[] loadXilinx() {
		String[] xilinxProgs = { "xst", "ngdbuild", "map", "par", "bitgen",
				"impact", "cpldfit", "hprep6" };
		String osname = System.getProperty("os.name");
		if (osname == null)
			throw new IllegalArgumentException("no os.name");
		else {
			if (osname.toLowerCase().indexOf("windows") != -1)
				for (int i = 0; i < xilinxProgs.length; i++)
					xilinxProgs[i] += ".exe";
		}
		return xilinxProgs;
	}

	private static String WorkSpace = "WorkSpace";
	private static String DirectoryName = "WorkPath";
	private static String XilinxName = "XilinxToolsPath";
	private static String AlteraName = "AlteraToolsPath";
	private static String HdlName = "GenerateHDLOnly";
	private static String HdlTypeName = "HDLTypeToGenerate";
	public static String Unknown = "Unknown";
	public static String VHDL = "VHDL";
	public static String VERILOG = "Verilog";
	private static String Boards = "FPGABoards";
	private static String SelectedBoard = "SelectedBoard";
	private String HomePath;
	private String SettingsFileName = ".LogisimFPGASettings";
	private Document SettingsDocument;
	boolean modified = false;
	private BoardList KnownBoards = new BoardList();

	public static final String[] AlteraPrograms = loadAltera();

	public static final String[] XilinxPrograms = loadXilinx();

	/* big TODO: add language support */
	public Settings() {

		// Detect if OS is Windows and if domain is hepia
		// if yes: The students can't write in homedirectory
		// so we redirect config file to homedrive (Z:\)
		if (System.getProperty("os.name").toLowerCase().contains("windows")
				&& System.getenv("USERDNSDOMAIN") != null
				&& System.getenv("USERDNSDOMAIN").equals("HEPIANET.ETAT-GE.CH")) {
			HomePath = System.getenv("HOMEDRIVE") + "\\";
			// check if we can write in this path
			File testCanWrite = new File(HomePath + "testpermission.tmp");
			try {
				testCanWrite.createNewFile();
				testCanWrite.delete();
			} catch (Exception e) {
				// if we can not: redirect to C:\TEMP
				HomePath = "C:\\TEMP\\";
			}
		} else {
			HomePath = System.getProperty("user.home");
			if (!HomePath.endsWith(File.separator))
				HomePath += File.separator;
		}

		File SettingsFile = new File(HomePath + SettingsFileName + ".xml");
		if (SettingsFile.exists()) {
			try {
				// Create instance of DocumentBuilderFactory
				DocumentBuilderFactory factory = DocumentBuilderFactory
						.newInstance();
				// Get the DocumentBuilder
				DocumentBuilder parser = factory.newDocumentBuilder();
				// Create blank DOM Document
				SettingsDocument = parser.parse(SettingsFile);
			} catch (Exception e) {
				JOptionPane.showMessageDialog(null,
						"Fatal Error: Cannot read FPGA settings file: "
								+ SettingsFile.getPath());
				System.exit(-1);
			}
		}
		if (!SettingsComplete()) {
			if (!WriteXml(SettingsFile)) {
				JOptionPane.showMessageDialog(null,
						"Fatal Error: Cannot write FPGA settings file: "
								+ SettingsFile.getPath());
				System.exit(-1);
			}
		}
	}

	private boolean AlteraToolsFound(String path) {
		for (int i = 0; i < AlteraPrograms.length; i++) {
			File test = new File(CorrectPath(path) + AlteraPrograms[i]);
			if (!test.exists())
				return false;
		}
		return true;
	}

	private String CorrectPath(String path) {
		if (path.endsWith(File.separator))
			return path;
		else
			return path + File.separator;
	}

	public String GetAlteraToolPath() {
		NodeList SettingsList = SettingsDocument
				.getElementsByTagName(WorkSpace);
		if (SettingsList.getLength() != 1) {
			return Unknown;
		}
		Node ThisWorkspace = SettingsList.item(0);
		NamedNodeMap WorkspaceParameters = ThisWorkspace.getAttributes();
		for (int i = 0; i < WorkspaceParameters.getLength(); i++) {
			if (WorkspaceParameters.item(i).getNodeName().equals(AlteraName)) {
				if (AlteraToolsFound(WorkspaceParameters.item(i).getNodeValue()))
					return WorkspaceParameters.item(i).getNodeValue();
				else {
					WorkspaceParameters.item(i).setNodeValue("Unknown");
					modified = true;
					return Unknown;
				}
			}
		}
		/* The attribute does not exists so add it */
		Attr altera = SettingsDocument.createAttribute(AlteraName);
		altera.setNodeValue(Unknown);
		Element workspace = (Element) SettingsList.item(0);
		workspace.setAttributeNode(altera);
		modified = true;
		return Unknown;
	}

	public Collection<String> GetBoardNames() {
		return KnownBoards.GetBoardNames();
	}

	public boolean GetHDLOnly() {
		NodeList SettingsList = SettingsDocument
				.getElementsByTagName(WorkSpace);
		if (SettingsList.getLength() != 1) {
			return true;
		}
		Node ThisWorkspace = SettingsList.item(0);
		NamedNodeMap WorkspaceParameters = ThisWorkspace.getAttributes();
		for (int i = 0; i < WorkspaceParameters.getLength(); i++) {
			if (WorkspaceParameters.item(i).getNodeName().equals(HdlName))
				return WorkspaceParameters.item(i).getNodeValue()
						.equals(Boolean.TRUE.toString());
		}
		/* The attribute does not exists so add it */
		Attr hdl = SettingsDocument.createAttribute(HdlName);
		hdl.setNodeValue(Boolean.TRUE.toString());
		Element workspace = (Element) SettingsList.item(0);
		workspace.setAttributeNode(hdl);
		modified = true;
		return true;
	}

	public String GetHDLType() {
		NodeList SettingsList = SettingsDocument
				.getElementsByTagName(WorkSpace);
		if (SettingsList.getLength() != 1) {
			return VHDL;
		}
		Node ThisWorkspace = SettingsList.item(0);
		NamedNodeMap WorkspaceParameters = ThisWorkspace.getAttributes();
		for (int i = 0; i < WorkspaceParameters.getLength(); i++) {
			if (WorkspaceParameters.item(i).getNodeName().equals(HdlTypeName)) {
				if (!WorkspaceParameters.item(i).getNodeValue().equals(VHDL)
						&& !WorkspaceParameters.item(i).getNodeValue()
								.equals(VERILOG)) {
					WorkspaceParameters.item(i).setNodeValue(VHDL);
					modified = true;
				}
				return WorkspaceParameters.item(i).getNodeValue();
			}
		}
		/* The attribute does not exists so add it */
		Attr hdl = SettingsDocument.createAttribute(HdlTypeName);
		hdl.setNodeValue(VHDL);
		Element workspace = (Element) SettingsList.item(0);
		workspace.setAttributeNode(hdl);
		modified = true;
		return VHDL;
	}

	public String GetSelectedBoard() {
		NodeList SettingsList = SettingsDocument.getElementsByTagName(Boards);
		if (SettingsList.getLength() != 1) {
			return null;
		}
		Node ThisWorkspace = SettingsList.item(0);
		NamedNodeMap WorkspaceParameters = ThisWorkspace.getAttributes();
		for (int i = 0; i < WorkspaceParameters.getLength(); i++) {
			if (WorkspaceParameters.item(i).getNodeName().equals(SelectedBoard)) {
				if (!KnownBoards.BoardInCollection(WorkspaceParameters.item(i)
						.getNodeValue())) {
					WorkspaceParameters.item(i)
							.setNodeValue(
									KnownBoards.GetBoardNames().toArray()[0]
											.toString());
					modified = true;
				}
				return WorkspaceParameters.item(i).getNodeValue();
			}
		}
		/* The attribute does not exists so add it */
		Attr selboard = SettingsDocument.createAttribute(SelectedBoard);
		selboard.setNodeValue(KnownBoards.GetBoardNames().toArray()[0]
				.toString());
		Element workspace = (Element) SettingsList.item(0);
		workspace.setAttributeNode(selboard);
		modified = true;
		return KnownBoards.GetBoardNames().toArray()[0].toString();
	}

	public String GetSelectedBoardFileName() {
		String SelectedBoardName = GetSelectedBoard();
		return KnownBoards.GetBoardFilePath(SelectedBoardName);
	}

	public String GetWorkspacePath() {
		NodeList SettingsList = SettingsDocument
				.getElementsByTagName(WorkSpace);
		if (SettingsList.getLength() != 1) {
			return HomePath;
		}
		Node ThisWorkspace = SettingsList.item(0);
		NamedNodeMap WorkspaceParameters = ThisWorkspace.getAttributes();
		for (int i = 0; i < WorkspaceParameters.getLength(); i++) {
			if (WorkspaceParameters.item(i).getNodeName().equals(DirectoryName))
				return WorkspaceParameters.item(i).getNodeValue();
		}

		return HomePath;
	}

	public String GetXilixToolPath() {
		NodeList SettingsList = SettingsDocument
				.getElementsByTagName(WorkSpace);
		if (SettingsList.getLength() != 1) {
			return Unknown;
		}
		Node ThisWorkspace = SettingsList.item(0);
		NamedNodeMap WorkspaceParameters = ThisWorkspace.getAttributes();
		for (int i = 0; i < WorkspaceParameters.getLength(); i++) {
			if (WorkspaceParameters.item(i).getNodeName().equals(XilinxName)) {
				if (XilinxToolsFound(WorkspaceParameters.item(i).getNodeValue()))
					return WorkspaceParameters.item(i).getNodeValue();
				else {
					WorkspaceParameters.item(i).setNodeValue("Unknown");
					modified = true;
					return Unknown;
				}
			}
		}
		/* The attribute does not exists so add it */
		Attr xilinx = SettingsDocument.createAttribute(XilinxName);
		xilinx.setNodeValue(Unknown);
		Element workspace = (Element) SettingsList.item(0);
		workspace.setAttributeNode(xilinx);
		modified = true;
		return Unknown;
	}

	public boolean SetAlteraToolPath(String path) {
		if (!AlteraToolsFound(path))
			return false;
		NodeList SettingsList = SettingsDocument
				.getElementsByTagName(WorkSpace);
		if (SettingsList.getLength() != 1) {
			return false;
		}
		Node ThisWorkspace = SettingsList.item(0);
		NamedNodeMap WorkspaceParameters = ThisWorkspace.getAttributes();
		for (int i = 0; i < WorkspaceParameters.getLength(); i++) {
			if (WorkspaceParameters.item(i).getNodeName().equals(AlteraName)) {
				WorkspaceParameters.item(i).setNodeValue(path);
				modified = true;
				return true;
			}
		}
		return false;
	}

	public boolean SetHdlOnly(boolean only) {
		NodeList SettingsList = SettingsDocument
				.getElementsByTagName(WorkSpace);
		if (SettingsList.getLength() != 1) {
			return false;
		}
		Node ThisWorkspace = SettingsList.item(0);
		NamedNodeMap WorkspaceParameters = ThisWorkspace.getAttributes();
		for (int i = 0; i < WorkspaceParameters.getLength(); i++) {
			if (WorkspaceParameters.item(i).getNodeName().equals(HdlName)) {
				WorkspaceParameters.item(i)
						.setNodeValue(Boolean.toString(only));
				modified = true;
				return true;
			}
		}
		return false;
	}

	public boolean SetHDLType(String hdl) {
		NodeList SettingsList = SettingsDocument
				.getElementsByTagName(WorkSpace);
		if (SettingsList.getLength() != 1) {
			return false;
		}
		if (!hdl.equals(VHDL) && !hdl.equals(VERILOG))
			return false;
		Node ThisWorkspace = SettingsList.item(0);
		NamedNodeMap WorkspaceParameters = ThisWorkspace.getAttributes();
		for (int i = 0; i < WorkspaceParameters.getLength(); i++) {
			if (WorkspaceParameters.item(i).getNodeName().equals(HdlTypeName)) {
				WorkspaceParameters.item(i).setNodeValue(hdl);
				modified = true;
				return true;
			}
		}
		return false;
	}

	public boolean SetSelectedBoard(String BoardName) {
		NodeList SettingsList = SettingsDocument.getElementsByTagName(Boards);
		if (SettingsList.getLength() != 1) {
			return false;
		}
		if (!KnownBoards.BoardInCollection(BoardName))
			return false;
		if (GetSelectedBoard().equals(BoardName))
			return true;
		Node ThisWorkspace = SettingsList.item(0);
		NamedNodeMap WorkspaceParameters = ThisWorkspace.getAttributes();
		for (int i = 0; i < WorkspaceParameters.getLength(); i++) {
			if (WorkspaceParameters.item(i).getNodeName().equals(SelectedBoard)) {
				WorkspaceParameters.item(i).setNodeValue(BoardName);
				modified = true;
				return true;
			}
		}
		return false;
	}

	private boolean SettingsComplete() {
		boolean result = true;
		if (SettingsDocument == null) {
			result = false;
			try {
				// Create instance of DocumentBuilderFactory
				DocumentBuilderFactory factory = DocumentBuilderFactory
						.newInstance();
				// Get the DocumentBuilder
				DocumentBuilder parser;
				parser = factory.newDocumentBuilder();
				// Create blank DOM Document
				SettingsDocument = parser.newDocument();
			} catch (ParserConfigurationException e) {
				JOptionPane.showMessageDialog(null,
						"Fatal Error: Cannot create settings Document!");
				System.exit(-4);
			}
			Element root = SettingsDocument.createElement(SettingsFileName
					.replace('.', '_'));
			SettingsDocument.appendChild(root);
		}

		NodeList RootList = SettingsDocument.getChildNodes();

		if (RootList.getLength() != 1) {
			JOptionPane.showMessageDialog(null,
					"Fatal Error: Settings file corrupted; please delete the file:"
							+ HomePath + SettingsFileName + ".xml");
			System.exit(-5);
		}

		NodeList SettingsList = SettingsDocument
				.getElementsByTagName(WorkSpace);
		if (SettingsList.getLength() > 1) {
			JOptionPane.showMessageDialog(null,
					"Fatal Error: Settings file corrupted; please delete the file:"
							+ HomePath + SettingsFileName + ".xml");
			System.exit(-5);
		}
		if (SettingsList.getLength() == 0) {
			Element workspace = SettingsDocument.createElement(WorkSpace);
			workspace.setAttribute(DirectoryName, HomePath
					+ "logisim_workspace" + File.separator);
			RootList.item(0).appendChild(workspace);
			SettingsList = SettingsDocument.getElementsByTagName(WorkSpace);
			result = false;
		}
		GetXilixToolPath();
		GetAlteraToolPath();
		GetHDLOnly();
		GetHDLType();

		SettingsList = SettingsDocument.getElementsByTagName(Boards);
		if (SettingsList.getLength() > 1) {
			JOptionPane.showMessageDialog(null,
					"Fatal Error: Settings file corrupted; please delete the file:"
							+ HomePath + SettingsFileName + ".xml");
			System.exit(-5);
		}
		if (SettingsList.getLength() == 0) {
			Element workspace = SettingsDocument.createElement(Boards);
			workspace.setAttribute(SelectedBoard, KnownBoards.GetBoardNames()
					.toArray()[0].toString());
			RootList.item(0).appendChild(workspace);
			SettingsList = SettingsDocument.getElementsByTagName(Boards);
			result = false;
		}
		GetSelectedBoard();

		result &= !modified;
		return result;
	}

	public boolean SetWorkspacePath(String path) {
		NodeList SettingsList = SettingsDocument
				.getElementsByTagName(WorkSpace);
		if (SettingsList.getLength() != 1) {
			return false;
		}
		Node ThisWorkspace = SettingsList.item(0);
		NamedNodeMap WorkspaceParameters = ThisWorkspace.getAttributes();
		for (int i = 0; i < WorkspaceParameters.getLength(); i++) {
			if (WorkspaceParameters.item(i).getNodeName().equals(DirectoryName)) {
				WorkspaceParameters.item(i).setNodeValue(path);
				modified = true;
				return true;
			}
		}
		return false;
	}

	public boolean SetXilinxToolPath(String path) {
		if (!XilinxToolsFound(path))
			return false;
		NodeList SettingsList = SettingsDocument
				.getElementsByTagName(WorkSpace);
		if (SettingsList.getLength() != 1) {
			return false;
		}
		Node ThisWorkspace = SettingsList.item(0);
		NamedNodeMap WorkspaceParameters = ThisWorkspace.getAttributes();
		for (int i = 0; i < WorkspaceParameters.getLength(); i++) {
			if (WorkspaceParameters.item(i).getNodeName().equals(XilinxName)) {
				WorkspaceParameters.item(i).setNodeValue(path);
				modified = true;
				return true;
			}
		}
		return false;
	}

	public boolean UpdateSettingsFile() {
		if (!modified)
			return true;
		File target = new File(HomePath + SettingsFileName + ".xml");
		return WriteXml(target);
	}

	private boolean WriteXml(File target) {
		try {
			TransformerFactory tranFactory = TransformerFactory.newInstance();
			tranFactory.setAttribute("indent-number", 3);
			Transformer aTransformer = tranFactory.newTransformer();
			aTransformer.setOutputProperty(OutputKeys.INDENT, "yes");
			Source src = new DOMSource(SettingsDocument);
			Result dest = new StreamResult(target);
			aTransformer.transform(src, dest);
			modified = false;
			return true;
		} catch (Exception e) {
			JOptionPane.showMessageDialog(null, e.getMessage());
			return false;
		}
	}

	private boolean XilinxToolsFound(String path) {
		for (int i = 0; i < XilinxPrograms.length; i++) {
			File test = new File(CorrectPath(path) + XilinxPrograms[i]);
			if (!test.exists())
				return false;
		}
		return true;
	}
}
