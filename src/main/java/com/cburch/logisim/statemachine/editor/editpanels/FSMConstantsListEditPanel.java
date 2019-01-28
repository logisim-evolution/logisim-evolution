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
import com.cburch.logisim.statemachine.fSMDSL.ConstantDef;
import com.cburch.logisim.statemachine.fSMDSL.ConstantDefList;
import com.cburch.logisim.statemachine.parser.FSMSerializer;

public class FSMConstantsListEditPanel extends JPanel implements TextListener{

//	JTextField condField ;
	EList<ConstantDef> list;

	JTextArea textArea = new JTextArea(10, 15);
	JScrollPane scrollPane = new JScrollPane(textArea);

	public FSMConstantsListEditPanel(EList<ConstantDef> constants) {
		super();
		this.list=constants;

		if(constants!=null & constants.size()>0) {
			Optional<String> command = constants.stream().map(
					(c)->
					(c.getName()+"="+PrettyPrinter.pp(c.getValue()))
			).reduce((x,y)->(x+";\n"+y));
			textArea.setRows(constants.size()+1);
			textArea.setText(command.get());
		}

		add(new JLabel("Constants"));
//		add(typeText);
//		typeText.addTextListener(this);

	}
	
	private EList<ConstantDef> checkInput(int result) {
		ConstantDefList res= null;
		if (result == JOptionPane.OK_OPTION) {
			String txt = textArea.getText();
			try {
				res= (ConstantDefList) FSMSerializer.parseConstantList(txt);
				EList<ConstantDef> commands = res.getConstants();
				return commands;
			} catch (Exception e) {
				JOptionPane.showMessageDialog(null, "Error : "+e.getMessage(), "Error in command predicate",JOptionPane.ERROR_MESSAGE);
				return null;
			}
		}
		return list;
	}
	public void configure() {
		EList<ConstantDef> commands=null;
		while(commands==null) {
			int dialog = JOptionPane.showConfirmDialog(this, scrollPane, "Configure Constant" ,JOptionPane.OK_CANCEL_OPTION);
			//int dialog = JOptionPane.showConfirmDialog(null, this, "Configure Constant" ,JOptionPane.OK_CANCEL_OPTION);
			if(dialog==JOptionPane.CANCEL_OPTION) return;
			commands = checkInput(dialog);
		}
		list.clear();
		list.addAll(commands);
	}

	@Override
	public void textValueChanged(TextEvent e) {
	}
}