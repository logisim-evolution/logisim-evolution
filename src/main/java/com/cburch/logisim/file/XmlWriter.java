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

package com.cburch.logisim.file;

import java.io.File;
import java.io.OutputStream;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Result;
import javax.xml.transform.Source;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import org.w3c.dom.Document;
import org.w3c.dom.Element;

import com.cburch.draw.model.AbstractCanvasObject;
import com.cburch.logisim.LogisimVersion;
import com.cburch.logisim.Main;
import com.cburch.logisim.circuit.Circuit;
import com.cburch.logisim.circuit.Wire;
import com.cburch.logisim.comp.Component;
import com.cburch.logisim.comp.ComponentFactory;
import com.cburch.logisim.data.Attribute;
import com.cburch.logisim.data.AttributeDefaultProvider;
import com.cburch.logisim.data.AttributeSet;
import com.cburch.logisim.tools.Library;
import com.cburch.logisim.tools.Tool;
import com.cburch.logisim.util.InputEventUtil;
import com.cburch.logisim.util.StringUtil;

class XmlWriter {
	static void write(LogisimFile file, OutputStream out, LibraryLoader loader,
			File destFile) throws ParserConfigurationException,
			TransformerConfigurationException, TransformerException {

		DocumentBuilderFactory docFactory = DocumentBuilderFactory
				.newInstance();
		DocumentBuilder docBuilder = docFactory.newDocumentBuilder();

		Document doc = docBuilder.newDocument();
		XmlWriter context;
		if (destFile != null) {
			String dstFilePath = destFile.getAbsolutePath();
			dstFilePath = dstFilePath.substring(0,
					dstFilePath.lastIndexOf(File.separator));
			context = new XmlWriter(file, doc, loader, dstFilePath);
		} else
			context = new XmlWriter(file, doc, loader);

		context.fromLogisimFile();

		TransformerFactory tfFactory = TransformerFactory.newInstance();
		try {
			tfFactory.setAttribute("indent-number", Integer.valueOf(2));
		} catch (IllegalArgumentException e) {
		}
		Transformer tf = tfFactory.newTransformer();
		tf.setOutputProperty(OutputKeys.ENCODING, "UTF-8");
		tf.setOutputProperty(OutputKeys.INDENT, "yes");
		try {
			tf.setOutputProperty("{http://xml.apache.org/xslt}indent-amount",
					"2");
		} catch (IllegalArgumentException e) {
		}

		Source src = new DOMSource(doc);
		Result dest = new StreamResult(out);
		tf.transform(src, dest);
	}

	private LogisimFile file;
	private Document doc;
	/**
	 * Path of the file which is being written on disk -- used to relativize
	 * components stored in it
	 */
	private String outFilepath;
	private LibraryLoader loader;
	private HashMap<Library, String> libs = new HashMap<Library, String>();

	private XmlWriter(LogisimFile file, Document doc, LibraryLoader loader) {
		this(file, doc, loader, null);
	}

	private XmlWriter(LogisimFile file, Document doc, LibraryLoader loader,
			String outFilepath) {
		this.file = file;
		this.doc = doc;
		this.loader = loader;
		this.outFilepath = outFilepath;
	}

	void addAttributeSetContent(Element elt, AttributeSet attrs,
			AttributeDefaultProvider source) {
		if (attrs == null)
			return;
		LogisimVersion ver = Main.VERSION;
		if (source != null && source.isAllDefaultValues(attrs, ver))
			return;
		for (Attribute<?> attrBase : attrs.getAttributes()) {
			@SuppressWarnings("unchecked")
			Attribute<Object> attr = (Attribute<Object>) attrBase;
			Object val = attrs.getValue(attr);
			if (attrs.isToSave(attr) && val != null) {
				Object dflt = source == null ? null : source
						.getDefaultAttributeValue(attr, ver);
				if (dflt == null || !dflt.equals(val)) {
					Element a = doc.createElement("a");
					a.setAttribute("name", attr.getName());
					String value = attr.toStandardString(val);
					if (attr.getName().equals("filePath")
							&& outFilepath != null) {
						Path outFP = Paths.get(outFilepath);
						Path attrValP = Paths.get(value);
						value = (outFP.relativize(attrValP)).toString();
						a.setAttribute("val", value);
					} else {
						if (value.indexOf("\n") >= 0) {
							a.appendChild(doc.createTextNode(value));
						} else {
							a.setAttribute("val", attr.toStandardString(val));
						}
					}
					elt.appendChild(a);
				}
			}
		}
	}

	Library findLibrary(ComponentFactory source) {
		if (file.contains(source)) {
			return file;
		}
		for (Library lib : file.getLibraries()) {
			if (lib.contains(source))
				return lib;
		}
		return null;
	}

	Library findLibrary(Tool tool) {
		if (libraryContains(file, tool)) {
			return file;
		}
		for (Library lib : file.getLibraries()) {
			if (libraryContains(lib, tool))
				return lib;
		}
		return null;
	}

	Element fromCircuit(Circuit circuit) {
		Element ret = doc.createElement("circuit");
		ret.setAttribute("name", circuit.getName());
		addAttributeSetContent(ret, circuit.getStaticAttributes(), null);
		if (!circuit.getAppearance().isDefaultAppearance()) {
			Element appear = doc.createElement("appear");
			for (Object o : circuit.getAppearance().getObjectsFromBottom()) {
				if (o instanceof AbstractCanvasObject) {
					Element elt = ((AbstractCanvasObject) o).toSvgElement(doc);
					if (elt != null) {
						appear.appendChild(elt);
					}
				}
			}
			ret.appendChild(appear);
		}
		for (Wire w : circuit.getWires()) {
			ret.appendChild(fromWire(w));
		}
		for (Component comp : circuit.getNonWires()) {
			Element elt = fromComponent(comp);
			if (elt != null)
				ret.appendChild(elt);
		}
		return ret;
	}

	Element fromComponent(Component comp) {
		ComponentFactory source = comp.getFactory();
		Library lib = findLibrary(source);
		String lib_name;
		if (lib == null) {
			loader.showError(source.getName() + " component not found");
			return null;
		} else if (lib == file) {
			lib_name = null;
		} else {
			lib_name = libs.get(lib);
			if (lib_name == null) {
				loader.showError("unknown library within file");
				return null;
			}
		}

		Element ret = doc.createElement("comp");
		if (lib_name != null)
			ret.setAttribute("lib", lib_name);
		ret.setAttribute("name", source.getName());
		ret.setAttribute("loc", comp.getLocation().toString());
		addAttributeSetContent(ret, comp.getAttributeSet(), comp.getFactory());
		return ret;
	}

	Element fromLibrary(Library lib) {
		Element ret = doc.createElement("lib");
		if (libs.containsKey(lib))
			return null;
		String name = "" + libs.size();
		String desc = loader.getDescriptor(lib);
		if (desc == null) {
			loader.showError("library location unknown: " + lib.getName());
			return null;
		}
		libs.put(lib, name);
		ret.setAttribute("name", name);
		ret.setAttribute("desc", desc);
		for (Tool t : lib.getTools()) {
			AttributeSet attrs = t.getAttributeSet();
			if (attrs != null) {
				Element toAdd = doc.createElement("tool");
				toAdd.setAttribute("name", t.getName());
				addAttributeSetContent(toAdd, attrs, t);
				if (toAdd.getChildNodes().getLength() > 0) {
					ret.appendChild(toAdd);
				}
			}
		}
		return ret;
	}

	Element fromLogisimFile() {
		Element ret = doc.createElement("project");
		doc.appendChild(ret);
		ret.appendChild(doc
				.createTextNode("\nThis file is intended to be "
						+ "loaded by Logisim-evolution (https://github.com/reds-heig/logisim-evolution).\n"));
		ret.setAttribute("version", "1.0");
		ret.setAttribute("source", Main.VERSION_NAME);

		for (Library lib : file.getLibraries()) {
			Element elt = fromLibrary(lib);
			if (elt != null)
				ret.appendChild(elt);
		}

		if (file.getMainCircuit() != null) {
			Element mainElt = doc.createElement("main");
			mainElt.setAttribute("name", file.getMainCircuit().getName());
			ret.appendChild(mainElt);
		}

		ret.appendChild(fromOptions());
		ret.appendChild(fromMouseMappings());
		ret.appendChild(fromToolbarData());

		for (Circuit circ : file.getCircuits()) {
			ret.appendChild(fromCircuit(circ));
		}
		return ret;
	}

	Element fromMouseMappings() {
		Element elt = doc.createElement("mappings");
		MouseMappings map = file.getOptions().getMouseMappings();
		for (Map.Entry<Integer, Tool> entry : map.getMappings().entrySet()) {
			Integer mods = entry.getKey();
			Tool tool = entry.getValue();
			Element toolElt = fromTool(tool);
			String mapValue = InputEventUtil.toString(mods.intValue());
			toolElt.setAttribute("map", mapValue);
			elt.appendChild(toolElt);
		}
		return elt;
	}

	Element fromOptions() {
		Element elt = doc.createElement("options");
		addAttributeSetContent(elt, file.getOptions().getAttributeSet(), null);
		return elt;
	}

	Element fromTool(Tool tool) {
		Library lib = findLibrary(tool);
		String lib_name;
		if (lib == null) {
			loader.showError(StringUtil.format("tool `%s' not found",
					tool.getDisplayName()));
			return null;
		} else if (lib == file) {
			lib_name = null;
		} else {
			lib_name = libs.get(lib);
			if (lib_name == null) {
				loader.showError("unknown library within file");
				return null;
			}
		}

		Element elt = doc.createElement("tool");
		if (lib_name != null)
			elt.setAttribute("lib", lib_name);
		elt.setAttribute("name", tool.getName());
		addAttributeSetContent(elt, tool.getAttributeSet(), tool);
		return elt;
	}

	Element fromToolbarData() {
		Element elt = doc.createElement("toolbar");
		ToolbarData toolbar = file.getOptions().getToolbarData();
		for (Tool tool : toolbar.getContents()) {
			if (tool == null) {
				elt.appendChild(doc.createElement("sep"));
			} else {
				elt.appendChild(fromTool(tool));
			}
		}
		return elt;
	}

	Element fromWire(Wire w) {
		Element ret = doc.createElement("wire");
		ret.setAttribute("from", w.getEnd0().toString());
		ret.setAttribute("to", w.getEnd1().toString());
		return ret;
	}

	boolean libraryContains(Library lib, Tool query) {
		for (Tool tool : lib.getTools()) {
			if (tool.sharesSource(query))
				return true;
		}
		return false;
	}
}
