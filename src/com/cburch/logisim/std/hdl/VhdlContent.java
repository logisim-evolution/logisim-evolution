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

package com.cburch.logisim.std.hdl;

import java.awt.Dimension;
import java.awt.Insets;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.swing.JOptionPane;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;

import com.cburch.hdl.HdlModel;
import com.cburch.logisim.instance.Port;
import com.cburch.logisim.util.Softwares;

public class VhdlContent extends HdlContent {

	public static VhdlContent create() {
		return new VhdlContent();
	}

	private static String loadTemplate() {
		InputStream input = VhdlContent.class.getResourceAsStream(RESOURCE);
		BufferedReader in = new BufferedReader(new InputStreamReader(input));

		StringBuilder tmp = new StringBuilder();
		String line;

		try {
			while ((line = in.readLine()) != null) {
				tmp.append(line);
				tmp.append(System.getProperty("line.separator"));
			}
		} catch (IOException ex) {
			return "";
		} finally {
			try {
				if (input != null)
					input.close();
			} catch (IOException ex) {
				Logger.getLogger(VhdlContent.class.getName()).log(Level.SEVERE,
						null, ex);
			}
		}

		return tmp.toString();
	}

	private static final String RESOURCE = "/resources/logisim/hdl/vhdl.templ";

	private static final String TEMPLATE = loadTemplate();

	protected StringBuffer content;
	protected Port[] inputs;
	protected Port[] outputs;
	protected String name;
	protected String libraries;
	protected String architecture;

	protected VhdlContent() {
		this.parseContent(TEMPLATE);
	}

	public VhdlContent clone() {
		try {
			VhdlContent ret = (VhdlContent) super.clone();
			ret.content = new StringBuffer(this.content);
			return ret;
		} catch (CloneNotSupportedException ex) {
			return this;
		}
	}

	@Override
	public boolean compare(HdlModel model) {
		return compare(model.getContent());
	}

	@Override
	public boolean compare(String value) {
		return content.toString().replaceAll("\\r\\n|\\r|\\n", " ")
				.equals(value.replaceAll("\\r\\n|\\r|\\n", " "));
	}

	public String getArchitecture() {
		if (architecture == null)
			return "";

		return architecture;
	}

	@Override
	public String getContent() {
		return content.toString();
	}

	public Port[] getInputs() {
		if (inputs == null)
			return new Port[0];

		return inputs;
	}

	public int getInputsNumber() {
		if (inputs == null)
			return 0;

		return inputs.length;
	}

	public String getLibraries() {
		if (libraries == null)
			return "";

		return libraries;
	}

	@Override
	public String getName() {
		if (name == null)
			return "";

		return name;
	}

	public Port[] getOutputs() {
		if (outputs == null)
			return new Port[0];

		return outputs;
	}

	public int getOutputsNumber() {
		if (outputs == null)
			return 0;

		return outputs.length;
	}

	public Port[] getPorts() {
		if (inputs == null || outputs == null)
			return new Port[0];

		return concat(inputs, outputs);
	}

	public int getPortsNumber() {
		if (inputs == null || outputs == null)
			return 0;

		return inputs.length + outputs.length;
	}

	public boolean parseContent(String content) {
		VhdlParser parser = new VhdlParser(content.toString());
		try {
			parser.parse();
		} catch (Exception ex) {
			JOptionPane.showMessageDialog(null, ex.getMessage(),
					Strings.get("validationParseError"),
					JOptionPane.ERROR_MESSAGE);
			return false;
		}

		name = parser.getName();
		libraries = parser.getLibraries();
		architecture = parser.getArchitecture();

		List<VhdlParser.PortDescription> inputsDesc = parser.getInputs();
		List<VhdlParser.PortDescription> outputsDesc = parser.getOutputs();
		inputs = new Port[inputsDesc.size()];
		outputs = new Port[outputsDesc.size()];

		for (int i = 0; i < inputsDesc.size(); i++) {
			VhdlParser.PortDescription desc = inputsDesc.get(i);
			inputs[i] = new Port(0, (i * VhdlEntity.PORT_GAP)
					+ VhdlEntity.HEIGHT, desc.getType(), desc.getWidth());
			inputs[i].setToolTip(Strings.getter(desc.getName()));
		}

		for (int i = 0; i < outputsDesc.size(); i++) {
			VhdlParser.PortDescription desc = outputsDesc.get(i);
			outputs[i] = new Port(VhdlEntity.WIDTH, (i * VhdlEntity.PORT_GAP)
					+ VhdlEntity.HEIGHT, desc.getType(), desc.getWidth());
			outputs[i].setToolTip(Strings.getter(desc.getName()));
		}

		this.content = new StringBuffer(content);
		fireContentSet();

		return true;
	}

	@Override
	public boolean setContent(String content) {
		StringBuffer title = new StringBuffer();
		StringBuffer result = new StringBuffer();

		switch (Softwares.validateVhdl(content, title, result)) {
		case Softwares.ERROR:
			JTextArea message = new JTextArea();
			message.setText(result.toString());
			message.setEditable(false);
			message.setLineWrap(false);
			message.setMargin(new Insets(5, 5, 5, 5));

			JScrollPane sp = new JScrollPane(message);
			sp.setPreferredSize(new Dimension(700, 400));

			JOptionPane.showOptionDialog(null, sp, title.toString(),
					JOptionPane.OK_OPTION, JOptionPane.ERROR_MESSAGE, null,
					new String[] { Strings.get("validationErrorButton") },
					Strings.get("validationErrorButton"));
			return false;
		case Softwares.ABORD:
			JOptionPane.showMessageDialog(null, result.toString(),
					title.toString(), JOptionPane.INFORMATION_MESSAGE);
			return false;
		case Softwares.SUCCESS:
			return parseContent(content);
		}

		return false;
	}

}
