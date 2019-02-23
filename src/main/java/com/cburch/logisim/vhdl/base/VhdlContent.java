package com.cburch.logisim.vhdl.base;

import static com.cburch.logisim.std.Strings.S;

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

import com.cburch.logisim.file.LogisimFile;
import com.cburch.logisim.fpga.designrulecheck.CorrectLabel;
import com.cburch.logisim.instance.Port;
import com.cburch.logisim.proj.Project;
import com.cburch.logisim.util.Softwares;

public class VhdlContent extends HdlContent {

	public static VhdlContent create(String name, LogisimFile file) {
        VhdlContent content = new VhdlContent(file);
        if (!content.parseContent(TEMPLATE.replaceAll("%entityname%", name))) {
            return null;
        }
        return content;
	}

	public static VhdlContent parse(String vhdl, LogisimFile file) {
        VhdlContent content = new VhdlContent(file);
        if (!content.setContent(vhdl)) {
            return null;
        }
        return content;
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

	private static final String RESOURCE = "/resources/logisim/hdl/vhdl_component.templ";

	private static final String TEMPLATE = loadTemplate();

	protected StringBuffer content;
	protected Port[] inputs;
	protected Port[] outputs;
	protected String name;
	protected String libraries;
	protected String architecture;
	private LogisimFile logiFile;

	protected VhdlContent(LogisimFile file) {
        logiFile = file;
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
					S.get("validationParseError"),
					JOptionPane.ERROR_MESSAGE);
			return false;
		}

		if (!parser.getName().equals(name)) {
            if (labelVHDLInvalidNotify(parser.getName(), logiFile))
                return false;
        } else {
            if (labelVHDLInvalidNotify(parser.getName(), null))
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
			inputs[i].setToolTip(S.getter(desc.getName()));
		}

		for (int i = 0; i < outputsDesc.size(); i++) {
			VhdlParser.PortDescription desc = outputsDesc.get(i);
			outputs[i] = new Port(VhdlEntity.WIDTH, (i * VhdlEntity.PORT_GAP)
					+ VhdlEntity.HEIGHT, desc.getType(), desc.getWidth());
			outputs[i].setToolTip(S.getter(desc.getName()));
		}

		this.content = new StringBuffer(content);
		fireContentSet();

		return true;
	}
	
	public void openEditor(Project proj) {
        VhdlEntityAttributes.getContentEditor(proj.getFrame(), this, proj).setVisible(true);
    }

    static final String ENTITY_PATTERN = "(\\s*\\bentity\\s+)%entityname%(\\s+is)\\b";
    static final String ARCH_PATTERN = "(\\s*\\barchitecture\\s+\\w+\\s+of\\s+)%entityname%\\b";
    static final String END_PATTERN = "(\\s*\\bend\\s+)%entityname%(\\s*;)";    

/**
 * Check if a given label could be a valid VHDL variable name
 * 
 * @param label
 *            candidate VHDL variable name
 * @return true if the label is NOT a valid name, false otherwise
 */
    public static boolean labelVHDLInvalid(String label) {
    	if (!label.matches("^[A-Za-z][A-Za-z0-9_]*") || label.endsWith("_")
			|| label.matches(".*__.*"))
    		return (true);
        if (CorrectLabel.VHDLKeywords.contains(label.toLowerCase()))
                return true;
        return (false);
    }

    public static boolean labelVHDLInvalidNotify(String label, LogisimFile file) {
        String err = null;
        if (!label.matches("^[A-Za-z][A-Za-z0-9_]*") || label.endsWith("_") || label.matches(".*__.*")) {
            err = S.get("vhdlInvalidNameError");
        } else if (CorrectLabel.VHDLKeywords.contains(label.toLowerCase())) {
            err = S.get("vhdlKeywordNameError");
        } else if (file != null && file.containsFactory(label)) {
            err = S.get("vhdlDuplicateNameError");
        } else {
            return false;
        }
        JOptionPane.showMessageDialog(null, label + ": "+err, S.get("validationParseError"),
                        JOptionPane.ERROR_MESSAGE);
        return true;
    }

    public boolean setName(String name) {
        if (name == null)
            return false;
        if (labelVHDLInvalidNotify(name, logiFile))
            return false;
        String entPat = ENTITY_PATTERN.replaceAll("%entityname%", this.name);
        String archPat = ARCH_PATTERN.replaceAll("%entityname%", this.name);
        String endPat = END_PATTERN.replaceAll("%entityname%", this.name);
        String s = content.toString();
        s = s.replaceAll("(?is)" + entPat, "$1"+name+"$2"); // entity NAME is
        s = s.replaceAll("(?is)" + archPat, "$1"+name); // architecture foo of NAME
        s = s.replaceAll("(?is)" + endPat, "$1"+name+"$2"); // end NAME ;
        return setContent(s);
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
					new String[] { S.get("validationErrorButton") },
					S.get("validationErrorButton"));
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
