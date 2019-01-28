package com.cburch.logisim.statemachine.editor.editpanels;

import java.util.Optional;

import javax.swing.BoxLayout;
import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;

import org.eclipse.emf.common.util.EList;

import com.cburch.logisim.statemachine.PrettyPrinter;
import com.cburch.logisim.statemachine.editor.view.FSMCustomFactory;
import com.cburch.logisim.statemachine.fSMDSL.Command;
import com.cburch.logisim.statemachine.fSMDSL.ConstantDef;
import com.cburch.logisim.statemachine.fSMDSL.ConstantDefList;
import com.cburch.logisim.statemachine.fSMDSL.FSM;
import com.cburch.logisim.statemachine.fSMDSL.State;
import com.cburch.logisim.statemachine.fSMDSL.Transition;
import com.cburch.logisim.statemachine.parser.FSMSerializer;
import com.cburch.logisim.statemachine.validation.FSMValidation;



public class FSMEditPanel extends JPanel{

	JTextField nameField ;
	JTextField codeField ;
	JCheckBox  initialFSM ;
	FSM fsm;
	private JTextField widthField;
	private JTextField heightField;

	JTextArea textArea = new JTextArea(10, 15);
	JScrollPane scrollPane = new JScrollPane(textArea);

	
	public FSMEditPanel(FSM fsm) {
		super();
		this.fsm=fsm;
		setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
		setSize(500,800);
		nameField = new JTextField(10);
		codeField = new JTextField(10);
		widthField = new JTextField(10);
		heightField = new JTextField(10);
		nameField.setText(fsm.getName());
		codeField.setText(""+fsm.getWidth());
		widthField.setText(""+fsm.getLayout().getWidth());
		heightField.setText(""+fsm.getLayout().getHeight());
		add(new JLabel("Name"));
		add(nameField);
		add(new JLabel("Code width "));
		add(codeField);
		add(new JLabel("Diagram width "));
		add(widthField);
		add(new JLabel("Diagram height "));
		add(heightField);
		add(new JLabel("Constants"));
		add(scrollPane);
		if(fsm.getConstants()!=null & fsm.getConstants().size()>0) {
			Optional<String> command = fsm.getConstants().stream().map(
					(c)->
					(PrettyPrinter.pp(c))
			).reduce((x,y)->(x+";\n"+y));
			textArea.setRows(fsm.getConstants().size()+10);
			textArea.setText(command.get());
		}

		codeField.addActionListener
		(	
			new java.awt.event.ActionListener() {
				public void actionPerformed(java.awt.event.ActionEvent e) {
				}
			}
		);

	}
	
	private EList<ConstantDef> checkInput(int result) {
		if (result == JOptionPane.OK_OPTION) {
			String txt = textArea.getText();
			try {
				ConstantDefList res= null;
				res= (ConstantDefList) FSMSerializer.parseConstantList(txt);
				return res.getConstants();
			} catch (Exception e) {
				JOptionPane.showMessageDialog(null, "Error : "+e.getMessage(), "Error in command predicate",JOptionPane.ERROR_MESSAGE);
				return null;
			}
		}
		return null;
	}

	public void configure() {
		boolean error = true;

		while(error) {
			try {
				int dialog = JOptionPane.showConfirmDialog(this, this, "State configuration",JOptionPane.OK_CANCEL_OPTION);
				int parseInt = Integer.parseInt(codeField.getText());
				EList<ConstantDef> checkInput = checkInput(dialog);
				if (checkInput!=null) {
					fsm.getConstants().clear();
					fsm.getConstants().addAll(checkInput);
					UpdateCrossReferences fixer = new UpdateCrossReferences(fsm);
					for(State s:fsm.getStates()) {
						for(Command c:s.getCommandList().getCommands()) {
							fixer.replaceRef(c);
						}
						for(Transition t:s.getTransition()) {
							fixer.replaceRef(t);
						}
						
					}
				}
				fsm.setName(nameField.getText());
				fsm.setWidth(parseInt);
				parseInt = Integer.parseInt(widthField.getText());
				fsm.getLayout().setWidth(Math.max(FSMCustomFactory.FSM_WIDTH, parseInt));
				parseInt = Integer.parseInt(heightField.getText());
				fsm.getLayout().setHeight(Math.max(FSMCustomFactory.FSM_HEIGHT, parseInt));
				error=false;
				
				if (!FSMValidation.isValidIdentifier(nameField.getText())) {
					JOptionPane.showMessageDialog(null, "Error: Please enter a valid identifer string (instead of "+nameField.getText()+")", "Error Message",
							JOptionPane.ERROR_MESSAGE);
					error = true;
				}
				if (FSMValidation.isReservedKeyword(nameField.getText())) {
					JOptionPane.showMessageDialog(null, "Error: "+nameField.getText()+" is a reserved keyword)", "Error Message",
							JOptionPane.ERROR_MESSAGE);
					error = true;
				}
			} catch (NumberFormatException e) {
				error=true;
			}
		}		
	}


}