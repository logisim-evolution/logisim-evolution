package com.cburch.logisim.statemachine.editor.editpanels;

import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JTextField;

import com.cburch.logisim.statemachine.fSMDSL.Port;
import com.cburch.logisim.statemachine.validation.FSMValidation;



public class FSMPortEditPanel extends JPanel{

	JTextField nameField ;
	JTextField widthField ;
	Port state;
	
	public FSMPortEditPanel(Port state) {
		super();
		this.state=state;

		nameField = new JTextField(10);
		widthField = new JTextField(10);
		nameField.setText(state.getName());
		widthField.setText(""+state.getWidth());

		add(new JLabel("Label"));
		add(nameField);
		add(new JLabel("Width"));
		add(widthField);

		widthField.addActionListener
		(	
			new java.awt.event.ActionListener() {
				public void actionPerformed(java.awt.event.ActionEvent e) {
				}
			}
		);

	}
	
	private boolean checkInput(int result) {
		if (result == JOptionPane.OK_OPTION) {
			String txt = widthField.getText();
			for (char c : txt.toCharArray()) {
				if (c < '0' || c >= '9') {
					JOptionPane.showMessageDialog(null, "Error: Please enter a (positive) integer value", "Error Message",
							JOptionPane.ERROR_MESSAGE);
					return true;
					
				}
			}
			if (!FSMValidation.isValidIdentifier(nameField.getText())) {
				JOptionPane.showMessageDialog(null, "Error: Please enter a valid identifer string (instead of "+nameField.getText()+")", "Error Message",
						JOptionPane.ERROR_MESSAGE);
				return true;
			}
			if (FSMValidation.isReservedKeyword(nameField.getText())) {
				JOptionPane.showMessageDialog(null, "Error: "+nameField.getText()+" is a reserved keyword)", "Error Message",
						JOptionPane.ERROR_MESSAGE);
				return true;
			}

		}
		return false;
	}
	public void configure() {
		boolean error=true;
		while(error) {
			int dialog = JOptionPane.showConfirmDialog(null, this, "Please Enter X and Y Values",JOptionPane.OK_CANCEL_OPTION);
			error = checkInput(dialog);
		}
		state.setName(nameField.getText());
		state.setWidth(Integer.parseInt(widthField.getText()));
	}
}