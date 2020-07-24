package com.cburch.logisim.statemachine.editor.editpanels;

import java.awt.event.TextEvent;
import java.awt.event.TextListener;
import java.util.Optional;

import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;

import org.eclipse.emf.common.util.EList;

import com.cburch.logisim.statemachine.PrettyPrinter;
import com.cburch.logisim.statemachine.fSMDSL.Command;
import com.cburch.logisim.statemachine.fSMDSL.CommandList;
import com.cburch.logisim.statemachine.fSMDSL.CommandStmt;
import com.cburch.logisim.statemachine.fSMDSL.FSM;
import com.cburch.logisim.statemachine.parser.FSMSerializer;

public class FSMCommandListEditPanel extends JPanel implements TextListener{

//	JTextField condField ;
	CommandList list;

	JTextArea textArea = new JTextArea(10, 15);
	JScrollPane scrollPane = new JScrollPane(textArea);

	public FSMCommandListEditPanel(CommandList state) {
		super();
		this.list=state;

		if(state!=null & state.getCommands().size()>0) {
			Optional<String> command = state.getCommands().stream().map(
					(c)->
					(c.getName().getName()+"="+PrettyPrinter.pp(c.getValue()))
			).reduce((x,y)->(x+";\n"+y));
			textArea.setRows(state.getCommands().size()+1);
			textArea.setText(command.get());
		}

		add(new JLabel("Commands"));
//		add(typeText);
//		typeText.addTextListener(this);

	}
	
	private EList<Command> checkInput(int result) {
		CommandStmt res= null;
		if (result == JOptionPane.OK_OPTION) {
			String txt = textArea.getText();
			try {
				FSM fsm = (FSM)list.eContainer().eContainer();
				res= (CommandStmt) FSMSerializer.parseCommandList(fsm, txt);
				EList<Command> commands = res.getCommands();
				for(Command c:commands) {
					UpdateCrossReferences fixer = new UpdateCrossReferences(fsm);
					fixer.replaceRef(c);
				}
				return commands;
			} catch (Exception e) {
				JOptionPane.showMessageDialog(null, "Error : "+e.getMessage(), "Error in command predicate",JOptionPane.ERROR_MESSAGE);
				return null;
			}
		}
		return list.getCommands();
	}
	public void configure() {
		EList<Command> commands=null;
		// display them in a message dialog

		while(commands==null) {
			int dialog = JOptionPane.showConfirmDialog(this, scrollPane, "Configure Command" ,JOptionPane.OK_CANCEL_OPTION);
			//int dialog = JOptionPane.showConfirmDialog(null, this, "Configure Command" ,JOptionPane.OK_CANCEL_OPTION);
			if(dialog==JOptionPane.CANCEL_OPTION) return;
			commands = checkInput(dialog);
		}
		list.getCommands().clear();
		list.getCommands().addAll(commands);
	}

	@Override
	public void textValueChanged(TextEvent e) {
	}
}